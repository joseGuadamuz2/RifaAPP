package com.example.lotteryapp

import android.app.Application
import com.example.lotteryapp.data.database.DatabaseProvider
import com.example.lotteryapp.repository.RaffleRepository

class LotteryApp : Application() {

    val repository: RaffleRepository by lazy {
        val database = DatabaseProvider.getDatabase(this)
        RaffleRepository(
            database = database,
            raffleDao = database.raffleDao(),
            ticketDao = database.ticketDao(),
            cancellationHistoryDao = database.cancellationHistoryDao(),
            winnerDao = database.winnerDao()
        )
    }
}