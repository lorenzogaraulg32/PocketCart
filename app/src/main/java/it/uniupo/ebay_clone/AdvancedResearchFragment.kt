package it.uniupo.ebay_clone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
import java.util.Locale


class AdvancedResearchFragment : Fragment() {

    //posizione
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lat: Double = 0.00
    private var long: Double = 0.00
    private var Posizione = LatLng(lat, long)
    var pos: ArrayList<Double> = ArrayList(2)
    private lateinit  var Titolo: String
    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    //gestione fragment
    private var code = 0
    private lateinit var res: PosResult

    //gestione ricerca
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User


    private var searchResultHM: HashMap<Item, String> = hashMapOf()
    private var resultAR: ArrayList<String> = arrayListOf()

    interface PosResult {
        fun getSearchResults(result : ArrayList<String>)
        fun openMap()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            res = context as PosResult
        } catch (e: Exception) {
            println("ADV Exception: $e")
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Titolo = getString(R.string.reg_pos_placeholder)
        val address = view.findViewById<TextView>(R.id.pos)
        address.text = Titolo

        code = arguments?.getString("code").toString().toInt()
        println("Codice = $code")
        if (code == 1) {
            Titolo = arguments?.getString("address").toString()
            lat = arguments?.getDouble("lat")!!.toDouble()
            lat = arguments?.getDouble("long")!!.toDouble()
            address.text = Titolo
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        auth = FirebaseAuth.getInstance()
        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
            }
        }

        //bottone ricerca
        view.findViewById<Button>(R.id.search_btn).setOnClickListener{
                pos.add(lat)
                pos.add(long)
                val dist: Double
                if (!view.findViewById<TextInputEditText>(R.id.search_dist_field).text.isNullOrEmpty()) {
                    dist = view.findViewById<TextInputEditText>(R.id.search_dist_field).toString().toDouble()
                } else {
                    dist = -1.0
                }
                val research = Research(
                    auth.currentUser!!.uid,
                    view.findViewById<TextInputEditText>(R.id.search_name_field).text.toString(),
                    view.findViewById<TextInputEditText>(R.id.search_price_min).text.toString(),
                    view.findViewById<TextInputEditText>(R.id.search_price_max).text.toString(),
                    view.findViewById<Switch>(R.id.switch_shipping).isChecked,
                    pos,
                    address.text.toString(),
                    dist
                )

            MainScope().launch {
                withContext(Dispatchers.IO) {
                    searchResultHM = researchResult(research)
                    println("Risultati ricerca HashMap = $searchResultHM")
                    resultAR = getItemId(searchResultHM)
                    if (view.findViewById<CheckBox>(R.id.save_search).isChecked) {
                        CoroutineScope(Dispatchers.IO).launch { research.saveSearch(research, db) }
                    }
                    res.getSearchResults(resultAR)
                }
            }
        }



        //bottone per aprire la mappa
        view.findViewById<Button>(R.id.pos_btn).setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestLocationPermission()
            } else {
                res.run { openMap() }
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_advanced_research, container, false)
    }

    private fun requestLocationPermission() {
        val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                fineLocationPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(fineLocationPermission)
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                coarseLocationPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(coarseLocationPermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireContext() as Activity,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getUserCurrentPosition() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                MainScope().launch {
                    Posizione = withContext(Dispatchers.Default) {
                        lat = location.latitude
                        long = location.longitude
                        LatLng(lat, long)
                    }
                    Titolo = withContext(Dispatchers.Default) {
                        setTitle(Posizione)
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_pos_err),
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    }

    private fun setTitle(currentLoc: LatLng): String {
        var title = ""

        val geoCoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addrList =
                geoCoder.getFromLocation(currentLoc.latitude, currentLoc.longitude, 3)!!
            val myAddress = addrList[0]
            title = myAddress.getAddressLine(0).toString()
        } catch (e: Exception) {
            println("Exception : $e")
        }
        return title
    }


    private fun getItemId(map: HashMap<Item, String>): ArrayList<String> {
        val res = arrayListOf<String>()
        for (items in map) {
            res.add(items.value)
        }
        for (id in res) {
            println(id)
        }
        return res
    }

    private suspend fun researchResult(research: Research): HashMap<Item, String> {
        val map: HashMap<Item, String> = research.queryNome(db, currentUser)

        if (map.isEmpty()) {
            return map
        }

        var resMap: HashMap<Item, String> = research.queryShipping(map)

        if (resMap.isEmpty()) {
            return hashMapOf()
        } else {
            map.clear()
            map.putAll(resMap)
        }

        if (research.searchPos?.isNotEmpty() == true && research.searchDistance != -1.0 && lat != 0.0 && long != 0.0) {
            resMap = research.queryPos(map)

            if (resMap.isEmpty()) {
                return resMap
            } else {
                map.clear()
                map.putAll(resMap)
            }
        }

        if (research.searchPriceMin?.isNotEmpty() == true || research.searchPriceMax?.isNotEmpty() == true) {
            resMap = research.queryPrice(map)

            if (resMap.isEmpty()) {
                return resMap
            } else {
                map.clear()
                map.putAll(resMap)
            }
        }

        return map
    }


}