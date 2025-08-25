import androidx.room.*
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "AudioFile",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["fk_User_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PatternAudioPhrase::class,
            parentColumns = ["id"],
            childColumns = ["fk_PatternAudioPhrase_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fk_User_id"), Index("fk_PatternAudioPhrase_id")]
)
data class AudioFile(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val path: String,
    val fileType: String,
    val fileName: String,
    val isPattern: Boolean,
    val audioDuration: Int,
    val recordedAt: Instant,
    @ColumnInfo(name = "fk_User_id") val userId: Int,
    @ColumnInfo(name = "fk_PatternAudioPhrase_id") val patternAudioPhraseId: UUID?
)