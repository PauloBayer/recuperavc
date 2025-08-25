import android.content.Context

object DbProvider {
    fun db(context: Context) = AppRoomDatabase.getInstance(context)
}