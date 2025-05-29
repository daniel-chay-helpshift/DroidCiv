// In your Android module (com/unciv/app/UncivApplication.kt)
package com.unciv.app

import android.app.Application

class UncivApplication : Application() {
    // The static isHelpshiftSupported flag is no longer strictly needed here
    // if all core code goes through IPlatformBridge.
    // You could remove it or keep it for Android-internal checks if desired.
    // companion object {
    //     var isHelpshiftSupported = false
    //         internal set // So only AndroidPlatformBridge can set it
    // }

    override fun onCreate() {
        super.onCreate()
        // The actual Helpshift initialization is now deferred to AndroidPlatformBridge,
        // which will be initialized by your AndroidLauncher and passed to UncivGame.
        // UncivApplication.onCreate() can be used for other Android-specific app setup
        // that doesn't involve cross-module state like isHelpshiftSupported.
    }
}
