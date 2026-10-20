package com.vaultly.app.domain

import com.vaultly.app.data.Item
import com.vaultly.app.data.ItemStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class DeadlineKind { RETURN, WARRANTY }

data class Deadline(
    val kind: DeadlineKind,
    val start: LocalDate,
    val end: LocalDate,
) {
    fun daysLeft(today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(today, end)
    fun isOver(today: LocalDate = LocalDate.now()): Boolean = daysLeft(today) < 0

    /** Fraction of the window still remaining, 0..1. */
    fun remainingFraction(today: LocalDate = LocalDate.now()): Float {
        val total = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1)
        return (daysLeft(today).toFloat() / total).coerceIn(0f, 1f)
    }
}

/** How urgently an item needs attention, used for colour and sorting. */
enum class Urgency { URGENT, SOON, SAFE, EXPIRED, CLOSED }

object Deadlines {
    const val URGENT_DAYS = 7
    const val SOON_DAYS = 30

    fun purchaseDate(item: Item): LocalDate = LocalDate.ofEpochDay(item.purchaseDate)

    fun returnDeadline(item: Item): Deadline? = item.returnDays?.takeIf { it > 0 }?.let {
        val start = purchaseDate(item)
        Deadline(DeadlineKind.RETURN, start, start.plusDays(it.toLong()))
    }

    fun warrantyDeadline(item: Item): Deadline? = item.warrantyMonths?.takeIf { it > 0 }?.let {
        val start = purchaseDate(item)
        Deadline(DeadlineKind.WARRANTY, start, start.plusMonths(it.toLong()))
    }

    fun all(item: Item): List<Deadline> = listOfNotNull(returnDeadline(item), warrantyDeadline(item))

    /** The nearest deadline that hasn't passed yet. */
    fun next(item: Item, today: LocalDate = LocalDate.now()): Deadline? =
        all(item).filterNot { it.isOver(today) }.minByOrNull { it.end }

    fun urgency(item: Item, today: LocalDate = LocalDate.now()): Urgency {
        if (item.statusEnum != ItemStatus.ACTIVE) return Urgency.CLOSED
        val next = next(item, today) ?: return if (all(item).isEmpty()) Urgency.SAFE else Urgency.EXPIRED
        val days = next.daysLeft(today)
        return when {
            days <= URGENT_DAYS -> Urgency.URGENT
            days <= SOON_DAYS -> Urgency.SOON
            else -> Urgency.SAFE
        }
    }

    /** Sort key: most urgent first, then items without deadlines, then expired. */
    fun sortKey(item: Item, today: LocalDate = LocalDate.now()): Long {
        val next = next(item, today)
        return when {
            item.statusEnum != ItemStatus.ACTIVE -> Long.MAX_VALUE
            next != null -> next.daysLeft(today)
            all(item).isEmpty() -> 100_000L
            else -> 200_000L
        }
    }

    fun describeLeft(days: Long): String = when {
        days < 0 -> "Ended"
        days == 0L -> "Ends today"
        days == 1L -> "1 day left"
        days < 60 -> "$days days left"
        days < 730 -> "${days / 30} months left"
        else -> "${days / 365} years left"
    }

    fun shortLeft(days: Long): String = when {
        days < 0 -> "Ended"
        days == 0L -> "Today"
        days < 60 -> "${days}d"
        days < 730 -> "${days / 30}mo"
        else -> "${days / 365}y"
    }
}
