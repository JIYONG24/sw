@file:Suppress("DEPRECATION")

package com.example.free_lamp_remote

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.net.Socket

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var vibrator: Vibrator
    private lateinit var countDownTimer: CountDownTimer
    var dataToSend = "s"
    var lockbtn = 0 // 1이면 잠금, 0이면 잠금해제
    var btnpressed = 0 // 1이면 눌린것, 0이면 뗀것
    var Lamp_OnOff = 0 // 1이면 on 0이면 off

    var timer_state = 0 //1이면 RUN
    var t_hour = "00"
    var t_min = "00"
    var t_sec = "00"


    val raspiIpAddress = "192.168.137.42"
    val raspiPort = 22222

    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 버튼 셋팅
        val btn_lightbulb = findViewById<ImageButton>(R.id.btn_lightbulb)
        val btn_f = findViewById<ImageButton>(R.id.btn_f)
        val btn_b = findViewById<ImageButton>(R.id.btn_b)
        val btn_l = findViewById<ImageButton>(R.id.btn_l)
        val btn_r = findViewById<ImageButton>(R.id.btn_r)
        val btn_lock = findViewById<ImageButton>(R.id.btn_lock)
        val lock_frame = findViewById<ImageView>(R.id.lock_frame)
        val btn_timer = findViewById<ImageButton>(R.id.btn_timer)
        val et_hour = findViewById<EditText>(R.id.et_hour)
        val et_min = findViewById<EditText>(R.id.et_min)
        val et_sec = findViewById<EditText>(R.id.et_sec)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val pressedColor = ContextCompat.getColor(this, R.color.pressedColor)
        val normalColor = ContextCompat.getColor(this, R.color.normalColor)
        val lockColor = ContextCompat.getColor(this, R.color.lockColor)

        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                var intHour = t_hour.toIntOrNull()
                if(intHour == null){
                    intHour = 0
                }

                var intMin = t_min.toIntOrNull()
                if(intMin == null){
                    intMin = 0
                }

                var intSec = t_sec.toIntOrNull()
                if(intSec == null){
                    intSec = 0
                }

                et_hour.setText(t_hour)
                et_min.setText(t_min)
                et_sec.setText(t_sec)

                if (intHour != null && intMin != null && intSec != null) {
                    var tempTime = intHour * 3600 + intMin * 60 + intSec // 시간을 초 단위로 변환

                    if (tempTime > 0) {
                        tempTime = tempTime - 1 // 분 단위로 시간을 1분씩 줄임
                        var updatedHour = tempTime / 3600 // 시간 계산
                        var updatedMin = (tempTime % 3600) / 60 // 분 계산
                        var updatedSec = tempTime % 60 // 초 계산

                        t_hour = String.format("%02d", updatedHour)
                        t_min = String.format("%02d", updatedMin)
                        t_sec = String.format("%02d", updatedSec)

                    } else {
                        // 카운트다운 종료
                        onFinish()
                    }
                }
            }

            override fun onFinish() {
                countDownTimer.cancel() // 타이머 중지
                timer_state = 0
                et_hour.setText(null)
                et_min.setText(null)
                et_sec.setText(null)
                btn_timer.backgroundTintList = ColorStateList.valueOf(lockColor)
                if(Lamp_OnOff == 1) {
                    btn_lightbulb.setBackgroundResource(R.drawable.gray_lightbulb)
                    dataToSend = "x"
                    Lamp_OnOff = 0
                }
                else if(Lamp_OnOff == 0) {
                    btn_lightbulb.setBackgroundResource(R.drawable.lightbulb)
                    dataToSend = "o"
                    Lamp_OnOff = 1
                }
                NetworkTask().execute()
            }
        }

        //전구 ON/OFF 버튼
        btn_lightbulb.setOnClickListener {
            Lamp_OnOff = 1 - this.Lamp_OnOff

            if (Lamp_OnOff == 0) {
                btn_lightbulb.setBackgroundResource(R.drawable.gray_lightbulb)
                dataToSend = "x"
                NetworkTask().execute()
            }
            if (Lamp_OnOff == 1) {
                btn_lightbulb.setBackgroundResource(R.drawable.lightbulb)
                dataToSend = "o"
                NetworkTask().execute()
            }
        }

        //lock 버튼
        btn_lock.setOnClickListener {
            lockbtn = 1 - this.lockbtn

            vibrateDevice(100)

            if (lockbtn == 0) {
                //버튼 활성화
                btn_lock.setBackgroundResource(R.drawable.lockoff)
                lock_frame.setBackgroundResource(R.drawable.lockbtn_frame)
                btn_f.backgroundTintList = ColorStateList.valueOf(normalColor)
                btn_b.backgroundTintList = ColorStateList.valueOf(normalColor)
                btn_l.backgroundTintList = ColorStateList.valueOf(normalColor)
                btn_r.backgroundTintList = ColorStateList.valueOf(normalColor)
            }
            if (lockbtn == 1) {
                //버튼 비활성화
                btn_lock.setBackgroundResource(R.drawable.lockon)
                lock_frame.setBackgroundResource(R.drawable.lockbtn_frame_gray)
                btn_f.backgroundTintList = ColorStateList.valueOf(lockColor)
                btn_b.backgroundTintList = ColorStateList.valueOf(lockColor)
                btn_l.backgroundTintList = ColorStateList.valueOf(lockColor)
                btn_r.backgroundTintList = ColorStateList.valueOf(lockColor)
            }
        }

        //forward 버튼
        btn_f.setOnTouchListener { view, motionEvent ->
            if (lockbtn==0) {
                when (motionEvent.action) {
                    //눌렸을 때
                    MotionEvent.ACTION_DOWN -> {
                        btn_f.backgroundTintList = ColorStateList.valueOf(pressedColor)
                        btnpressed = 1
                        dataToSend = "f"
                        NetworkTask().execute()
                    }
                    //떼면 다시 원래 색상으로 복구
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        btn_f.backgroundTintList = ColorStateList.valueOf(normalColor)
                        btnpressed = 0
                        dataToSend = "s"
                        NetworkTask().execute()
                    }
                }
            }
            true
        }

        //backward 버튼
        btn_b.setOnTouchListener { view, motionEvent ->
            if (lockbtn==0) {
                when (motionEvent.action) {
                    //눌렸을 때
                    MotionEvent.ACTION_DOWN -> {
                        btn_b.backgroundTintList = ColorStateList.valueOf(pressedColor)
                        btnpressed = 1
                        dataToSend = "b"
                        NetworkTask().execute()
                    }
                    //떼면 다시 원래 색상으로 복구
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        btn_b.backgroundTintList = ColorStateList.valueOf(normalColor)
                        btnpressed = 0
                        dataToSend = "s"
                        NetworkTask().execute()
                    }
                }
            }
            true
        }

        //left 버튼
        btn_l.setOnTouchListener { view, motionEvent ->
            if (lockbtn==0) {
                when (motionEvent.action) {
                    //눌렸을 때
                    MotionEvent.ACTION_DOWN -> {
                        btn_l.backgroundTintList = ColorStateList.valueOf(pressedColor)

                        btnpressed = 1
                        dataToSend = "l"
                        NetworkTask().execute()
                    }
                    //떼면 다시 원래 색상으로 복구
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        btn_l.backgroundTintList = ColorStateList.valueOf(normalColor)
                        btnpressed = 0
                        dataToSend = "s"
                        NetworkTask().execute()
                    }
                }
            }
            true
        }

        //right 버튼
        btn_r.setOnTouchListener { view, motionEvent ->
            if (lockbtn==0) {
                when (motionEvent.action) {
                    //눌렸을 때
                    MotionEvent.ACTION_DOWN -> {
                        btn_r.backgroundTintList = ColorStateList.valueOf(pressedColor)

                        btnpressed = 1
                        dataToSend = "r"
                        NetworkTask().execute()
                    }
                    //떼면 다시 원래 색상으로 복구
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        btn_r.backgroundTintList = ColorStateList.valueOf(normalColor)
                        btnpressed = 0
                        dataToSend = "s"
                        NetworkTask().execute()
                    }
                }
            }
            true
        }

        btn_timer.setOnClickListener {
            if(timer_state == 0){
                t_hour = et_hour.text.toString()
                t_min = et_min.text.toString()
                t_sec = et_sec.text.toString()

                timer_state = 1
                btn_timer.backgroundTintList = ColorStateList.valueOf(normalColor)

                countDownTimer.start()
            }
            else if(timer_state == 1){
                timer_state = 0

                et_hour.setText(null)
                et_min.setText(null)
                et_sec.setText(null)

                countDownTimer.cancel() // 타이머 중지

                btn_timer.backgroundTintList = ColorStateList.valueOf(lockColor)
            }
        }
    }

    private fun vibrateDevice(duration: Long) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }

    private inner class NetworkTask : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            if(lockbtn == 0 || ((lockbtn == 1) && ((dataToSend == "o") || (dataToSend == "x")))) {
                try {
                    if(btnpressed == 1) {
                        vibrateDevice(200)
                    }
                    else if(btnpressed == 0) {
                        vibrateDevice(50)
                    }

                    val socket = Socket(raspiIpAddress, raspiPort)
                    val outputStream = socket.getOutputStream()

                    outputStream.write(dataToSend.toByteArray())

                    outputStream.close()
                    socket.close()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        }
    }
}