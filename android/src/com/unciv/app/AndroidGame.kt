// File: android/src/com/unciv/app/AndroidGame.kt (Modified)
package com.unciv.app

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.ViewTreeObserver
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidGraphics
import com.badlogic.gdx.math.Rectangle
import com.unciv.UncivGame
import com.unciv.interfaces.IHelpshiftPlatformBridge // << ADD IMPORT
import com.unciv.logic.event.EventBus
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.basescreen.UncivStage
import com.unciv.utils.Concurrency
import com.unciv.utils.Log

class AndroidGame(
    private val activity: Activity, // Keep for Android-specific needs within this class
    platformBridge: IHelpshiftPlatformBridge // << ADD IPlatformBridge parameter
) : UncivGame(platformBridge, isConsoleMode = false) { // << PASS to UncivGame super

    private var lastOrientation = activity.resources.configuration.orientation

    init {
        // The UncivGame superclass constructor is called first.
        // UncivGame.Current is set in UncivGame.create().
        // UncivGame.Current.platformBridge.initializePlatformIntegrations() is called in UncivGame.create().
    }

    fun addScreenObscuredListener() {
        try { // Add try-catch for safety
            val contentView = (Gdx.graphics as AndroidGraphics).view
            contentView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                private var lastFrame: Rect? = null
                private var lastVisibleStage: Rectangle? = null

                override fun onGlobalLayout() {
                    if (!isInitializedProxy() || screen == null) return

                    val currentScreen = screen as? BaseScreen ?: return
                    val stage = currentScreen.stage as? UncivStage ?: return
                    val localContentView = contentView ?: return // Check if contentView is still valid

                    val currentFrame = Rect()
                    localContentView.getWindowVisibleDisplayFrame(currentFrame)

                    val horizontalRatio = stage.width / localContentView.width.toFloat()
                    val verticalRatio = stage.height / localContentView.height.toFloat()

                    val visibleStage = Rectangle(
                        currentFrame.left * horizontalRatio,
                        (localContentView.height - currentFrame.bottom) * verticalRatio,
                        currentFrame.width() * horizontalRatio,
                        currentFrame.height() * verticalRatio
                    )

                    if (lastFrame == currentFrame && lastVisibleStage == visibleStage) return
                    lastFrame = Rect(currentFrame)
                    lastVisibleStage = Rectangle(visibleStage)

                    val currentOrientation = activity.resources.configuration.orientation
                    if (lastOrientation != currentOrientation) {
                        lastOrientation = currentOrientation
                        return
                    }

                    Concurrency.runOnGLThread {
                        EventBus.send(UncivStage.VisibleAreaChanged(visibleStage))
                    }
                }
            })
        } catch (e: Exception) {
            Log.error("Error in addScreenObscuredListener", e)
        }
    }

    fun setDeepLinkedGame(intent: Intent) {
        try { // Add try-catch
            super.deepLinkedMultiplayerGame = if (intent.action != Intent.ACTION_VIEW) null else {
                val uri: Uri? = intent.data
                // Ensure consistency with how UncivGame.tryLoadDeepLinkedGame expects the parameter
                uri?.getQueryParameter("id") ?: uri?.getQueryParameter("gameID")
            }
        } catch (e: Exception) {
            Log.error("Error in setDeepLinkedGame", e)
        }
    }

    fun isInitializedProxy(): Boolean = super.isInitialized

    // Define the interface AndroidLauncher might implement for screen obscured events
    interface ScreenObscuredListenerActivity {
        fun setScreenObscuredListener(listener: (Boolean) -> Unit)
    }
}
