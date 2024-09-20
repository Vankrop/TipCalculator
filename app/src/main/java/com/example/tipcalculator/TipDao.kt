package com.example.tipcalculator

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface TipDao {
    @Insert
    suspend fun insertTip(tip: Tip)

    @Delete
    suspend fun deleteTip(tip: Tip)

    @Query("SELECT * FROM tips")
    suspend fun getAllTips(): List<Tip>
}
