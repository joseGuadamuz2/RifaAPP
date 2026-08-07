package com.example.lotteryapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TicketStatus {
    AVAILABLE,
    RESERVED,
    SOLD
}

@Entity(
    tableName = "tickets",
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
data class Ticket(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val raffleId: String,
    val number: String,
    val buyerName: String? = null,
    val buyerPhone: String? = null,
    val status: TicketStatus = TicketStatus.AVAILABLE,
    val imageSent: Boolean = false,
    val groupId: String? = null
)