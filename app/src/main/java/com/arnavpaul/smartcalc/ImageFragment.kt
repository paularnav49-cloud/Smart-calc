package com.arnavpaul.smartcalc

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import java.io.OutputStream
import java.net.URL
import java.net.URLEncoder
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
                try {
                    val url = "https://image.pollinations.ai/prompt/" +
                        URLEncoder.encode(prompt, "UTF-8") +
                        "?width=1024&height=1024&nologo=true&model=flux"
                    val conn = URL(url).openConnection() as HttpsURLConnection
                    conn.connectTimeout = 60000
                    conn.readTimeout = 90000
                    val bmp = BitmapFactory.decodeStream(conn.inputStream)
                    conn.disconnect()
                    activity?.runOnUiThread {
                        busy = false
                        go.isEnabled = true
                        progress.visibility = View.GONE
                        if (bmp == null) {
                            error.text = "Could not generate the image. The service may be busy. Try again."
                            error.visibility = View.VISIBLE
                        } else {
                            bitmap = bmp
                            result.setImageBitmap(bmp)
                            result.visibility = View.VISIBLE
                            saveHint.visibility = View.VISIBLE
                            result.alpha = 0f
                            result.scaleX = 0.94f
                            result.scaleY = 0.94f
                            result.animate().alpha(1f).scaleX(1f).scaleY(1f)
                                .setDuration(320)
                                .setInterpolator(AccelerateDecelerateInterpolator())
                                .start()
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        busy = false
                        go.isEnabled = true
                        progress.visibility = View.GONE
                        error.text = "Network error. Check your connection and try again."
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
