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

class UnitFragment : Fragment() {

    private val cats = listOf("Length", "Weight", "Volume", "Temperature")

    private val units = mapOf(
        "Length" to listOf("Millimeter", "Centimeter", "Meter", "Kilometer", "Inch", "Foot", "Yard", "Mile"),
        "Weight" to listOf("Milligram", "Gram", "Kilogram", "Tonne", "Ounce", "Pound"),
        "Volume" to listOf("Milliliter", "Liter", "Gallon (US)", "Pint (US)", "Cup"),
        "Temperature" to listOf("Celsius", "Fahrenheit", "Kelvin")
    )

    private val factor = mapOf(
        "Millimeter" to 0.001, "Centimeter" to 0.01, "Meter" to 1.0, "Kilometer" to 1000.0,
        "Inch" to 0.0254, "Foot" to 0.3048, "Yard" to 0.9144, "Mile" to 1609.344,
        "Milligram" to 1e-6, "Gram" to 0.001, "Kilogram" to 1.0, "Tonne" to 1000.0,
        "Ounce" to 0.0283495, "Pound" to 0.453592,
        "Milliliter" to 0.001, "Liter" to 1.0, "Gallon (US)" to 3.78541, "Pint (US)" to 0.473176, "Cup" to 0.24
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_unit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val cat = view.findViewById<MaterialAutoCompleteTextView>(R.id.unit_cat)
        val from = view.findViewById<MaterialAutoCompleteTextView>(R.id.unit_from)
        val to = view.findViewById<MaterialAutoCompleteTextView>(R.id.unit_to)

        cat.setSimpleItems(cats.toTypedArray())
        cat.setOnItemClickListener { _, _, pos, _ ->
            setupUnits(cats[pos], from, to)
            show("")
        }
        setupUnits("Length", from, to)
        cat.setText(cats[0], false)

        view.findViewById<MaterialButton>(R.id.unit_go).setOnClickListener { convert() }
    }

    private fun setupUnits(cat: String, from: MaterialAutoCompleteTextView, to: MaterialAutoCompleteTextView) {
        val list = units[cat] ?: return
        from.setSimpleItems(list.toTypedArray())
        to.setSimpleItems(list.toTypedArray())
        from.setText(list[0], false)
        to.setText(list[1], false)
    }

    private fun convert() {
        val cat = view?.findViewById<MaterialAutoCompleteTextView>(R.id.unit_cat)?.text?.toString() ?: return
        val v = view?.findViewById<TextInputEditText>(R.id.unit_value)?.text?.toString()?.toDoubleOrNull()
        if (v == null) {
            show("Enter a value to convert.")
            return
        }
        val from = view?.findViewById<MaterialAutoCompleteTextView>(R.id.unit_from)?.text?.toString() ?: return
        val to = view?.findViewById<MaterialAutoCompleteTextView>(R.id.unit_to)?.text?.toString() ?: return

        val result = if (cat == "Temperature") {
            fromC(toC(v, from), to)
        } else {
            val f = factor[from]
            val t = factor[to]
            if (f == null || t == null) {
                show("That unit is not supported.")
                return
            }
            v * f / t
        }
        show(Evaluator.fmt(result) + " " + to)
    }

    private fun toC(v: Double, u: String): Double = when (u) {
        "Fahrenheit" -> (v - 32) * 5.0 / 9.0
        "Kelvin" -> v - 273.15
        else -> v
    }

    private fun fromC(v: Double, u: String): Double = when (u) {
        "Fahrenheit" -> v * 9.0 / 5.0 + 32
        "Kelvin" -> v + 273.15
        else -> v
    }

    private fun show(s: String) {
        view?.findViewById<TextView>(R.id.unit_result)?.text = s
    }
}
