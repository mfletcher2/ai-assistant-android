package lol.max.assistantgpt.api

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.provider.MediaStore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.time.LocalDate
import java.time.LocalTime

class PackageListRequest {
    fun getPackages(context: Context?): List<PackageResult> {
        if (context == null)
            return listOf()

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgAppsList: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(mainIntent, 0)

        val resultList = ArrayList<PackageResult>()
        pkgAppsList.forEach {
            resultList.add(
                PackageResult(
                    it.loadLabel(context.packageManager).toString(),
                    it.activityInfo.packageName
                )
            )
        }
        return resultList
    }
}

data class PackageResult(val appName: String, val packageName: String)

class PackageRunRequest {
    @JsonPropertyDescription("Name of the package to run")
    @JsonProperty(required = true)
    lateinit var packageName: String
    fun runPackage(context: Context?): Boolean {
        if (context == null) return false
        val i = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        i.addCategory(Intent.CATEGORY_LAUNCHER)

        if (i.resolveActivity(context.packageManager) == null) return false

        context.startActivity(i)
        return true
    }
}

class DateAndTimeRequest {
    fun getDateAndTime(): DateAndTime {
        val date = LocalDate.now()
        val time = LocalTime.now()
        return DateAndTime(
            date.dayOfWeek.name,
            date.dayOfMonth,
            date.monthValue,
            date.year,
            time.hour,
            time.minute,
            time.second
        )
    }
}

data class DateAndTime(
    val dayOfWeek: String,
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
    val hour: Int,
    val minute: Int,
    val second: Int
)

class PlayMusicRequest {
    var songTitle: String? = null
    var artist: String? = null
    var album: String? = null
    var genre: String? = null

    fun playMusic(context: Context?): Boolean {
        if (context == null) return false
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)

        if (songTitle != null) {
            intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio")
            intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, songTitle)
            intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
            intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album)
            intent.putExtra(MediaStore.EXTRA_MEDIA_GENRE, genre)
            intent.putExtra(SearchManager.QUERY, songTitle)
        } else if (album != null) {
            intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)
            intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album)
            intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
            intent.putExtra(MediaStore.EXTRA_MEDIA_GENRE, genre)
            intent.putExtra(SearchManager.QUERY, album)
        } else if (artist != null) {
            intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
            intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
            intent.putExtra(MediaStore.EXTRA_MEDIA_GENRE, genre)
            intent.putExtra(SearchManager.QUERY, artist)
        } else if (genre != null) {
            intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE)
            intent.putExtra(MediaStore.EXTRA_MEDIA_GENRE, genre)
            intent.putExtra(SearchManager.QUERY, genre)
        } else {
            intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            intent.putExtra(SearchManager.QUERY, "")
        }

        try {
            // do not show activity in foreground
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        } catch (e: Exception) {
            return false
        }
        return true
    }
}

