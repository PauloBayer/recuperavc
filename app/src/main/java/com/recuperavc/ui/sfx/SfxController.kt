package com.recuperavc.ui.sfx

import android.media.SoundPool
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.recuperavc.R

/** Enumerate the sounds you want to use. */
enum class Sfx {
    CLICK,
    START_RECORDING,
    STOP_RECORDING,
    PROCESSING_DONE,
    RIGHT_ANSWER,
    WRONG_ANSWER,
    BUBBLE
}

/**
 * Holds a SoundPool + the loaded sound IDs.
 * Everything is preloaded once; play() is instant after load completes.
 */
@Stable
class SfxController internal constructor(
    private val soundPool: SoundPool,
    private val idMap: Map<Sfx, Int>,
    private val loadedIds: State<Set<Int>>
) {
    fun play(
        sfx: Sfx,
        volume: Float = 1f,
        rate: Float = 1f,
        loop: Boolean = false
    ) {
        val id = idMap[sfx] ?: return
        if (id in loadedIds.value) {
            soundPool.play(id, volume, volume, /*priority*/1, if (loop) -1 else 0, rate)
        }
    }

    fun release() = soundPool.release()
}

/**
 * Call this once per screen (or at your app root) to preload all SFX and get a controller.
 * Files must exist in res/raw with these names (underscores, not dashes):
 *  - short_pop.mp3
 *  - start_recording.wav
 *  - stop_recording.wav
 *  - processing_done.wav
 *  - right_answer.wav
 *  - wrong_answer.wav
 *  - bubble_pop.wav
 */
@Composable
fun rememberSfxController(maxStreams: Int = 8): SfxController {
    val context = LocalContext.current

    val controller = remember {
        val attrs = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val soundPool = SoundPool.Builder()
            .setAudioAttributes(attrs)
            .setMaxStreams(maxStreams)
            .build()

        val loaded = mutableStateOf(emptySet<Int>())
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                // Ensure we touch Compose state on main
                mainHandler.post {
                    loaded.value = loaded.value + sampleId
                }
            }
        }

        val ids = linkedMapOf<Sfx, Int>().apply {
            put(Sfx.CLICK,           soundPool.load(context, R.raw.short_pop,        1))
            put(Sfx.START_RECORDING, soundPool.load(context, R.raw.start_recording,  1))
            put(Sfx.STOP_RECORDING,  soundPool.load(context, R.raw.stop_recording,   1))
            put(Sfx.PROCESSING_DONE, soundPool.load(context, R.raw.processing_done,  1))
            put(Sfx.RIGHT_ANSWER,    soundPool.load(context, R.raw.right_answer,     1))
            put(Sfx.WRONG_ANSWER,    soundPool.load(context, R.raw.wrong_answer,     1))
            put(Sfx.BUBBLE,          soundPool.load(context, R.raw.bubble_pop,       1))
        }

        SfxController(soundPool, ids, loaded)
    }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    return controller
}

/** Helper to add SFX to any onClick lambda. */
fun (() -> Unit).withSfx(sfx: SfxController, sound: Sfx = Sfx.CLICK): () -> Unit = {
    sfx.play(sound)
    this()
}
