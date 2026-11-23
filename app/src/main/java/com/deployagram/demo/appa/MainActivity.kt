package com.deployagram.demo.appa

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * The one screen of AppA: enter an email, press "Recommend book", see the suggestion. "Load Config" calls AliceMessager,
 * sending "Hello" once a book has been recommended, and an empty payload before that.
 */
class MainActivity : ComponentActivity() {

    private val bookOverflow = BookOverflowClient()
    private val aliceMessagerApp = AliceMessagerClient()

    private var recommendation: BookSuggestion? = null

    private lateinit var result: TextView
    private lateinit var configResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = EditText(this).apply {
            hint = "Email address"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val recommend = Button(this).apply {
            text = "Recommend book"
            setOnClickListener { recommendBookFor(email.text.toString().trim()) }
        }
        result = TextView(this).apply {
            gravity = Gravity.START
            textSize = 18f
            setTextColor(Color.BLACK)
        }
        val loadConfig = Button(this).apply {
            text = "Load Config"
            setOnClickListener { loadConfig() }
        }
        configResult = TextView(this).apply {
            gravity = Gravity.START
            textSize = 18f
            setTextColor(Color.BLACK)
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            addView(email)
            addView(recommend)
            addView(result)
            addView(loadConfig)
            addView(configResult)
        })
    }

    private fun recommendBookFor(email: String) {
        result.text = "Asking BookOverflow…"
        lifecycleScope.launch {
            val outcome = bookOverflow.suggestionFor(email)
            outcome.onSuccess { recommendation = it }
            result.text = outcome.fold(
                onSuccess = { it.describe() },
                onFailure = { failure -> messageFor(email, failure) },
            )
        }
    }

    private fun loadConfig() {
        // "Hello" once a book has been recommended, and an empty payload before that.
        val payload = if (recommendation != null) "Hello" else null
        configResult.text = "Asking AliceMessager…"
        lifecycleScope.launch {
            val outcome = runCatching { aliceMessagerApp.sendConfig(payload) }
            configResult.text = outcome.fold(
                onSuccess = { it },
                onFailure = { failure ->
                    if (failure is AliceMessagerRefused) failure.reply else "Sorry, ${failure.message}"
                },
            )
        }
    }

    private fun messageFor(email: String, failure: Throwable): String =
        if (email.isEmpty() && failure is BookOverflowRefused && failure.statusCode == 400) {
            "You must enter a valid email!"
        } else {
            "Sorry, no suggestion right now (${failure.message})"
        }
}
