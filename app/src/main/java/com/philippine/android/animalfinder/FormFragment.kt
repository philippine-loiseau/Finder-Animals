package com.philippine.android.animalfinder

import `in`.madapps.placesautocomplete.PlaceAPI
import `in`.madapps.placesautocomplete.adapter.PlacesAutoCompleteAdapter
import `in`.madapps.placesautocomplete.model.Place
import android.Manifest.permission
import android.app.Activity.RESULT_OK
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_form.*


class FormFragment : Fragment() {

    private var objectData: MapsViewModel? = null
    private lateinit var viewModel: MapsViewModel

    companion object {
        fun newInstance() = FormFragment()
        private const val REQUEST_CAPTURE_IMAGE = 100
    }

    private lateinit var listener: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpSpinners()

        val placesApi = PlaceAPI.Builder().apiKey(listener.resources.getString(R.string.google_maps_key)).build(listener)
        autoCompleteEditText.setAdapter(PlacesAutoCompleteAdapter(listener, placesApi))
        autoCompleteEditText.onItemClickListener =
                AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val place = parent.getItemAtPosition(position) as Place
                    autoCompleteEditText.setText(place.description)
                }

        profile_pic.setOnClickListener { openCameraIntent() }

        saveFab.setOnClickListener {

            viewModel.race = race_spinner.selectedItem.toString()
            viewModel.taille = taille_spinner.selectedItem.toString()
            viewModel.animal = animal_spinner.selectedItem.toString()
            viewModel.adresse = autoCompleteEditText.text.toString()
            viewModel.description = descriptionEditText.text.toString()
            viewModel.sante = santeEditText.text.toString()
            val bitmap = (profile_pic.drawable as BitmapDrawable).bitmap
            viewModel.profilePic = ImageUtil.convert(bitmap)

            var bundle = Bundle()
            bundle.putString("data", Gson().toJson(viewModel))
            listener.onNavigationInteraction(MapsFragment.newInstance(), bundle)

        }

        if (arguments?.getString("data") != null) {
            objectData = Gson().fromJson<MapsViewModel>(arguments?.getString("data"), MapsViewModel::class.java)
            if(objectData !=null) {
                autoCompleteEditText.setText(objectData?.adresse)
            }
        }


    }

    private fun setUpSpinners() {

        ArrayAdapter.createFromResource(
                context!!,
                R.array.animals_array,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            animal_spinner.adapter = adapter
        }

        animal_spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, position: Int, id: Long) {
                val array = if (position == 0) R.array.race_cat_array else R.array.race_dog_array
                animal_img.setImageDrawable(ResourcesCompat.getDrawable(listener.resources, if (position == 0) R.drawable.icons8_cat_64 else R.drawable.icons8_dog_64, null))

                ArrayAdapter.createFromResource(
                        context!!,
                        array,
                        android.R.layout.simple_spinner_item
                ).also { adapter2 ->
                    adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    race_spinner.adapter = adapter2
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        ArrayAdapter.createFromResource(
                context!!,
                R.array.taille_array,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            taille_spinner.adapter = adapter
        }
    }

    private fun openCameraIntent() {

        if (ContextCompat.checkSelfPermission(listener, permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(listener, arrayOf(permission.CAMERA), 10)
            return
        }

        val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (pictureIntent.resolveActivity(listener.packageManager) != null) {
            startActivityForResult(pictureIntent, REQUEST_CAPTURE_IMAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            10 -> openCameraIntent()
            else -> {
            }
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MapsViewModel::class.java)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CAPTURE_IMAGE && resultCode == RESULT_OK && data != null && data.extras != null) {
            val imageBitmap = data.extras!!.get("data") as Bitmap
            profile_pic.setImageBitmap(imageBitmap)
        }
    }

}
