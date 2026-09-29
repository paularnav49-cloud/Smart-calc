package com.arnavpaul.smartcalc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class FundDetailFragment(private val schemeCode: String, private val schemeName: String) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_fund_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.fund_detail_back).setOnClickListener {
            (activity as? MainActivity)?.swap(FundFragment())
        }
        view.findViewById<TextView>(R.id.fund_detail_name).text = schemeName

        val progress = view.findViewById<ProgressBar>(R.id.fund_detail_progress)
        val error = view.findViewById<TextView>(R.id.fund_detail_error)
        val body = view.findViewById<android.view.ViewGroup>(R.id.fund_detail_body)
        val houseView = view.findViewById<TextView>(R.id.fund_detail_house)
        val catView = view.findViewById<TextView>(R.id.fund_detail_category)
        val navView = view.findViewById<TextView>(R.id.fund_detail_nav)
        val dateView = view.findViewById<TextView>(R.id.fund_detail_nav_date)
        val ret1 = view.findViewById<TextView>(R.id.fund_detail_ret_1y)
        val ret3 = view.findViewById<TextView>(R.id.fund_detail_ret_3y)
        val ret5 = view.findViewById<TextView>(R.id.fund_detail_ret_5y)
        val abs1 = view.findViewById<TextView>(R.id.fund_detail_abs_1y)
        val abs3 = view.findViewById<TextView>(R.id.fund_detail_abs_3y)
        val abs5 = view.findViewById<TextView>(R.id.fund_detail_abs_5y)
        val chartHolder = view.findViewById<FrameLayout>(R.id.fund_detail_chart)

        thread {
            var err: String? = null
            var house = ""
            var category = ""
            val navs = ArrayList<Array<Any>>()
            try {
                val c = URL("https://api.mfapi.in/mf/" + schemeCode).openConnection() as HttpsURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 30000
                val text = c.inputStream.bufferedReader().use { it.readText() }
                c.disconnect()
                val obj = JSONObject(text)
                val meta = obj.optJSONObject("meta")
                if (meta != null) {
                    house = meta.optString("fund_house", "")
                    category = meta.optString("scheme_category", "")
                }
                val arr = obj.getJSONArray("data")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val d = o.optString("date", "")
                    val nav = o.optString("nav", "").toDoubleOrNull()
                    if (nav != null && d.isNotEmpty()) navs.add(arrayOf(d, nav))
                }
            } catch (e: Exception) {
                err = "Could not load scheme data. Check your connection and try again."
            }

            activity?.runOnUiThread {
                progress.visibility = View.GONE
                if (err != null || navs.isEmpty()) {
                    error.text = err ?: "No NAV data available."
                    error.visibility = View.VISIBLE
                    return@runOnUiThread
                }
                body.visibility = View.VISIBLE
                houseView.text = house
                catView.text = category
                val fmt = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                val cur = navs[0][1] as Double
                navView.text = String.format("%.4f", cur)
                dateView.text = "NAV as on " + navs[0][0]
                showReturn(fmt, navs, 1, ret1, abs1)
                showReturn(fmt, navs, 3, ret3, abs3)
                showReturn(fmt, navs, 5, ret5, abs5)
                chartHolder.addView(NavChartView(requireContext(), chartPoints(fmt, navs)))
            }
        }
    }

    private fun findPastNav(fmt: SimpleDateFormat, navs: List<Array<Any>>, years: Int): Double? {
        val curDate = fmt.parse(navs[0][0] as String) ?: return null
        val target = Calendar.getInstance()
        target.time = curDate
        target.add(Calendar.YEAR, -years)
        for (i in 1 until navs.size) {
            val d = fmt.parse(navs[i][0] as String) ?: continue
            if (!d.after(target.time)) return navs[i][1] as Double
        }
        return null
    }

    private fun showReturn(fmt: SimpleDateFormat, navs: List<Array<Any>>, years: Int, cagrView: TextView, absView: TextView) {
        val cur = navs[0][1] as Double
        val old = findPastNav(fmt, navs, years)
        if (old == null || old <= 0.0) {
            cagrView.text = "n/a"
            cagrView.setTextColor(requireContext().getColor(R.color.md_on_background_muted))
            absView.text = ""
            return
        }
        val cagr = (Math.pow(cur / old, 1.0 / years) - 1.0) * 100.0
        val abs = (cur / old - 1.0) * 100.0
        cagrView.text = String.format("%+.2f%% p.a.", cagr)
        absView.text = String.format("%+.2f%% total", abs)
        val color = if (cagr >= 0) Color.parseColor("#1B873F") else Color.parseColor("#C62828")
        cagrView.setTextColor(color)
        absView.setTextColor(color)
    }

    private fun chartPoints(fmt: SimpleDateFormat, navs: List<Array<Any>>): FloatArray {
        val curDate = fmt.parse(navs[0][0] as String)
        var end = navs.size
        if (curDate != null) {
            val cutoff = Calendar.getInstance()
            cutoff.time = curDate
            cutoff.add(Calendar.YEAR, -3)
            for (i in 1 until navs.size) {
                val d = fmt.parse(navs[i][0] as String)
                if (d != null && d.before(cutoff.time)) {
                    end = i
                    break
                }
            }
        }
        val src = ArrayList<Float>()
        for (i in 0 until end) src.add((navs[i][1] as Double).toFloat())
        src.reverse()
        if (src.isEmpty()) return FloatArray(0)
        val n = min(150, src.size)
        val pts = FloatArray(n)
        for (j in 0 until n) {
            val idx = j * (src.size - 1) / max(n - 1, 1)
            pts[j] = src[idx]
        }
        return pts
    }

    private class NavChartView(ctx: Context, private val pts: FloatArray) : View(ctx) {

        private val linePaint: Paint
        private val fillPaint: Paint
        private val dotPaint: Paint

        init {
            val accent = ctx.getColor(R.color.md_accent)
            linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            linePaint.color = accent
            linePaint.style = Paint.Style.STROKE
            linePaint.strokeWidth = 4f
            linePaint.strokeCap = Paint.Cap.ROUND
            fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            fillPaint.color = accent
            fillPaint.alpha = 36
            dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            dotPaint.color = accent
            dotPaint.style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            if (pts.size < 2) return
            var mn = pts[0]
            var mx = pts[0]
            for (v in pts) {
                mn = min(mn, v)
                mx = max(mx, v)
            }
            if (mx - mn < 0.000001f) mx = mn + 1f
            val w = width.toFloat()
            val h = height.toFloat()
            val pad = 10f
            val line = Path()
            val fill = Path()
            for (i in pts.indices) {
                val x = pad + (w - 2 * pad) * i / (pts.size - 1)
                val y = pad + (h - 2 * pad) * (1f - (pts[i] - mn) / (mx - mn))
                if (i == 0) {
                    line.moveTo(x, y)
                    fill.moveTo(x, h - pad)
                    fill.lineTo(x, y)
                } else {
                    line.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo(w - pad, h - pad)
            fill.close()
            canvas.drawPath(fill, fillPaint)
            canvas.drawPath(line, linePaint)
            val lastX = w - pad
            val lastY = pad + (h - 2 * pad) * (1f - (pts[pts.size - 1] - mn) / (mx - mn))
            canvas.drawCircle(lastX, lastY, 6f, dotPaint)
        }
    }
}