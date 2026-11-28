package com.example.reading

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * إعدادات الصوت والموسيقى الهادئة داخل واحة القراءة الذكية (تُحفظ محلياً).
 */
data class ReadingSoundSettings(
    val sfxEnabled: Boolean = true,
    val bgMusicEnabled: Boolean = true,
    val bgMusicVolume: Float = 0.36f
)

/**
 * محرك المؤثرات التفاعلية الناعمة والموسيقى الخلفية المولّدة برمجياً لواحة القراءة الذكية:
 * - يستبدل صوت "البيب" الحاد بنقرات خشبية/فقاعية ناعمة وخافتة وقصيرة عبر AudioTrack.
 * - يولّد موسيقى أطفال هادئة شبيهة بصندوق الموسيقى والزايلوفون بدون إنترنت وبدون ملفات إضافية.
 * - يطبّق الخفض التلقائي الذكي (Smart Audio Ducking) فور بدء أي تعليق صوتي أو نطق حرف/كلمة،
 *   ثم يعيد الموسيقى تدريجياً وبنعومة بعد انتهاء النطق ليبقى التعليق الصوتي واضحاً 100%.
 */
class ReadingOasisSoundEngine(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(SOUND_PREFS_NAME, Context.MODE_PRIVATE)
    private val soundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ReadingSoundSettings> = _settings.asStateFlow()

    @Volatile
    private var isReleased = false

    @Volatile
    private var isLifecyclePaused = false

    @Volatile
    private var isVoiceoverActive = false

    @Volatile
    private var duckHoldUntilMs = 0L

    private var musicLoopJob: Job? = null

    // عينات PCM مولّدة مسبقاً ومخزنة بالذاكرة لتشغيل فوري دون أي تأخير
    private val softTapPcm: ShortArray by lazy { synthesizeSoftBubbleWoodTap() }
    private val successChimePcm: ShortArray by lazy { synthesizeSoftSuccessChime() }
    private val victoryFanfarePcm: ShortArray by lazy { synthesizeVictoryXylophone() }
    private val puzzleSnapPcm: ShortArray by lazy { synthesizeMagneticPuzzleSnap() }
    private val gentleTryAgainPcm: ShortArray by lazy { synthesizeGentleTryAgain() }

    init {
        if (_settings.value.bgMusicEnabled) {
            startBackgroundMusicIfNeeded()
        }
    }

    private fun loadSettings(): ReadingSoundSettings {
        val sfx = prefs.getBoolean(KEY_SFX_ENABLED, true)
        val music = prefs.getBoolean(KEY_BG_MUSIC_ENABLED, true)
        val volume = prefs.getFloat(KEY_BG_MUSIC_VOLUME, 0.36f).coerceIn(0.05f, 1.0f)
        return ReadingSoundSettings(
            sfxEnabled = sfx,
            bgMusicEnabled = music,
            bgMusicVolume = volume
        )
    }

    private fun persistSettings(value: ReadingSoundSettings) {
        prefs.edit()
            .putBoolean(KEY_SFX_ENABLED, value.sfxEnabled)
            .putBoolean(KEY_BG_MUSIC_ENABLED, value.bgMusicEnabled)
            .putFloat(KEY_BG_MUSIC_VOLUME, value.bgMusicVolume.coerceIn(0.05f, 1.0f))
            .apply()
    }

    fun setSfxEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(sfxEnabled = enabled)
        _settings.value = updated
        persistSettings(updated)
        if (enabled) {
            playSoftTap()
        }
    }

    fun setBgMusicEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(bgMusicEnabled = enabled)
        _settings.value = updated
        persistSettings(updated)
        if (enabled && !isLifecyclePaused && !isReleased) {
            startBackgroundMusicIfNeeded()
        } else {
            stopBackgroundMusicLoop()
        }
    }

    fun setBgMusicVolume(volume: Float) {
        val clamped = volume.coerceIn(0.05f, 1.0f)
        val updated = _settings.value.copy(bgMusicVolume = clamped)
        _settings.value = updated
        persistSettings(updated)
    }

    /**
     * يُستدعى تلقائياً عند بدء أو انتهاء أي تعليق صوتي تعليمي لتطبيق الخفض الذكي السلس.
     */
    fun setVoiceoverSpeaking(speaking: Boolean) {
        isVoiceoverActive = speaking
        if (!speaking) {
            // إبقاء الخفض لفترة قصيرة بعد انتهاء الجملة لمنع التذبذب بين المقاطع المتتابعة
            duckHoldUntilMs = System.currentTimeMillis() + 300L
        }
    }

    fun pauseForLifecycle() {
        isLifecyclePaused = true
        stopBackgroundMusicLoop()
    }

    fun resumeForLifecycle() {
        isLifecyclePaused = false
        if (!isReleased && _settings.value.bgMusicEnabled) {
            startBackgroundMusicIfNeeded()
        }
    }

    fun playSoftTap() {
        if (isReleased || !_settings.value.sfxEnabled) return
        playShortPcmAsync(softTapPcm, volumeGain = 0.72f)
    }

    fun playSoftSuccessChime() {
        if (isReleased || !_settings.value.sfxEnabled) return
        playShortPcmAsync(successChimePcm, volumeGain = 0.80f)
    }

    fun playSoftVictoryFanfare() {
        if (isReleased || !_settings.value.sfxEnabled) return
        playShortPcmAsync(victoryFanfarePcm, volumeGain = 0.85f)
    }

    fun playPuzzleSnap() {
        if (isReleased || !_settings.value.sfxEnabled) return
        playShortPcmAsync(puzzleSnapPcm, volumeGain = 0.86f)
    }

    fun playGentleTryAgain() {
        if (isReleased || !_settings.value.sfxEnabled) return
        playShortPcmAsync(gentleTryAgainPcm, volumeGain = 0.70f)
    }

    private fun playShortPcmAsync(pcm: ShortArray, volumeGain: Float) {
        soundScope.launch(Dispatchers.IO) {
            if (isReleased || pcm.isEmpty()) return@launch
            var track: AudioTrack? = null
            try {
                val byteSize = pcm.size * 2
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(byteSize)

                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val format = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                val effectiveGain = if (isVoiceoverActive) volumeGain * 0.50f else volumeGain
                track.setVolume(effectiveGain.coerceIn(0.05f, 1.0f))
                track.write(pcm, 0, pcm.size)
                track.play()

                val durationMs = ((pcm.size * 1000L) / SAMPLE_RATE) + 45L
                delay(durationMs)
            } catch (_: Throwable) {
                // تجاهل بهدوء في حال انشغال جهاز الصوت
            } finally {
                runCatching {
                    track?.stop()
                    track?.release()
                }
            }
        }
    }

    private fun startBackgroundMusicIfNeeded() {
        if (isReleased || isLifecyclePaused || !_settings.value.bgMusicEnabled) return
        if (musicLoopJob?.isActive == true) return

        musicLoopJob = soundScope.launch(Dispatchers.IO) {
            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                val format = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()

                // لحن طفولي هادئ ولطيف يشبه صندوق الموسيقى والزايلوفون (سلّم دو الخماسي الدافئ)
                // كل عنصر: (تردد اللحن العلوي Hz، تردد المرافقة السفلية Hz أو 0، المدة بالثواني)
                val melody = listOf(
                    MusicStep(523.25, 261.63, 0.42), // C5 + C4
                    MusicStep(659.25, 0.0, 0.42),    // E5
                    MusicStep(783.99, 329.63, 0.42), // G5 + E4
                    MusicStep(659.25, 0.0, 0.42),    // E5
                    MusicStep(880.00, 349.23, 0.48), // A5 + F4
                    MusicStep(783.99, 0.0, 0.40),    // G5
                    MusicStep(659.25, 261.63, 0.72), // E5 + C4
                    MusicStep(0.0, 0.0, 0.24),       // سكتة لطيفة

                    MusicStep(587.33, 293.66, 0.42), // D5 + D4
                    MusicStep(659.25, 0.0, 0.42),    // E5
                    MusicStep(783.99, 392.00, 0.42), // G5 + G4
                    MusicStep(880.00, 0.0, 0.42),    // A5
                    MusicStep(1046.50, 329.63, 0.54),// C6 + E4
                    MusicStep(880.00, 0.0, 0.36),    // A5
                    MusicStep(783.99, 392.00, 0.74), // G5 + G4
                    MusicStep(0.0, 0.0, 0.26),       // سكتة لطيفة

                    MusicStep(659.25, 261.63, 0.42), // E5 + C4
                    MusicStep(783.99, 0.0, 0.42),    // G5
                    MusicStep(880.00, 349.23, 0.42), // A5 + F4
                    MusicStep(783.99, 0.0, 0.42),    // G5
                    MusicStep(659.25, 329.63, 0.44), // E5 + E4
                    MusicStep(587.33, 293.66, 0.44), // D5 + D4
                    MusicStep(523.25, 261.63, 0.84), // C5 + C4
                    MusicStep(0.0, 0.0, 0.36)        // استراحة ناعمة قبل إعادة الدورة
                )

                val chunkSamples = 512
                val buffer = ShortArray(chunkSamples)
                var stepIndex = 0
                var sampleInStep = 0
                var currentDuckGain = 1.0f
                var smoothUserVolume = _settings.value.bgMusicVolume

                while (isActive && !isReleased && !isLifecyclePaused && _settings.value.bgMusicEnabled) {
                    val step = melody[stepIndex]
                    val totalStepSamples = (step.durationSec * SAMPLE_RATE).toInt().coerceAtLeast(1)

                    val nowMs = System.currentTimeMillis()
                    val shouldDuck = isVoiceoverActive || nowMs < duckHoldUntilMs
                    val targetDuck = if (shouldDuck) DUCKED_GAIN else 1.0f
                    val targetUserVol = _settings.value.bgMusicVolume.coerceIn(0.0f, 1.0f)

                    // سرعة هبوط سريعة وناعمة عند بدء النطق، وعودة أبطأ وهادئة بعد الانتهاء
                    val duckRate = if (shouldDuck) 0.0028f else 0.00055f

                    for (i in 0 until chunkSamples) {
                        currentDuckGain += (targetDuck - currentDuckGain) * duckRate
                        smoothUserVolume += (targetUserVol - smoothUserVolume) * 0.002f

                        val t = sampleInStep.toDouble() / SAMPLE_RATE.toDouble()
                        var sampleVal = 0.0

                        if (step.melodyHz > 0.0) {
                            // غلاف زايلوفون/صندوق موسيقى ناعم (دخول فائق النعومة وتلاشي دافئ)
                            val attack = (t / 0.012).coerceIn(0.0, 1.0)
                            val decay = exp(-t * 4.4)
                            val env = attack * decay
                            val fundamental = sin(2.0 * PI * step.melodyHz * t)
                            val harmonic2 = sin(2.0 * PI * step.melodyHz * 2.0 * t) * 0.16 * exp(-t * 8.5)
                            sampleVal += (fundamental + harmonic2) * env * 0.62
                        }

                        if (step.bassHz > 0.0) {
                            val bassAttack = (t / 0.025).coerceIn(0.0, 1.0)
                            val bassDecay = exp(-t * 2.6)
                            val bassWave = sin(2.0 * PI * step.bassHz * t)
                            sampleVal += bassWave * bassAttack * bassDecay * 0.28
                        }

                        // مستوى أقصى لطيف جداً لكي تظل الموسيقى مريحة وغير مزعجة
                        val scaled = sampleVal * smoothUserVolume * currentDuckGain * MAX_BG_AMPLITUDE * 32767.0
                        buffer[i] = scaled.roundToInt().coerceIn(-32767, 32767).toShort()

                        sampleInStep++
                        if (sampleInStep >= totalStepSamples) {
                            sampleInStep = 0
                            stepIndex = (stepIndex + 1) % melody.size
                        }
                    }

                    val written = track.write(buffer, 0, chunkSamples)
                    if (written < 0) break
                }
            } catch (_: Throwable) {
                // إغلاق هادئ
            } finally {
                runCatching {
                    track?.pause()
                    track?.flush()
                    track?.stop()
                    track?.release()
                }
            }
        }
    }

    private fun stopBackgroundMusicLoop() {
        musicLoopJob?.cancel()
        musicLoopJob = null
    }

    fun release() {
        isReleased = true
        stopBackgroundMusicLoop()
        soundScope.cancel()
    }

    private data class MusicStep(
        val melodyHz: Double,
        val bassHz: Double,
        val durationSec: Double
    )

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val DUCKED_GAIN = 0.16f
        private const val MAX_BG_AMPLITUDE = 0.24
        private const val SOUND_PREFS_NAME = "reading_oasis_sound_prefs"
        private const val KEY_SFX_ENABLED = "sfx_enabled"
        private const val KEY_BG_MUSIC_ENABLED = "bg_music_enabled"
        private const val KEY_BG_MUSIC_VOLUME = "bg_music_volume"

        /**
         * نقرة خشبية/فقاعية ناعمة جداً وخافتة وقصيرة (~38ms) لا تسبب أي إزعاج أو تداخل مع التعليق الصوتي.
         */
        private fun synthesizeSoftBubbleWoodTap(): ShortArray {
            val durationSec = 0.038
            val count = (SAMPLE_RATE * durationSec).toInt()
            val out = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / count.toDouble()
                // ارتفاع ترددي ناعم يشبه فقاعة ماء أو نقرة ماريمبا خشبية دافئة
                val freq = 390.0 + 210.0 * progress
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val attack = (t / 0.004).coerceIn(0.0, 1.0)
                val decay = exp(-t * 92.0)
                val wave = sin(phase) * 0.85 + sin(phase * 1.5) * 0.15
                val sample = wave * attack * decay * 0.22 * 32767.0
                out[i] = sample.roundToInt().coerceIn(-32767, 32767).toShort()
            }
            return out
        }

        /**
         * رنّة نجاح ناعمة دافئة من نغمتين خشبيتين.
         */
        private fun synthesizeSoftSuccessChime(): ShortArray {
            val notes = listOf(587.33 to 0.0, 880.0 to 0.095) // D5 -> A5
            val durationSec = 0.34
            val count = (SAMPLE_RATE * durationSec).toInt()
            val out = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                var sum = 0.0
                for ((freq, startSec) in notes) {
                    val lt = t - startSec
                    if (lt >= 0.0) {
                        val env = (lt / 0.008).coerceIn(0.0, 1.0) * exp(-lt * 13.5)
                        sum += (sin(2.0 * PI * freq * lt) + 0.18 * sin(2.0 * PI * freq * 2.0 * lt)) * env
                    }
                }
                out[i] = (sum * 0.24 * 32767.0).roundToInt().coerceIn(-32767, 32767).toShort()
            }
            return out
        }

        /**
         * نغمة احتفالية لطيفة على الزايلوفون عند إنجاز محطة أو كلمة.
         */
        private fun synthesizeVictoryXylophone(): ShortArray {
            val notes = listOf(
                523.25 to 0.00,  // C5
                659.25 to 0.09,  // E5
                783.99 to 0.18,  // G5
                1046.50 to 0.28  // C6
            )
            val durationSec = 0.62
            val count = (SAMPLE_RATE * durationSec).toInt()
            val out = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                var sum = 0.0
                for ((freq, startSec) in notes) {
                    val lt = t - startSec
                    if (lt >= 0.0) {
                        val env = (lt / 0.008).coerceIn(0.0, 1.0) * exp(-lt * 10.0)
                        sum += (sin(2.0 * PI * freq * lt) + 0.20 * sin(2.0 * PI * freq * 2.0 * lt)) * env
                    }
                }
                out[i] = (sum * 0.23 * 32767.0).roundToInt().coerceIn(-32767, 32767).toShort()
            }
            return out
        }

        /**
         * صوت تعشيق البازل المغناطيسي: نقرة التحام لطيفة تليها ومضة موسيقية مبهجة.
         */
        private fun synthesizeMagneticPuzzleSnap(): ShortArray {
            val durationSec = 0.36
            val count = (SAMPLE_RATE * durationSec).toInt()
            val out = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                var sum = 0.0
                // نقرة تعشيق خشبية سريعة
                if (t < 0.045) {
                    val clickEnv = (t / 0.003).coerceIn(0.0, 1.0) * exp(-t * 95.0)
                    sum += sin(2.0 * PI * (310.0 + t * 4200.0) * t) * clickEnv * 0.9
                }
                // بريق نغمتين متصاعدتين لحظة الالتحام
                val t1 = t - 0.030
                if (t1 >= 0.0) {
                    val env1 = (t1 / 0.007).coerceIn(0.0, 1.0) * exp(-t1 * 14.0)
                    sum += sin(2.0 * PI * 783.99 * t1) * env1 * 0.72
                }
                val t2 = t - 0.095
                if (t2 >= 0.0) {
                    val env2 = (t2 / 0.007).coerceIn(0.0, 1.0) * exp(-t2 * 11.5)
                    sum += (sin(2.0 * PI * 1046.50 * t2) + 0.2 * sin(2.0 * PI * 2093.0 * t2)) * env2 * 0.78
                }
                out[i] = (sum * 0.25 * 32767.0).roundToInt().coerceIn(-32767, 32767).toShort()
            }
            return out
        }

        /**
         * نغمة تنبيه هابطة ناعمة جداً عند اختيار قطعة خاطئة (بدلاً من صوت الخطأ الحاد).
         */
        private fun synthesizeGentleTryAgain(): ShortArray {
            val durationSec = 0.19
            val count = (SAMPLE_RATE * durationSec).toInt()
            val out = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val freq = 350.0 - 95.0 * (i.toDouble() / count.toDouble())
                phase += 2.0 * PI * freq / SAMPLE_RATE
                val env = (t / 0.012).coerceIn(0.0, 1.0) * exp(-t * 18.0)
                val sample = sin(phase) * env * 0.20 * 32767.0
                out[i] = sample.roundToInt().coerceIn(-32767, 32767).toShort()
            }
            return out
        }
    }
}

