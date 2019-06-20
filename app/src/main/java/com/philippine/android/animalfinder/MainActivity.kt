package com.philippine.android.animalfinder

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity(), FragmentInterface {

    var listData: MutableList<MapsViewModel> = ArrayList()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val SETUP_PERMISSION_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            onNavigationInteraction(MapsFragment.newInstance(), Bundle())
        }
    }

    override fun onNavigationInteraction(fragment: Fragment, bundle: Bundle) {
        supportFragmentManager.popBackStack()
        val ft = supportFragmentManager.beginTransaction()
        fragment.arguments = bundle
        ft.replace(R.id.container, fragment)
        ft.commitNowAllowingStateLoss()
    }

    override fun onBackPressed() {
        var fragmentz = getVisibleFragment()
        if(fragmentz != null) {
            for (fragment in supportFragmentManager.fragments) {
                if (fragment != null) {
                    supportFragmentManager.beginTransaction().remove(fragment).commit()
                }
            }
            onNavigationInteraction(MapsFragment.newInstance(), Bundle())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            if(requestCode == SETUP_PERMISSION_REQUEST_CODE || LOCATION_PERMISSION_REQUEST_CODE == requestCode){
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onBackPressed()
                }
            }
    }

    private fun getVisibleFragment(): Fragment? {
        val fragmentManager = this@MainActivity.supportFragmentManager
        val fragments = fragmentManager.fragments
        for (fragment in fragments) {
            if (fragment != null && fragment.isVisible)
                return fragment
        }
        return null
    }
}
