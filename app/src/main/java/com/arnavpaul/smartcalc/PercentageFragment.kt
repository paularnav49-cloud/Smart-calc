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

class PercentageFragment : Fragment() {

    private val modes = listOf("X% of Y", "X is what % of Y", "% change from X to Y")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_percent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val spinner = view.findViewById<Spinner>(R.id.percent_mode)
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, modes)
        view.findViewById<Button>(R.id.percent_go).setOnClickListener { compute(spinner.selectedItemPosition) }
    }

    private fun num(id: Int): Double? =
        view?.findViewById<EditText>(id)?.text?.toString()?.toDoubleOrNull()

    private fun show(s: String) {
        view?.findViewById<TextView>(R.id.percent_result)?.text = s
    }

    private fun compute(mode: Int) {
        val a = num(R.id.percent_a)
        val b = num(R.id.percent_b)
        if (a == null || b == null) {
            show("Enter both numbers")
            return
        }
        when (mode) {
            0 -> show(Evaluator.fmt(a / 100.0 * b))
            1 -> if (b == 0.0) show("Cannot divide by 0") else show(Evaluator.fmt(a / b * 100.0) + "%")
            2 -> if (a == 0.0) show("Cannot divide by 0") else show(Evaluator.fmt((b - a) / a * 100.0) + "%")
        }
    }
}
