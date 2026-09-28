package com.arnavpaul.smartcalc

import android.Manifest
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {

    private val messages = ArrayList<Message>()
    private lateinit var adapter: ChatAdapter
    private lateinit var input: EditText

    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrEmpty()) input.setText(text)
    }

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startSpeech() else Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val rv = findViewById<RecyclerView>(R.id.chat_list)
        adapter = ChatAdapter(messages)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        input = findViewById(R.id.chat_input)
        val send = findViewById<ImageButton>(R.id.chat_send)
        val mic = findViewById<ImageButton>(R.id.chat_mic)
        val bar = findViewById<LinearLayout>(R.id.chat_bar)
        animateBarBorder(bar, input)

        mic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) startSpeech()
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        }

        send.setOnClickListener { v ->
            v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()

            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            if (BuildConfig.GROQ_API_KEY.isEmpty()) {
                messages.add(Message("assistant", "No Groq API key set. Add groqApiKey=... to local.properties and rebuild."))
                adapter.notifyDataSetChanged()
                return@setOnClickListener
            }
            input.setText("")
            messages.add(Message("user", text))
            messages.add(Message("assistant", ""))
            adapter.notifyDataSetChanged()
            rv.scrollToPosition(messages.size - 1)

            val history = messages.filter { it.content.isNotEmpty() }.map { Message(it.role, it.content) }
            GroqClient.chat(history,
                onResult = { reply ->
                    runOnUiThread {
                        messages[messages.size - 1] = Message("assistant", reply)
                        adapter.notifyDataSetChanged()
                        rv.scrollToPosition(messages.size - 1)
                    }
                },
                onError = { err ->
                    runOnUiThread {
                        messages[messages.size - 1] = Message("assistant", err)
                        adapter.notifyDataSetChanged()
                        rv.scrollToPosition(messages.size - 1)
                    }
                })
        }
    }

    private fun startSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your question")
        }
        try {
            speech.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateBarBorder(bar: LinearLayout, input: EditText) {
        val bg = bar.background.mutate() as GradientDrawable
        val dp = resources.displayMetrics.density
        val idleColor = getColor(R.color.md_outline)
        val focusColor = getColor(R.color.md_accent)

        input.setOnFocusChangeListener { _, hasFocus ->
            val fromColor: Int
            val toColor: Int
            val fromW: Float
            val toW: Float
            if (hasFocus) {
                fromColor = idleColor; toColor = focusColor; fromW = dp; toW = 2 * dp
            } else {
                fromColor = focusColor; toColor = idleColor; fromW = 2 * dp; toW = dp
            }
            val c = PropertyValuesHolder.ofInt("c", fromColor, toColor)
            val w = PropertyValuesHolder.ofFloat("w", fromW, toW)
            ValueAnimator.ofPropertyValuesHolder(c, w).apply {
                duration = 280
                addUpdateListener { a ->
                    val color = a.getAnimatedValue("c") as Int
                    val width = (a.getAnimatedValue("w") as Float).toInt()
                    bg.setStroke(width, color)
                    bar.background = bg
                }
                start()
            }
        }
    }
}
