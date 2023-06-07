package com.mlprivacy.apppredictattack

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_test).setOnClickListener {
            val modelAttack = ModelAttack().apply {
                init()
            }
            modelAttack.sendPredictRequest(ModelAttack.MyCallback { result ->
                Log.e("test", "预测完成, 处理预测结果 $result")
            })
        }
    }
}