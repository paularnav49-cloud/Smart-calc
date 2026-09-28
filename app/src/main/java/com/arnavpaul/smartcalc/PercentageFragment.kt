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

class PercentageFragment : Fragment() {

    private val modes = listOf("X% of Y", "X is what % of Y", "% change from X to Y")
    private var selectedMode = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_percent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val modeField = view.findViewById<MaterialAutoCompleteTextView>(R.id.percent_mode_field)
        modeField.setSimpleItems(modes.toTypedArray())
        modeField.setOnItemClickListener { _, _, pos, _ -> selectedMode = pos }
        modeField.setText(modes[selectedMode], false)

        view.findViewById<MaterialButton>(R.id.percent_go).setOnClickListener { compute(modes[selectedMode]) }
    }

    private fun compute(mode: String) {
        val a = view?.findViewById<TextInputEditText>(R.id.percent_a)?.text?.toString()?.toDoubleOrNull()
        val b = view?.findViewById<TextInputEditText>(R.id.percent_b)?.text?.toString()?.toDoubleOrNull()
        val result = view?.findViewById<TextView>(R.id.percent_result) ?: return
        if (a == null || b == null) {
            result.text = "Enter both numbers to continue."
            result.setTextColor(requireContext().getColor(R.color.md_on_background_muted))
            return
        }
        val value = when (mode) {
            modes[0] -> a / 100.0 * b
            modes[1] -> if (b == 0.0) null else a / b * 100.0
            else -> if (a == 0.0) null else (b - a) / a * 100.0
        }
        if (value == null) {
            result.text = "Cannot divide by zero. Change the numbers and try again."
            result.setTextColor(requireContext().getColor(R.color.md_error))
        } else {
            result.text = Evaluator.fmt(value) + if (mode != modes[0]) "%" else ""
            result.setTextColor(requireContext().getColor(R.color.md_accent))
        }
    }
}
