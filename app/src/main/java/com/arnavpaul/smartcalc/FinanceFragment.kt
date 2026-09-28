package com.arnavpaul.smartcalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class FinanceFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_finance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val act = activity as? MainActivity
        view.findViewById<Button>(R.id.fin_commodity).setOnClickListener { act?.swap(CommodityFragment()) }
        view.findViewById<Button>(R.id.fin_shares).setOnClickListener { act?.swap(SharesFragment()) }
        view.findViewById<Button>(R.id.fin_funds).setOnClickListener { act?.swap(FundFragment()) }
    }
}
