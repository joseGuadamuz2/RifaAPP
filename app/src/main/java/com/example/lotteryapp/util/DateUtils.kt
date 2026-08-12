package com.example.lotteryapp.util

import java.util.Calendar
import java.util.TimeZone

object DateUtils {

    /**
     * Convierte un timestamp UTC-midnight (como el que devuelve el DatePicker de Material3)
     * en un número comparable YYYYMMDD del día que representa.
     */
    fun dayNumber(utcMillis: Long): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = utcMillis
        return ymd(cal)
    }

    /** Número YYYYMMDD del día de hoy en la zona horaria local del dispositivo. */
    fun localTodayDayNumber(): Int {
        val cal = Calendar.getInstance()
        return ymd(cal)
    }

    /** true si la fecha (utcMillis) es hoy o una fecha posterior. */
    fun isSameDayOrAfter(utcMillis: Long): Boolean =
        dayNumber(utcMillis) >= localTodayDayNumber()

    private fun ymd(cal: Calendar): Int =
        cal.get(Calendar.YEAR) * 10000 +
                (cal.get(Calendar.MONTH) + 1) * 100 +
                cal.get(Calendar.DAY_OF_MONTH)
}