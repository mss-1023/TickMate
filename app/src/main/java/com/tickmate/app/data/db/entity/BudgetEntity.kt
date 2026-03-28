package com.tickmate.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey
    val id: Long = 1,
    val monthlyBudget: Double = 0.0,
    val thresholdPercent: Int = 80,
    val yearMonth: String       // YYYY-MM
)
