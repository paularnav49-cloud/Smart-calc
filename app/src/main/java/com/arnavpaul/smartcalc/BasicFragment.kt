package com.arnavpaul.smartcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import androidx.fragment.app.Fragment

class BasicFragment : Fragment() {

    private lateinit var display: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_basic, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        display = view.findViewById(R.id.display)
        val pad = view.findViewById<GridLayout>(R.id.pad)

        val keys = listOf(
            "C", "(", ")", "÷",
            "7", "8", "9", "×",
            "4", "5", "6", "−",
            "1", "2", "3", "+",
            "0", ".", "⌫", "="
        )

        keys.forEach { k ->
            val b = Button(requireContext())
            b.text = k
            b.textSize = 20f
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
            "C" -> display.setText("")
            "⌫" -> {
                val t = display.text.toString()
                if (t.isNotEmpty()) display.setText(t.substring(0, t.length - 1))
            }
            "=" -> display.setText(Evaluator.fmt(Evaluator.eval(display.text.toString())))
            else -> display.append(k)
        }
    }
}
