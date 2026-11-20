package com.tabitos.app.features.shell

import androidx.lifecycle.ViewModel
import com.tabitos.app.core.startup.DeferredSource
import com.tabitos.app.core.startup.StartupSequence
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Lets the composition root tell the startup sequence what the user sees (spec 027). */
@HiltViewModel
class StartupViewModel @Inject constructor(private val sequence: StartupSequence) : ViewModel() {
    fun todayShown() = sequence.markTodayShown()
    fun request(sources: Set<DeferredSource>) = sequence.request(sources)
}
