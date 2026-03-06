package com.omniagent.app

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.omniagent.app.security.AccessControl
import com.omniagent.app.security.FileSandbox

/**
 * OmniAgent Application — Initializes all subsystems on startup.
 */
class OmniAgentApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Python runtime (Chaquopy)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Initialize security subsystems
        AccessControl.initialize(this)
        FileSandbox.initialize(filesDir)
    }
}
