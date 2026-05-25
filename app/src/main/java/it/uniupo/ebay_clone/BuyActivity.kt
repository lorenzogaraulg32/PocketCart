package it.uniupo.ebay_clone

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import it.uniupo.ebay_clone.databinding.ActivityBuyBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

class BuyActivity : AppCompatActivity(), MapsFragment.MapsResult {

    private lateinit var auth: FirebaseAuth
    private val dataBase = FirebaseFirestore.getInstance()
    private lateinit var currentUser: User

    lateinit var itemId: String
    private lateinit var itemPagina: Item

    private var mapsFragment = MapsFragment()

    private lateinit var binding: ActivityBuyBinding


    override fun getMapsResults(titolo: String, latitude: Double, longitude: Double) {
        binding.buyItemPosUser.text = titolo
        binding.buyChangePositionBtn.isEnabled = true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mapsFragment.isAdded) {
                    supportFragmentManager.beginTransaction().remove(mapsFragment).commit()
                    binding.buyChangePositionBtn.isEnabled = true
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        itemPagina = Item()

        auth = FirebaseAuth.getInstance()

        itemId = intent.getStringExtra("item").toString()
        println("item id = $itemId ")

        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, dataBase)
                itemPagina = Item().getCurrentItem(dataBase, itemId)
            }
            if (currentUser.Status != 0) {
                Toast.makeText(
                    this@BuyActivity,
                    R.string.toast_authorization_missing,
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            settaElementiPagina()
            retriveImages()
        }

        binding.buyChangePositionBtn.setOnClickListener {
            binding.buyChangePositionBtn.isEnabled = false
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_container, mapsFragment).commit()
        }

        binding.buyItemBtn.setOnClickListener {
            val saldoFinale = currentUser.Budget - itemPagina.Prezzo
            binding.buyItemBtn.isEnabled = false
            if (binding.buyItemPosUser.text.isNullOrEmpty() || binding.buyItemPosUser.text.equals(
                    getString(R.string.where_to_ship)
                )
            ) {
                Toast.makeText(
                    this@BuyActivity,
                    getString(R.string.toast_buy_err1),
                    Toast.LENGTH_LONG
                ).show()
                binding.buyItemBtn.isEnabled = true
            } else {
                MainScope().launch {
                    withContext(Dispatchers.Main) {
                        val doc = dataBase.collection("User")
                            .whereEqualTo("Username", itemPagina.Proprietario).get().await()
                        val venditore = doc.documents.get(0).toObject<User>()
                        currentUser = User().getCurrentUser(auth, dataBase)
                        if (currentUser.Status == 1 || currentUser.Status == 2) {
                            Toast.makeText(
                                this@BuyActivity,
                                getString(R.string.toast_authorization_missing),
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        } else if (currentUser.Username != itemPagina.Proprietario) {
                            if (saldoFinale < 0) {
                                Toast.makeText(
                                    this@BuyActivity,
                                    getString(R.string.toast_buy_err_poor),
                                    Toast.LENGTH_LONG
                                ).show()
                                binding.buyItemBtn.isEnabled = true
                            } else {
                                if (venditore != null) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        currentUser.Budget = saldoFinale
                                        currentUser.updateUserBudget(dataBase, auth, saldoFinale)
                                        itemPagina.Venduto = true
                                        itemPagina.DataVendita = Date().time
                                        itemPagina.Acquirente = currentUser.Username
                                        itemPagina.updateItemVenduto(dataBase, itemId)
                                        venditore.Budget += itemPagina.Prezzo
                                        val userInfo = HashMap<String, Any>()
                                        userInfo["Budget"] = venditore.Budget
                                        dataBase.collection("User")
                                            .document(doc.documents.get(0).id)
                                            .update(userInfo)
                                        checkPref()
                                        Toast.makeText(
                                            this@BuyActivity,
                                            getString(R.string.toast_buy_good_ending),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@BuyActivity,
                                        getString(R.string.toast_err_buy2),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@BuyActivity,
                                getString(R.string.toast_err_buy3),
                                Toast.LENGTH_LONG
                            ).show()
                            binding.buyItemBtn.isEnabled = true
                        }
                    }
                }
            }
        }

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private suspend fun checkPref() {
        val query = dataBase.collection("User").get().await()
        for (docs in query) {
            val u = docs.toObject<User>()
            val iterator = u.Preferiti.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                if (p.key.equals(itemId)) {
                    iterator.remove()
                    u.updateUserPrefMod(dataBase, docs.id)
                    println("Modificato il preferito di ${docs.id} = ${u.Username}")
                }
            }
        }
    }

    private fun settaElementiPagina() {
        runOnUiThread {
            binding.buyItemName.text = itemPagina.Nome
            binding.buyItemDescription.text = itemPagina.Descrizione
            binding.buyItemPrezzo.text = "PREZZO: ${Item().ScaleDouble(itemPagina.Prezzo)}€"
            binding.buyItemPos.text = itemPagina.getItemPosition(this)

            if (itemPagina.Spedizione == true) {
                if (currentUser.Posizione.isEmpty()) {
                    binding.buyItemPosUser.text = getString(R.string.where_to_ship)
                } else {
                    binding.buyItemPosUser.text = currentUser.getUserPosition(this)
                }
            } else {
                binding.buyItemPosUser.text = getString(R.string.ritiro_in_zona)
                binding.buyUserPosTitle.text = getString(R.string.ritiro_in_zona2)
                binding.buyChangePositionBtn.visibility = View.GONE
            }

            binding.buySaldoUtente.text =
                getString(R.string.your_credit) + Item().ScaleDouble(currentUser.Budget)
                    .toString() + getString(R.string.euro)
            val saldoFinale = Item().ScaleDouble(currentUser.Budget - itemPagina.Prezzo).toDouble()
            if (saldoFinale < 0) {
                binding.buySaldoFinale.text =
                    getString(R.string.poor_err)
                binding.buySaldoFinale.setTextColor(ContextCompat.getColor(this, R.color.red_err))
            } else {
                binding.buySaldoFinale.text =
                    Item().ScaleDouble(currentUser.Budget)
                        .toString() + getString(R.string.euro) + "-" + Item().ScaleDouble(itemPagina.Prezzo)
                        .toString() + getString(R.string.euro) + "=" + Item().ScaleDouble(
                        saldoFinale
                    ).toString() + getString(R.string.euro)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private suspend fun retriveImages() {
        var count = 0
        withContext(Dispatchers.IO) {
            for (foto in itemPagina.Foto) {
                var bitmap: Bitmap?
                val storageFile =
                    FirebaseStorage.getInstance().getReference("images/$foto")
                val localFile = File.createTempFile(foto + "tmp", "jpg")
                storageFile.getFile(localFile).addOnSuccessListener {
                    bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                    settaImmagine(count, bitmap)
                    count++
                }.addOnFailureListener {
                    Toast.makeText(
                        this@BuyActivity,
                        getString(R.string.toast_img_err),
                        Toast.LENGTH_SHORT
                    ).show()
                    bitmap = BitmapFactory.decodeResource(
                        this@BuyActivity.resources,
                        R.drawable.image_placeholder
                    )
                }
            }
        }
    }

    private fun settaImmagine(count: Int, bitmap: Bitmap?) {
        if (count == 0) {
            binding.imagev.setImageBitmap(bitmap)
            binding.imagev.scaleType = ImageView.ScaleType.CENTER_CROP
        } else if (count == 1) {
            binding.imagex.setImageBitmap(bitmap)
            binding.imagex.scaleType = ImageView.ScaleType.CENTER_CROP
        } else if (count == 2) {
            binding.imagey.setImageBitmap(bitmap)
            binding.imagey.scaleType = ImageView.ScaleType.CENTER_CROP
        } else if (count == 3) {
            binding.imagew.setImageBitmap(bitmap)
            binding.imagew.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }
}