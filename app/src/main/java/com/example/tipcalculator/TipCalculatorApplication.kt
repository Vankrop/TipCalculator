package com.example.tipcalculator

import android.app.Application
import android.content.Context

class TipCalculatorApplication : Application() {
    init {
        app = this
    }

    companion object {
        private lateinit var app: TipCalculatorApplication

        fun getAppContext(): Context =
            app.applicationContext
    }
}
