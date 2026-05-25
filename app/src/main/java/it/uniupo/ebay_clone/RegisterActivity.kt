package it.uniupo.ebay_clone

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import it.uniupo.ebay_clone.databinding.ActivityRegistrazioneBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class RegisterActivity : AppCompatActivity(), RegistrationFragmentUserData.RegResult,
    RegistrationFragmentUserPosition.PosResult, MapsFragment.MapsResult {

    override fun getMapsResults(titolo: String, latitude: Double, longitude: Double) {
        MainScope().launch {
            withContext(Dispatchers.Main) {
                lat = latitude
                long = longitude
                pos.clear()
                pos.add(lat)
                pos.add(long)
                indirizzo = titolo
                println("Tornato dal maps fragment, lat:$lat, long:$long, addr:$indirizzo")
                supportFragmentManager.beginTransaction()
                    .detach(posFragment)
                    .commit()

                var bundle = Bundle()
                bundle.putString("code", "1")
                bundle.putString("address", titolo)
                bundle.putDouble("lat", latitude)
                bundle.putDouble("long", longitude)
                posFragment.arguments = bundle
                supportFragmentManager.beginTransaction()
                    .remove(mapsFragment)
                    .attach(posFragment)
                    .commit()
            }
        }
    }

    override fun getResults(
        nome: String,
        email: String,
        username: String,
        password: String,
        esito: Boolean
    ) {
        if(esito) {
            this.nome = nome
            this.email = email
            this.username = username
            this.password = password
            println("Nome : ${this.nome}, Email: ${this.email}, Username: ${this.username}, password: ${this.password}")
            regUser(checkPos)
        }else{
            binding.btnReg.isEnabled = true
        }
    }

    override fun getPosResults(lat: Double, long: Double, address: String) {
        this.lat = lat
        this.long = long
        this.indirizzo = address
        pos.clear()
        pos.add(lat)
        pos.add(long)
        checkPos = true
        println("Check pos: $checkPos, indirizzo : ${this.indirizzo}")
    }

    override fun openMap() {
        mapsFragment = MapsFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.main_holder, mapsFragment)
            .attach(mapsFragment)
            .commit()
    }


    private lateinit var indirizzo : String
    private var lat: Double = 0.0
    private var long: Double = 0.0
    var pos: ArrayList<Double> = ArrayList(2)
    private var checkPos = false


    private var dataFragment = RegistrationFragmentUserData()
    private var posFragment = RegistrationFragmentUserPosition()
    private var mapsFragment = MapsFragment()

    private lateinit var binding: ActivityRegistrazioneBinding
    private lateinit var Auth: FirebaseAuth
    var db = FirebaseFirestore.getInstance()

    var nome = ""
    var email = ""
    var username = ""
    var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //grafica della topbar
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            insets.consumeSystemWindowInsets()
        }
        indirizzo = getString(R.string.base_pos_err)

        binding = ActivityRegistrazioneBinding.inflate(layoutInflater)
        Auth = FirebaseAuth.getInstance()
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = window.insetsController
            windowInsetsController?.hide(WindowInsets.Type.navigationBars())
            windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        //setting iniziale dei fragment
        setUIDesign()
        //questa serveper far stareil bottone in fondo e non seguirela tastiera
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        binding.btnReg.setOnClickListener {
            binding.btnReg.isEnabled = false
            if (dataFragment.isDetached) {
                binding.dataFragBtn.isClickable = false
                binding.posFragBtn.isClickable = false
                binding.dataFragBtn.setBackgroundResource(R.drawable.button_switch)
                binding.dataFragBtn.setTextColor(
                    ContextCompat.getColor(
                        this@RegisterActivity,
                        R.color.text_color
                    )
                )

                val bundle = Bundle()
                bundle.putString("code", "1")
                dataFragment.arguments = bundle

                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_left,  // Animation for the entering fragment
                        R.anim.slide_out_right, // Animation for the exiting fragment
                        R.anim.slide_in_right,  // Animation for the entering fragment (reverse)
                        R.anim.slide_out_left   // Animation for the exiting fragment (reverse)
                    )
                    .detach(posFragment)
                    .attach(dataFragment)
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .detach(dataFragment)
                    .commit()

                val bundle = Bundle()
                bundle.putString("code", "1")
                dataFragment.arguments = bundle
                supportFragmentManager.beginTransaction()
                    .attach(dataFragment)
                    .commit()
            }
            binding.dataFragBtn.isPressed = true
            binding.posFragBtn.setBackgroundResource(R.color.input_color_focussed)
            binding.posFragBtn.setTextColor(
                ContextCompat.getColor(
                    this@RegisterActivity,
                    R.color.text_color3
                )
            )
            binding.posFragBtn.isPressed = false
            binding.posFragBtn.isClickable = true
        }

        binding.regHaveAcc.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.mainHolder.setOnClickListener {
            if (!mapsFragment.isAdded) {
                it.hideKeyboard()
            }
        }

        //bottoni switch fragments
        binding.dataFragBtn.setOnClickListener {
            MainScope().launch {

                withContext(Dispatchers.Main) {
                    if (dataFragment.isDetached) {
                        binding.dataFragBtn.isClickable = false
                        binding.posFragBtn.isClickable = false
                        binding.dataFragBtn.setBackgroundResource(R.drawable.button_switch)
                        binding.dataFragBtn.setTextColor(
                            ContextCompat.getColor(
                                this@RegisterActivity,
                                R.color.text_color
                            )
                        )

                        val bundle = Bundle()
                        bundle.putString("code", "0")
                        dataFragment.arguments = bundle

                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                R.anim.slide_in_left,  // Animation for the entering fragment
                                R.anim.slide_out_right, // Animation for the exiting fragment
                                R.anim.slide_in_right,  // Animation for the entering fragment (reverse)
                                R.anim.slide_out_left   // Animation for the exiting fragment (reverse)
                            )
                            .detach(posFragment)
                            .attach(dataFragment)
                            .commit()
                        binding.dataFragBtn.isPressed = true
                        binding.posFragBtn.setBackgroundResource(R.color.input_color_focussed)
                        binding.posFragBtn.setTextColor(
                            ContextCompat.getColor(
                                this@RegisterActivity,
                                R.color.text_color3
                            )
                        )
                        binding.posFragBtn.isPressed = false
                        binding.posFragBtn.isClickable = true
                    }
                }
            }
        }

        binding.posFragBtn.setOnClickListener {
            MainScope().launch {
                withContext(Dispatchers.Main) {
                    if (posFragment.isDetached) {
                        binding.dataFragBtn.isClickable = false
                        binding.posFragBtn.isClickable = false
                        binding.posFragBtn.setBackgroundResource(R.drawable.button_switch)
                        binding.posFragBtn.setTextColor(
                            ContextCompat.getColor(
                                this@RegisterActivity,
                                R.color.text_color
                            )
                        )
                        var bundle = Bundle()
                        bundle.putString("code", "0")
                        bundle.putString("address", indirizzo)
                        bundle.putDouble("lat", lat)
                        bundle.putDouble("long", long)
                        posFragment.arguments = bundle
                        supportFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                R.anim.slide_in_right,  // Animation for the entering fragment
                                R.anim.slide_out_left,  // Animation for the exiting fragment
                                R.anim.slide_in_left,   // Animation for the entering fragment (reverse)
                                R.anim.slide_out_right  // Animation for the exiting fragment (reverse)
                            )
                            .detach(dataFragment)
                            .attach(posFragment)
                            .commit()
                        binding.posFragBtn.isPressed = true
                        binding.dataFragBtn.setBackgroundResource(R.color.input_color_focussed)
                        binding.dataFragBtn.setTextColor(
                            ContextCompat.getColor(
                                this@RegisterActivity,
                                R.color.text_color3
                            )
                        )
                        binding.dataFragBtn.isPressed = false
                        binding.dataFragBtn.isClickable = true
                    }
                }
            }
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mapsFragment.isAdded) {
                    supportFragmentManager.beginTransaction().remove(mapsFragment)
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    private fun setUIDesign() {
        dataFragment = RegistrationFragmentUserData()
        posFragment = RegistrationFragmentUserPosition()
        var bundle = Bundle()
        bundle.putString("code", "0")
        dataFragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_holder, dataFragment)
            .commit()

        bundle = Bundle()
        bundle.putString("code", "0")
        bundle.putString("address", indirizzo)
        bundle.putDouble("lat", lat)
        bundle.putDouble("long", long)
        posFragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_holder, posFragment)
            .commit()

        supportFragmentManager.beginTransaction()
            .detach(posFragment)
            .commit()

        bundle = Bundle()
        bundle.putString("code", "0")
        dataFragment.arguments = bundle
        supportFragmentManager.beginTransaction()
            .attach(dataFragment)
            .commit()
    }


    //funzione per rimuovere la tastiera e i focus
    private fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onRestart() {
        super.onRestart()
        if (Auth.currentUser != null) {
            finish()
        }
    }

    private fun regUser(checkPos: Boolean) {

        val user = User()
        Toast.makeText(this, getString(R.string.reg_doing), Toast.LENGTH_SHORT).show()
        Auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user.NomeCompleto = nome
                user.Email = email
                user.Username = username
                user.Admin = false
                when {
                    checkPos -> user.Posizione = pos
                    !checkPos -> user.Posizione = ArrayList()
                }
                user.addUserToDb(db, Auth)
                Toast.makeText(this, getString(R.string.reg_good), Toast.LENGTH_SHORT).show()
                var intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                binding.btnReg.isEnabled = true
            } else {
                binding.btnReg.isEnabled = true
                Toast.makeText(this, getString(R.string.reg_fail), Toast.LENGTH_SHORT).show()
            }
        }
    }


}






