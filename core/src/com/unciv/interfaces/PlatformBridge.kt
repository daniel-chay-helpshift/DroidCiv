
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

    /**
     * Triggers the creation of a specified number of Helpshift issues for testing.
     * This operation is performed asynchronously.
     *
     * @param issueCount The number of issues to create.
     * @param baseMessage A base message for the issues (e.g., "Automated Test Issue").
     * @param tags A list of tags to apply to each issue.
     * @param customFields A map of custom fields (CIFs) to apply.
     * @param callback A function to be called upon completion or failure.
     * Parameters: (success: Boolean, message: String)
     */
    fun createBulkHelpshiftIssues(
        issueCount: Int,
        baseMessage: String,
        tags: List<String>,
        customFields: Map<String, String>, // Assuming simple string CIFs for this example
        callback: (success: Boolean, message: String) -> Unit
    )
}
