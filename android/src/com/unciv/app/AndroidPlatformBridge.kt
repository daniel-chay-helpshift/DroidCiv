// File: android/src/com/unciv/app/AndroidPlatformBridge.kt
package com.unciv.app // Or your specific Android package

import android.app.Activity
import android.app.Application
import android.util.Base64 // For Basic Auth encoding
import android.util.Log
import com.helpshift.Helpshift
import com.helpshift.UnsupportedOSVersionException
// Ensure this import path to YOUR Android module's BuildConfig is correct:
import com.unciv.app.BuildConfig // Example: If your app's package is com.unciv.app
import com.unciv.interfaces.IPlatformBridge
import com.unciv.interfaces.HelpshiftOptions

// Imports for Ktor HTTP Client & Coroutines (ensure these are in your build.gradle)
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay // For delays between API calls
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer // For serializing list of tags
import kotlinx.serialization.builtins.MapSerializer  // For serializing map for meta
import kotlinx.serialization.builtins.serializer    // For String.serializer()
import kotlinx.serialization.json.Json
import java.net.URLEncoder // For URL encoding form parameters

class AndroidPlatformBridge(
    private val application: Application,
    private val activityProvider: () -> Activity? // Function to safely get the current foreground Activity
) : IPlatformBridge {

    private var helpshiftInitializedSuccessfully = false
    private val backgroundScope = CoroutineScope(Dispatchers.IO) // Scope for background tasks

    // Ktor HTTP Client instance
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        // You can add default request configurations here if needed
        // For example, for handling API errors by default:
        // expectSuccess = false // Then check response.status manually
    }

    override fun initializePlatformIntegrations() {
        Log.i("AndroidPlatformBridge", "Initializing Helpshift...")
        val installConfig = mutableMapOf<String, Any>()
        installConfig["enableLogging"] = BuildConfig.DEBUG

        try {
            Helpshift.install(
                application,
                BuildConfig.HELPSHIFT_PLATFORM_ID,
                BuildConfig.HELPSHIFT_DOMAIN_NAME,
                installConfig
            )
            helpshiftInitializedSuccessfully = true
            Log.i("AndroidPlatformBridge", "Helpshift SDK initialized successfully.")
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

    // << IMPLEMENTATION for createBulkHelpshiftIssues >>
    override fun createBulkHelpshiftIssues(
        issueCount: Int,
        baseMessage: String,
        tags: List<String>,
        customFields: Map<String, String>,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        if (!helpshiftInitializedSuccessfully) {
            // It's good practice to ensure the callback is invoked on the main thread if it updates UI
            CoroutineScope(Dispatchers.Main).launch {
                callback(false, "Helpshift SDK not initialized.")
            }
            return
        }

        backgroundScope.launch { // Launch on a background thread (Dispatchers.IO)
            var successCount = 0
            // Cap at 100 for safety, but user requested min 100. Adjust if needed.
            val issuesToCreate = if (issueCount < 1) 1 else issueCount
            val delayBetweenRequestsMs = 2000L // 2 seconds delay - !! ADJUST BASED ON RATE LIMITS from Helpshift Support !!

            // CRITICAL WARNING: API Key should NOT be hardcoded or easily extractable in production.
            // BuildConfig is slightly better than hardcoding but still part of the APK.
            // For a true production app, this kind of bulk creation initiated by a client
            // with an embedded key is highly discouraged. This is for testing ONLY.
            val apiKey = BuildConfig.HELPSHIFT_API_KEY // Define this in your build.gradle's buildConfigField
            val domain = BuildConfig.HELPSHIFT_DOMAIN_NAME

            // Consult Helpshift API documentation for the exact issue creation URL.
            // It might be just /issues, or /platform-id/issues if issues are specific to an app at creation.
            val url = "https://api.helpshift.com/v1/$domain/issues"

            val jsonParser = Json { ignoreUnknownKeys = true } // For serializing tags and meta

            for (i in 1..issuesToCreate) {
                try {
                    val issuePayloadParts = mutableListOf<String>()
                    issuePayloadParts.add("message-body=${encodeURLParameter("$baseMessage #$i (Bulk Test - ${System.currentTimeMillis()})")}")
                    issuePayloadParts.add("email=${encodeURLParameter("bulktestuser$i@example-unciv.com")}")
                    issuePayloadParts.add("author-name=${encodeURLParameter("Unciv Bulk Tester $i")}")
                    issuePayloadParts.add("platform-type=${encodeURLParameter("android")}")

                    // Serialize tags list to JSON string
                    val tagsJsonString = jsonParser.encodeToString(ListSerializer(String.serializer()), tags)
                    issuePayloadParts.add("tags=${encodeURLParameter(tagsJsonString)}")

                    // Prepare meta field (custom fields)
                    val metaMap = customFields.toMutableMap()
                    metaMap["bulk_issue_index"] = i.toString() // Add an index as a custom field
                    val metaJsonString = jsonParser.encodeToString(MapSerializer(String.serializer(), String.serializer()), metaMap)
                    issuePayloadParts.add("meta=${encodeURLParameter(metaJsonString)}")

                    val formUrlEncodedPayload = issuePayloadParts.joinToString("&")

                    Log.d("AndroidPlatformBridge", "Creating issue $i of $issuesToCreate...")
                    // Log.v("AndroidPlatformBridge", "URL: $url, Payload: $formUrlEncodedPayload") // Verbose logging for payload

                    // Ktor HTTP POST request
                    val response: HttpResponse = httpClient.post(url) {
                        // Basic Authentication: API Key as username, password can be blank or 'X' (or anything if only username matters)
                        val authVal = "Basic " + Base64.encodeToString("$apiKey:".toByteArray(), Base64.NO_WRAP)
                        header(HttpHeaders.Authorization, authVal)
                        contentType(ContentType.Application.FormUrlEncoded)
                        setBody(formUrlEncodedPayload)
                    }

                    if (response.status == HttpStatusCode.Created) {
                        successCount++
                        Log.i("AndroidPlatformBridge", "Successfully created issue $i. Status: ${response.status}")
                    } else {
                        Log.e("AndroidPlatformBridge", "Failed to create issue $i: ${response.status} - ${response.bodyAsText()}")
                        // Optional: break or collect errors
                    }

                    if (i < issuesToCreate) { // Don't delay after the last request
                        delay(delayBetweenRequestsMs) // Respect rate limits
                    }

                } catch (e: Exception) {
                    Log.e("AndroidPlatformBridge", "Exception creating issue $i: ${e.message}", e)
                    // Optionally break loop or collect errors for the callback
                    // Ensure callback is still invoked on the main thread in case of early exit
                    withContext(Dispatchers.Main) {
                        callback(false, "Exception during bulk issue creation: ${e.message}")
                    }
                    return@launch // Exit the coroutine
                }
            }

            val finalMessage = "$successCount out of $issuesToCreate issues created."
            Log.i("AndroidPlatformBridge", finalMessage)
            // Switch back to the main thread to call the callback (if it interacts with UI/LibGDX Gdx.app)
            withContext(Dispatchers.Main) {
                callback(successCount > 0 && successCount == issuesToCreate, finalMessage)
            }
        }
    }
}

// Helper function for URL encoding, place it in this file or a utility file
private fun encodeURLParameter(value: String): String {
    return try {
        URLEncoder.encode(value, "UTF-8")
    } catch (e: Exception) {
        // Should not happen with UTF-8
        value // fallback
    }
}
