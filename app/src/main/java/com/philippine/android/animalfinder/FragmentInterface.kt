package com.philippine.android.animalfinder

import android.os.Bundle
import android.support.v4.app.Fragment

interface FragmentInterface {
    fun onNavigationInteraction(fragment: Fragment, bundle: Bundle) {}
}