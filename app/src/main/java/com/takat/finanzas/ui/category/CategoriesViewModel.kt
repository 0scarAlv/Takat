package com.takat.finanzas.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: FinanceRepository) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> =
        repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateCategory(category: CategoryEntity, name: String, icon: String) {
        viewModelScope.launch {
            repository.updateCategory(category.copy(name = name, emoji = icon))
        }
    }
}
