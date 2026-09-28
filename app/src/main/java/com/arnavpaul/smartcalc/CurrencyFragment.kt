package com.arnavpaul.smartcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
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
        val from = view.findViewById<MaterialAutoCompleteTextView>(R.id.cur_from)
        val to = view.findViewById<MaterialAutoCompleteTextView>(R.id.cur_to)
        from.setSimpleItems(codes.toTypedArray())
        to.setSimpleItems(codes.toTypedArray())
        from.setText(codes[0], false)
        to.setText(codes[3], false)

        view.findViewById<MaterialButton>(R.id.cur_swap).setOnClickListener {
            val f = from.text.toString()
            from.setText(to.text.toString(), false)
            to.setText(f, false)
        }

        view.findViewById<MaterialButton>(R.id.cur_go).setOnClickListener {
            val r = rates
            if (r == null) fetch { ok -> if (ok) convert() else show("Could not load rates. Check your connection and try again.") }
            else convert()
        }
    }

    private fun convert() {
        val amount = view?.findViewById<TextInputEditText>(R.id.cur_amount)?.text?.toString()?.toDoubleOrNull()
        if (amount == null) {
            show("Enter an amount to convert.")
            return
        }
        val r = rates ?: return
        val from = view?.findViewById<MaterialAutoCompleteTextView>(R.id.cur_from)?.text?.toString() ?: return
        val to = view?.findViewById<MaterialAutoCompleteTextView>(R.id.cur_to)?.text?.toString() ?: return
        val f = r[from]
        val t = r[to]
        if (f == null || t == null) {
            show("That currency is not supported.")
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
