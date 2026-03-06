package com.omniagent.app.di

import android.content.Context
import com.omniagent.app.data.local.OmniAgentDatabase
import com.omniagent.app.data.repository.OmniAgentRepository
import com.omniagent.app.domain.repository.AnalysisRepository
import com.omniagent.app.kernel.PythonKernelManager
import com.omniagent.app.security.AccessControl
import com.omniagent.app.security.FileSandbox

/**
 * Manual Dependency Injection Container.
 * Centralizes creation and management of core dependencies,
 * making the architecture "DI-Ready" without needing Hilt/Dagger yet.
 */
class AppContainer(private val applicationContext: Context) {

    // === Core Initializations ===
    init {
        AccessControl.initialize(applicationContext)
        FileSandbox.initialize(applicationContext.filesDir)
    }

    // === Database Layer ===
    val database: OmniAgentDatabase by lazy {
        OmniAgentDatabase.getDatabase(applicationContext)
    }

    // === AI Kernel Layer ===
    val pythonKernelManager: PythonKernelManager by lazy {
        PythonKernelManager()
    }

    // === Repository Layer (Domain Implementations) ===
    val analysisRepository: AnalysisRepository by lazy {
        OmniAgentRepository(
            analysisLogDao = database.analysisLogDao(),
            kernelManager = pythonKernelManager
        )
    }
}
