package com.stretchres.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.stretchres.app.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedPreset: ResolutionPreset? = null

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            showSnack("Shizuku permission granted!")
            refreshStatus()
        } else {
            showSnack("Permission denied — see ADB instructions below")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPresetSpinner()
        setupButtons()
        refreshStatus()
    }

    private fun setupPresetSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Presets.list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPresets.adapter = adapter
        binding.spinnerPresets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedPreset = Presets.list[pos]
                binding.editWidth.setText(selectedPreset!!.width.toString())
                binding.editHeight.setText(selectedPreset!!.height.toString())
                binding.editDpi.setText(selectedPreset!!.dpi.toString())
                binding.tvPresetDesc.text = selectedPreset!!.description
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        binding.btnApply.setOnClickListener {
            val w = binding.editWidth.text.toString().toIntOrNull()
            val h = binding.editHeight.text.toString().toIntOrNull()
            val d = binding.editDpi.text.toString().toIntOrNull()
            if (w == null || h == null || d == null || w < 240 || h < 240 || d < 100) {
                showSnack("Please enter valid values (min 240x240, DPI 100)")
                return@setOnClickListener
            }
            applyResolution(w, h, d)
        }

        binding.btnReset.setOnClickListener {
            resetResolution()
        }

        binding.btnShizukuGrant.setOnClickListener {
            if (!ShizukuService.isAvailable()) {
                showShizukuNotAvailableDialog()
            } else {
                ShizukuService.requestPermission(permissionListener)
            }
        }

        binding.btnCopyAdb.setOnClickListener {
            val w = binding.editWidth.text.toString().ifBlank { "1280" }
            val h = binding.editHeight.text.toString().ifBlank { "720" }
            val d = binding.editDpi.text.toString().ifBlank { "240" }
            val cmd = "adb shell wm size ${w}x${h} && adb shell wm density $d"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd))
            showSnack("ADB command copied!")
        }

        binding.btnRefresh.setOnClickListener { refreshStatus() }
    }

    private fun applyResolution(w: Int, h: Int, dpi: Int) {
        if (!ShizukuService.isAvailable()) {
            showShizukuNotAvailableDialog()
            return
        }
        if (!ShizukuService.hasPermission()) {
            ShizukuService.requestPermission(permissionListener)
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        Thread {
            val result = ShizukuService.applyResolution(w, h, dpi)
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                if (result.isSuccess) {
                    showSnack("✓ Resolution set to ${w}x${h} @ ${dpi}dpi")
                    refreshStatus()
                } else {
                    showSnack("Failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }.start()
    }

    private fun resetResolution() {
        if (!ShizukuService.isAvailable() || !ShizukuService.hasPermission()) {
            val cmd = "adb shell wm size reset && adb shell wm density reset"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Reset", cmd))
            showSnack("ADB reset command copied to clipboard!")
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        Thread {
            val result = ShizukuService.resetResolution()
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                if (result.isSuccess) {
                    showSnack("✓ Resolution reset to default")
                    refreshStatus()
                } else {
                    showSnack("Failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }.start()
    }

    private fun refreshStatus() {
        if (ShizukuService.isAvailable() && ShizukuService.hasPermission()) {
            Thread {
                val size = ShizukuService.getCurrentSize()
                val density = ShizukuService.getCurrentDensity()
                runOnUiThread {
                    binding.tvShizukuStatus.text = "● Shizuku: Connected"
                    binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    if (size != null && density != null) {
                        binding.tvCurrentRes.text = "Current: ${size.first}x${size.second} @ ${density}dpi"
                    }
                }
            }.start()
        } else if (ShizukuService.isAvailable()) {
            binding.tvShizukuStatus.text = "● Shizuku: Need Permission"
            binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            binding.tvCurrentRes.text = "Current: Unknown (grant permission)"
        } else {
            binding.tvShizukuStatus.text = "● Shizuku: Not Running"
            binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            binding.tvCurrentRes.text = "Current: Use ADB commands below"
        }
    }

    private fun showShizukuNotAvailableDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Shizuku Not Running")
            .setMessage(
                "Shizuku is not running. You have two options:\n\n" +
                "1. Install & start the Shizuku app (recommended, no PC needed with wireless debugging)\n\n" +
                "2. Use ADB manually from a PC:\n" +
                "   adb shell wm size <width>x<height>\n" +
                "   adb shell wm density <dpi>\n\n" +
                "Tap 'Copy ADB Cmd' button to copy the current settings as an ADB command."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSnack(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuService.removePermissionListener(permissionListener)
    }
}
