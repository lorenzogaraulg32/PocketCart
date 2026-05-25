package it.uniupo.ebay_clone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RegistrationFragmentUserPosition : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lat: Double = 44.913333
    private var long: Double = 8.62
    private var Posizione = LatLng(lat, long)
    private lateinit var Titolo: String

    private var code = 0

    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    private lateinit var res: RegistrationFragmentUserPosition.PosResult

    interface PosResult {
        fun getPosResults(lat: Double, long: Double, address: String)
        fun openMap()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            res = context as PosResult
        } catch (e: Exception) {
            println("POS Exception: $e")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var address = view.findViewById<TextView>(R.id.pos)
        Titolo = getString(R.string.base_pos_err)
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


        view.findViewById<Button>(R.id.btn_current_pos).setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                getUserCurrentPosition()
                delay(500)
                address.text = Titolo
            }
        }

        view.findViewById<Button>(R.id.btn_pos).setOnClickListener {
            if (Titolo != getString(R.string.base_pos_err) && address.text != getString(R.string.base_pos_err)) {
                res.getPosResults(lat, long, address.text.toString())
                Toast.makeText(
                    requireContext().applicationContext,
                    getString(R.string.posizione_confermata), Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    requireContext().applicationContext,
                    getString(R.string.nessuna_posizione), Toast.LENGTH_LONG
                ).show()
            }
        }

        view.findViewById<Button>(R.id.btn_map).setOnClickListener {
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

        return inflater.inflate(R.layout.fragment_registration_user_position, container, false)
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
            println("Autorizzazzione fine non data sad")
            permissionsToRequest.add(fineLocationPermission)
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                coarseLocationPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("Autorizzazzione coarse non data sad")
            permissionsToRequest.add(coarseLocationPermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            println("C'è qualche autoriazzazione da chiedere")
            ActivityCompat.requestPermissions(
                requireContext() as Activity,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private suspend fun getUserCurrentPosition() {
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
                println("Trovata Posizione corrente")
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
                    getString(R.string.emualtor_err),
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


}