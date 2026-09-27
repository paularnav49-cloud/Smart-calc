package com.arnavpaul.smartcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class CurrencyFragment : Fragment() {

    private val codes = listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD", "CHF", "CNY", "SGD", "AED", "ZAR", "BRL", "RUB", "KRW")
    private var rates: Map<String, Double>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_currency, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val from = view.findViewById<Spinner>(R.id.cur_from)
        val to = view.findViewById<Spinner>(R.id.cur_to)
        from.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, codes)
        to.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, codes)
        from.setSelection(0)
        to.setSelection(3)

        view.findViewById<Button>(R.id.cur_swap).setOnClickListener {
            val f = from.selectedItemPosition
            from.setSelection(to.selectedItemPosition)
            to.setSelection(f)
        }

        view.findViewById<Button>(R.id.cur_go).setOnClickListener {
            val r = rates
            if (r == null) fetch { ok -> if (ok) convert() else show("Could not load rates. Check internet.") }
            else convert()
        }
    }

    private fun convert() {
        val amount = view?.findViewById<EditText>(R.id.cur_amount)?.text?.toString()?.toDoubleOrNull()
        if (amount == null) {
            show("Enter an amount")
            return
        }
        val r = rates ?: return
        val from = view?.findViewById<Spinner>(R.id.cur_from)?.selectedItem as? String ?: return
        val to = view?.findViewById<Spinner>(R.id.cur_to)?.selectedItem as? String ?: return
        val f = r[from]
        val t = r[to]
        if (f == null || t == null) {
            show("Currency not supported")
            return
        }
        show(Evaluator.fmt(amount * t / f) + " " + to)
    }

    private fun fetch(cb: (Boolean) -> Unit) {
        Thread {
            var ok = false
            try {
                val conn = URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpsURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val obj = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val ratesObj = obj.getJSONObject("rates")
                val map = HashMap<String, Double>()
                codes.forEach { c -> if (ratesObj.has(c)) map[c] = ratesObj.getDouble(c) }
                if (map.isNotEmpty()) {
                    rates = map
                    ok = true
                }
            } catch (e: Exception) {
                ok = false
            }
            val result = ok
            activity?.runOnUiThread { cb(result) }
        }.start()
    }

    private fun show(s: String) {
        view?.findViewById<TextView>(R.id.cur_result)?.text = s
    }
}
