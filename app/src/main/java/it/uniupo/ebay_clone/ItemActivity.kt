package it.uniupo.ebay_clone

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import it.uniupo.ebay_clone.databinding.ActivityItemNewBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File


class ItemActivity : AppCompatActivity(), PhotoView.PhotoResult {

    override fun closeFrag() {
        binding.layoutItem.setRenderEffect(null)
        binding.photoHolder.isClickable = true
        binding.layoutItem.isClickable = true
    }

    val dataBase = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User

    private var refreshFragments = false
    private lateinit var binding: ActivityItemNewBinding

    private var infoFragment = Item_info_fragment()
    private var contactFragment = Item_contact_fragment()
    private var photoFragment = PhotoView()

    private var images: ArrayList<Bitmap> = arrayListOf()
    private var currentPhoto = 0

    lateinit var itemPagina: Item
    lateinit var itemId: String
    lateinit var userId: String

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityItemNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //variabili oggetto
        itemPagina = Item()
        itemId = intent.getStringExtra("id").toString()

        binding.photoHolder.isClickable = true
        binding.layoutItem.isClickable = true

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        userId = user!!.uid

        MainScope().launch {
            withContext(Dispatchers.IO) {
                refreshItem()
                println("SIZE OF IMAGES main execution= ${images.size}")
                binding.photoHolder.isClickable = true
                binding.layoutItem.isClickable = true
            }
        }

