package com.arnavpaul.smartcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

class FundFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_funds, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.funds_back).setOnClickListener {
            (activity as? MainActivity)?.swap(FinanceFragment())
        }

        val query = view.findViewById<TextInputEditText>(R.id.fund_query)
        val search = view.findViewById<Button>(R.id.fund_search)
        val results = view.findViewById<LinearLayout>(R.id.fund_results)
        val progress = view.findViewById<ProgressBar>(R.id.fund_progress)
        val error = view.findViewById<TextView>(R.id.fund_error)

        search.setOnClickListener {
            val q = query.text.toString().trim()
            if (q.isEmpty()) return@setOnClickListener
            results.removeAllViews()
            error.visibility = View.GONE
            progress.visibility = View.VISIBLE

            thread {
                val found = ArrayList<Array<String>>()
                var err: String? = null
                try {
                    val url = "https://api.mfapi.in/mf/search?q=" + URLEncoder.encode(q, "UTF-8")
                    val c = URL(url).openConnection() as HttpsURLConnection
                    c.connectTimeout = 15000
                    c.readTimeout = 20000
                    val text = c.inputStream.bufferedReader().use { it.readText() }
                    c.disconnect()
                    val arr = JSONArray(text)
                    for (i in 0 until Math.min(arr.length(), 8)) {
                        val o = arr.getJSONObject(i)
                        found.add(arrayOf(o.optString("schemeCode", ""), o.optString("schemeName", "")))
                    }
                } catch (e: Exception) {
                    err = "Search failed. Check your connection and try again."
                }

                activity?.runOnUiThread {
                    progress.visibility = View.GONE
                    if (err != null) {
                        error.text = err
                        error.visibility = View.VISIBLE
                        return@runOnUiThread
                    }
                    if (found.isEmpty()) {
                        error.text = "No schemes found for that name."
                        error.visibility = View.VISIBLE
                        return@runOnUiThread
                    }
                    for (f in found) addResultRow(results, f[0], f[1])
                }
            }
        }
    }

    private fun addResultRow(container: LinearLayout, code: String, name: String) {
        val ctx = requireContext()
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.VERTICAL
        val pad = (12 * resources.displayMetrics.density).toInt()
        row.setPadding(0, pad, 0, pad)

        val title = TextView(ctx)
        title.text = name
        title.textSize = 14f
        title.setTextColor(ctx.getColor(R.color.md_on_background))

        val sub = TextView(ctx)
        sub.text = "Tap for NAV and returns"
        sub.textSize = 12f
        sub.setTextColor(ctx.getColor(R.color.md_on_background_muted))
        sub.setPadding(0, (4 * resources.displayMetrics.density).toInt(), 0, 0)

        row.addView(title)
        row.addView(sub)
        row.setOnClickListener { (activity as? MainActivity)?.swap(FundDetailFragment(code, name)) }
        container.addView(row)
    }
}