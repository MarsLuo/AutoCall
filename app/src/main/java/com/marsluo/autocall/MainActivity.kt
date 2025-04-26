package com.marsluo.autocall

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.marsluo.autocall.databinding.ActivityMainBinding
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var telephonyManager: TelephonyManager
    private var isCalling = false
    private var currentCallCount = 0
    private var maxCallCount = 0
    private var ringTime = 0
    private var timer: Timer? = null
    private var phoneNumber = ""

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化TelephonyManager
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        // 设置默认值
        binding.ringTimeInput.setText("30") // 默认振铃30秒
        binding.callCountInput.setText("10") // 默认拨打10次

        // 设置开始按钮点击事件
        binding.startButton.setOnClickListener {
            if (!isCalling) {
                startCalling()
            } else {
                stopCalling()
            }
        }

        // 检查权限
        checkPermissions()
    }

    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要权限才能拨打电话", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCalling() {
        // 获取用户输入
        phoneNumber = binding.phoneNumberInput.text.toString()
        maxCallCount = binding.callCountInput.text.toString().toIntOrNull() ?: 10
        ringTime = binding.ringTimeInput.text.toString().toIntOrNull() ?: 30

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "请输入电话号码", Toast.LENGTH_SHORT).show()
            return
        }

        // 注册电话状态监听
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        // 更新UI状态
        isCalling = true
        currentCallCount = 0
        binding.startButton.text = "停止拨打"
        binding.statusText.text = "准备拨打电话..."

        // 开始拨打电话
        makeCall()
    }

    private fun stopCalling() {
        // 取消电话状态监听
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        
        // 取消定时器
        timer?.cancel()
        timer = null

        // 更新UI状态
        isCalling = false
        binding.startButton.text = "开始拨打"
        binding.statusText.text = "已停止拨打"
    }

    private fun makeCall() {
        if (currentCallCount >= maxCallCount) {
            stopCalling()
            binding.statusText.text = "已达到最大拨打次数"
            return
        }

        currentCallCount++
        binding.statusText.text = "正在拨打第 $currentCallCount 次..."

        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$phoneNumber")
        startActivity(intent)

        // 设置定时器，在指定时间后挂断电话
        timer?.cancel()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (isCalling) {
                        // 这里需要实现挂断电话的逻辑
                        // 注意：Android 10及以上版本不允许直接挂断电话
                        // 需要用户手动挂断
                        binding.statusText.text = "请手动挂断电话，准备下一次拨打..."
                    }
                }
            }
        }, ringTime * 1000L)
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    // 电话挂断或空闲状态
                    if (isCalling) {
                        // 等待一段时间后再次拨打
                        timer?.schedule(object : TimerTask() {
                            override fun run() {
                                runOnUiThread {
                                    makeCall()
                                }
                            }
                        }, 2000) // 等待2秒后再次拨打
                    }
                }
                TelephonyManager.CALL_STATE_RINGING -> {
                    // 电话振铃状态
                    binding.statusText.text = "电话正在振铃..."
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // 电话接通状态
                    binding.statusText.text = "电话已接通"
                    stopCalling()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCalling()
    }
}