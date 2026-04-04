package com.shade.app.ui.user

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shade.app.data.local.entities.ContactEntity
import com.shade.app.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val shadeId: String = checkNotNull(savedStateHandle["shadeId"])

    private val _contactState = MutableStateFlow<ContactEntity?>(null)
    val contactState = _contactState.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    init {
        loadContact()
    }

    private fun loadContact() {
        viewModelScope.launch {
            _contactState.value = contactRepository.getContactByShadeId(shadeId)
        }
    }

    fun saveContact(name: String) {
        val currentContact = _contactState.value ?: return

        if (currentContact.savedName == name || name.isBlank()) {
            return
        }

        viewModelScope.launch {
            contactRepository.updateContactName(shadeId, name)
            _saveSuccess.emit(Unit)
        }
    }
}
