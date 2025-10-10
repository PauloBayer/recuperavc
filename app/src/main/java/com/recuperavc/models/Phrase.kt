package com.recuperavc.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.recuperavc.models.enums.PhraseType
import java.util.UUID

@Entity(tableName = "Phrase")
data class Phrase(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val description: String,
    val type: PhraseType
)