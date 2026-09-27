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
        val cat = view.findViewById<Spinner>(R.id.unit_cat)
        cat.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cats)
        setupUnits(view, "Length")

        cat.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>, v: View?, pos: Int, id: Long) {
                setupUnits(view, cats[pos])
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>) {}
        }

        view.findViewById<Button>(R.id.unit_go).setOnClickListener { convert() }
    }

    private fun setupUnits(view: View, cat: String) {
        val list = units[cat] ?: return
        view.findViewById<Spinner>(R.id.unit_from).adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, list)
        view.findViewById<Spinner>(R.id.unit_to).adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, list)
    }

    private fun convert() {
        val cat = view?.findViewById<Spinner>(R.id.unit_cat)?.selectedItem as? String ?: return
        val v = view?.findViewById<EditText>(R.id.unit_value)?.text?.toString()?.toDoubleOrNull()
        if (v == null) {
            show("Enter a value")
            return
        }
        val from = view?.findViewById<Spinner>(R.id.unit_from)?.selectedItem as? String ?: return
        val to = view?.findViewById<Spinner>(R.id.unit_to)?.selectedItem as? String ?: return

        val result = if (cat == "Temperature") {
            fromC(toC(v, from), to)
        } else {
            val f = factor[from]
            val t = factor[to]
            if (f == null || t == null) {
                show("Not supported")
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
