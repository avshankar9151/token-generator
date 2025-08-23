package com.example.tokengenerator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.tokengenerator.data.Token
import com.example.tokengenerator.data.AppDatabase
import com.example.tokengenerator.repository.TokenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TokenViewModel(application: Application) : AndroidViewModel(application) {

    private val _newlyAddedTokens = MutableStateFlow<List<Token>>(emptyList())
    val newlyAddedTokens = _newlyAddedTokens.asStateFlow()
    private val repository: TokenRepository
    val allTokens: LiveData<List<Token>>

    init {
        val dao = AppDatabase.getInstance(application).tokenDao()
        repository = TokenRepository(dao)
        allTokens = repository.allTokens
    }

    fun insert(token: Token) {
        viewModelScope.launch {
            repository.insert(token)
        }
    }

    fun insertAll(tokens: List<Token>) {
        viewModelScope.launch {
            val insertedIds = repository.insertAll(tokens)
            val addedTokens = getTokensByIds(insertedIds)
            _newlyAddedTokens.value = addedTokens
        }
    }

    fun update(token: Token) {
        viewModelScope.launch {
            repository.update(token)
        }
    }

    fun delete(token: Token) {
        viewModelScope.launch {
            repository.delete(token)
        }
    }

    suspend fun getTokensByIds(tokenIds: List<Long>): List<Token> {
        return repository.getTokensByIds(tokenIds)
    }

    fun clearNewlyAddedTokens() {
        _newlyAddedTokens.value = emptyList()
    }
}
