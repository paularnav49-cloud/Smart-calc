package com.arnavpaul.smartcalc

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

class CommodityFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_commodity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.commodity_back).setOnClickListener {
            (activity as? MainActivity)?.swap(FinanceFragment())
        }

        val progress = view.findViewById<ProgressBar>(R.id.commodity_progress)
        val error = view.findViewById<TextView>(R.id.commodity_error)
        val goldPrice = view.findViewById<TextView>(R.id.gold_price)
        val goldChg = view.findViewById<TextView>(R.id.gold_chg)
        val silverPrice = view.findViewById<TextView>(R.id.silver_price)
        val silverChg = view.findViewById<TextView>(R.id.silver_chg)
        val oilPrice = view.findViewById<TextView>(R.id.oil_price)
        val oilChg = view.findViewById<TextView>(R.id.oil_chg)

        progress.visibility = View.VISIBLE
        error.visibility = View.GONE

        thread {
            var inr = 0.0
            val quotes = HashMap<String, DoubleArray>()
            try {
                val c = URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpsURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 15000
                inr = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                    .getJSONObject("rates").getDouble("INR")
                c.disconnect()
            } catch (e: Exception) { }

            for (sym in arrayOf(arrayOf("GC=F", "gold"), arrayOf("SI=F", "silver"), arrayOf("CL=F", "oil"))) {
                try {
                    val c = URL("https://query1.finance.yahoo.com/v8/finance/chart/" +
                        sym[0] + "?interval=1d&range=5d").openConnection() as HttpsURLConnection
                    c.connectTimeout = 15000
                    c.readTimeout = 15000
                    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                    val meta = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                        .getJSONObject("chart").getJSONObject("result").getJSONObject(0).getJSONObject("meta")
                    val price = meta.getDouble("regularMarketPrice")
                    val prev = meta.optDouble("chartPreviousClose", price)
                    quotes[sym[1] as String] = doubleArrayOf(price, prev)
                    c.disconnect()
                } catch (e: Exception) { }
            }

            activity?.runOnUiThread {
                progress.visibility = View.GONE
                val g = quotes["gold"]
                val s = quotes["silver"]
                val o = quotes["oil"]
                if (g == null && s == null && o == null) {
                    error.text = "Could not load prices. Check your connection and try again."
                    error.visibility = View.VISIBLE
                    return@runOnUiThread
                }
                g?.let {
                    goldPrice.text = fmtInr(it[0], inr)
                    setChg(goldChg, it[0], it[1])
                } ?: run { goldPrice.text = "unavailable" }
                s?.let {
                    silverPrice.text = fmtInr(it[0], inr)
                    setChg(silverChg, it[0], it[1])
                } ?: run { silverPrice.text = "unavailable" }
                o?.let {
                    oilPrice.text = fmtInr(it[0], inr)
                    setChg(oilChg, it[0], it[1])
                } ?: run { oilPrice.text = "unavailable" }
            }
        }
    }

    private fun fmtInr(usd: Double, inr: Double): String {
        return if (inr > 0) {
            val v = usd * inr
            java.text.DecimalFormat("#,##,##0").format(v) + " INR"
        } else {
            Evaluator.fmt(usd) + " USD"
        }
    }

    private fun setChg(view: TextView, price: Double, prev: Double) {
        if (prev == 0.0) {
            view.text = ""
            return
        }
        val pct = (price - prev) / prev * 100.0
        view.text = String.format("%+.2f%%", pct)
        val green = Color.parseColor("#1B873F")
        val red = Color.parseColor("#C62828")
        view.setTextColor(if (pct >= 0) green else red)
    }
}
