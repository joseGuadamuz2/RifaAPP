package com.example.lotteryapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lotteryapp.data.dao.CancellationHistoryDao
import com.example.lotteryapp.data.dao.RaffleDao
import com.example.lotteryapp.data.dao.TicketDao
import com.example.lotteryapp.data.dao.WinnerDao
import com.example.lotteryapp.data.entity.CancellationHistory
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.Winner

@Database(
    entities = [Raffle::class, Ticket::class, CancellationHistory::class, Winner::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun raffleDao(): RaffleDao
    abstract fun ticketDao(): TicketDao
    abstract fun cancellationHistoryDao(): CancellationHistoryDao
    abstract fun winnerDao(): WinnerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE raffles ADD COLUMN modality TEXT NOT NULL DEFAULT 'SENCILLA'"
                )
                db.execSQL(
                    "ALTER TABLE raffles ADD COLUMN groupSize INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS winners (
                        id TEXT NOT NULL PRIMARY KEY,
                        raffleId TEXT NOT NULL,
                        winningNumber TEXT NOT NULL,
                        buyerName TEXT NOT NULL,
                        buyerPhone TEXT,
                        prizeAmount REAL,
                        registeredAt INTEGER NOT NULL,
                        notified INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (raffleId) REFERENCES raffles(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_winners_raffleId ON winners(raffleId)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Corrige el nombre mal escrito del enum RaffleSource.MANUAl -> MANUAL
                db.execSQL("UPDATE raffles SET source = 'MANUAL' WHERE source = 'MANUAl'")
                // Recrea la tabla tickets sin la columna sin uso imageSent
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tickets_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        raffleId TEXT NOT NULL,
                        number TEXT NOT NULL,
                        buyerName TEXT,
                        buyerPhone TEXT,
                        status TEXT NOT NULL,
                        groupId TEXT,
                        FOREIGN KEY (raffleId) REFERENCES raffles(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO tickets_new (id, raffleId, number, buyerName, buyerPhone, status, groupId)
                    SELECT id, raffleId, number, buyerName, buyerPhone, status, groupId FROM tickets
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE tickets")
                db.execSQL("ALTER TABLE tickets_new RENAME TO tickets")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tickets_raffleId ON tickets(raffleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tickets_groupId ON tickets(groupId)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // El premio del ganador se registra con el nombre del premio de la rifa
                db.execSQL("ALTER TABLE winners ADD COLUMN prizeName TEXT")
            }
        }
    }
}