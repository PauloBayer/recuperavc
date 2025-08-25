import androidx.room.*
import java.util.UUID

@Entity(
    tableName = "CoherenceReportGroup",
    primaryKeys = ["idCoherenceReport", "idPatternCoherenceReport"],
    foreignKeys = [
        ForeignKey(
            entity = CoherenceReport::class,
            parentColumns = ["id"],
            childColumns = ["idCoherenceReport"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PatternCoherencePhrase::class,
            parentColumns = ["id"],
            childColumns = ["idPatternCoherenceReport"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idCoherenceReport"), Index("idPatternCoherenceReport")]
)
data class CoherenceReportGroup(
    val idCoherenceReport: UUID,
    val idPatternCoherenceReport: UUID
)