package it.uniupo.ebay_clone

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import it.uniupo.ebay_clone.databinding.ActivityStatisticBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class StatisticActivity : AppCompatActivity() , MapsFragment.MapsResult {

    class Posizione {
        var utente: String = ""
        var valore: Double = 0.0

        constructor(utente: String, valore: Double) {
            this.utente = utente
            this.valore = valore
        }

        override fun toString(): String {
            return utente + " " + valore
        }
    }

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentUser: User
    private lateinit var binding: ActivityStatisticBinding
    private var mapsFragment = MapsFragment()

    var allItems = 0
    var userItems = 0

    var avgQty = 0
    var totalDaysAvg = 0
    var sellstat: Double = 0.0
    var center : kotlin.collections.ArrayList<Double> = arrayListOf()

    var classifica = ArrayList<Posizione>(5)

    var df=DecimalFormat("0.000")
    var df2=DecimalFormat("0.00")

    override fun getMapsResults(titolo: String, latitude: Double, longitude: Double) {
        center.add(latitude)
        center.add(longitude)
        binding.puntoMappa.text = titolo
        binding.mapsBtn.isEnabled = true
        binding.distBtn.isEnabled = true
        binding.userItemBtn.isEnabled = true
        binding.backBtn.isEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
            }
            if (!currentUser.Admin) {
                finish()
            }
        }

        queryGlobalItem()
        querySellStat()
        queryClassifica()

        binding.userItemBtn.setOnClickListener {
            queryUserItem()
        }

        binding.mapsBtn.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.main_holder, mapsFragment).commit()
            binding.mapsBtn.isEnabled = false
            binding.distBtn.isEnabled = false
            binding.userItemBtn.isEnabled = false
            binding.backBtn.isEnabled = false
        }

        binding.distBtn.setOnClickListener {
            queryItemsRangePos()
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

    }

    private fun queryGlobalItem() {
        db.collection("Item").get().addOnSuccessListener { documents ->
            var count = 0
            for (document in documents) {
                if (!document.get("Venduto").toString().toBoolean()) {
                    count++
                }
            }
            allItems = count
            binding.nOggetti.text = allItems.toString()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun queryUserItem() {
        db.collection("User").get().addOnSuccessListener { documents ->
            var trovato = false
            var admin = false
            for (document in documents) {
                if (document.get("Username").toString()
                        .equals(binding.ricercaItemUserField.text.toString())
                ) {
                    trovato = true
                    if (document.get("Admin").toString().toBoolean()) {
                        admin = true
                    }
                }
            }
            if (trovato) {
                if (!admin) {
                    db.collection("Item").get().addOnSuccessListener { documents ->
                        var count = 0
                        for (document in documents) {
                            if (document.get("Proprietario").toString()
                                    .equals(binding.ricercaItemUserField.text.toString()) && !document.get("Venduto")
                                    .toString().toBoolean()
                            ) {
                                count++
                            }
                        }
                        userItems = count
                        binding.nOggettiUtenteTitle.text = "${binding.ricercaItemUserField.text.toString()}:"
                        binding.nOggettiUtente.text = userItems.toString()
                        binding.ricercaItemUserField.text?.clear()
                    }
                } else {
                    binding.nOggettiUtenteTitle.text = "${binding.ricercaItemUserField.text.toString()}:"
                    binding.nOggettiUtente.text = getString(R.string.admin)
                    binding.ricercaItemUserField.text?.clear()
                }
            } else {
                binding.nOggettiUtenteTitle.text = getString(R.string.non_trovato)
                binding.ricercaItemUserField.text?.clear()
            }
        }
    }

    //avg giorni
    private fun querySellStat() {
        db.collection("Item").get().addOnSuccessListener { documents ->
            for (document in documents) {
                if (document.get("Venduto").toString().toBoolean()) {
                    var inizio = document.id.toLong()
                    var fine = document.get("DataVendita").toString().toLong()
                    var giorni = (fine - inizio) / (1000 * 60 * 60 * 24)
                    totalDaysAvg += giorni.toInt()
                    avgQty++
                }
            }
            sellstat = (totalDaysAvg.toDouble() / avgQty.toDouble())
            binding.nOggettiDay.text = df.format(sellstat)
            totalDaysAvg = 0
            avgQty = 0
        }
    }

    private fun queryClassifica() {
        db.collection("User").get().addOnSuccessListener { documents ->
            for (document in documents) {
                if (!document.get("Admin").toString().toBoolean()) {
                    var valTmp = document.get("Valutazione").toString().toDouble()
                    var username = document.get("Username").toString()
                    classifica.add(Posizione(username, valTmp))
                }
            }
            println(classifica)
            var claTmp = classifica.sortedByDescending({ it.valore })
            println(claTmp)
            if (claTmp.size > 5) {
                for (i in 0..4) {
                    settaClassifica(i, claTmp.get(i).valore, claTmp.get(i).utente)
                }
            } else {
                for (i in 0..claTmp.size - 1) {
                    settaClassifica(i, claTmp.get(i).valore, claTmp.get(i).utente)
                }
            }
            classifica.clear()
        }
    }

    private fun settaClassifica(pos: Int, valore: Double, user: String) {
        var entry = user + " : " + df2.format(valore)
        when (pos) {
            0 -> binding.primo.text = user + "\n" + df2.format(valore)
            1 -> binding.secondo.text = user + "\n" + df2.format(valore)
            2 -> binding.terzo.text = user + "\n" + df2.format(valore)
            3 -> binding.quarto.text = "4° " + entry
            4 -> binding.quinto.text = "5° " + entry
        }
    }


    private fun queryItemsRangePos(){
        if(center.isEmpty()){
            Toast.makeText(this, getString(R.string.toast_stat_pos), Toast.LENGTH_LONG).show()
            return
        }
        if(binding.ricercaItemDistField.text.isNullOrEmpty()){
            Toast.makeText(this, getString(R.string.toast_stat_dist), Toast.LENGTH_LONG).show()
            return
        }
        var count = 0
        val dist = binding.ricercaItemDistField.text.toString().toInt()
        db.collection("Item").get().addOnSuccessListener { doc ->
            for(docs in doc){
                if(docs.get("Venduto") == false) {
                    var pos: ArrayList<Double> = docs.get("Posizione") as ArrayList<Double>
                    var res = calculateDistance(pos[0], pos[1], center[0], center[1])
                    if (res <= dist) {
                        count++
                    }
                }
            }
            binding.resPos.text = getString(R.string.res_pos_stats) + count.toString()
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)

        val a = sin(latDistance / 2) * sin(latDistance / 2) + cos(Math.toRadians(lat1)) * cos(
            Math.toRadians(lat2)
        ) * sin(lonDistance / 2) * sin(lonDistance / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

}