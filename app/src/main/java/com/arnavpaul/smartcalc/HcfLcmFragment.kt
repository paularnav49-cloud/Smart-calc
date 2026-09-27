package com.arnavpaul.smartcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class HcfLcmFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_hcf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btn_hcf).setOnClickListener { compute(true) }
        view.findViewById<Button>(R.id.btn_lcm).setOnClickListener { compute(false) }
    }

    private fun gcd(a: Long, b: Long): Long {
        var x = Math.abs(a)
        var y = Math.abs(b)
        while (y != 0L) {
            val t = x % y
            x = y
            y = t
        }
        return x
    }

    private fun compute(hcf: Boolean) {
        val raw = view?.findViewById<EditText>(R.id.hcf_input)?.text?.toString().orEmpty()
        val nums = raw.split(",", " ", "\n")
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toLongOrNull() }
        if (nums.size < 2) {
            show("Enter at least 2 whole numbers")
            return
        }
        if (hcf) {
            var g = nums[0]
            for (n in nums.drop(1)) g = gcd(g, n)
            show(if (g == 0L) "HCF of zeros is 0" else "HCF = " + g)
        } else {
            var l = nums[0]
            for (n in nums.drop(1)) {
                val g = gcd(l, n)
                if (g == 0L) {
                    show("LCM with 0 is 0")
                    return
                }
                l = l / g * n
            }
            show("LCM = " + l)
        }
    }

    private fun show(s: String) {
        view?.findViewById<TextView>(R.id.hcf_result)?.text = s
    }
}
