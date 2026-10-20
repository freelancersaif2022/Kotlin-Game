package com.vaultly.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultly.app.data.Item
import com.vaultly.app.data.ItemRepository
import com.vaultly.app.data.ItemStatus
import com.vaultly.app.data.Prefs
import com.vaultly.app.domain.Deadlines
import com.vaultly.app.domain.Urgency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class Filter(val label: String) { ALL("All"), RETURNS("Returns"), WARRANTY("Warranty"), EXPIRED("Expired"), ARCHIVED("Archived") }

data class HomeState(
    val loading: Boolean = true,
    val items: List<Item> = emptyList(),
    val counts: Map<Filter, Int> = emptyMap(),
    val activeCount: Int = 0,
    val attention: Int = 0,
    val warranties: Int = 0,
    val protectedValue: Double = 0.0,
    val currency: String = "USD",
    val totalItems: Int = 0,
)

class HomeViewModel(repo: ItemRepository, prefs: Prefs) : ViewModel() {
    val filter = MutableStateFlow(Filter.ALL)
    val query = MutableStateFlow("")

    val state: StateFlow<HomeState> = combine(repo.items, filter, query, prefs.currency) { all, f, q, currency ->
        val active = all.filter { it.statusEnum == ItemStatus.ACTIVE }
        fun matches(item: Item, filter: Filter): Boolean {
            val urgency = Deadlines.urgency(item)
            return when (filter) {
                Filter.ALL -> item.statusEnum == ItemStatus.ACTIVE
                Filter.RETURNS -> item.statusEnum == ItemStatus.ACTIVE &&
                    Deadlines.returnDeadline(item)?.isOver() == false
                Filter.WARRANTY -> item.statusEnum == ItemStatus.ACTIVE &&
                    Deadlines.warrantyDeadline(item)?.isOver() == false
                Filter.EXPIRED -> urgency == Urgency.EXPIRED
                Filter.ARCHIVED -> item.statusEnum != ItemStatus.ACTIVE
            }
        }
        val needle = q.trim().lowercase()
        val visible = all
            .filter { matches(it, f) }
            .filter {
                needle.isEmpty() || it.title.lowercase().contains(needle) || it.store.lowercase().contains(needle) ||
                    it.categoryEnum.label.lowercase().contains(needle) || it.serial.lowercase().contains(needle)
            }
            .sortedWith(compareBy<Item> { Deadlines.sortKey(it) }.thenByDescending { it.purchaseDate })
        val protectedItems = active.filter { item -> Deadlines.next(item) != null }
        HomeState(
            loading = false,
            items = visible,
            counts = Filter.entries.associateWith { fl -> all.count { matches(it, fl) } },
            activeCount = active.size,
            attention = active.count { Deadlines.urgency(it) == Urgency.URGENT },
            warranties = active.count { Deadlines.warrantyDeadline(it)?.let { d -> !d.isOver() } == true },
            protectedValue = protectedItems.filter { it.currency == currency }.sumOf { it.price ?: 0.0 },
            currency = currency,
            totalItems = all.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}
