package com.example.lotteryapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "cancellation_history",
    foreignKeys = [
        ForeignKey(
            entity = Ticket::class,
            parentColumns = ["id"],
            childColumns = ["ticketId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ticketId")]
)
data class CancellationHistory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val ticketId: String,
    val previousBuyerName: String,
    val previousBuyerPhone: String? = null,
    val cancellationDate: Long
)