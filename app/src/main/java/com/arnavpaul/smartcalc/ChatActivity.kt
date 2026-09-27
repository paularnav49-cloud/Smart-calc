package com.arnavpaul.smartcalc

import android.os.Bundle
import android.widget.EditText
import android.widget.Button
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
        findViewById<Button>(R.id.chat_send).setOnClickListener {
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
}