/**
 * أيقونة إعدادات صوت الواحة المرسومة بـ Canvas (ترس لطيف متوهج مع نوتة موسيقية).
 */
@Composable
fun PlayfulSoundSettingsIcon(
    musicActive: Boolean,
    tint: Color = ReadingColors.InkDark,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "sound_settings_icon")
    val gearAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_spin"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.50f
        val outerR = w * 0.42f
        val innerR = w * 0.28f

        rotate(degrees = if (musicActive) gearAngle else 0f, pivot = Offset(cx, cy)) {
            val teeth = 8
            for (i in 0 until teeth) {
                val angle = i * (2.0 * PI / teeth)
                val p1 = Offset(cx + (innerR * cos(angle)).toFloat(), cy + (innerR * sin(angle)).toFloat())
                val p2 = Offset(cx + (outerR * cos(angle)).toFloat(), cy + (outerR * sin(angle)).toFloat())
                drawLine(
                    color = tint,
                    start = p1,
                    end = p2,
                    strokeWidth = w * 0.14f,
                    cap = StrokeCap.Round
                )
            }
        }
        drawCircle(
            color = tint,
            radius = innerR,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.11f)
        )
        drawCircle(
            color = if (musicActive) ReadingColors.MeadowGreen else tint.copy(alpha = 0.5f),
            radius = w * 0.11f,
            center = Offset(cx, cy)
        )
    }
}

