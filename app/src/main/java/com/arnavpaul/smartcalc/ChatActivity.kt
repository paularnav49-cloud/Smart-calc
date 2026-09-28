package com.arnavpaul.smartcalc

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {

    private val messages = ArrayList<Message>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val rv = findViewById<RecyclerView>(R.id.chat_list)
        adapter = ChatAdapter(messages)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val input = findViewById<EditText>(R.id.chat_input)
        val send = findViewById<ImageButton>(R.id.chat_send)
        animateInputBorder(input)

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

    private fun animateInputBorder(input: EditText) {
        val bg = input.background.mutate() as GradientDrawable
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
                    input.background = bg
                }
                start()
            }
        }
    }
}
