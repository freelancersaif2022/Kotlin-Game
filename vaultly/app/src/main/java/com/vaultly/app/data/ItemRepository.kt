package com.vaultly.app.data

import com.vaultly.app.storage.ImageStore
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val dao: ItemDao, private val images: ImageStore) {
    val items: Flow<List<Item>> = dao.observeAll()

    fun observe(id: Long): Flow<Item?> = dao.observe(id)
    suspend fun get(id: Long): Item? = dao.get(id)
    suspend fun active(): List<Item> = dao.active()
    suspend fun activeCount(): Int = dao.activeCount()

    /** Saves the item and deletes image files that the previous version referenced but this one doesn't. */
    suspend fun save(item: Item): Long {
        val previous = if (item.id != 0L) dao.get(item.id) else null
        val id = dao.upsert(item)
        if (previous != null) {
            val kept = (item.photoList + listOfNotNull(item.receiptImage)).toSet()
            (previous.photoList + listOfNotNull(previous.receiptImage))
                .filterNot { it in kept }
                .forEach { images.delete(it) }
        }
        return if (item.id != 0L) item.id else id
    }

    suspend fun setStatus(item: Item, status: ItemStatus) {
        dao.upsert(item.copy(status = status.name))
    }

    suspend fun delete(item: Item) {
        dao.delete(item)
        (item.photoList + listOfNotNull(item.receiptImage)).forEach { images.delete(it) }
    }
}
