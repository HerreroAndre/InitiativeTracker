package com.dmc.initiativetracker.ui.screen.image_library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.initiativetracker.domain.model.LibraryImage
import com.dmc.initiativetracker.repository.ImageLibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ImageLibraryUiState(
    val images: List<LibraryImage> = emptyList(),
    val searchQuery: String = "",
    val sortOption: ImageLibrarySortOption = ImageLibrarySortOption.CREATED_DESC,
    val isSortMenuOpen: Boolean = false
)

class ImageLibraryViewModel(
    private val repo: ImageLibraryRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortOption = MutableStateFlow(ImageLibrarySortOption.CREATED_DESC)
    private val isSortMenuOpen = MutableStateFlow(false)

    val uiState: StateFlow<ImageLibraryUiState> =
        combine(
            repo.observeImages(),
            searchQuery,
            sortOption,
            isSortMenuOpen
        ) { images, query, sort, sortMenuOpen ->
            val filtered = images.filter { image ->
                query.isBlank() || image.name.contains(query, ignoreCase = true)
            }

            val sorted = when (sort) {
                ImageLibrarySortOption.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
                ImageLibrarySortOption.CREATED_ASC -> filtered.sortedBy { it.createdAt }
                ImageLibrarySortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
                ImageLibrarySortOption.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            }

            ImageLibraryUiState(
                images = sorted,
                searchQuery = query,
                sortOption = sort,
                isSortMenuOpen = sortMenuOpen
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ImageLibraryUiState()
        )

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    fun openSortMenu() {
        isSortMenuOpen.value = true
    }

    fun closeSortMenu() {
        isSortMenuOpen.value = false
    }

    fun selectSort(option: ImageLibrarySortOption) {
        sortOption.value = option
        isSortMenuOpen.value = false
    }

    fun addImage(name: String, imageUri: String) = viewModelScope.launch {
        repo.addImage(name = name, imageUri = imageUri)
    }

    fun deleteImage(id: Long) = viewModelScope.launch {
        repo.deleteImage(id)
    }
    fun renameImage(id: Long, name: String) = viewModelScope.launch {
        repo.renameImage(id, name)
    }
}