package com.example.tokengenerator.repository

import androidx.lifecycle.LiveData
import com.example.tokengenerator.data.Token
import com.example.tokengenerator.data.TokenDao

class TokenRepository(private val tokenDao: TokenDao) {

    val allTokens: LiveData<List<Token>> = tokenDao.getAllTokens()

    suspend fun insert(token: Token): Long {
        return tokenDao.insert(token)
    }

    suspend fun insertAll(tokens: List<Token>): List<Long> {
        return tokenDao.insertAll(tokens)
    }

    suspend fun update(token: Token) {
        tokenDao.update(token)
    }

    suspend fun delete(token: Token) {
        tokenDao.delete(token)
    }

    suspend fun getTokensByIds(tokenIds: List<Long>): List<Token> {
        return tokenDao.getTokensByIds(tokenIds)
    }
}
