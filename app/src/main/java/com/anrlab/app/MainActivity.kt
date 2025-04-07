package com.anrlab.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast

const val TAG = "MainActivity"
class MainActivity : Activity() {
    private val _mutex = Any()

    inner class LockerThread internal constructor() : Thread() {
        init {
            name = "APP: Locker"
        }

        override fun run() {
            synchronized(_mutex) {
                while (true) Sleep(this@MainActivity)
            }
        }
    }

    private fun _deadLock() {
        LockerThread().start()

        Handler().postDelayed({
            synchronized(_mutex) {
                Log.e("ANR-Failed", "There should be a dead lock before this message")
            }
        }, 1000)
    }


    /**
     * Mode:
     * 0 - All Threads
     * 1 - Main Thread only
     * 2 - Filtered
     * */
    private var mode = 1
    /**
     * Crash:
     * true - Crash
     * false - Silent
     * */
    private var crash = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val application: MyApp = application as MyApp

        val minAnrDurationButton = findViewById<View>(R.id.minAnrDuration) as Button
        minAnrDurationButton.setText("${application.duration} seconds")
        minAnrDurationButton.setOnClickListener {
            application.duration = application.duration % 6 + 2
            minAnrDurationButton.setText("${application.duration} seconds")
        }

        val reportModeButton = findViewById<View>(R.id.reportMode) as Button
        reportModeButton.text = "All threads"
        reportModeButton.setOnClickListener {
            mode = (mode + 1) % 3
            when (mode) {
                0 -> {
                    reportModeButton.text = "All threads"
                    application.anrWatchDog.setReportAllThreads()
                }

                1 -> {
                    reportModeButton.text = "Main thread only"
                    application.anrWatchDog.setReportMainThreadOnly()
                }

                2 -> {
                    reportModeButton.text = "Filtered"
                    application.anrWatchDog.setReportThreadNamePrefix("APP:")
                }
            }
        }

        val behaviourButton = findViewById<View>(R.id.behaviour) as Button
        behaviourButton.text = "Crash"
        behaviourButton.setOnClickListener {
            crash = !crash
            if (crash) {
                behaviourButton.text = "Crash"
                application.anrWatchDog.setANRListener(null)
            } else {
                behaviourButton.text = "Silent"
                application.anrWatchDog.setANRListener(application.silentListener)
            }
        }

        findViewById<View>(R.id.threadSleep).setOnClickListener {
            Log.d(TAG, "Sleeping for 8 sec")
//            Toast.makeText(this, "Sleeping for 8 sec", Toast.LENGTH_SHORT).show()
            Sleep(this)
        }

        findViewById<View>(R.id.infiniteLoop).setOnClickListener { InfiniteLoop(this) }

        findViewById<View>(R.id.deadlock).setOnClickListener { _deadLock() }
    }

    companion object {
        private fun Sleep(context: Context) {
            try {
                Thread.sleep((context.applicationContext as MyApp).duration * 1000.toLong())
                Log.d(TAG, "The thread wake up after 8 sec")
//                Toast.makeText(context.applicationContext, "The thread wake up after 8 sec", Toast.LENGTH_SHORT).show()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        private fun InfiniteLoop(context: Context) {
            var i = 0
            Log.d(TAG, "Infinite loop is starting")
//            Toast.makeText(context.applicationContext, "Infinite loop is starting", Toast.LENGTH_SHORT).show()
            while (true) {
                i++
            }
        }
    }
}