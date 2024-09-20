package com.example.tipcalculator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TipCalculatorViewModel : ViewModel() {
    private val _billAmount = MutableStateFlow("")
    val billAmount: StateFlow<String> get() = _billAmount

    private val _applyTaxesState = MutableStateFlow(false)
    val applyTaxesState: StateFlow<Boolean> get() = _applyTaxesState

    private val _tipPercentState = MutableStateFlow(15f)
    val tipPercentState: StateFlow<Float> get() = _tipPercentState

    private val _numberOfPeopleState = MutableStateFlow(1)
    val numberOfPeopleState: StateFlow<Int> get() = _numberOfPeopleState

    private val _latitude = MutableStateFlow("0.0")
    val latitude: StateFlow<String> get() = _latitude

    private val _longitude = MutableStateFlow("0.0")
    val longitude: StateFlow<String> get() = _longitude

    private val _tipsList = MutableStateFlow<List<Tip>>(emptyList())
    val tipsList: StateFlow<List<Tip>> get() = _tipsList

    private val tipDatabase: TipDatabase = TipDatabase.getDatabase(getCurrentContext())
    private val tipDao: TipDao = tipDatabase.tipDao()

    val tipAmount: StateFlow<Double> = _billAmount.map { billAmount ->
        val amount = billAmount.toDoubleOrNull() ?: 0.0
        val percent = _tipPercentState.value.toDouble() / 100
        (amount * percent).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    val totalBillAmount: StateFlow<Double> = combine(
        tipAmount,
        _billAmount
    ) { tipAmount, billAmount ->
        val amount = billAmount.toDoubleOrNull() ?: 0.0
        amount + tipAmount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    fun onTextFieldValueChange(value: String) {
        _billAmount.value = value
    }

    fun onApplyTaxesChange(value: Boolean) {
        _applyTaxesState.value = value
    }

    fun onTipPercentChange(value: Float) {
        _tipPercentState.value = value
    }

    fun onNumberOfPeopleChange(value: Int) {
        _numberOfPeopleState.value = value
    }

    fun onLatitudeChange(value: String) {
        _latitude.value = value
    }

    fun onLongitudeChange(value: String) {
        _longitude.value = value
    }

    fun addTip(tip: Tip) {
        viewModelScope.launch {
            tipDao.insertTip(tip)
            _tipsList.value = tipDao.getAllTips() // Refresh tips list after insertion
        }
    }

    fun deleteTip(tip: Tip) {
        viewModelScope.launch {
            tipDao.deleteTip(tip)
            _tipsList.value = tipDao.getAllTips() // Refresh tips list after deletion
        }
    }

    fun fetchTips() {
        viewModelScope.launch {
            _tipsList.value = tipDao.getAllTips() // Load tips from database
        }
    }

    // Function to get the current application context
    fun getCurrentContext(): Context {
        return TipCalculatorApplication.getAppContext()
    }
}
