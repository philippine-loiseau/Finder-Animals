package com.philippine.android.animalfinder

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import java.util.*

class MapsFragment : Fragment(), OnMapReadyCallback {

    companion object {
        fun newInstance() = MapsFragment()
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val SETUP_PERMISSION_REQUEST_CODE = 3
    }

    private lateinit var viewModel: MapsViewModel
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var listener: MainActivity
    //    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.maps_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(listener)

//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(p0: LocationResult) {
//                super.onLocationResult(p0)
//            }
//        }

        createLocationRequest()

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MapsViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()

        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentInterface) {
            listener = context as MainActivity
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    locationUpdateState = true
                    startLocationUpdates()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

//        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMapClickListener { latLng ->
            var geocoder = Geocoder(listener, Locale("fr", "FR"))
            var addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.size > 0) {
                val address = addresses[0].getAddressLine(0)
                val city = addresses[0].locality
                val country = addresses[0].countryName

                var objectData = MapsViewModel()
                objectData.adresse = "$address $city $country"
                val bundle = Bundle()
                bundle.putString("data", Gson().toJson(objectData))

                listener.onNavigationInteraction(FormFragment.newInstance(), bundle)
            }
        }

        setUpMap()
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(context!!, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(listener, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), SETUP_PERMISSION_REQUEST_CODE)
            return
        }


        map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        fusedLocationClient.lastLocation.addOnSuccessListener(listener) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
            }
        }
        map.setInfoWindowAdapter(CustomInfoWindowGoogleMap(listener))

        if (arguments?.getString("data") != null) {
            viewModel = Gson().fromJson<MapsViewModel>(arguments?.getString("data"), MapsViewModel::class.java)

            listener.listData.add(viewModel)
            placeMarkerOnMap(listener.listData)
        }


    }

    private fun placeMarkerOnMap(listData: MutableList<MapsViewModel>) {

        for (model in listData) {
            if (model.adresse != null && model.animal != null && model.animal!!.isNotBlank()) {
                var addressLocation = getLocationFromAddress(listener, model.adresse!!)
                if (addressLocation != null) {
                    val markerOptions = MarkerOptions()
                            .position(addressLocation)
                            .title(model.animal)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

                    val marker = map.addMarker(markerOptions)
                    marker.tag = model

                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context!!, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(listener, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
        fusedLocationClient.lastLocation.addOnSuccessListener(listener) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
            }
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(listener)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(listener, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }
    }

    private fun getLocationFromAddress(context: Context, strAddress: String): LatLng? {
        val coder = Geocoder(context)
        var address: List<Address>?
        var p1: LatLng? = null

        try {
            address = coder.getFromLocationName(strAddress, 5)
            if (address == null) {
                return null
            }
            var location = address[0]
            location.latitude
            location.longitude

            p1 = LatLng(location.latitude, location.longitude)
        } catch (e: Exception) {
            e.printStackTrace(); }

        return p1
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            SETUP_PERMISSION_REQUEST_CODE -> setUpMap()
            LOCATION_PERMISSION_REQUEST_CODE -> startLocationUpdates()
            else -> { }
        }
    }

    class CustomInfoWindowGoogleMap(private val context: Context) : GoogleMap.InfoWindowAdapter {

        @SuppressLint("InflateParams")
        override fun getInfoContents(p0: Marker?): View {

            val mInfoView = (context as Activity).layoutInflater.inflate(R.layout.custom_infowindow, null)
            val mInfoWindow: MapsViewModel = p0?.tag as MapsViewModel
//
            (mInfoView.findViewById(R.id.nameTxt) as TextView).text = mInfoWindow.animal
            (mInfoView.findViewById(R.id.mobileTxt) as TextView).text = mInfoWindow.description
            (mInfoView.findViewById(R.id.addressTxt) as TextView).text = mInfoWindow.sante
            if(mInfoWindow.profilePic != null) {
                (mInfoView.findViewById(R.id.clientPic) as ImageView).setImageDrawable(BitmapDrawable(context.resources, ImageUtil.convert(mInfoWindow.profilePic!!)))
            }
            when (mInfoWindow.animal.toString()) {
                "Cat" -> (mInfoView.findViewById(R.id.catOrDogImg) as ImageView).setImageDrawable(context.resources.getDrawable(R.drawable.icons8_cat_64))
                "Dog" -> (mInfoView.findViewById(R.id.catOrDogImg) as ImageView).setImageDrawable(context.resources.getDrawable(R.drawable.icons8_dog_64))
                else -> {
                }
            }

            return mInfoView
        }

        override fun getInfoWindow(p0: Marker?): View? {
            return null
        }
    }

}
