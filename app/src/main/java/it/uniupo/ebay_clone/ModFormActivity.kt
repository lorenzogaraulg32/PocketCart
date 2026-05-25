package it.uniupo.ebay_clone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import it.uniupo.ebay_clone.databinding.ActivityModFormBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ModFormActivity : AppCompatActivity(), MapsFragment.MapsResult {

    private lateinit var itemModId: String
    private lateinit var itemMod: Item
    private lateinit var binding: ActivityModFormBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User
    val db = FirebaseFirestore.getInstance()

    private var array_rimozione = ArrayList<String>()

    //Posizione
    private val mapsFragment = MapsFragment()
    private var lat: Double = 0.0
    private var long: Double = 0.0
    private var indirizzo = ""
    var pos = ArrayList<Double>(2)
    var fotoMod = false

    override fun getMapsResults(titolo: String, latitude: Double, longitude: Double) {
        lat = latitude
        long = longitude
        binding.pos.text = titolo
        indirizzo = titolo
        pos.clear()
        pos.add(lat)
        pos.add(long)
        println("latitudine: $lat, longitudine: $long, titolo: $indirizzo")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemModId = intent.getStringExtra("itemId").toString()
        spinnerDefault()
        var spinnerAdapter: ArrayAdapter<CharSequence>
        spinnerAdapter =
            ArrayAdapter.createFromResource(
                this,
                R.array.condizioni,
                R.layout.spinner_item
            )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerCondizioni.adapter = spinnerAdapter

        auth = FirebaseAuth.getInstance()

        val getImage =
            registerForActivityResult(ActivityResultContracts.GetContent(), ActivityResultCallback {
                if (it != null) {
                    when (itemMod.Foto.size) {
                        0 -> {
                            binding.fotoErr.visibility = View.INVISIBLE
                            binding.imagev.setImageURI(it)
                            binding.imagev.scaleType = ImageView.ScaleType.CENTER_CROP
                            itemMod.Foto.add("tmp0")
                        }

                        1 -> {
                            binding.imagew.setImageURI(it)
                            binding.imagew.scaleType = ImageView.ScaleType.CENTER_CROP
                            itemMod.Foto.add("tmp1")
                        }

                        2 -> {
                            binding.imagex.setImageURI(it)
                            binding.imagex.scaleType = ImageView.ScaleType.CENTER_CROP
                            itemMod.Foto.add("tmp2")
                        }

                        3 -> {
                            binding.imagey.setImageURI(it)
                            binding.imagey.scaleType = ImageView.ScaleType.CENTER_CROP
                            itemMod.Foto.add("tmp3")
                        }
                    }
                    println(itemMod.Foto)
                } else {
                    println("errore immagine")
                }
            })

        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
                itemMod = db.collection("Item").document(itemModId).get().await()
                    .toObject(Item::class.java)!!
                retriveImages()
            }
            if (currentUser.Status == 1) finish()
            if (currentUser.Status == 2) finish()
            settaElementiPagina()

        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.modBtn.setOnClickListener {
            disableAllBtn()
            Toast.makeText(this, getString(R.string.mod), Toast.LENGTH_LONG).show()
            MainScope().launch {
                withContext(Dispatchers.Main) {
                    modificaOggetto()
                }
            }
        }

        binding.fotoBtn.setOnClickListener {
            if (itemMod.Foto.size >= 4) {
                Toast.makeText(this,
                    getString(R.string.max_photo), Toast.LENGTH_SHORT).show()
            } else {
                if (!fotoMod) fotoMod = true
                getImage.launch("image/*")
            }
        }

        binding.fotoBtnElim.setOnClickListener {
            if (!fotoMod) fotoMod = true
            eliminaUltimaFoto()
        }

        binding.sellLayout.setOnClickListener {
            it.hideKeyboard()
            binding.itemName.clearFocus()
            binding.spinnerCategory.clearFocus()
            binding.spinnerCondizioni.clearFocus()
            binding.spinnerSubCategory.clearFocus()
            binding.itemDescription.clearFocus()
            binding.itemPrice.clearFocus()
        }

        binding.spinnerCategory.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val spinnerAdapter: ArrayAdapter<CharSequence>
                    when {
                        position == 0 -> {
                            spinnerDefault()
                        }

                        position == 1 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@ModFormActivity,
                                R.array.sotto_categorie_elettronica,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 2 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@ModFormActivity,
                                R.array.sotto_categorie_fashion,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 3 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@ModFormActivity,
                                R.array.sotto_categorie_casa,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 4 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@ModFormActivity,
                                R.array.sotto_categorie_salute,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 5 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@ModFormActivity,
                                R.array.sotto_categorie_sport,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 6 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@ModFormActivity,
                                R.array.sotto_categorie_libri,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 7 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@ModFormActivity,
                                R.array.sotto_categorie_animali,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 8 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@ModFormActivity,
                                R.array.sotto_categorie_macchina,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 9 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@ModFormActivity,
                                R.array.sotto_categorie_ufficio,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }
                    }

                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }

            }

        binding.posBtnSelectFromMap.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_holder, mapsFragment).commit()
        }

        binding.posBtnSelectFromUser.setOnClickListener {
            if (currentUser.Posizione.isEmpty()) {
                Toast.makeText(this,
                    getString(R.string.pos_not_saved), Toast.LENGTH_LONG).show()
            } else {
                lat = currentUser.Posizione[0]
                long = currentUser.Posizione[1]
                indirizzo = getAddress()
                binding.pos.text = indirizzo
                pos.clear()
                pos.add(lat)
                pos.add(long)
                println("latitude : $lat, longitude: $long, address: $indirizzo")
            }
        }
    }


    override fun onResume() {
        super.onResume()
        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
            }
            if (currentUser.Status == 1) finish()
            if (currentUser.Status == 2) finish()
        }
    }

    private fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    fun isNumericWithDecimal(input: String): Boolean {
        val regex = Regex("[0-9.]+")
        return input.matches(regex)
    }


    private fun settaElementiPagina() {
        runOnUiThread {
            binding.itemNameField.setText(itemMod.Nome)
            binding.itemDescriptionField.setText(itemMod.Descrizione)
            binding.itemPriceField.setText(Item().ScaleDouble(itemMod.Prezzo))
            binding.pos.text = itemMod.getItemPosition(this)
            binding.spinnerCondizioni.setSelection(getConditionInt(itemMod.Stato.toString()))
            binding.spinnerTitleCategoryPrev.text = getString(R.string.prev) + itemMod.Categoria
            binding.spinnerTitleSubCategoryPrev.text = getString(R.string.prev) + itemMod.SottoCategoria
            binding.shipping.isChecked = itemMod.Spedizione
        }
    }


    private fun getAddress(): String {
        var addr = ""
        val geoCoder = Geocoder(this, Locale.getDefault())
        try {
            val addrList =
                geoCoder.getFromLocation(lat, long, 3)!!
            val myAddress = addrList[0]
            addr = myAddress.getAddressLine(0).toString()
        } catch (e: Exception) {
            println("Exception : $e")
        }
        return addr
    }


    private fun spinnerDefault() {
        var spinnerAdapter: ArrayAdapter<CharSequence> =
            ArrayAdapter.createFromResource(
                this,
                R.array.categorie,
                R.layout.spinner_item
            )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerCategory.adapter = spinnerAdapter

        spinnerAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.sotto_categorie_empty,
            R.layout.spinner_item
        )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerSubCategory.adapter = spinnerAdapter
    }


    private suspend fun modificaOggetto() {
        val itemMap = HashMap<String, Any>()
        itemMap["Nome"] = binding.itemNameField.text.toString()
        itemMap["Descrizione"] = binding.itemDescriptionField.text.toString()

        if (binding.spinnerCategory.selectedItemPosition != 0) {
            itemMap["Categoria"] = binding.spinnerCategory.selectedItem.toString()
        }
        if (binding.spinnerSubCategory.selectedItemPosition != 0) {
            itemMap["SottoCategoria"] = binding.spinnerSubCategory.selectedItem.toString()
        }

        try {
            binding.itemPriceField.text.toString().toDouble()
            itemMap["Prezzo"] = binding.itemPriceField.text.toString().toDouble()
        } catch (e: Exception) {
            println("Errore modifica E : $e")
        }

        if (fotoMod && itemMod.Foto.isNotEmpty()) {
            val nuoveFoto = ArrayList<String>()
            println("Array foto : ${itemMod.Foto}")
            for ((count, i) in itemMod.Foto.withIndex()) {
                if (i.startsWith("tmp")) {
                    var newTmp = ""
                    when (count) {
                        0 -> newTmp = caricaFoto(binding.imagev, 0)
                        1 -> newTmp = caricaFoto(binding.imagew, 1)
                        2 -> newTmp = caricaFoto(binding.imagex, 2)
                        3 -> newTmp = caricaFoto(binding.imagey, 3)
                    }
                    nuoveFoto.add(newTmp)
                } else {
                    nuoveFoto.add(i)
                }
            }
            itemMap["Foto"] = nuoveFoto
            if (array_rimozione.isNotEmpty()) {
                rimuoviFotodaStorage()
            }
        }
        if (pos.isNotEmpty()) {
            itemMap["Posizione"] = pos
        }
        itemMap["Stato"] = getConditionInt(binding.spinnerCondizioni.selectedItem.toString())
        itemMap["Spedizione"] = binding.shipping.isChecked

        if (itemMod.Prezzo != binding.itemPriceField.text.toString().toDouble()) {
            checkPref()
        }

        db.collection("Item").document(itemModId).update(itemMap).addOnSuccessListener {
            Toast.makeText(this,
                getString(R.string.positive_mod_msg), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun disableAllBtn() {
        binding.backBtn.isEnabled = false
        binding.fotoBtn.isEnabled = false
        binding.fotoBtnElim.isEnabled = false
        binding.modBtn.isEnabled = false
        binding.posBtnSelectFromMap.isEnabled = false
        binding.posBtnSelectFromUser.isEnabled = false
    }

    private fun enableAllBtn() {
        binding.backBtn.isEnabled = true
        binding.fotoBtn.isEnabled = true
        binding.fotoBtnElim.isEnabled = true
        binding.modBtn.isEnabled = true
        binding.posBtnSelectFromMap.isEnabled = true
        binding.posBtnSelectFromUser.isEnabled = true
    }


    private fun checkItem() {
        val nomeItem = binding.itemNameField.text.toString()
        val descrizioneItem = binding.itemDescriptionField.text.toString()
        val prezzoItem = binding.itemPriceField.text.toString()
        val pos = binding.pos.text


        var checkPos: Boolean
        var checkNome: Boolean
        var checkDesc: Boolean
        var checkPrice: Boolean


        //nome
        if (nomeItem.isNotEmpty()) {
            if (nomeItem.length < 3) {
                binding.nameErr.text = getString(R.string.name_length_err1)
                binding.nameErr.visibility = View.VISIBLE
                checkNome = false
            } else if (nomeItem.length > 25) {
                binding.nameErr.text = getString(R.string.name_length_err2)
                binding.nameErr.visibility = View.VISIBLE
                checkNome = false
            } else {
                checkNome = true
            }
        } else {
            binding.nameErr.visibility = View.VISIBLE
            checkNome = false
        }

        //Descrizione
        if (descrizioneItem.isNotEmpty()) {
            if (descrizioneItem.length < 3) {
                binding.descriptionErr.text = getString(R.string.desc_length_err1)
                binding.descriptionErr.visibility = View.VISIBLE
                checkDesc = false
            } else if (nomeItem.length > 400) {
                binding.descriptionErr.text = getString(R.string.desc_length_err2)
                binding.descriptionErr.visibility = View.VISIBLE
                checkDesc = false
            } else {
                checkDesc = true
            }
        } else {
            binding.descriptionErr.visibility = View.VISIBLE
            checkDesc = false
        }

        //check Prezzo
        if (prezzoItem.isNotEmpty() && isNumericWithDecimal(prezzoItem)) {
            try {
                java.lang.Double.parseDouble(prezzoItem)
                checkPrice = true
            } catch (e: Exception) {
                binding.priceErr.text = getString(R.string.price_format_err)
                binding.priceErr.visibility = View.VISIBLE
                checkPrice = false
            }
        } else if (prezzoItem.length > 6) {
            binding.priceErr.text = getString(R.string.price_tooHigh_err)
            binding.priceErr.visibility = View.VISIBLE
            checkPrice = false
        } else {
            checkPrice = false
        }

        //check pos
        if (pos.equals(R.string.reg_pos_placeholder)) {
            //non è stata inserita nessuna Posizione
            binding.posErr.text = getString(R.string.ob_pos_err)
            binding.posErr.visibility = View.VISIBLE
            checkPos = false
        } else {
            binding.posErr.text = ""
            binding.posErr.visibility = View.INVISIBLE
            checkPos = true
        }

        if (checkNome && checkDesc && checkPrice && checkPos) {
            MainScope().launch {
                modificaOggetto()
            }
        } else {
            enableAllBtn()
            println("Errore check item")
            return
        }
    }

    private suspend fun checkPref() {
        println("Sto verificando i preferiti dell'utente")
        val query = db.collection("User").get().await()
        for (doc in query) {
            println("sto ciclando gli utenti")
            val pref = doc.get("Preferiti") as? HashMap<String, Boolean>
            pref?.let {
                println("Preferiti non null")
                for (p in it) {
                    println("preferito : $itemModId")
                    if (p.key == itemModId) {
                        println("Trovato id oggetto: $itemModId")
                        val user = doc.toObject(User::class.java)
                        user.Preferiti[itemModId] = true
                        user.updateUserPrefMod(db, doc.id)
                    }
                }
            }
        }
    }


    private fun getConditionInt(condizione: String): Int {
        var condizioneInt = 0
        if (condizione == getString(R.string.cond0)) {
            condizioneInt = 1
        } else if (condizione == getString(R.string.cond1)) {
            condizioneInt = 2
        } else if (condizione == getString(R.string.cond2)) {
            condizioneInt = 3
        } else if (condizione == getString(R.string.cond3)) {
            condizioneInt = 4
        }
        return condizioneInt
    }


    //recupera le immagini da caricare
    private suspend fun retriveImages() {
        var count = 0
        for (foto in itemMod.Foto) {
            withContext(Dispatchers.IO) {
                var bitmap: Bitmap?
                val storageFile =
                    FirebaseStorage.getInstance().getReference("images/$foto")
                val localFile = File.createTempFile(foto + "tmp", "jpg")
                storageFile.getFile(localFile).addOnSuccessListener {
                    bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                    runOnUiThread {
                        settaImmagine(count, bitmap)
                    }
                    count++
                }.addOnFailureListener {
                    Toast.makeText(
                        this@ModFormActivity, getString(R.string.toast_photo_err), Toast.LENGTH_SHORT
                    ).show()
                    bitmap = BitmapFactory.decodeResource(
                        this@ModFormActivity.resources,
                        R.drawable.image_placeholder
                    )
                }
            }
        }
    }

    //setta l'immagine nella posizione giusta
    private fun settaImmagine(count: Int, bitmap: Bitmap?) {
        if (count == 0) {
            binding.imagev.setImageBitmap(bitmap)
            binding.imagev.scaleType = ScaleType.CENTER_CROP
        } else if (count == 1) {
            binding.imagew.setImageBitmap(bitmap)
            binding.imagew.scaleType = ScaleType.CENTER_CROP
        } else if (count == 2) {
            binding.imagex.setImageBitmap(bitmap)
            binding.imagex.scaleType = ScaleType.CENTER_CROP
        } else if (count == 3) {
            binding.imagey.setImageBitmap(bitmap)
            binding.imagey.scaleType = ScaleType.CENTER_CROP
        }
    }

    //questa funziona
    private fun eliminaUltimaFoto() {

        if (itemMod.Foto.isEmpty()) {
            return
        }

        val ultima = itemMod.Foto.size - 1
        val rm = itemMod.Foto.removeAt(ultima)
        if (!rm.contains("tmp")) {
            array_rimozione.add(rm)
        }

        val placeholder = R.drawable.image_placeholder

        when (ultima) {
            3 -> {
                binding.imagey.setImageResource(placeholder)
                binding.imagey.scaleType = ScaleType.FIT_CENTER
            }

            2 -> {
                binding.imagex.setImageResource(placeholder)
                binding.imagex.scaleType = ScaleType.FIT_CENTER
            }

            1 -> {
                binding.imagew.setImageResource(placeholder)
                binding.imagew.scaleType = ScaleType.FIT_CENTER
            }

            0 -> {
                binding.imagev.setImageResource(placeholder)
                binding.imagev.scaleType = ScaleType.FIT_CENTER
                binding.fotoErr.text = getString(R.string.back_original_photo)
                binding.fotoErr.visibility = View.VISIBLE
            }
        }
    }

    //questa funziona
    private suspend fun caricaFoto(imagev: ImageView, pos: Int): String {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        val now = Date()
        val fileName = sdf.format(now) + "_" + pos.toString()
        val storageReference = FirebaseStorage.getInstance().getReference("images/$fileName")

        val bitmap = Bitmap.createBitmap(imagev.width, imagev.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        imagev.draw(canvas)

        val megaByteCount = (bitmap.byteCount) / (1024.0 * 1024.0)
        val compressionQuality = if (megaByteCount < 1.00) {
            100
        } else {
            70
        }
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, baos)
        val data = baos.toByteArray()

        storageReference.putBytes(data).await()
        return fileName
    }

    private fun rimuoviFotodaStorage() {
        for (i in 0..array_rimozione.size - 1) {
            val storageRef =
                FirebaseStorage.getInstance().getReference("images/" + array_rimozione.get(i))
            storageRef.delete().addOnSuccessListener {
            }
        }
    }

}