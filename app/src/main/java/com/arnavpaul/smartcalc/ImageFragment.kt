package com.arnavpaul.smartcalc

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.io.OutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

class ImageFragment : Fragment() {

    private var bitmap: Bitmap? = null
    private var busy = false

    private val writePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) saveImage() else Toast.makeText(requireContext(), "Storage permission is required to save", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val promptField = view.findViewById<TextView>(R.id.image_prompt)
        val go = view.findViewById<Button>(R.id.image_go)
        val progress = view.findViewById<ProgressBar>(R.id.image_progress)
        val error = view.findViewById<TextView>(R.id.image_error)
        val result = view.findViewById<ImageView>(R.id.image_result)
        val saveHint = view.findViewById<TextView>(R.id.image_save_hint)

        go.setOnClickListener {
            if (busy) return@setOnClickListener
            if (BuildConfig.OPENROUTER_API_KEY.isEmpty()) {
                error.text = "OpenRouter key is not configured in this build."
                error.visibility = View.VISIBLE
                return@setOnClickListener
            }
            val prompt = promptField.text.toString().trim()
            if (prompt.isEmpty()) {
                Toast.makeText(requireContext(), "Describe the image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            busy = true
            go.isEnabled = false
            error.visibility = View.GONE
            result.visibility = View.GONE
            saveHint.visibility = View.GONE
            progress.visibility = View.VISIBLE

            thread {
                var bmp: Bitmap? = null
                var errText: String? = null
                try {
                    val conn = URL("https://openrouter.ai/api/v1/images").openConnection() as HttpsURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Authorization", "Bearer " + BuildConfig.OPENROUTER_API_KEY)
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 30000
                    conn.readTimeout = 180000
                    conn.doOutput = true
                    val body = JSONObject()
                        .put("model", "google/gemini-2.5-flash-image")
                        .put("prompt", prompt)
                        .toString()
                    conn.outputStream.use { it.write(body.toByteArray()) }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val b64 = JSONObject(text)
                            .getJSONArray("data")
                            .getJSONObject(0)
                            .getString("b64_json")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else {
                        val text = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                        val msg = try { JSONObject(text).optJSONObject("error")?.optString("message") ?: text.take(140) } catch (e: Exception) { text.take(140) }
                        errText = "OpenRouter error " + code + ": " + msg
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    errText = "Network error. Check your connection and try again."
                }

                val finalBmp = bmp
                val finalErr = errText
                activity?.runOnUiThread {
                    busy = false
                    go.isEnabled = true
                    progress.visibility = View.GONE
                    if (finalBmp != null) {
                        bitmap = finalBmp
                        result.setImageBitmap(finalBmp)
                        result.visibility = View.VISIBLE
                        saveHint.visibility = View.VISIBLE
                        result.alpha = 0f
                        result.scaleX = 0.94f
                        result.scaleY = 0.94f
                        result.animate().alpha(1f).scaleX(1f).scaleY(1f)
                            .setDuration(320)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                    } else {
                        error.text = finalErr ?: "Could not generate the image. Try again."
                        error.visibility = View.VISIBLE
                    }
                }
            }
        }

        result.setOnClickListener {
            if (bitmap == null) return@setOnClickListener
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImage()
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) saveImage()
                else writePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun saveImage() {
        val bmp = bitmap ?: return
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "smartcalc_" + System.currentTimeMillis() + ".png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SmartCalc")
                }
            }
            val uri: Uri? = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) throw IllegalStateException("insert failed")
            val out: OutputStream? = requireContext().contentResolver.openOutputStream(uri)
            out?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(requireContext(), "Saved to Pictures/SmartCalc", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not save the image", Toast.LENGTH_SHORT).show()
        }
    }
}
