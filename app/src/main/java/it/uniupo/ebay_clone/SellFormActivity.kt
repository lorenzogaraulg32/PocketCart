package it.uniupo.ebay_clone

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import it.uniupo.ebay_clone.databinding.ActivitySellFormBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.lang.Double.parseDouble
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SellFormActivity : AppCompatActivity(), MapsFragment.MapsResult {

    private lateinit var binding: ActivitySellFormBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User
    val db = FirebaseFirestore.getInstance()

    //Foto
    val images = ArrayList<ImageView>(4)
    lateinit var imagev: ImageView
    lateinit var imagew: ImageView
    lateinit var imagex: ImageView
    lateinit var imagey: ImageView

    //condizione serve globale per forza
    var condizioneItem = 0

    //Posizione
    private val mapsFragment = MapsFragment()
    private var lat: Double = 0.0
    private var long: Double = 0.0
    private var indirizzo = ""
    var pos: ArrayList<Double> = ArrayList(2)


    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun getMapsResults(titolo: String, latitude: Double, longitude: Double) {
        lat = latitude
        long = longitude
        binding.pos.text = titolo
        indirizzo = titolo
        pos[0] = lat
        pos[1] = long
        println("latitudine: $lat, longitudine: $long, titolo: $indirizzo")
        enableAllBtn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySellFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //inizializzazione database e autenticazione utente e retrival dell'utente
        auth = FirebaseAuth.getInstance()
        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
            }
            if (currentUser.Status != 0) {
                Toast.makeText(
                    this@SellFormActivity,
                    getString(R.string.toast_authorization_missing),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

        //spinner adapter
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

        //Posizione
        pos.add(0, lat)
        pos.add(1, long)
        var indirizzo = getString(R.string.reg_pos_placeholder)
        binding.pos.text = indirizzo

        //imageView
        imagev = findViewById(R.id.imagev)
        imagew = findViewById(R.id.imagew)
        imagex = findViewById(R.id.imagex)
        imagey = findViewById(R.id.imagey)

        val getImage =
            registerForActivityResult(
                ActivityResultContracts.GetContent(),
                ActivityResultCallback<Uri?> {
                    if (it != null) {
                        if (images.size == 0) {
                            imagev.setImageURI(it)
                            imagev.scaleType = ImageView.ScaleType.CENTER_CROP
                            images.add(imagev)
                        } else if (images.size == 1) {
                            imagex.setImageURI(it)
                            imagex.scaleType = ImageView.ScaleType.CENTER_CROP
                            images.add(imagex)
                        } else if (images.size == 2) {
                            imagew.setImageURI(it)
                            imagew.scaleType = ImageView.ScaleType.CENTER_CROP
                            images.add(imagew)
                        } else if (images.size == 3) {
                            imagey.setImageURI(it)
                            imagey.scaleType = ImageView.ScaleType.CENTER_CROP
                            images.add(imagey)
                        }
                    } else {
                        println("Errore immagine")
                    }
                })

        binding.fotoBtn.setOnClickListener {
            if (images.size >= 4) {
                Toast.makeText(this, getString(R.string.max_photo_err), Toast.LENGTH_SHORT).show()
            } else {
                getImage.launch("image/*")
            }
        }

        binding.fotoBtnElim.setOnClickListener {
            eliminaUltimaFoto()
        }

        binding.posBtnSelectFromMap.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_holder, mapsFragment).commit()
        }

        binding.posBtnSelectFromUser.setOnClickListener {
            if (currentUser.Posizione.isEmpty()) {
                Toast.makeText(this, getString(R.string.pos_not_saved), Toast.LENGTH_LONG).show()
            } else {
                lat = currentUser.Posizione[0]
                long = currentUser.Posizione[1]
                indirizzo = getAddress()
                binding.pos.text = indirizzo
                pos[0] = lat
                pos[1] = long
                println("latitude : $lat, longitude: $long, address: $indirizzo")
            }
        }

        binding.backBtn.setOnClickListener {
            finish()
        }
        //gestione back button
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mapsFragment.isAdded) {
                    supportFragmentManager.beginTransaction().remove(mapsFragment).commit()
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)


        binding.sellBtn.setOnClickListener {
            disableAllBtn()
            binding.nameErr.visibility = INVISIBLE
            binding.descriptionErr.visibility = INVISIBLE
            binding.priceErr.visibility = INVISIBLE
            binding.spinnerCatErr.visibility = INVISIBLE
            binding.spinnerSubCatErr.visibility = INVISIBLE
            binding.spinnerCondErr.visibility = INVISIBLE
            binding.posErr.visibility = INVISIBLE
            checkItem()
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
                                this@SellFormActivity,
                                R.array.sotto_categorie_elettronica,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 2 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@SellFormActivity,
                                R.array.sotto_categorie_fashion,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 3 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@SellFormActivity,
                                R.array.sotto_categorie_casa,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 4 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@SellFormActivity,
                                R.array.sotto_categorie_salute,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 5 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@SellFormActivity,
                                R.array.sotto_categorie_sport,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 6 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@SellFormActivity,
                                R.array.sotto_categorie_libri,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 7 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@SellFormActivity,
                                R.array.sotto_categorie_animali,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 8 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@SellFormActivity,
                                R.array.sotto_categorie_macchina,
                                R.layout.spinner_item
                            )
                            spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            binding.spinnerSubCategory.adapter = spinnerAdapter
                        }

                        position == 9 -> {
                            spinnerAdapter = ArrayAdapter.createFromResource(
                                this@SellFormActivity,
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
    }

    override fun onResume() {
        super.onResume()
        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
            }
            if (currentUser.Status != 0) {
                Toast.makeText(
                    this@SellFormActivity,
                    getString(R.string.toast_authorization_missing),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun disableAllBtn() {
        binding.backBtn.isEnabled = false
        binding.fotoBtn.isEnabled = false
        binding.fotoBtnElim.isEnabled = false
        binding.sellBtn.isEnabled = false
        binding.posBtnSelectFromMap.isEnabled = false
        binding.posBtnSelectFromUser.isEnabled = false
    }

    private fun enableAllBtn() {
        binding.backBtn.isEnabled = true
        binding.fotoBtn.isEnabled = true
        binding.fotoBtnElim.isEnabled = true
        binding.sellBtn.isEnabled = true
        binding.posBtnSelectFromMap.isEnabled = true
        binding.posBtnSelectFromUser.isEnabled = true
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

    fun isNumericWithDecimal(input: String): Boolean {
        val regex = Regex("[0-9.]+")
        return input.matches(regex)
    }


    private fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
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

    //controlla che tutti i campi dell'oggetto siano completi
    private fun checkItem() {
        val nomeItem = binding.itemNameField.text.toString() //fatto
        val categoriaItem = binding.spinnerCategory.selectedItem //fatto
        val sottoCategoriaItem = binding.spinnerSubCategory.selectedItem //fatto
        val descrizioneItem = binding.itemDescriptionField.text.toString() //fatto
        val prezzoItem = binding.itemPriceField.text.toString()//fatto
        val pos = binding.pos.text
        condizioneItem = getConditionInt(binding.spinnerCondizioni.selectedItem.toString())

        var checkPos: Boolean
        var checkNome: Boolean
        var checkDesc: Boolean
        var checkCategory: Boolean
        var checkSubCategory: Boolean
        var checkCondizione: Boolean
        var checkPrice: Boolean
        var checkImg: Boolean

        //nome
        if (nomeItem.isNotEmpty()) {
            if (nomeItem.length < 3) {
                binding.nameErr.text = getString(R.string.name_length_err1)
                binding.nameErr.visibility = VISIBLE
                checkNome = false
            } else if (nomeItem.length > 25) {
                binding.nameErr.text = getString(R.string.name_length_err2)
                binding.nameErr.visibility = VISIBLE
                checkNome = false
            } else {
                checkNome = true
            }
        } else {
            binding.nameErr.visibility = VISIBLE
            checkNome = false
        }

        //immagini
        if (images.isEmpty()) {
            binding.fotoErr.text = getString(R.string.photo_missing)
            binding.fotoErr.visibility = VISIBLE
            checkImg = false
        } else {
            binding.fotoErr.text = ""
            binding.fotoErr.visibility = INVISIBLE
            checkImg = true
        }

        //Descrizione
        if (descrizioneItem.isNotEmpty()) {
            if (descrizioneItem.length < 3) {
                binding.descriptionErr.text = getString(R.string.desc_length_err1)
                binding.descriptionErr.visibility = VISIBLE
                checkDesc = false
            } else if (nomeItem.length > 400) {
                binding.descriptionErr.text = getString(R.string.desc_length_err2)
                binding.descriptionErr.visibility = VISIBLE
                checkDesc = false
            } else {
                checkDesc = true
            }
        } else {
            binding.descriptionErr.visibility = VISIBLE
            checkDesc = false
        }

        //spinner Categoria
        if (categoriaItem.toString().isNotEmpty()) {
            checkCategory = true
        } else {
            binding.spinnerCatErr.text = getString(R.string.err_not_empty)
            binding.spinnerCatErr.visibility = VISIBLE
            checkCategory = false
        }

        //spinner  sotto Categoria
        if (sottoCategoriaItem.toString().isNotEmpty()) {
            checkSubCategory = true
        } else {
            binding.spinnerSubCatErr.text = getString(R.string.err_not_empty)
            binding.spinnerSubCatErr.visibility = VISIBLE
            checkSubCategory = false
        }

        //spinner condizione
        if (condizioneItem != 0) {
            checkCondizione = true
        } else {
            binding.spinnerCondErr.text = getString(R.string.err_not_empty)
            binding.spinnerCondErr.visibility = VISIBLE
            checkCondizione = false
        }

        //check Prezzo
        if (prezzoItem.isNotEmpty() && isNumericWithDecimal(prezzoItem)) {
            try {
                parseDouble(prezzoItem)
                checkPrice = true
            } catch (e: Exception) {
                binding.priceErr.text = getString(R.string.price_format_err)
                binding.priceErr.visibility = VISIBLE
                checkPrice = false
            }
        } else if (prezzoItem.length > 6) {
            binding.priceErr.text = getString(R.string.price_tooHigh_err)
            binding.priceErr.visibility = VISIBLE
            checkPrice = false
        } else {
            checkPrice = false
            binding.priceErr.text = getString(R.string.err_not_empty)
            binding.priceErr.visibility = VISIBLE
        }

        //check pos
        if (pos.equals(R.string.reg_pos_placeholder)) {
            //non è stata inserita nessuna Posizione
            binding.posErr.text = getString(R.string.pos_err)
            binding.posErr.visibility = VISIBLE
            checkPos = false
        } else {
            binding.posErr.text = ""
            binding.posErr.visibility = INVISIBLE
            checkPos = true
        }

        if (checkNome && checkImg && checkDesc && checkCategory && checkSubCategory && checkCondizione && checkPrice && checkPos) {
            sellItem(currentUser, condizioneItem)
        } else {
            println("Errore check item")
            enableAllBtn()
            return
        }
    }

    //elimina l'ultima Foto aggiunta
    private fun eliminaUltimaFoto() {
        if (images.isEmpty()) {
            return
        }
        val ultima = images.size - 1
        val imageView = images[ultima]

        val placeholder = R.drawable.image_placeholder
        imageView.setImageResource(placeholder)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        images.removeAt(ultima)
    }

    //carica una Foto nello storage
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


    //funzione sell item
    private fun sellItem(user: User, cond: Int) {

        val nomeItem = binding.itemNameField.text.toString()
        val categoriaItem = binding.spinnerCategory.selectedItem.toString()
        val sottoCategoriaItem = binding.spinnerSubCategory.selectedItem.toString()
        val descrizioneItem = binding.itemDescriptionField.text.toString()
        val prezzoItem = binding.itemPriceField.text.toString()

        val f: ArrayList<String> = ArrayList(4)
        var count = 0

        MainScope().launch {
            withContext(Dispatchers.IO) {
                for (image in images) {
                    val f1 = caricaFoto(image, count)
                    f.add(f1)
                    count++
                }
            }
            val item: Item = Item(
                nomeItem,
                categoriaItem,
                sottoCategoriaItem,
                pos,
                descrizioneItem,
                prezzoItem.toDouble(),
                cond,
                f,
                user.Username,
                binding.shipping.isChecked,
                "",
                false,
                0
            )
            println(item)
            item.addItemtoDb(db)
            Toast.makeText(
                applicationContext,
                getString(R.string.selll_good_ending),
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent()
            setResult(0, intent)
            finish()
        }
    }


    //converte la condizione da stringa a intero
    private fun getConditionInt(condizione: String): Int {
        var condizioneInt = 0
        if (condizione.equals(getString(R.string.cond0))) {
            condizioneInt = 1
        } else if (condizione.equals(getString(R.string.cond1))) {
            condizioneInt = 2
        } else if (condizione.equals(getString(R.string.cond2))) {
            condizioneInt = 3
        } else if (condizione.equals(getString(R.string.cond3))) {
            condizioneInt = 4
        }

        return condizioneInt
    }
}


