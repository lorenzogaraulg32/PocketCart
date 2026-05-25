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
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MapsFragment : Fragment() {

    private lateinit var mMap: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lat: Double = 44.913333
    private var long: Double = 8.62
    private var Posizione = LatLng(lat, long)
    private var Titolo: String = "nothing"
    private var marker: Marker? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    private lateinit var res: MapsResult

    interface MapsResult {
        fun getMapsResults(titolo: String, latitude: Double, longitude: Double)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            res = context as MapsResult
            getUserCurrentPosition()
        } catch (e: Exception) {
            println("Maps Exception: $e")
        }
    }


    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        setMarker()
        mMap.setOnMapClickListener {
            MainScope().launch {
                Posizione = withContext(Dispatchers.Default) {
                    lat = it.latitude
                    long = it.longitude
                    LatLng(lat, long)
                }
                Titolo = withContext(Dispatchers.Default) {
                    setTitle(Posizione)
                }
                setMarker()
            }
        }

        view?.findViewById<Button>(R.id.conf_btn)?.setOnClickListener {
            //ritorno lat, long,indirizzo e poi kill il fragment
            res.getMapsResults(Titolo, lat, long)
            parentFragmentManager.beginTransaction().remove(this).commit()
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        MainScope().launch {
            Posizione = withContext(Dispatchers.Default) {
                LatLng(lat, long)
            }
            Titolo = withContext(Dispatchers.Default) {
                setTitle(Posizione)
            }
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            mapFragment?.getMapAsync(callback)
        }

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

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(),
                    getString(R.string.pos_auth_not_err), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.pos_auth_err),
                    Toast.LENGTH_LONG
                ).show()
            }
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
                    setMarker()
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

    private fun setMarker(): Marker {
        marker?.remove()
        marker = mMap.addMarker(MarkerOptions().position(Posizione).title(Titolo))!!
        marker?.showInfoWindow()
        val showAddress = view?.findViewById<TextView>(R.id.pos_text)
        showAddress?.text = Titolo
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Posizione, 18f))
        return marker as Marker
    }

    @Suppress("DEPRECATION")
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
