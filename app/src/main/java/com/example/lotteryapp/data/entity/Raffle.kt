package com.example.lotteryapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RaffleSource {
    LOTERIA_NACIONAL,
    CHANCES,
    SORTEO,
    MANUAL,
    OTRO;

    val displayName: String
        get() = when (this) {
            LOTERIA_NACIONAL -> "Lotería Nacional"
            CHANCES -> "Chances"
            SORTEO -> "Sorteo Especial"
            MANUAL -> "Manual"
            OTRO -> "Otro"
        }
}

enum class RaffleStatus {
    ACTIVE,
    CLOSED
}

enum class RaffleModality {
    SENCILLA,
    GROUPS
}

@Entity(tableName = "raffles")
data class Raffle(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val prizeName: String,
    val prizeDescription: String? = null,
    val prizePhotoPath: String? = null,
    val prizeValue: Double? = null,
    val ticketPrice: Double,
    val drawDate: Long,
    val source: RaffleSource,
    val modality: RaffleModality = RaffleModality.SENCILLA,
    val groupSize: Int = 1,
    val winningNumber: String? = null,
    val status: RaffleStatus = RaffleStatus.ACTIVE
)