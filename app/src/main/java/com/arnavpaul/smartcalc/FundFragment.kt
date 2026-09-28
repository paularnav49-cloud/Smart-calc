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
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
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

        val detail = TextView(ctx)
        detail.textSize = 13f
        detail.setTextColor(ctx.getColor(R.color.md_accent))
        detail.setPadding(0, (6 * resources.displayMetrics.density).toInt(), 0, 0)

        row.addView(title)
        row.addView(detail)
        row.setOnClickListener { loadFund(code, detail) }
        container.addView(row)
    }

    private fun loadFund(code: String, detail: TextView) {
        detail.text = "loading..."
        thread {
            var err: String? = null
            var navs = ArrayList<Array<Any>>()
            try {
                val c = URL("https://api.mfapi.in/mf/" + code).openConnection() as HttpsURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 30000
                val text = c.inputStream.bufferedReader().use { it.readText() }
                c.disconnect()
                val arr = JSONObject(text).getJSONArray("data")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val d = o.optString("date", "")
                    val n = o.optString("nav", "")
                    val nav = n.toDoubleOrNull()
                    if (nav != null && d.isNotEmpty()) navs.add(arrayOf(d, nav))
                }
            } catch (e: Exception) {
                err = "Could not load scheme data."
            }

            activity?.runOnUiThread {
                if (err != null) {
                    detail.text = err
                    return@runOnUiThread
                }
                if (navs.isEmpty()) {
                    detail.text = "No NAV data available."
                    return@runOnUiThread
                }
                detail.text = buildSummary(navs)
            }
        }
    }

    private fun buildSummary(navs: ArrayList<Array<Any>>): String {
        val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val cur = navs[0][1] as Double
        val curDate = fmt.parse(navs[0][0] as String)
        val sb = StringBuilder()
        sb.append("Current NAV: ").append(String.format("%.4f", cur))
        sb.append("  (").append(navs[0][0]).append(")")
        val periods = arrayOf(1, 3, 5)
        for (years in periods) {
            val target = Calendar.getInstance()
            target.time = curDate
            target.add(Calendar.YEAR, -years)
            var oldNav: Double? = null
            for (i in navs.size - 1 downTo 1) {
                val d = fmt.parse(navs[i][0] as String)
                if (d != null && !d.before(target.time)) {
                    oldNav = navs[i][1] as Double
                } else if (d != null && d.before(target.time)) {
                    break
                }
            }
            if (oldNav != null && oldNav > 0 && cur > oldNav) {
                val cagr = (Math.pow(cur / oldNav, 1.0 / years) - 1.0) * 100.0
                sb.append("\n").append(years).append("Y return: ")
                    .append(String.format("%+.2f%% p.a.", cagr))
            }
        }
        return sb.toString()
    }
}