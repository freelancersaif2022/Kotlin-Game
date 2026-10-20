package com.vaultly.app.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultly.app.VaultlyApp
import com.vaultly.app.data.Category
import com.vaultly.app.data.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditState(
    val loading: Boolean = false,
    val isNew: Boolean = true,
    val title: String = "",
    val store: String = "",
    val category: Category = Category.OTHER,
    val price: String = "",
    val currency: String = "USD",
    val purchaseDate: LocalDate = LocalDate.now(),
    val returnDays: Int? = 30,
    val warrantyMonths: Int? = null,
    val serial: String = "",
    val notes: String = "",
    val receiptImage: String? = null,
    val photos: List<String> = emptyList(),
    val scanning: Boolean = false,
    /** Short description of what OCR filled in, shown as a confirmation chip. */
    val autoFilled: List<String> = emptyList(),
    val scanFailed: Boolean = false,
    val saving: Boolean = false,
    val titleError: Boolean = false,
)

class EditViewModel(private val app: VaultlyApp, private val itemId: Long?, imageUri: Uri?) : ViewModel() {
    private val _state = MutableStateFlow(EditState(currency = app.prefs.currency.value, isNew = itemId == null))
    val state: StateFlow<EditState> = _state.asStateFlow()

    private var original: Item? = null
    private var warrantyTouched = false
    private var returnTouched = false

    init {
        if (itemId != null) load(itemId)
        if (imageUri != null) importReceipt(imageUri)
    }

    private fun load(id: Long) = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val item = app.repository.get(id) ?: return@launch
        original = item
        warrantyTouched = true
        returnTouched = true
        _state.update {
            it.copy(
                loading = false,
                isNew = false,
                title = item.title,
                store = item.store,
                category = item.categoryEnum,
                price = item.price?.let { p -> if (p % 1.0 == 0.0) p.toLong().toString() else "%.2f".format(p) } ?: "",
                currency = item.currency,
                purchaseDate = LocalDate.ofEpochDay(item.purchaseDate),
                returnDays = item.returnDays,
                warrantyMonths = item.warrantyMonths,
                serial = item.serial,
                notes = item.notes,
                receiptImage = item.receiptImage,
                photos = item.photoList,
            )
        }
    }

    fun importReceipt(uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(scanning = true, scanFailed = false, autoFilled = emptyList()) }
        val path = app.images.import(uri)
        if (path == null) {
            _state.update { it.copy(scanning = false, scanFailed = true) }
            return@launch
        }
        val previous = _state.value.receiptImage
        _state.update { it.copy(receiptImage = path) }
        // Drop the replaced image only if it was never saved on the item.
        if (previous != null && previous != original?.receiptImage) app.images.delete(previous)

        val parsed = app.reader.read(path)
        if (parsed == null) {
            _state.update { it.copy(scanning = false, scanFailed = true) }
            return@launch
        }
        val filled = mutableListOf<String>()
        _state.value.let { s ->
            var next = s.copy(scanning = false)
            if (s.store.isBlank()) parsed.store?.let { next = next.copy(store = it); filled += "store" }
            if (s.title.isBlank()) parsed.suggestedTitle?.let { next = next.copy(title = it); filled += "item" }
            parsed.date?.let { if (s.isNew || original == null) { next = next.copy(purchaseDate = it); filled += "date" } }
            if (s.price.isBlank()) {
                (parsed.items.maxByOrNull { it.price }?.price?.takeIf { parsed.items.size == 1 } ?: parsed.total)?.let {
                    next = next.copy(price = "%.2f".format(it)); filled += "price"
                }
            }
            parsed.currency?.let { next = next.copy(currency = it) }
            if (s.category == Category.OTHER) parsed.category?.let {
                next = next.copy(category = it); filled += "category"
                if (!warrantyTouched) next = next.copy(warrantyMonths = it.defaultWarrantyMonths)
            }
            if (!returnTouched) parsed.returnDays?.let { next = next.copy(returnDays = it); filled += "return policy" }
            _state.value = next.copy(autoFilled = filled, scanFailed = filled.isEmpty())
        }
    }

    fun addPhoto(uri: Uri) = viewModelScope.launch {
        val path = app.images.import(uri) ?: return@launch
        _state.update { it.copy(photos = it.photos + path) }
    }

    fun removePhoto(path: String) {
        _state.update { it.copy(photos = it.photos - path) }
        if (original?.photoList?.contains(path) != true) app.images.delete(path)
    }

    fun removeReceipt() {
        val path = _state.value.receiptImage ?: return
        _state.update { it.copy(receiptImage = null, autoFilled = emptyList(), scanFailed = false) }
        if (original?.receiptImage != path) app.images.delete(path)
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v, titleError = false) }
    fun setStore(v: String) = _state.update { it.copy(store = v) }
    fun setPrice(v: String) = _state.update { it.copy(price = v.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(12)) }
    fun setCurrency(v: String) = _state.update { it.copy(currency = v) }
    fun setDate(v: LocalDate) = _state.update { it.copy(purchaseDate = v) }
    fun setSerial(v: String) = _state.update { it.copy(serial = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }
    fun setReturnDays(v: Int?) { returnTouched = true; _state.update { it.copy(returnDays = v) } }
    fun setWarranty(v: Int?) { warrantyTouched = true; _state.update { it.copy(warrantyMonths = v) } }
    fun setCategory(c: Category) = _state.update {
        if (!warrantyTouched) it.copy(category = c, warrantyMonths = c.defaultWarrantyMonths) else it.copy(category = c)
    }

    /** Saves and returns the item id, or null when validation fails. */
    suspend fun save(): Long? {
        val s = _state.value
        val title = s.title.trim().ifBlank { s.store.trim() }
        if (title.isBlank()) {
            _state.update { it.copy(titleError = true) }
            return null
        }
        _state.update { it.copy(saving = true) }
        val price = s.price.replace(',', '.').toDoubleOrNull()
        val base = original ?: Item(title = title, store = "", purchaseDate = 0)
        val item = base.copy(
            title = title,
            store = s.store.trim().ifBlank { "Unknown store" },
            category = s.category.name,
            price = price,
            currency = s.currency,
            purchaseDate = s.purchaseDate.toEpochDay(),
            returnDays = s.returnDays,
            warrantyMonths = s.warrantyMonths,
            serial = s.serial.trim(),
            notes = s.notes.trim(),
            receiptImage = s.receiptImage,
            photos = s.photos.joinToString("|"),
        )
        val id = app.repository.save(item)
        // Keep the saved images when the screen closes.
        original = item.copy(id = id)
        _state.update { it.copy(saving = false) }
        return id
    }

    /** Called when the user leaves without saving: delete images imported in this session. */
    fun discard() {
        val s = _state.value
        val keep = original?.let { (it.photoList + listOfNotNull(it.receiptImage)).toSet() } ?: emptySet()
        (s.photos + listOfNotNull(s.receiptImage)).filterNot { it in keep }.forEach { app.images.delete(it) }
    }
}
