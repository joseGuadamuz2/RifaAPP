package com.example.lotteryapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "winners",
    foreignKeys = [
        ForeignKey(
            entity = Raffle::class,
            parentColumns = ["id"],
            childColumns = ["raffleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("raffleId")]
)
data class Winner(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val raffleId: String,
    val winningNumber: String,
    val buyerName: String,
    val buyerPhone: String? = null,
    val prizeName: String? = null,
    val prizeAmount: Double? = null,
    val registeredAt: Long = System.currentTimeMillis(),
    val notified: Boolean = false
)