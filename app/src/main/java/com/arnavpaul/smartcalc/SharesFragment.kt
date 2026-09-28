package com.arnavpaul.smartcalc

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

class SharesFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_shares, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.shares_back).setOnClickListener {
            (activity as? MainActivity)?.swap(FinanceFragment())
        }

        val query = view.findViewById<TextInputEditText>(R.id.share_query)
        val search = view.findViewById<Button>(R.id.share_search)
        val results = view.findViewById<LinearLayout>(R.id.share_results)
        val progress = view.findViewById<ProgressBar>(R.id.share_progress)
        val error = view.findViewById<TextView>(R.id.share_error)

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
                    val url = "https://query1.finance.yahoo.com/v1/finance/search?q=" +
                        URLEncoder.encode(q, "UTF-8") + "&quotesCount=8"
                    val c = URL(url).openConnection() as HttpsURLConnection
                    c.connectTimeout = 15000
                    c.readTimeout = 15000
                    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                    val text = c.inputStream.bufferedReader().use { it.readText() }
                    c.disconnect()
                    val arr = JSONObject(text).optJSONArray("quotes") ?: org.json.JSONArray()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val sym = o.optString("symbol", "")
                        val name = o.optString("shortname", o.optString("longname", ""))
                        val exch = o.optString("exchange", "")
                        if (sym.endsWith(".NS") || sym.endsWith(".BO")) {
                            found.add(arrayOf(sym, name, exch))
                        }
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
                        error.text = "No Indian listings found for that name."
                        error.visibility = View.VISIBLE
                        return@runOnUiThread
                    }
                    for (f in found) addResultRow(results, f[0], f[1], f[2])
                }
            }
        }
    }

    private fun addResultRow(container: LinearLayout, symbol: String, name: String, exch: String) {
        val ctx = requireContext()
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.VERTICAL
        val pad = (12 * resources.displayMetrics.density).toInt()
        row.setPadding(0, pad, 0, pad)

        val title = TextView(ctx)
        title.text = name
        title.textAppearance = android.R.style.TextAppearance_Material_Subhead
        title.textSize = 15f
        title.setTextColor(getColor(requireContext(), R.color.md_on_background))

        val sub = TextView(ctx)
        sub.text = symbol + "  &#183;  " + exch
        sub.textSize = 12f
        sub.setTextColor(getColor(requireContext(), R.color.md_on_background_muted))

        val price = TextView(ctx)
        price.textSize = 15f
        price.setTextColor(getColor(requireContext(), R.color.md_accent))
        price.setPadding(0, (6 * resources.displayMetrics.density).toInt(), 0, 0)

        row.addView(title)
        row.addView(sub)
        row.addView(price)
        row.setOnClickListener { fetchQuote(symbol, price, row) }
        container.addView(row)
    }

    private fun fetchQuote(symbol: String, priceView: TextView, row: LinearLayout) {
        priceView.text = "loading..."
        thread {
            var text: String? = null
            var err: String? = null
            try {
                val c = URL("https://query1.finance.yahoo.com/v8/finance/chart/" +
                    symbol + "?interval=1d&range=5d").openConnection() as HttpsURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 15000
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                text = c.inputStream.bufferedReader().use { it.readText() }
                c.disconnect()
            } catch (e: Exception) {
                err = "Could not load the quote."
            }
            activity?.runOnUiThread {
                if (err != null) {
                    priceView.text = err
                    return@runOnUiThread
                }
                try {
                    val meta = JSONObject(text).getJSONObject("chart").getJSONObject("result")
                        .getJSONObject(0).getJSONObject("meta")
                    val p = meta.getDouble("regularMarketPrice")
                    val prev = meta.optDouble("chartPreviousClose", p)
                    val cur = meta.optString("currency", "INR")
                    var out = java.text.DecimalFormat("#,##,##0.00").format(p) + " " + cur
                    if (prev > 0) {
                        val pct = (p - prev) / prev * 100.0
                        out += String.format("  (%+.2f%%)", pct)
                        priceView.setTextColor(if (pct >= 0) Color.parseColor("#1B873F") else Color.parseColor("#C62828"))
                    }
                    priceView.text = out
                } catch (e: Exception) {
                    priceView.text = "Could not load the quote."
                }
            }
        }
    }
}
