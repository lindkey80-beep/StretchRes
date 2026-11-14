package com.stretchres.app

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuService {

    private const val REQUEST_CODE = 1001

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            false
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermission(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(REQUEST_CODE)
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    fun runCommand(command: String): Result<String> {
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (exit == 0) Result.success(output.trim())
            else Result.failure(Exception("Error (exit $exit): $error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun applyResolution(width: Int, height: Int, dpi: Int): Result<String> {
        val sizeResult = runCommand("wm size ${width}x${height}")
        if (sizeResult.isFailure) return sizeResult
        return runCommand("wm density $dpi")
    }

    fun resetResolution(): Result<String> {
        val sizeResult = runCommand("wm size reset")
        if (sizeResult.isFailure) return sizeResult
        return runCommand("wm density reset")
    }

    fun getCurrentSize(): Pair<Int, Int>? {
        val result = runCommand("wm size")
        if (result.isFailure) return null
        val line = result.getOrDefault("").lines().find { it.contains("Physical size") }
            ?: result.getOrDefault("").lines().firstOrNull() ?: return null
        val match = Regex("(\\d+)x(\\d+)").find(line) ?: return null
        val (w, h) = match.destructured
        return Pair(w.toInt(), h.toInt())
    }

    fun getCurrentDensity(): Int? {
        val result = runCommand("wm density")
        if (result.isFailure) return null
        val match = Regex("(\\d+)").find(result.getOrDefault("")) ?: return null
        return match.value.toInt()
    }
}
