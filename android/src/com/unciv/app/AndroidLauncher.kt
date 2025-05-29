// File: android/src/com/unciv/app/AndroidLauncher.kt (Modified)
package com.unciv.app

import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
// Import your AndroidPlatformBridge (ensure the package is correct if it's different)
// import com.unciv.app.AndroidPlatformBridge // Likely in the same package, or e.g., com.unciv.app.services
import com.unciv.logic.files.UncivFiles
import com.unciv.ui.components.fonts.Fonts
import com.unciv.utils.Display
import com.unciv.utils.Log
import java.io.File
// Removed java.lang.Exception as it's general; specific exceptions are better if known.

// Assuming AndroidGame.ScreenObscuredListenerActivity is defined in your AndroidGame.kt
// If not, you can remove ", AndroidGame.ScreenObscuredListenerActivity" and the related methods.
open class AndroidLauncher : AndroidApplication(), AndroidGame.ScreenObscuredListenerActivity {

    private var gameInstance: AndroidGame? = null // Renamed from 'game' to avoid potential conflicts with base class 'game' field
    private var screenObscuredListener: ((Boolean) -> Unit)? = null // For ScreenObscuredListenerActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup Android logging
        Log.backend = AndroidLogBackend(this)

        // Setup Android display
        val displayImpl = AndroidDisplay(this)
        Display.platform = displayImpl

        // Setup Android fonts
        Fonts.fontImplementation = AndroidFont()

        // Setup Android custom saver-loader
        UncivFiles.saverLoader = AndroidSaverLoader(this)
        UncivFiles.preferExternalStorage = true

        val settings = UncivFiles.getSettingsForPlatformLaunchers(filesDir.path)
        val config = AndroidApplicationConfiguration().apply { useImmersiveMode = settings.androidHideSystemUi }

        // Setup orientation, immersive mode and display cutout
        displayImpl.setOrientation(settings.displayOrientation)
        displayImpl.setCutoutFromUiThread(settings.androidCutout)

        // Create notification channels for Multiplayer notificator
        MultiplayerTurnCheckWorker.createNotificationChannels(applicationContext)

        copyMods()

        // << STEP 1: Create the AndroidPlatformBridge instance >>
        // It takes the Application context and a lambda that provides the current Activity.
        val androidBridge = AndroidPlatformBridge(application) { this@AndroidLauncher /* 'this' is the Activity */ }

        // << STEP 2: Pass the androidBridge to the AndroidGame constructor >>
        // This requires AndroidGame.kt to have its constructor modified to accept IPlatformBridge.
        gameInstance = AndroidGame(this, androidBridge) // 'this' is AndroidLauncher (Activity)

        initialize(gameInstance, config) // Pass the AndroidGame instance

        // Ensure gameInstance is not null before calling methods on it
        gameInstance?.setDeepLinkedGame(intent)
        gameInstance?.addScreenObscuredListener()
    }

    private fun copyMods() {
        val internalModsDir = File("${filesDir.path}/mods")
        val externalPath = getExternalFilesDir(null)?.path ?: return
        val externalModsDir = File("$externalPath/mods")
        try {
            if (!externalModsDir.exists()) externalModsDir.mkdirs()
            if (externalModsDir.exists()) externalModsDir.copyRecursively(internalModsDir, true)
        } catch (ex: Exception) {
            Log.error("AndroidLauncher: Failed to copy mods", ex) // Added tag for clarity
        }
    }

    override fun onPause() {
        val currentGame = this.gameInstance // Use the renamed variable
        // Check if currentGame and its properties are initialized before using them
        if (currentGame != null && currentGame.isInitializedProxy()
            && currentGame.gameInfo != null
            && currentGame.settings.multiplayer.turnCheckerEnabled
            && currentGame.files.getMultiplayerSaves().any()
        ) {
            MultiplayerTurnCheckWorker.startTurnChecker(
                applicationContext, currentGame.files, currentGame.gameInfo!!, currentGame.settings.multiplayer)
        }
        super.onPause()
    }

    override fun onResume() {
        try {
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag(MultiplayerTurnCheckWorker.WORK_TAG)
            with(NotificationManagerCompat.from(this)) {
                cancel(MultiplayerTurnCheckWorker.NOTIFICATION_ID_INFO)
                cancel(MultiplayerTurnCheckWorker.NOTIFICATION_ID_SERVICE)
            }
        } catch (ignore: Exception) {
            Log.error("AndroidLauncher: Error cancelling WorkManager tasks or notifications onResume", ignore)
        }
        super.onResume()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        gameInstance?.setDeepLinkedGame(intent) // Use the renamed variable
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // Good practice to call super first
        val saverLoader = UncivFiles.saverLoader as? AndroidSaverLoader // Safe cast
        saverLoader?.onActivityResult(requestCode, data)
    }

    // Implementation for the example ScreenObscuredListenerActivity interface
    // (Keep this if AndroidGame.ScreenObscuredListenerActivity is part of your design)
    override fun setScreenObscuredListener(listener: (Boolean) -> Unit) {
        this.screenObscuredListener = listener
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        try {
            // Notify listener about focus change (which often implies screen obscurity)
            screenObscuredListener?.invoke(!hasFocus)
        } catch (e: Exception) {
            Log.error("AndroidLauncher: Error in screenObscuredListener callback", e)
        }
    }
}
