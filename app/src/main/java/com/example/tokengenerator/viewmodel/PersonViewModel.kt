package com.example.tokengenerator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.tokengenerator.data.Person
import com.example.tokengenerator.data.AppDatabase
import com.example.tokengenerator.repository.PersonRepository
import kotlinx.coroutines.launch

class PersonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PersonRepository
    val allPersons: LiveData<List<Person>>

    init {
        val dao = AppDatabase.getInstance(application).personDao()
        repository = PersonRepository(dao)
        allPersons = repository.allPersons
    }

    fun insert(person: Person) {
        viewModelScope.launch {
            repository.insert(person)
        }
    }

    fun update(person: Person) {
        viewModelScope.launch {
            repository.update(person)
        }
    }

    fun delete(person: Person) {
        viewModelScope.launch {
            repository.delete(person)
        }
    }

    suspend fun get(personId: Long): Person {
        return repository.get(personId)
    }
}
