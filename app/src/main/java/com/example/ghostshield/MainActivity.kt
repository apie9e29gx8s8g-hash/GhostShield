package com.example.ghostshield

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var btnToggle: Button
    private var isRunning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnToggle = findViewById(R.id.btn_toggle)
        btnToggle.setOnClickListener {
            if (!isRunning) {
                val intent = VpnService.prepare(this)
                if (intent != null) startActivityForResult(intent, 1)
                else startVpn()
            } else stopVpn()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) startVpn()
    }
    private fun startVpn() {
        startService(Intent(this, GhostVpnService::class.java))
        isRunning = true
        btnToggle.text = "Stop VPN"
        btnToggle.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
    }
    private fun stopVpn() {
        stopService(Intent(this, GhostVpnService::class.java))
        isRunning = false
        btnToggle.text = "Start VPN"
        btnToggle.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
    }
}
