package com.example.tokengenerator.repository

import androidx.lifecycle.LiveData
import com.example.tokengenerator.data.Person
import com.example.tokengenerator.data.PersonDao

class PersonRepository(private val personDao: PersonDao) {

    val allPersons: LiveData<List<Person>> = personDao.getAllPersons()

    suspend fun insert(person: Person) {
        personDao.insert(person)
    }

    suspend fun update(person: Person) {
        personDao.update(person)
    }

    suspend fun delete(person: Person) {
        personDao.delete(person)
    }

    suspend fun get(personId: Long): Person {
        return personDao.get(personId)
    }
}
