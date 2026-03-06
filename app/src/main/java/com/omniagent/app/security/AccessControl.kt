package com.omniagent.app.security

import android.content.Context
import android.content.SharedPreferences
import com.omniagent.app.core.model.UserRole

/**
 * Role-Based Access Control (RBAC) Manager.
 * Manages Admin/User roles with secure local storage.
 */
object AccessControl {

    private const val PREFS_NAME = "omniagent_access_control"
    private const val KEY_CURRENT_ROLE = "current_role"
    private const val KEY_ADMIN_PIN_HASH = "admin_pin_hash"
    private const val DEFAULT_ADMIN_PIN = "1234" // Default PIN — user should change

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Set default admin PIN if not exists
        if (!prefs.contains(KEY_ADMIN_PIN_HASH)) {
            setAdminPin(DEFAULT_ADMIN_PIN)
        }
    }

    /**
     * Get current user role.
     */
    fun getCurrentRole(): UserRole {
        val roleStr = prefs.getString(KEY_CURRENT_ROLE, "user") ?: "user"
        return UserRole.fromString(roleStr)
    }

    /**
     * Attempt to authenticate as admin with PIN.
     */
    fun authenticateAdmin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_ADMIN_PIN_HASH, "") ?: ""
        val inputHash = hashPin(pin)
        return if (storedHash == inputHash) {
            prefs.edit().putString(KEY_CURRENT_ROLE, "admin").apply()
            true
        } else {
            false
        }
    }

    /**
     * Switch to user role.
     */
    fun switchToUser() {
        prefs.edit().putString(KEY_CURRENT_ROLE, "user").apply()
    }

    /**
     * Set new admin PIN (requires current admin access).
     */
    fun setAdminPin(newPin: String): Boolean {
        if (newPin.length < 4) return false
        prefs.edit().putString(KEY_ADMIN_PIN_HASH, hashPin(newPin)).apply()
        return true
    }

    /**
     * Check if current role can access admin features.
     */
    fun canAccessAdminFeatures(): Boolean {
        return getCurrentRole() == UserRole.ADMIN
    }

    /**
     * Check if user can clear logs (admin only).
     */
    fun canClearLogs(): Boolean = canAccessAdminFeatures()

    /**
     * Check if user can view encrypted data (admin only).
     */
    fun canViewDecryptedData(): Boolean = canAccessAdminFeatures()

    /**
     * Simple hash function for PIN storage.
     * Uses SHA-256 for secure local storage.
     */
    private fun hashPin(pin: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
