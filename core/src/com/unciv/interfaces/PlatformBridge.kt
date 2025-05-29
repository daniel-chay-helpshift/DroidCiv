
package com.unciv.interfaces

import kotlin.collections.Map // Use Kotlin's Map
typealias HelpshiftOptions = Map<String, Any>

interface IPlatformBridge {
    /**
     * Initializes any platform-specific SDKs like Helpshift.
     * Called early in the game's lifecycle.
     */
    fun initializePlatformIntegrations()

    /**
     * Checks if the Helpshift UI (FAQs, Contact Us) can be shown.
     * This would be true on Android if the SDK initialized successfully, false otherwise.
     */
    fun isHelpshiftFeatureAvailable(): Boolean

    /**
     * Shows the Helpshift FAQs screen.
     * @param options A map for any API configurations (e.g., custom metadata, tags).
     */
    fun showHelpshiftFAQs(options: HelpshiftOptions = emptyMap())

    /**
     * Shows the Helpshift Conversation/Contact Us screen.
     * @param options A map for any API configurations (e.g., CIFs, tags).
     */
    fun showHelpshiftConversation(options: HelpshiftOptions)

    // You can add other platform-specific methods here if needed in the future
}
