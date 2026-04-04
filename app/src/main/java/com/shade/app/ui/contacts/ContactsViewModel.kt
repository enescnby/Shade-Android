package com.shade.app.ui.contacts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
    val contacts: List<ContactEntity> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SHADE_CONTACTS"
    }

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "ContactsViewModel başlatıldı")
        observeContacts()
    }

    private fun observeContacts() {
        Log.d(TAG, "Kişiler dinleniyor...")
        contactRepository.getAllContacts()
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { contacts ->
                Log.d(TAG, "Kişi listesi güncellendi: ${contacts.size} kişi")
                _uiState.update { it.copy(contacts = contacts, isLoading = false) }
            }
            .catch { e ->
                Log.e(TAG, "Kişi listesi alınamadı: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        Log.d(TAG, "Arama sorgusu değişti: '$query'")
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            observeContacts()
        } else {
            contactRepository.searchContacts(query)
                .onEach { contacts ->
                    Log.d(TAG, "Arama sonucu: ${contacts.size} kişi ('$query')")
                    _uiState.update { it.copy(contacts = contacts) }
                }
                .catch { e ->
                    Log.e(TAG, "Arama hatası: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }

    fun deleteContact(contact: ContactEntity) {
        Log.d(TAG, "Kişi siliniyor: ${contact.shadeId}")
        viewModelScope.launch {
            contactRepository.deleteContact(contact)
            Log.d(TAG, "Kişi silindi: ${contact.shadeId}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ContactsViewModel temizlendi")
    }
}