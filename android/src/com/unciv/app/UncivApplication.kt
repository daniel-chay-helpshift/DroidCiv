
package com.unciv.app // Or Unciv's main Android package

import android.app.Application
import android.util.Log
import com.helpshift.Helpshift
import com.helpshift.UnsupportedOSVersionException
// import com.unciv.app.android.BuildConfig // Assuming BuildConfig is generated in this package
import java.util.HashMap

class UncivApplication : Application() {

    companion object {
        var isHelpshiftSupported = false
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Helpshift Initialization
        val installConfig = HashMap<String, Any>()
        installConfig["enableLogging"] = BuildConfig.DEBUG // Enable for debug builds, disable for release [45]
        // installConfig["isForChina"] = true // Uncomment and set to true if targeting China [30]
        // Add other install-time configurations from Table 1 as needed

        try {
            Helpshift.install(
                this,
                BuildConfig.HELPSHIFT_PLATFORM_ID,      // From buildConfigField
                BuildConfig.HELPSHIFT_DOMAIN_NAME,    // From buildConfigField
                installConfig
            )
            isHelpshiftSupported = true
            Log.i("UncivApplication", "Helpshift SDK initialized successfully.")

            // Set the event listener immediately after successful initialization
            Helpshift.setHelpshiftEventsListener(MyHelpshiftEventsHandler()) // [43, 54]
            Log.i("UncivApplication", "HelpshiftEventsListener registered.")

        } catch (e: UnsupportedOSVersionException) { // [30, 43]
            isHelpshiftSupported = false
            Log.e("UncivApplication", "Helpshift SDK is not supported on this OS version (API < 24). Fallback support mechanism should be used.", e)
            // SDK will be non-operational.
        } catch (e: Exception) {
            isHelpshiftSupported = false
            // Catch any other unexpected initialization errors
            Log.e("UncivApplication", "Critical error installing Helpshift SDK.", e)
        }
    }
}
