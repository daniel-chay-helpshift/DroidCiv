package com.unciv.app

import android.util.Log
import com.helpshift.HelpshiftAuthenticationFailureReason // [43]
import com.helpshift.HelpshiftEvent // Import necessary constants [43, 54]
import com.helpshift.HelpshiftEventsListener // [43, 54]

class MyHelpshiftEventsHandler : HelpshiftEventsListener {
    private val TAG = "UncivHelpshiftEvents"

    override fun onEventOccurred(eventName: String, data: Map<String, Any>) { // [43, 54]
        Log.d(TAG, "Helpshift Event Occurred: $eventName, Data: $data")
        when (eventName) {
            HelpshiftEvent.SDK_SESSION_STARTED -> { // [54]
                Log.i(TAG, "Helpshift SDK session started.")
                // Potentially log this for analytics or app state tracking
            }
            HelpshiftEvent.CONVERSATION_START -> { // [54]
                val message = data as? String // [54]
                Log.i(TAG, "User started a new conversation. Initial message: $message")
                // Useful for analytics on conversation initiation
            }
            HelpshiftEvent.MESSAGE_ADD -> { // [54]
                val messageType = data as? String // [54]
                val messageBody = data as? String // [54]
                Log.i(TAG, "User added a message. Type: $messageType, Body: $messageBody")
                // Can distinguish between text and attachments
            }
            HelpshiftEvent.AGENT_MESSAGE_RECEIVED -> { // Available from SDK 10.3.0+ [41, 54]
                val messageBody = data as? String // [54]
                // val attachments = data as? List<Map<String, Any>> // [54]
                Log.i(TAG, "Agent message received. Body: $messageBody")
                // This event is NOT triggered for bot messages or automations [54]
                // Unciv could use this to show a custom in-app notification if user is not on chat screen
            }
            HelpshiftEvent.WIDGET_TOGGLE -> { // [54]
                val isVisible = data as? Boolean?: false // [54]
                Log.i(TAG, "Helpshift widget visibility changed. IsVisible: $isVisible")
                // Useful for knowing when user enters/exits Helpshift UI
            }
            HelpshiftEvent.CONVERSATION_STATUS -> { // [54]
                val issueId = data as? String // [54]
                val isIssueOpen = data as? Boolean?: false // [54]
                Log.i(TAG, "Conversation status update. Issue ID: $issueId, IsOpen: $isIssueOpen")
            }
            HelpshiftEvent.RECEIVED_UNREAD_MESSAGE_COUNT -> { // For polling unread count [43]
                val count = data as? Int?: 0 // [43]
                val fromCache = data as? Boolean?: false // [43]
                Log.i(TAG, "Unread message count: $count (From Cache: $fromCache)")
                // Update UI badge for help icon
            }
            HelpshiftEvent.CONVERSATION_END -> Log.i(TAG, "Conversation ended.") // [54]
            HelpshiftEvent.CONVERSATION_RESOLVED -> Log.i(TAG, "Conversation resolved by agent.") // [54]
            HelpshiftEvent.CONVERSATION_REJECTED -> Log.i(TAG, "Conversation rejected by agent.") // [54]
            HelpshiftEvent.ACTION_CLICKED -> { // [54]
                val actionType = data as? String // [54]
                val actionData = data as? String // [54]
                Log.i(TAG, "User clicked an action card. Type: $actionType, Action: $actionData")
            }
            // Handle other events as needed
            else -> {
                Log.w(TAG, "Received unhandled Helpshift event: $eventName")
            }
        }
    }

    override fun onUserAuthenticationFailure(reason: HelpshiftAuthenticationFailureReason) { // [43]
        Log.e(TAG, "Helpshift User Authentication Failed. Reason: $reason")
        // E.g., if HMAC token is invalid, prompt user to re-login to Unciv or notify server
    }
}