        //bottoni topbar
        binding.delBtn.setOnClickListener {
            checkStatusUtente(2)
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.prefModBtn.setOnClickListener {
            if (currentUser.Username.equals(itemPagina.Proprietario)) {
                var intent = Intent(this, ModFormActivity::class.java)
                intent.putExtra("itemId", itemId)
                startActivity(intent)
            } else if (currentUser.Admin) {
                var intent = Intent(this, ModFormActivity::class.java)
                intent.putExtra("itemId", itemId)
                startActivity(intent)
            } else {
                checkStatusUtente(1)
            }
        }

        binding.rewBtn.setOnClickListener {
            if (itemPagina.Venduto == true) {
                CoroutineScope(Dispatchers.Main).launch {
                    leaveRewiew()
                }
            } else {
                AlertDialog.Builder(this@ItemActivity)
                    .setTitle(getString(R.string.rew_allert))
                    .setNeutralButton("OK") { dialog: DialogInterface, _: Int ->
                        dialog.dismiss()
                    }
            }
        }

        //bottoni fragment
        binding.infoBtn.setOnClickListener {
            binding.dividerSlide2.visibility = View.GONE
            slideToLeft(Item_info_fragment.newInstance(itemPagina, itemId))
            binding.dividerSlide1.visibility = View.VISIBLE
            binding.contactBtn.isClickable = true
            binding.infoBtn.isClickable = false
        }

        binding.contactBtn.setOnClickListener {
            binding.dividerSlide1.visibility = View.GONE
            slideToRigth(Item_contact_fragment.newInstance(itemPagina))
            binding.dividerSlide2.visibility = View.VISIBLE
            binding.contactBtn.isClickable = false
            binding.infoBtn.isClickable = true
        }

        //bottoni foto
        binding.forwardPhotoBtn.setOnClickListener {
            currentPhoto++
            if (currentPhoto < images.size) {
                binding.image.setImageBitmap(images[currentPhoto])
            } else {
                currentPhoto = images.size - 1
            }
            println(currentPhoto)
        }
        binding.backPhotoBtn.setOnClickListener {
            currentPhoto--
            if (currentPhoto >= 0) {
                binding.image.setImageBitmap(images[currentPhoto])
            } else {
                currentPhoto = 0
            }
            println(currentPhoto)
        }

        binding.image.setOnClickListener {
            binding.layoutItem.setRenderEffect(
                RenderEffect.createBlurEffect(
                    20.0f, 20.0f, Shader.TileMode.MIRROR
                )

            )
            photoFragment = PhotoView.newInstance(images[currentPhoto])
            supportFragmentManager.beginTransaction()
                .replace(R.id.photo_holder, photoFragment)
                .addToBackStack(null)
                .commit()
            binding.photoHolder.isClickable = false
            binding.layoutItem.isClickable = false
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    private fun slideToRigth(newFrag: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.holder_item, newFrag)
            .commitNow()
    }

    private fun slideToLeft(newFrag: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            .replace(R.id.holder_item, newFrag)
            .commitNow()
    }


    private fun settaElementiPagina() {
        MainScope().launch {
            withContext(Dispatchers.IO) {
                retriveImages()
            }
            runOnUiThread {
                //bottoni
                if (itemPagina.Proprietario.lowercase() != currentUser.Username.lowercase()) {
                    if (currentUser.Preferiti.contains(itemId)) {
                        binding.prefModBtn.setImageResource(R.drawable.baseline_star_24)
                    } else {
                        binding.prefModBtn.setImageResource(R.drawable.baseline_star_outline_24)
                    }
                } else if (!itemPagina.Venduto) {
                    binding.prefModBtn.setImageResource(R.drawable.baseline_mode_24)
                    binding.delBtn.visibility = View.VISIBLE
                }

                //testi e UI
                binding.itemName.text = itemPagina.Nome.uppercase()
                binding.itemPrice.text = Item().ScaleDouble(itemPagina.Prezzo) + "€"
                binding.dividerSlide1.visibility = View.VISIBLE
                binding.dividerSlide2.visibility = View.GONE

                //fragment
                val bundle = Bundle()
                bundle.putString("itemId", itemId)
                println("ItemActiivity ID = $itemId ")
                infoFragment = Item_info_fragment.newInstance(itemPagina, itemId)
                contactFragment = Item_contact_fragment.newInstance(itemPagina)
                supportFragmentManager.beginTransaction()
                    .add(R.id.holder_item, contactFragment)
                    .add(R.id.holder_item, infoFragment)
                    .commitNow()

                supportFragmentManager.beginTransaction()
                    .detach(contactFragment)
                    .detach(infoFragment)
                    .commitNow()

                supportFragmentManager.beginTransaction()
                    .attach(infoFragment)
                    .commitNow()

                binding.image.setImageBitmap(images[0])


                //restore visibility and remove loading
                binding.loading.stopNestedScroll()
                binding.loading.visibility = View.INVISIBLE
                binding.backBtn.visibility = View.VISIBLE
                binding.prefModBtn.visibility = View.VISIBLE
                binding.rewBtn.visibility = View.VISIBLE
                binding.layoutItem.visibility = View.VISIBLE
                binding.infoBtn.isClickable = false
            }
        }
    }


    private suspend fun leaveRewiew() {
        for (rev in currentUser.RecensioniLasciate) {
            if (rev == itemId) {
                Toast.makeText(this, getString(R.string.rew_toast_already), Toast.LENGTH_LONG)
                    .show()
                return
            }
        }
        val dialogView = View.inflate(this, R.layout.review, null)
        val recensione = dialogView.findViewById<EditText>(R.id.rewiew_num)
        val commento = dialogView.findViewById<EditText>(R.id.rewiew_comment)
        AlertDialog.Builder(this@ItemActivity)
            .setTitle(getString(R.string.rew_title_allert))
            .setMessage(getString(R.string.rew_msg_allert))
            .setPositiveButton(getString(R.string.conf_allert)) { dialog: DialogInterface, _: Int ->
                if (recensione.text.toString().toInt() > 5 || recensione.text.isNullOrEmpty()) {
                    Toast.makeText(
                        this@ItemActivity,
                        getString(R.string.reww_err_alleert), Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                }
                val rew = Recensioni(
                    recensione.text.toString().toInt(),
                    commento.text.toString().orEmpty(),
                    currentUser.Username,
                    itemId
                )

                if (itemPagina.Acquirente == currentUser.Username) {
                    dataBase.collection("User")
                        .whereEqualTo("Username", itemPagina.Proprietario)
                        .get().addOnSuccessListener {
                            val proprietario = it.documents[0].toObject(User::class.java)!!
                            proprietario.Valutazioni.add(rew)
                            proprietario.Valutazione = calcolaMedia(proprietario.Valutazioni)
                            currentUser.RecensioniLasciate.add(itemId)
                            proprietario.updateRewiews(dataBase)
                            currentUser.updateRewiewsList(dataBase, auth)
                            Toast.makeText(
                                this,
                                getString(R.string.rew_feedback1),
                                Toast.LENGTH_LONG
                            ).show()
                        }

                } else if (itemPagina.Proprietario == currentUser.Username) {
                    dataBase.collection("User")
                        .whereEqualTo("Username", itemPagina.Acquirente)
                        .get().addOnSuccessListener {
                            val acquirente = it.documents[0].toObject(User::class.java)!!
                            acquirente.Valutazioni.add(rew)
                            acquirente.Valutazione = calcolaMedia(acquirente.Valutazioni)
                            currentUser.RecensioniLasciate.add(itemId)
                            acquirente.updateRewiews(dataBase)
                            currentUser.updateRewiewsList(dataBase, auth)
                            Toast.makeText(
                                this,
                                getString(R.string.rew_feedback2),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .setNegativeButton(getString(R.string.annulla_allert)) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .setView(dialogView)
            .show()
    }

    private fun calcolaMedia(valutazioni: ArrayList<Recensioni>): Double {
        var voti = ArrayList<Int>()
        for (entries in valutazioni) {
            voti.add(entries.value!!)
        }

        var tot = 0.0
        for (v in voti) {
            tot += v
        }
        return (tot / valutazioni.size)
    }

    private suspend fun retriveImages() {
        println("Item foto : ${itemPagina.Foto.size}")

        val imageDeferreds = itemPagina.Foto.map { foto ->
            val storageReference = FirebaseStorage.getInstance().getReference("images/$foto")
            val localFile = File.createTempFile(foto + "tmp", "jpg")

            CoroutineScope(Dispatchers.IO).async {
                try {
                    storageReference.getFile(localFile).await()
                    BitmapFactory.decodeFile(localFile.absolutePath)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ItemActivity,
                            getString(R.string.img_err, e.message),
                            Toast.LENGTH_SHORT
                        ).show()
                        null
                    }
                }
            }
        }

        images = imageDeferreds.mapNotNull { it.await() } as ArrayList<Bitmap>
        println("DIM IMGS ${images.size}")
    }

    private fun preferito() {
        if (currentUser.Username != itemPagina.Proprietario && currentUser.Username != itemPagina.Acquirente) {
            if (currentUser.Preferiti.contains(itemId)) {
                currentUser.Preferiti.remove(itemId)
                var map = HashMap<String, Any>()
                map.put("Preferiti", currentUser.Preferiti)
                dataBase.collection("User").document(userId).update(map).addOnSuccessListener {
                    Toast.makeText(
                        this@ItemActivity,
                        getString(R.string.toast_toggle_pref_1), Toast.LENGTH_SHORT
                    )
                        .show()
                    binding.prefModBtn.setImageResource(R.drawable.baseline_star_outline_24)
                }
            } else {
                currentUser.Preferiti.put(itemId, false)
                var map = HashMap<String, Any>()
                map.put("Preferiti", currentUser.Preferiti)
                dataBase.collection("User").document(userId).update(map).addOnSuccessListener {
                    Toast.makeText(
                        this@ItemActivity,
                        getString(R.string.toast_toggle_pref_2), Toast.LENGTH_SHORT
                    )
                        .show()
                    binding.prefModBtn.setImageResource(R.drawable.baseline_star_24)
                }
            }
        }
    }

    private fun rimuoviOggetto() {
        AlertDialog.Builder(this).setTitle(getString(R.string.rimuovi) + itemPagina.Nome)
            .setMessage(getString(R.string.allert_delete_item))
            .setPositiveButton(getString(R.string.yessir)) { _: DialogInterface, _: Int ->
                dataBase.collection("Item").document(itemId).delete().addOnSuccessListener {
                    finish()
                }
            }.setNegativeButton(getString(R.string.nosir)) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            images.clear()
            if (refreshFragments) {
                refreshItem()
            }
            println("SIZE OF IMAGES on resume = ${images.size}")
        }

    }

    override fun onPause() {
        super.onPause()
        refreshFragments = false
    }


    private suspend fun refreshItem() {
        currentUser = User().getCurrentUser(auth, dataBase)
        itemPagina = Item().getCurrentItem(dataBase, itemId)
        if (currentUser.Status == 2 || currentUser.Status == 3) {
            finish()
        }
        settaElementiPagina()
    }

    private fun checkStatusUtente(code: Int) {
        MainScope().launch {
            withContext(Dispatchers.Main) {
                currentUser = User().getCurrentUser(auth, dataBase)
            }
            if (currentUser.Status == 2 || currentUser.Status == 3) {
                Toast.makeText(
                    this@ItemActivity,
                    getString(R.string.auth_notification_missing),
                    Toast.LENGTH_LONG
                )
                    .show()
                finish()
            } else if (currentUser.Status == 1) {
                Toast.makeText(
                    this@ItemActivity,
                    getString(R.string.auth_notification_missing),
                    Toast.LENGTH_LONG
                )
                    .show()
            } else {
                if (code == 1) {
                    preferito()
                } else if (code == 2) {
                    rimuoviOggetto()
                }
            }
        }
    }


}