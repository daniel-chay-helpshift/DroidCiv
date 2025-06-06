// File: android/src/com/unciv/app/AndroidPlatformBridge.kt
// (Create a new package e.g. com.unciv.app.services or place it in com.unciv.app)
package com.unciv.app // Or your specific Android package

import android.app.Activity
import android.app.Application
import android.util.Log
import com.helpshift.Helpshift
import com.helpshift.UnsupportedOSVersionException
// Ensure this import path to YOUR Android module's BuildConfig is correct:
// Or, if your Android app module has a different package ID for BuildConfig, e.g.:
// import com.yourcompany.uncivandroid.BuildConfig 
import com.unciv.interfaces.IHelpshiftPlatformBridge
import com.unciv.interfaces.HelpshiftOptions

class AndroidHelpshiftPlatformBridge(
    private val application: Application,
    private val activityProvider: () -> Activity? // Function to safely get the current foreground Activity
) : IHelpshiftPlatformBridge {

    private var helpshiftInitializedSuccessfully = false

    override fun initializePlatformIntegrations() {
        Log.i("AndroidPlatformBridge", "Initializing Helpshift...")
        val installConfig = mutableMapOf<String, Any>()

        // IMPORTANT: Verify this BuildConfig.DEBUG path. 
        // It should point to the BuildConfig generated for your Android app module.
        installConfig["enableLogging"] = BuildConfig.DEBUG

        try {
            Helpshift.install(
                application,
                BuildConfig.HELPSHIFT_PLATFORM_ID,  // From your Android module's build.gradle -> buildConfigField
                BuildConfig.HELPSHIFT_DOMAIN_NAME, // From your Android module's build.gradle -> buildConfigField
                installConfig
            )
            helpshiftInitializedSuccessfully = true
            Log.i("AndroidPlatformBridge", "Helpshift SDK initialized successfully.")

            // MyHelpshiftEventsHandler must be in your Android module
            Helpshift.setHelpshiftEventsListener(MyHelpshiftEventsHandler())
            Log.i("AndroidPlatformBridge", "HelpshiftEventsListener registered.")

        } catch (e: UnsupportedOSVersionException) {
            helpshiftInitializedSuccessfully = false
            Log.e("AndroidPlatformBridge", "Helpshift SDK is not supported on this OS version.", e)
        } catch (e: Exception) {
            helpshiftInitializedSuccessfully = false
            Log.e("AndroidPlatformBridge", "Critical error installing Helpshift SDK.", e)
        }
    }

    override fun isHelpshiftFeatureAvailable(): Boolean {
        return helpshiftInitializedSuccessfully
    }

    private fun getCurrentActivity(): Activity? {
        val activity = activityProvider()
        if (activity == null) {
            Log.e("AndroidPlatformBridge", "Cannot perform Helpshift action: Current Activity is null.")
        }
        return activity
    }

    override fun showHelpshiftFAQs(options: HelpshiftOptions) {
        if (helpshiftInitializedSuccessfully) {
            getCurrentActivity()?.let { currentActivity ->
                Log.d("AndroidPlatformBridge", "Showing Helpshift FAQs with options: $options")
                Helpshift.showFAQs(currentActivity, options)
            }
        } else {
            Log.w("AndroidPlatformBridge", "Helpshift not available. Cannot show FAQs.")
        }
    }

    override fun showHelpshiftConversation(options: HelpshiftOptions) {
        if (helpshiftInitializedSuccessfully) {
            getCurrentActivity()?.let { currentActivity ->
                Log.d("AndroidPlatformBridge", "Showing Helpshift Conversation with options: $options")
                Helpshift.showConversation(currentActivity, options)
            }
        } else {
            Log.w("AndroidPlatformBridge", "Helpshift not available. Cannot show Conversation.")
        }
    }
}
