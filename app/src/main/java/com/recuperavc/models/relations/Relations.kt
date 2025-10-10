package com.recuperavc.models.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.recuperavc.models.AudioFile
import com.recuperavc.models.AudioReport
import com.recuperavc.models.AudioReportGroup
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.CoherenceReportGroup
import com.recuperavc.models.MotionReport
import com.recuperavc.models.Phrase

data class AudioReportWithFiles(
    @Embedded val report: AudioReport,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AudioReportGroup::class,
            parentColumn = "idAudioReport",
            entityColumn = "idAudioFile"
        )
    )
    val files: List<AudioFile>
)

data class CoherenceReportWithPhrases(
    @Embedded val report: CoherenceReport,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CoherenceReportGroup::class,
            parentColumn = "idCoherenceReport",
            entityColumn = "idPhrase"
        )
    )
    val phrases: List<Phrase>
)