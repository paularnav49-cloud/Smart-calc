package com.arnavpaul.smartcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class BasicFragment : Fragment() {

    private lateinit var display: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_basic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        display = view.findViewById(R.id.display)
        val pad = view.findViewById<GridLayout>(R.id.pad)

        val keys = listOf(
            "C" to "op", "(" to "op", ")" to "op", "÷" to "op",
            "7" to "num", "8" to "num", "9" to "num", "×" to "op",
            "4" to "num", "5" to "num", "6" to "num", "−" to "op",
            "1" to "num", "2" to "num", "3" to "num", "+" to "op",
            "0" to "num", "." to "num", "⌫" to "op", "=" to "op"
        )

        keys.forEach { (k, kind) ->
            val b = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonStyle)
            b.text = k
            b.textSize = 20f
            b.insetTop = 0
            b.insetBottom = 0
            b.stateListAnimator = null
            b.background = null
            b.backgroundTintList = null
            b.setTextColor(requireContext().getColorStateList(if (kind == "op") R.color.md_accent else R.color.md_on_background))
            b.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12 * resources.displayMetrics.density
                setColor(requireContext().getColor(if (kind == "op") R.color.md_accent_container else R.color.md_surface_variant))
            }
            val p = GridLayout.LayoutParams()
            p.width = 0
            p.height = 0
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            p.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            p.setMargins(4, 4, 4, 4)
            b.layoutParams = p
            b.setOnClickListener { onKey(k) }
            pad.addView(b)
        }
    }

    private fun onKey(k: String) {
        when (k) {
            "C" -> display.text = ""
            "⌫" -> {
                val t = display.text.toString()
                if (t.isNotEmpty()) display.text = t.substring(0, t.length - 1)
            }
            "=" -> display.text = Evaluator.fmt(Evaluator.eval(display.text.toString()))
            else -> display.text = display.text.toString() + k
        }
    }
}
