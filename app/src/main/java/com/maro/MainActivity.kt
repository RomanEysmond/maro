package com.maro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.navigation.NavigationRoot
import com.maro.notifications.AppVisibility
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    // A chat to open, requested by tapping a message notification; cleared once navigation has handled it.
    private val openChatRequest = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Not after a recreation (rotation): the same intent would open the chat a second time.
        if (savedInstanceState == null) openChatRequest.value = intent.chatIdExtra()
        setContent {
            val chatToOpen by openChatRequest.collectAsState()
            MaroTheme {
                NavigationRoot(
                    openChatRequest = chatToOpen,
                    onOpenChatRequestHandled = { openChatRequest.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.chatIdExtra()?.let { openChatRequest.value = it }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.onActivityStarted()
    }

    override fun onStop() {
        AppVisibility.onActivityStopped()
        super.onStop()
    }

    private fun Intent.chatIdExtra(): String? = getStringExtra(EXTRA_CHAT_ID)

    companion object {
        const val EXTRA_CHAT_ID = "com.maro.extra.CHAT_ID"
    }
}
