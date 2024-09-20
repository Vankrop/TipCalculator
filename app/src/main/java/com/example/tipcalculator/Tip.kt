package com.example.tipcalculator
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tips")
data class Tip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billAmount: Double,
    val tipPercentage: Double,
    val numberOfPersons: Int,
    val latitude: Double,
    val longitude: Double,
    val taxesApplied: Boolean
)
