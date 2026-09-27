package com.arnavpaul.smartcalc

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) swap(BasicFragment())

        findViewById<BottomNavigationView>(R.id.bottom_nav).setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_basic -> swap(BasicFragment())
                R.id.nav_percent -> swap(PercentageFragment())
                R.id.nav_currency -> swap(CurrencyFragment())
                R.id.nav_unit -> swap(UnitFragment())
                R.id.nav_hcf -> swap(HcfLcmFragment())
            }
            true
        }

        findViewById<TextView>(R.id.btn_voice).setOnClickListener { swap(VoiceFragment()) }
        findViewById<TextView>(R.id.btn_chat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
    }

    private fun swap(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
    }
}
