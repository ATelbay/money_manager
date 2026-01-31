package com.atelbay.money_manager.feature.categories.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atelbay.money_manager.core.model.Category
import com.atelbay.money_manager.core.model.TransactionType
import com.atelbay.money_manager.feature.categories.domain.usecase.GetCategoryByIdUseCase
import com.atelbay.money_manager.feature.categories.domain.usecase.SaveCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCategoryByIdUseCase: GetCategoryByIdUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
) : ViewModel() {

    private val categoryId: Long? = savedStateHandle.get<Long>("id")

    private val _state = MutableStateFlow(
        CategoryEditState(
            categoryId = categoryId,
            availableIcons = AVAILABLE_ICONS.toImmutableList(),
            availableColors = AVAILABLE_COLORS.toImmutableList(),
        ),
    )
    val state: StateFlow<CategoryEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (categoryId != null) {
                val category = getCategoryByIdUseCase(categoryId).first()
                if (category != null) {
                    _state.update {
                        it.copy(
                            name = category.name,
                            type = category.type,
                            selectedIcon = category.icon,
                            selectedColor = category.color,
                            isLoading = false,
                        )
                    }
                    return@launch
                }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun setName(name: String) {
        _state.update { it.copy(name = name, nameError = null) }
    }

    fun setType(type: TransactionType) {
        _state.update { it.copy(type = type) }
    }

    fun selectIcon(icon: String) {
        _state.update { it.copy(selectedIcon = icon) }
    }

    fun selectColor(color: Long) {
        _state.update { it.copy(selectedColor = color) }
    }

    fun save(onComplete: () -> Unit) {
        val current = _state.value

        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = "Введите название категории") }
            return
        }

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            saveCategoryUseCase(
                Category(
                    id = current.categoryId ?: 0,
                    name = current.name.trim(),
                    icon = current.selectedIcon,
                    color = current.selectedColor,
                    type = current.type,
                ),
            )
            onComplete()
        }
    }

    companion object {
        val AVAILABLE_ICONS = listOf(
            "restaurant",
            "directions_car",
            "sports_esports",
            "shopping_bag",
            "medical_services",
            "home",
            "phone_android",
            "school",
            "subscriptions",
            "more_horiz",
            "payments",
            "work",
            "card_giftcard",
            "trending_up",
            "flight",
            "local_cafe",
            "fitness_center",
            "pets",
            "child_care",
            "checkroom",
            "local_grocery_store",
            "local_gas_station",
            "build",
            "savings",
            "account_balance",
            "attach_money",
            "redeem",
            "volunteer_activism",
            "celebration",
            "music_note",
        )

        val AVAILABLE_COLORS = listOf(
            0xFFE57373,
            0xFF64B5F6,
            0xFFBA68C8,
            0xFFFFB74D,
            0xFFEF5350,
            0xFF78909C,
            0xFF4DD0E1,
            0xFF7986CB,
            0xFF9575CD,
            0xFF90A4AE,
            0xFF81C784,
            0xFF4DB6AC,
            0xFFF06292,
            0xFF4FC3F7,
            0xFFA5D6A7,
            0xFFFFD54F,
        )
    }
}
