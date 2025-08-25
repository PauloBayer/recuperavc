import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class UserWithAudioFiles(
    @Embedded val user: User,
    @Relation(
        parentColumn = "id",
        entityColumn = "fk_User_id"
    )
    val audioFiles: List<AudioFile>
)

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
            entityColumn = "idPatternCoherenceReport"
        )
    )
    val phrases: List<PatternCoherencePhrase>
)