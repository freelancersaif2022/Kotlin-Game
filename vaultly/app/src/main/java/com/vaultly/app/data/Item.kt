package com.vaultly.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One protected purchase: the receipt, its deadlines and supporting photos. */
@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val store: String,
    val category: String = Category.OTHER.name,
    val price: Double? = null,
    val currency: String = "USD",
    /** Purchase date as epoch day. */
    val purchaseDate: Long,
    /** Return window in days, or null when the item can't be returned. */
    val returnDays: Int? = null,
    /** Warranty length in months, or null when there is none. */
    val warrantyMonths: Int? = null,
    val receiptImage: String? = null,
    /** Extra proof photos, stored as absolute paths separated by '|'. */
    val photos: String = "",
    val notes: String = "",
    val serial: String = "",
    val status: String = ItemStatus.ACTIVE.name,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val photoList: List<String> get() = photos.split('|').filter { it.isNotBlank() }
    val categoryEnum: Category get() = Category.fromName(category)
    val statusEnum: ItemStatus get() = runCatching { ItemStatus.valueOf(status) }.getOrDefault(ItemStatus.ACTIVE)
}

enum class ItemStatus { ACTIVE, RETURNED, ARCHIVED }
