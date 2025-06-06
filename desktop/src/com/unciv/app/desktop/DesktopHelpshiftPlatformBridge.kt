// File: desktop/src/com/unciv/app/desktop/DesktopPlatformBridge.kt
// (Or place in core if it has no desktop-specific dependencies)
package com.unciv.app.desktop // Or your chosen package

import com.unciv.interfaces.IHelpshiftPlatformBridge
import com.unciv.interfaces.HelpshiftOptions
import com.unciv.utils.Log // Assuming Log works on desktop

class DesktopHelpshiftPlatformBridge : IHelpshiftPlatformBridge {
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
}
