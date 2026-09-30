package com.shadowbody.app.ui.wardrobe

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shadowbody.app.ShadowBodyApp
import com.shadowbody.app.data.local.WardrobeItem
import com.shadowbody.app.data.repository.WardrobeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WardrobeViewModel(
    private val repository: WardrobeRepository,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val allItems: StateFlow<List<WardrobeItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredItems: StateFlow<List<WardrobeItem>> =
        combine(allItems, _searchQuery, _selectedCategory) { items, query, category ->
            var result = items
            if (category != null) {
                result = result.filter { it.category == category }
            }
            if (query.isNotBlank()) {
                val q = query.lowercase()
                result = result.filter {
                    it.name.lowercase().contains(q) ||
                        it.clothingType.lowercase().contains(q) ||
                        it.color.lowercase().contains(q)
                }
            }
            result
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val itemCount: StateFlow<Int> = allItems
        .combine(_selectedCategory) { items, category ->
            if (category == null) items.size else items.count { it.category == category }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
    }

    fun addItem(item: WardrobeItem) {
        viewModelScope.launch {
            repository.addItem(item)
            _message.value = "Item added."
        }
    }

    fun updateItem(item: WardrobeItem) {
        viewModelScope.launch {
            repository.updateItem(item)
            _message.value = "Item updated."
        }
    }

    fun deleteItem(item: WardrobeItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _message.value = "Item deleted."
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            _message.value = "Item deleted."
        }
    }

    fun toggleEnabled(item: WardrobeItem) {
        viewModelScope.launch {
            repository.setEnabled(item.id, !item.isEnabled)
            _message.value = if (item.isEnabled) "Item disabled." else "Item enabled."
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val shadow = app as ShadowBodyApp
            return WardrobeViewModel(shadow.wardrobeRepository) as T
        }
    }
}