/**
 * أيقونة نوتة موسيقية طفولية مرسومة على Canvas لنافذة إعدادات الصوت.
 */
@Composable
fun PlayfulMusicNoteIcon(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val stem = Path().apply {
            moveTo(w * 0.36f, h * 0.72f)
            lineTo(w * 0.36f, h * 0.24f)
            lineTo(w * 0.78f, h * 0.16f)
            lineTo(w * 0.78f, h * 0.62f)
        }
        drawPath(stem, color = tint, style = stroke)
        drawCircle(color = tint, radius = w * 0.13f, center = Offset(w * 0.28f, h * 0.74f))
        drawCircle(color = tint, radius = w * 0.13f, center = Offset(w * 0.70f, h * 0.64f))
        if (!active) {
            drawLine(
                color = ReadingColors.CoralOrange,
                start = Offset(w * 0.14f, h * 0.16f),
                end = Offset(w * 0.86f, h * 0.86f),
                strokeWidth = w * 0.11f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * نافذة إعدادات صوت وموسيقى واحة القراءة الذكية بأسلوب طفولي واضح ومبهج.
 */
@Composable
fun ReadingSoundSettingsDialog(
    settings: ReadingSoundSettings,
    onToggleSfx: (Boolean) -> Unit,
    onToggleBgMusic: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color.White,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(3.dp, ReadingColors.SunYellow, RoundedCornerShape(30.dp))
                    .testTag("reading_sound_settings_dialog")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ترويسة مبهجة
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0EA5E9),
                                        Color(0xFF38BDF8),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.24f)),
                                contentAlignment = Alignment.Center
                            ) {
                                PlayfulMusicNoteIcon(
                                    active = settings.bgMusicEnabled,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "إعدادات صوت الواحة",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "تحكّم بالمؤثرات والموسيقى الهادئة بكل سهولة",
                                    fontFamily = ReadingTajawalFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color.White.copy(alpha = 0.94f)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1) مفتاح النقرات والمؤثرات التفاعلية الناعمة
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF0F9FF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color(0xFFBAE6FD), RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(ReadingColors.SkyBlue.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PlayfulSpeakerIcon(
                                            isSpeaking = settings.sfxEnabled,
                                            tint = ReadingColors.SkyBlueShadow,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "صوت النقرات والمؤثرات",
                                            fontFamily = ReadingTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.5.sp,
                                            color = ReadingColors.InkDark
                                        )
                                        Text(
                                            text = "نقرة خشبية ناعمة ورنّات التشجيع",
                                            fontFamily = ReadingTajawalFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = ReadingColors.InkSoft
                                        )
                                    }
                                }
                                Switch(
                                    checked = settings.sfxEnabled,
                                    onCheckedChange = onToggleSfx,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ReadingColors.MeadowGreen
                                    ),
                                    modifier = Modifier.testTag("reading_sound_sfx_switch")
                                )
                            }
                        }

                        // 2) مفتاح موسيقى الأطفال الهادئة بالخلفية + شريط التحكم بمستوى الصوت
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFAF5FF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color(0xFFE9D5FF), RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(ReadingColors.BerryPurple.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            PlayfulMusicNoteIcon(
                                                active = settings.bgMusicEnabled,
                                                tint = ReadingColors.BerryPurple,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "موسيقى الواحة الهادئة",
                                                fontFamily = ReadingTajawalFont,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.5.sp,
                                                color = ReadingColors.InkDark
                                            )
                                            Text(
                                                text = "لحن صندوق موسيقى هادئ بدون إنترنت",
                                                fontFamily = ReadingTajawalFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = ReadingColors.InkSoft
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = settings.bgMusicEnabled,
                                        onCheckedChange = onToggleBgMusic,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = ReadingColors.BerryPurple
                                        ),
                                        modifier = Modifier.testTag("reading_sound_music_switch")
                                    )
                                }

                                // شريط التحكم السلس بمستوى الصوت
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "مستوى صوت الموسيقى الخلفية",
                                            fontFamily = ReadingTajawalFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = if (settings.bgMusicEnabled) ReadingColors.InkDark else ReadingColors.InkSoft
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (settings.bgMusicEnabled) ReadingColors.BerryPurple else Color(0xFFCBD5E1)
                                        ) {
                                            Text(
                                                text = "${(settings.bgMusicVolume * 100).roundToInt()}%",
                                                fontFamily = ReadingTajawalFont,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Slider(
                                        value = settings.bgMusicVolume,
                                        onValueChange = onVolumeChange,
                                        enabled = settings.bgMusicEnabled,
                                        valueRange = 0.05f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ReadingColors.BerryPurple,
                                            activeTrackColor = ReadingColors.BerryPurple,
                                            inactiveTrackColor = Color(0xFFE9D5FF)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reading_sound_music_slider")
                                    )
                                }
                            }
                        }

                        // ملاحظة مطمئنة عن الخفض التلقائي الذكي
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFECFDF5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(16.dp))
                        ) {
                            Text(
                                text = "تنخفض الموسيقى تلقائياً وبنعومة عند نطق الحروف والكلمات ليبقى التعليق الصوتي واضحاً تماماً.",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF065F46),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        PlayfulBouncyButton(
                            onClick = onDismiss,
                            backgroundColor = ReadingColors.MeadowGreen,
                            shadowColor = ReadingColors.MeadowShadow,
                            cornerRadius = 20.dp,
                            lipDepth = 4.dp,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            testTagValue = "reading_sound_settings_done_btn"
                        ) {
                            Text(
                                text = "حفظ وإغلاق",
                                fontFamily = ReadingTajawalFont,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
