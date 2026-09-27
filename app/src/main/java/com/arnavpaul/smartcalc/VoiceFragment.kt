package com.arnavpaul.smartcalc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class VoiceFragment : Fragment() {

    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        val heard = view?.findViewById<TextView>(R.id.voice_heard)
        val out = view?.findViewById<TextView>(R.id.voice_result)
        if (text.isNullOrEmpty()) {
            heard?.text = "Nothing heard. Try again."
            out?.text = ""
        } else {
            heard?.text = text
            val v = Evaluator.eval(wordsToExpr(text))
            out?.text = if (v.isNaN() || v.isInfinite()) "Could not calculate that" else "= " + Evaluator.fmt(v)
        }
    }

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startSpeech()
        else view?.findViewById<TextView>(R.id.voice_heard)?.text = "Microphone permission is required"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_voice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.voice_mic).setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                startSpeech()
            } else {
                permission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a calculation, e.g. 25 plus 17 into 3")
        }
        try {
            speech.launch(intent)
        } catch (e: Exception) {
            view?.findViewById<TextView>(R.id.voice_heard)?.text = "Speech recognition not available on this device"
        }
    }

    private fun wordsToExpr(input: String): String {
        var e = " " + input.lowercase() + " "
        e = e.replace("percent of", "/100*").replace("% of", "/100*").replace("percent", "/100")
        e = e.replace("multiplied by", "*").replace("divided by", "/")
        e = e.replace(" into ", "*").replace(" times ", "*").replace(" x ", "*")
        e = e.replace(" plus ", "+").replace(" minus ", "-").replace(" by ", "/")
        e = e.replace(" point ", ".").replace(" of ", "*")
        return e
    }
}
