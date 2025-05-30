// File: desktop/src/com/unciv/app/desktop/DesktopPlatformBridge.kt
// (Or place in core if it has no desktop-specific dependencies)
package com.unciv.app.desktop // Or your chosen package

import com.unciv.interfaces.IPlatformBridge
import com.unciv.interfaces.HelpshiftOptions
import com.unciv.utils.Log // Assuming Log works on desktop

class DesktopPlatformBridge : IPlatformBridge {
    override fun initializePlatformIntegrations() {
        Log.debug("DesktopPlatformBridge: Initializing (no Helpshift).")
    }

    override fun isHelpshiftFeatureAvailable(): Boolean {
        return false
    }

    override fun showHelpshiftFAQs(options: HelpshiftOptions) {
        Log.debug("DesktopPlatformBridge: ShowHelpshiftFAQs called (no-op). Options: $options")
        // Optionally, show a message to the user: "Help FAQs are only available on Android."
    }

    override fun showHelpshiftConversation(options: HelpshiftOptions) {
        Log.debug("DesktopPlatformBridge: ShowHelpshiftConversation called (no-op). Options: $options")
    }

    override fun createBulkHelpshiftIssues(
        issueCount: Int,
        baseMessage: String,
        tags: List<String>,
        customFields: Map<String, String>,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        val msg = "Bulk issue creation ($issueCount) not supported on Desktop."
        Log.debug("DesktopPlatformBridge", msg)
        // Simulate async callback for consistency if MainMenuScreen expects it
        kotlin.concurrent.timer(initialDelay = 100L, period = 10000L, action = { // Won't actually repeat
            com.badlogic.gdx.Gdx.app.postRunnable { callback(false, msg) }
            this.cancel()
        })
    }
}
