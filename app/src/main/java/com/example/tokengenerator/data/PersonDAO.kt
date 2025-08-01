package com.example.tokengenerator.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PersonDao {

    @Insert
    suspend fun insert(person: Person)

    @Update
    suspend fun update(person: Person)

    @Delete
    suspend fun delete(person: Person)

    @Query("SELECT * FROM persons ORDER BY name ASC, age ASC")
    fun getAllPersons(): LiveData<List<Person>>
}
