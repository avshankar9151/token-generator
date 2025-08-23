package com.example.tokengenerator.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "tokens",
    indices = [Index(value = ["personId"])],
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class Token(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personId: Int,
    val noOfPerson: Int,
    val issuedOn: Date = Date()
)