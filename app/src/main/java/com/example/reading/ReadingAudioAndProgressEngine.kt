package com.example.reading

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

data class ReadingHeroRankInfo(
    val level: Int,
    val title: String,
    val minStars: Int,
    val nextLevelStars: Int?
)

data class ReadingLeaderboardEntry(
    val rank: Int,
    val studentId: String,
    val studentName: String,
    val grade: String,
    val totalStars: Int,
    val completedLessons: Int,
    val avatarId: String,
    val level: Int,
    val heroTitle: String,
    val nextLevelStars: Int?,
    val lastUpdated: Long
)

sealed interface LeaderboardFetchResult {
    data class Success(val entries: List<ReadingLeaderboardEntry>) : LeaderboardFetchResult
    data class Error(val message: String) : LeaderboardFetchResult
}

/**
 * حساب اللقب والمستوى التلقائي للطالب في واحة القراءة الذكية حسب مجموع نجماته.
 */
fun resolveReadingHeroTitle(totalStars: Int): ReadingHeroRankInfo = when {
    totalStars <= 5 -> ReadingHeroRankInfo(level = 1, title = "مستكشف الحروف", minStars = 0, nextLevelStars = 6)
    totalStars <= 15 -> ReadingHeroRankInfo(level = 2, title = "نجم المقاطع", minStars = 6, nextLevelStars = 16)
    totalStars <= 30 -> ReadingHeroRankInfo(level = 3, title = "بطل الكلمات", minStars = 16, nextLevelStars = 31)
    totalStars <= 50 -> ReadingHeroRankInfo(level = 4, title = "فارس القراءة", minStars = 31, nextLevelStars = 51)
    else -> ReadingHeroRankInfo(level = 5, title = "أسطورة الواحة", minStars = 51, nextLevelStars = null)
}

data class ReadingProgressState(
    val lessonStars: Map<String, Int> = emptyMap(),
    val completedPillars: Map<String, Set<String>> = emptyMap(),
    val bonusStars: Int = 0,
    val avatarId: String = ReadingAvatarCatalog.DEFAULT_AVATAR_ID,
    val updatedAt: Long = 0L
) {
    val totalStars: Int
        get() = lessonStars.values.sum() + bonusStars

    val completedLessonsCount: Int
        get() = lessonStars.count { it.value > 0 }

    fun starsForLesson(lessonId: String): Int = (lessonStars[lessonId] ?: 0).coerceIn(0, 3)
}

/**
 * محرك الصوت الحقيقي (Real Human/Child MP3 Voice Pack) وحفظ التقدم في واحة القراءة الذكية:
 * 1) أولوية التشغيل الفوري والآمن عبر android.media.MediaPlayer بالترتيب:
 *    (أ) الذاكرة المحلية للتطبيق: filesDir/reading_audio/{md5_12}.mp3
 *    (ب) حزمة الأصول المدمجة للعمل بدون إنترنت: assets/reading_audio/{md5_12}.mp3 (مع فك ضغط تلقائي إلى cacheDir)
 *    (ج) تقسيم تلقائي للعبارات المركّبة عند علامات الترقيم (، : . ! ؟) وتشغيل أجزائها بالتتابع عبر MP3
 *    (د) تحميل اختياري لمرة واحدة في الخلفية عند ضبط remoteVoiceBaseUrl
 *    (هـ) إيقاف تام للصوت الآلي (ENABLE_ROBOTIC_TTS_FALLBACK = false) لضمان صوت بشري حقيقي 100%.
 * 2) نطق متتابع ومقطعي متزامن بالصوت الحقيقي عبر MediaPlayer.setOnCompletionListener.
 * 3) حفظ محلي فوري للنجوم والشخصية الرمزية ومزامنة ساحة أبطال الصف مع Firebase RTDB.
 */
class ReadingAudioAndProgressEngine(
    context: Context,
    private val schoolCode: String,
    private val studentId: String,
    private val studentName: String,
    private val studentGrade: String = ""
) {
    private val appContext = context.applicationContext
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var tts: TextToSpeech? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private val pendingTtsContinuations = ConcurrentHashMap<String, CancellableContinuation<Unit>>()
    private val inFlightDownloads = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var isTtsReady = false
    @Volatile
    private var isReleased = false
    private var pendingSpeech: String? = null
    private var segmentJob: Job? = null
    private var speakingTimeoutJob: Job? = null

    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 78)
    }.getOrNull()

    private val _progress = MutableStateFlow(loadCachedProgress(appContext, schoolCode, studentId))
    val progress: StateFlow<ReadingProgressState> = _progress.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        if (ENABLE_ROBOTIC_TTS_FALLBACK) {
            initTextToSpeech()
        }
        syncFromCloudInBackground()
    }

    private fun initTextToSpeech() {
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                if (isReleased) return@TextToSpeech
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@TextToSpeech
                    selectBestArabicVoice(engine)
                    engine.setSpeechRate(CHILD_SPEECH_RATE)
                    engine.setPitch(CHILD_SPEECH_PITCH)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            utteranceId?.let { id ->
                                pendingTtsContinuations.remove(id)?.let { cont ->
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            }
                            if (segmentJob?.isActive != true && activeMediaPlayer == null) {
                                _isSpeaking.value = false
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            utteranceId?.let { id ->
                                pendingTtsContinuations.remove(id)?.let { cont ->
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            }
                            if (segmentJob?.isActive != true && activeMediaPlayer == null) {
                                _isSpeaking.value = false
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            utteranceId?.let { id ->
                                pendingTtsContinuations.remove(id)?.let { cont ->
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            }
                            if (segmentJob?.isActive != true && activeMediaPlayer == null) {
                                _isSpeaking.value = false
                            }
                        }
                    })
                    isTtsReady = true
                    val queued = pendingSpeech
                    if (!queued.isNullOrBlank()) {
                        pendingSpeech = null
                        speak(queued)
                    }
                }
            }
        }
    }

    /**
     * يبحث في tts.voices عن أفضل صوت عربي عالي الجودة (يفضّل الأصوات النسائية/الشبابية
     * الشبكية أو العصبية مثل ar-xa و ar-SA و ar-EG ويستبعد الأصوات الآلية الرديئة).
     */
    private fun selectBestArabicVoice(engine: TextToSpeech) {
        val hasNetwork = isNetworkAvailable()
        val bestVoice = runCatching {
            val allVoices = engine.voices.orEmpty()
            allVoices
                .filter { voice ->
                    val lang = voice.locale?.language.orEmpty()
                    val features = voice.features.orEmpty()
                    val isInstalled = !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                    val networkOk = !voice.isNetworkConnectionRequired || hasNetwork
                    lang.equals("ar", ignoreCase = true) && isInstalled && networkOk
                }
                .maxByOrNull { voice -> scoreArabicVoice(voice, hasNetwork) }
        }.getOrNull()

        if (bestVoice != null) {
            val setRes = runCatching { engine.voice = bestVoice }.isSuccess
            if (setRes) return
        }

        val fallbackLocales = listOf(
            Locale("ar", "SA"),
            Locale("ar", "EG"),
            Locale("ar", "IQ"),
            Locale("ar")
        )
        for (loc in fallbackLocales) {
            val res = engine.setLanguage(loc)
            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                break
            }
        }
    }

    private fun scoreArabicVoice(voice: Voice, hasNetwork: Boolean): Int {
        val name = voice.name.lowercase(Locale.ROOT)
        val localeTag = voice.locale?.toLanguageTag().orEmpty().lowercase(Locale.ROOT)
        var score = 0

        // تقييم جودة الصوت من النظام
        score += when {
            voice.quality >= Voice.QUALITY_VERY_HIGH -> 130
            voice.quality >= Voice.QUALITY_HIGH -> 105
            voice.quality >= Voice.QUALITY_NORMAL -> 45
            else -> -90
        }

        // تفضيل الأصوات العصبية/الشبكية عالية الجودة عند توفر الاتصال
        if (name.contains("network") || name.contains("wavenet") || name.contains("neural")) {
            score += if (hasNetwork) 55 else -40
        }
        if (name.contains("local") && voice.quality >= Voice.QUALITY_HIGH) {
            score += 35
        }

        // تفضيل نطاقات ar-xa و ar-SA و ar-EG المعروفة بجودتها العالية
        if (name.contains("ar-xa") || localeTag.startsWith("ar-xa")) score += 40
        if (localeTag.startsWith("ar-sa") || name.contains("ar-sa")) score += 32
        if (localeTag.startsWith("ar-eg") || name.contains("ar-eg")) score += 28
        if (localeTag.startsWith("ar-iq")) score += 24

        // تفضيل الأصوات النسائية/الشبابية اللطيفة المناسبة للأطفال (مثل ar-xa-x-arz / ard / arf)
        val warmFemaleHints = listOf(
            "arz", "ard", "arf", "arc", "female", "woman", "girl",
            "sara", "salma", "leila", "laila", "mariam", "hoda", "zeina", "f0", "w0"
        )
        if (warmFemaleHints.any { name.contains(it) }) {
            score += 35
        }

        // استبعاد الأصوات الآلية أو الرديئة القديمة
        val roboticHints = listOf("espeak", "pico", "classic", "low", "robot", "compact", "male", "arm", "arn")
        if (roboticHints.any { name.contains(it) }) {
            score -= 65
        }

        return score
    }

    private fun isNetworkAvailable(): Boolean {
        return runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val net = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(net) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnected == true
            }
        }.getOrDefault(false)
    }

    /**
     * توليد اسم الملف الصوتي الموحّد (أول 12 حرفاً صغيراً من بصمة MD5 + ".mp3").
     */
    fun audioFileNameForText(text: String): String = Companion.audioFileNameForText(text)

    /**
     * يشغّل الصوت الحقيقي المطابق للنص من حزمة الـ MP3 بالأولوية:
     * (أ) filesDir/reading_audio/{fileName}
     * (ب) assets/reading_audio/{fileName} (مع فك ضغط تلقائي لمرة واحدة في cacheDir/reading_audio/)
     * (ج) إذا لم يُعثر على ملف مطابق للنص الكامل، يفحص وجود علامات الترقيم (، أو : أو . أو ! أو ؟)
     *     ويقّسم النص لتشغيل كل جزء عبر ملفه الصوتي بالترتيب عبر speakSequence.
     * (د) تحميل تلقائي في الخلفية لمرة واحدة (إذا تم ضبط remoteVoiceBaseUrl)
     * (هـ) لا يعمل TextToSpeech الآلي مطلقاً طالما ENABLE_ROBOTIC_TTS_FALLBACK = false.
     */
    fun speak(text: String, audioKey: String? = null) {
        if (isReleased) return
        val cleaned = normalizeTextForAudio(text)
        if (cleaned.isEmpty()) return

        segmentJob?.cancel()
        speakingTimeoutJob?.cancel()
        stopActiveMediaPlayer()
        runCatching { tts?.stop() }

        val md5FileName = audioFileNameForText(cleaned)
        val resolvedLegacyKey = audioKey?.takeIf { it.isNotBlank() } ?: mapTextToAudioKey(cleaned)

        if (playMp3Priority(fileName = md5FileName, legacyAudioKey = resolvedLegacyKey)) {
            return
        }

        // إذا لم يُعثر على ملف MP3 مطابق للنص الكامل، قسّم النص عند علامات الترقيم وشغّل كل جزء بالترتيب
        val punctuationParts = splitByPunctuation(cleaned)
        if (punctuationParts.size > 1 || (punctuationParts.size == 1 && punctuationParts[0] != cleaned)) {
            speakSequence(*punctuationParts.toTypedArray())
            return
        }

        // تحميل اختياري لمرة واحدة في الخلفية ليعمل بدون إنترنت لاحقاً
        scheduleBackgroundVoiceDownload(md5FileName)

        // إيقاف تام للصوت الآلي عندما يكون ENABLE_ROBOTIC_TTS_FALLBACK = false
        if (ENABLE_ROBOTIC_TTS_FALLBACK) {
            speakWithTtsFallback(cleaned)
        }
    }

    /**
     * يشغّل قائمة نصوص بالتتابع السلس عبر MediaPlayer مع فاصل قصير 120ms بين كل مقطع وآخر
     * عبر setOnCompletionListener، مع تجاهل أي عنصر فارغ.
     */
    fun speakSequence(vararg parts: String) {
        if (isReleased) return
        val cleanedParts = parts
            .map { normalizeTextForAudio(it) }
            .filter { it.isNotEmpty() }
        if (cleanedParts.isEmpty()) return

        segmentJob?.cancel()
        speakingTimeoutJob?.cancel()
        stopActiveMediaPlayer()
        runCatching { tts?.stop() }

        segmentJob = engineScope.launch {
            _isSpeaking.value = true
            try {
                cleanedParts.forEachIndexed { index, part ->
                    val played = playTextOrSplitPunctuationAndAwait(part)
                    if (played && index < cleanedParts.lastIndex) {
                        delay(SEQUENCE_PAUSE_MS)
                    }
                }
            } finally {
                stopActiveMediaPlayer()
                _isSpeaking.value = false
            }
        }
    }

    /**
     * نطق عبارات التشجيع الطفولية الحقيقية عشوائياً عبر ملفات الـ MP3 الثابتة والخالية من المتغيرات.
     */
    fun playEncouragement(isSuccess: Boolean) {
        val phrase = if (isSuccess) {
            SUCCESS_ENCOURAGEMENTS.random()
        } else {
            TRY_AGAIN_ENCOURAGEMENTS.random()
        }
        speak(phrase)
    }

    /**
     * النطق المقطعي المتزامن بالصوت الحقيقي (للكلمات المقطّعة أو أبيات النشيد بيتاً ببيت):
     * يشغّل المقاطع بالتتابع عبر MediaPlayer.setOnCompletionListener مع مهلة لطيفة 180ms بين كل مقطع وآخر،
     * ويرسل فهرس المقطع النشط لحظة بدء تشغيل صوته الحقيقي، ثم يشغّل صوت الكلمة الكاملة في النهاية.
     */
    fun speakSyllablesSequentially(
        syllables: List<String>,
        wholeWord: String? = null,
        pauseBetweenMs: Long = SYLLABLE_PAUSE_MS,
        onActiveIndex: (Int) -> Unit = {},
        onWholeWordActive: (Boolean) -> Unit = {},
        onFinished: () -> Unit = {}
    ) {
        if (isReleased || syllables.isEmpty()) {
            onFinished()
            return
        }
        segmentJob?.cancel()
        speakingTimeoutJob?.cancel()
        stopActiveMediaPlayer()
        runCatching { tts?.stop() }

        segmentJob = engineScope.launch {
            _isSpeaking.value = true
            onWholeWordActive(false)
            try {
                syllables.forEachIndexed { index, rawSyllable ->
                    val cleanedSyllable = normalizeTextForAudio(rawSyllable)
                    if (cleanedSyllable.isNotEmpty()) {
                        onActiveIndex(index)
                        playTextOrSplitPunctuationAndAwait(cleanedSyllable)
                        if (index < syllables.lastIndex || !wholeWord.isNullOrBlank()) {
                            delay(pauseBetweenMs)
                        }
                    }
                }
                onActiveIndex(-1)

                val cleanedWhole = wholeWord?.let { normalizeTextForAudio(it) }.orEmpty()
                if (cleanedWhole.isNotEmpty()) {
                    onWholeWordActive(true)
                    playTextOrSplitPunctuationAndAwait(cleanedWhole)
                    onWholeWordActive(false)
                }
                onFinished()
            } finally {
                onActiveIndex(-1)
                onWholeWordActive(false)
                stopActiveMediaPlayer()
                _isSpeaking.value = false
            }
        }
    }

    /**
     * نطق متسلسل للمقاطع مع إبلاغ الواجهة برقم المقطع النشط لإضاءته وقفزه بالتزامن مع الصوت،
     * ثم نطق الكلمة كاملة في النهاية (متوافق تماماً مع شاشة الدروس الحالية).
     */
    fun speakSegmentedWord(
        word: SegmentedWord,
        onActiveSyllableIndex: (Int) -> Unit,
        onWholeWordActive: (Boolean) -> Unit = {},
        onFinished: () -> Unit = {}
    ) {
        speakSyllablesSequentially(
            syllables = word.syllables.map { it.spokenText },
            wholeWord = word.spokenWord,
            pauseBetweenMs = SYLLABLE_PAUSE_MS,
            onActiveIndex = onActiveSyllableIndex,
            onWholeWordActive = onWholeWordActive,
            onFinished = onFinished
        )
    }

    /**
     * يشغّل نصاً واحداً عبر MP3؛ وإن لم يوجد ملف كامل وكان يحتوي على علامات ترقيم (، : . ! ؟)
     * يقسمه ويشغّل كل جزء عبر ملفه الصوتي بالتتابع مع فاصل 120ms.
     */
    private suspend fun playTextOrSplitPunctuationAndAwait(cleanedText: String): Boolean {
        if (playSingleMp3AndAwait(cleanedText)) {
            return true
        }

        val subParts = splitByPunctuation(cleanedText)
        if (subParts.size > 1 || (subParts.size == 1 && subParts[0] != cleanedText)) {
            var anyPlayed = false
            subParts.forEachIndexed { idx, sub ->
                val playedSub = playSingleMp3AndAwait(sub)
                if (playedSub) {
                    anyPlayed = true
                    if (idx < subParts.lastIndex) {
                        delay(SEQUENCE_PAUSE_MS)
                    }
                }
            }
            if (anyPlayed) return true
        }

        scheduleBackgroundVoiceDownload(audioFileNameForText(cleanedText))

        if (!ENABLE_ROBOTIC_TTS_FALLBACK) {
            return false
        }

        playWithTtsAndAwait(cleanedText)
        return true
    }

    /**
     * يحاول تشغيل ملف MP3 مطابق للنص عبر MediaPlayer.setOnCompletionListener وينتظر اكتماله.
     */
    private suspend fun playSingleMp3AndAwait(cleanedText: String): Boolean {
        val md5FileName = audioFileNameForText(cleanedText)
        val legacyKey = mapTextToAudioKey(cleanedText)

        var startedMp3 = false
        val completedViaMp3 = withTimeoutOrNull(12_000L) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val resumed = AtomicBoolean(false)
                fun finish(ok: Boolean) {
                    if (resumed.compareAndSet(false, true) && cont.isActive) {
                        cont.resume(ok)
                    }
                }
                startedMp3 = playMp3Priority(
                    fileName = md5FileName,
                    legacyAudioKey = legacyKey,
                    keepSpeakingState = true,
                    onComplete = { finish(true) }
                )
                if (!startedMp3) {
                    finish(false)
                } else {
                    cont.invokeOnCancellation {
                        stopActiveMediaPlayer()
                    }
                }
            }
        }

        if (startedMp3 && completedViaMp3 != null) {
            return true
        }
        if (startedMp3) {
            stopActiveMediaPlayer()
            return true
        }
        return false
    }

    private suspend fun playWithTtsAndAwait(cleanedText: String) {
        if (!ENABLE_ROBOTIC_TTS_FALLBACK) return
        if (!isTtsReady || tts == null) {
            delay((cleanedText.length * 85L).coerceIn(450L, 1400L))
            return
        }

        val utteranceId = "seq_tts_${System.nanoTime()}"
        val estimatedTimeout = (cleanedText.length * 140L + 900L).coerceIn(1400L, 6500L)
        withTimeoutOrNull(estimatedTimeout) {
            suspendCancellableCoroutine<Unit> { cont ->
                pendingTtsContinuations[utteranceId] = cont
                cont.invokeOnCancellation {
                    pendingTtsContinuations.remove(utteranceId)
                    runCatching { tts?.stop() }
                }
                val queuedOk = runCatching {
                    tts?.setSpeechRate(CHILD_SPEECH_RATE)
                    tts?.setPitch(CHILD_SPEECH_PITCH)
                    tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.SUCCESS
                }.getOrDefault(false)
                if (!queuedOk) {
                    pendingTtsContinuations.remove(utteranceId)
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    private fun speakWithTtsFallback(cleanedText: String) {
        if (!ENABLE_ROBOTIC_TTS_FALLBACK) return
        if (!isTtsReady) {
            pendingSpeech = cleanedText
            return
        }
        runCatching {
            val utteranceId = "reading_utterance_${System.nanoTime()}"
            _isSpeaking.value = true
            tts?.setSpeechRate(CHILD_SPEECH_RATE)
            tts?.setPitch(CHILD_SPEECH_PITCH)
            tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            speakingTimeoutJob?.cancel()
            speakingTimeoutJob = engineScope.launch {
                val estimatedMs = (cleanedText.length * 95L + 400L).coerceIn(700L, 4500L)
                delay(estimatedMs)
                if (activeMediaPlayer == null && segmentJob?.isActive != true) {
                    _isSpeaking.value = false
                }
            }
        }
    }

    /**
     * يبحث عن ملف الصوت الحقيقي MP3 بالترتيب المعتمد ويشغّله فوراً عبر android.media.MediaPlayer:
     * (أ) context.filesDir/reading_audio/{fileName}
     * (ب) assets/reading_audio/{fileName} (openFd أولاً، وإذا كان مضغوطاً يُنسخ لمرة واحدة إلى cacheDir/reading_audio/{fileName})
     * ثم التحقق التوافقي الاختياري من legacyAudioKey (filesDir / cacheDir / res/raw).
     */
    private fun playMp3Priority(
        fileName: String,
        legacyAudioKey: String? = null,
        keepSpeakingState: Boolean = false,
        onComplete: (() -> Unit)? = null
    ): Boolean {
        if (isReleased) return false
        stopActiveMediaPlayer()

        // (أ) الذاكرة المحلية للتطبيق: context.filesDir/reading_audio/{fileName}
        val localFilesMp3 = File(File(appContext.filesDir, AUDIO_CACHE_DIR), fileName)
        if (localFilesMp3.exists() && localFilesMp3.length() > MIN_VALID_MP3_BYTES) {
            if (startMediaPlayerFromFile(localFilesMp3, keepSpeakingState, onComplete)) {
                return true
            }
        }

        // (ب-1) التحقق أولاً من نسخة مستخرجة مسبقاً في cacheDir/reading_audio/{fileName}
        val cachedAssetMp3 = File(File(appContext.cacheDir, AUDIO_CACHE_DIR), fileName)
        if (cachedAssetMp3.exists() && cachedAssetMp3.length() > MIN_VALID_MP3_BYTES) {
            if (startMediaPlayerFromFile(cachedAssetMp3, keepSpeakingState, onComplete)) {
                return true
            }
        }

        // (ب-2) حزمة الأصول المدمجة: assets/reading_audio/{fileName}
        val assetPath = "$AUDIO_CACHE_DIR/$fileName"
        if (tryPlayFromAssets(assetPath, cachedAssetMp3, keepSpeakingState, onComplete)) {
            return true
        }

        // توافق إضافي مع مفاتيح الصوت المسماة (في حال توفر ملف باسم مفتاح صريح أو res/raw)
        if (!legacyAudioKey.isNullOrBlank()) {
            val legacyCandidates = listOf(
                File(File(appContext.filesDir, AUDIO_CACHE_DIR), "$legacyAudioKey.mp3"),
                File(File(appContext.cacheDir, AUDIO_CACHE_DIR), "$legacyAudioKey.mp3")
            )
            val legacyFile = legacyCandidates.firstOrNull { it.exists() && it.length() > MIN_VALID_MP3_BYTES }
            if (legacyFile != null && startMediaPlayerFromFile(legacyFile, keepSpeakingState, onComplete)) {
                return true
            }

            val rawResId = appContext.resources.getIdentifier(legacyAudioKey, "raw", appContext.packageName)
            if (rawResId != 0 && startMediaPlayerFromRawRes(rawResId, keepSpeakingState, onComplete)) {
                return true
            }
        }

        return false
    }

    /**
     * يجرّب تشغيل الملف مباشرة من assets عبر openFd؛ وإذا كان مضغوطاً ينسخه لمرة واحدة
     * إلى cacheDir/reading_audio/{fileName} ويشغّله عبر MediaPlayer.setDataSource(file.absolutePath).
     */
    private fun tryPlayFromAssets(
        assetPath: String,
        cacheTargetFile: File,
        keepSpeakingState: Boolean,
        onComplete: (() -> Unit)?
    ): Boolean {
        // المحاولة 1: ملف غير مضغوط في assets عبر openFd
        val playedDirectFd = runCatching {
            appContext.assets.openFd(assetPath).use { afd ->
                startMediaPlayerConfigured(keepSpeakingState, onComplete) { mp ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            }
        }.getOrDefault(false)
        if (playedDirectFd) return true

        // المحاولة 2: ملف مضغوط في assets -> نسخة تلقائية لمرة واحدة إلى cacheDir/reading_audio/
        val extractedFile = extractCompressedAssetOnce(assetPath, cacheTargetFile) ?: return false
        return startMediaPlayerFromFile(extractedFile, keepSpeakingState, onComplete)
    }

    private fun extractCompressedAssetOnce(assetPath: String, cacheTargetFile: File): File? {
        return runCatching {
            appContext.assets.open(assetPath).use { input ->
                cacheTargetFile.parentFile?.mkdirs()
                val tmpFile = File(cacheTargetFile.parentFile, "${cacheTargetFile.name}.tmp")
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
                if (tmpFile.length() > MIN_VALID_MP3_BYTES) {
                    if (cacheTargetFile.exists()) cacheTargetFile.delete()
                    if (tmpFile.renameTo(cacheTargetFile)) cacheTargetFile else tmpFile
                } else {
                    tmpFile.delete()
                    null
                }
            }
        }.getOrNull()
    }

    private fun startMediaPlayerFromFile(
        file: File,
        keepSpeakingState: Boolean,
        onComplete: (() -> Unit)?
    ): Boolean {
        return startMediaPlayerConfigured(keepSpeakingState, onComplete) { mp ->
            mp.setDataSource(file.absolutePath)
        }
    }

    private fun startMediaPlayerFromRawRes(
        rawResId: Int,
        keepSpeakingState: Boolean,
        onComplete: (() -> Unit)?
    ): Boolean {
        return runCatching {
            appContext.resources.openRawResourceFd(rawResId)?.use { afd ->
                startMediaPlayerConfigured(keepSpeakingState, onComplete) { mp ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            } ?: false
        }.getOrDefault(false)
    }

    private fun startMediaPlayerConfigured(
        keepSpeakingState: Boolean,
        onComplete: (() -> Unit)?,
        attachDataSource: (MediaPlayer) -> Unit
    ): Boolean {
        var player: MediaPlayer? = null
        return runCatching {
            val finishedOnce = AtomicBoolean(false)
            fun releasePlayerAndNotify(mp: MediaPlayer) {
                if (!keepSpeakingState && activeMediaPlayer === mp) {
                    _isSpeaking.value = false
                }
                if (activeMediaPlayer === mp) {
                    activeMediaPlayer = null
                }
                runCatching { mp.release() }
                if (finishedOnce.compareAndSet(false, true)) {
                    onComplete?.invoke()
                }
            }

            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                attachDataSource(this)
                setOnCompletionListener { mp -> releasePlayerAndNotify(mp) }
                setOnErrorListener { mp, _, _ ->
                    releasePlayerAndNotify(mp)
                    true
                }
                prepare()
                start()
            }
            activeMediaPlayer = player
            _isSpeaking.value = true
            true
        }.getOrElse {
            runCatching { player?.release() }
            if (activeMediaPlayer === player) {
                activeMediaPlayer = null
            }
            false
        }
    }

    /**
     * (ج) تحميل تلقائي لمرة واحدة في الخلفية عند تفعيل remoteVoiceBaseUrl:
     * يحفظ ملف MP3 في filesDir/reading_audio/ ليعمل بدون إنترنت لاحقاً.
     */
    private fun scheduleBackgroundVoiceDownload(fileName: String) {
        val baseUrl = remoteVoiceBaseUrl.trim().trimEnd('/')
        if (baseUrl.isEmpty() || isReleased || !isNetworkAvailable()) return

        val targetFile = File(File(appContext.filesDir, AUDIO_CACHE_DIR), fileName)
        if (targetFile.exists() && targetFile.length() > MIN_VALID_MP3_BYTES) return
        if (!inFlightDownloads.add(fileName)) return

        engineScope.launch(Dispatchers.IO) {
            try {
                val endpoint = "$baseUrl/$fileName"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 7000
                }
                try {
                    if (conn.responseCode in 200..299) {
                        targetFile.parentFile?.mkdirs()
                        val tmpFile = File(targetFile.parentFile, "$fileName.part")
                        conn.inputStream.use { input ->
                            FileOutputStream(tmpFile).use { output ->
                                input.copyTo(output)
                                output.flush()
                            }
                        }
                        if (tmpFile.length() > MIN_VALID_MP3_BYTES) {
                            if (targetFile.exists()) targetFile.delete()
                            tmpFile.renameTo(targetFile)
                        } else {
                            tmpFile.delete()
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (_: Throwable) {
                // تجاهل أخطاء الشبكة بهدوء مع بقاء البديل الاحتياطي فعالاً
            } finally {
                inFlightDownloads.remove(fileName)
            }
        }
    }

    private fun stopActiveMediaPlayer() {
        val mp = activeMediaPlayer
        activeMediaPlayer = null
        if (mp != null) {
            runCatching {
                mp.setOnCompletionListener(null)
                mp.setOnErrorListener(null)
                if (mp.isPlaying) mp.stop()
            }
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
    }

    fun playTapPop() {
        runCatching {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 55)
        }
    }

    fun playSuccessChime() {
        engineScope.launch(Dispatchers.Default) {
            runCatching {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                delay(130L)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
            }
        }
    }

    fun playVictoryFanfare() {
        engineScope.launch(Dispatchers.Default) {
            runCatching {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 110)
                delay(120L)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 130)
                delay(140L)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 240)
            }
        }
    }

    fun playGentleTryAgain() {
        runCatching {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 120)
        }
    }

    /**
     * تسجيل إنجاز نشاط داخل الدرس ومنح النجوم وحفظها فوراً في SharedPreferences
     * ثم مزامنتها بالخلفية مع Firebase RTDB.
     */
    fun recordActivityWin(lessonId: String, pillarKey: String) {
        val current = _progress.value
        val existingPillars = current.completedPillars[lessonId].orEmpty()
        val updatedPillarsSet = existingPillars + pillarKey
        val newLessonStarCount = maxOf(
            current.starsForLesson(lessonId),
            updatedPillarsSet.size.coerceAtMost(3)
        )
        val alreadyHadPillar = pillarKey in existingPillars
        val updatedLessonStars = current.lessonStars.toMutableMap().apply {
            put(lessonId, newLessonStarCount)
        }
        val updatedPillarsMap = current.completedPillars.toMutableMap().apply {
            put(lessonId, updatedPillarsSet)
        }
        val nextBonus = if (alreadyHadPillar) current.bonusStars + 1 else current.bonusStars

        val updated = ReadingProgressState(
            lessonStars = updatedLessonStars,
            completedPillars = updatedPillarsMap,
            bonusStars = nextBonus,
            avatarId = current.avatarId,
            updatedAt = System.currentTimeMillis()
        )
        _progress.value = updated
        saveLocalProgress(appContext, schoolCode, studentId, updated)
        syncToCloudInBackground(updated)
    }

    /**
     * تحديث الشخصية الرمزية الخاصة بواحة القراءة محلياً ومزامنتها سحابياً فوراً
     * داخل عقدة reading_progress المعزولة دون المساس بملف الطالب الرسمي.
     */
    fun saveAndSyncAvatar(
        schoolCode: String = this.schoolCode,
        studentId: String = this.studentId,
        studentName: String = this.studentName,
        grade: String = this.studentGrade,
        avatarId: String
    ) {
        val validAvatar = ReadingAvatarCatalog.byId(avatarId).id
        val current = _progress.value
        val updated = current.copy(
            avatarId = validAvatar,
            updatedAt = System.currentTimeMillis()
        )
        _progress.value = updated
        saveLocalProgress(appContext, schoolCode, studentId, updated)
        engineScope.launch(Dispatchers.IO) {
            pushProgressToCloudBlocking(
                state = updated,
                schoolCode = schoolCode,
                studentId = studentId,
                studentName = studentName,
                studentGrade = grade
            )
        }
    }

    suspend fun fetchGradeLeaderboard(targetGrade: String): LeaderboardFetchResult =
        Companion.fetchGradeLeaderboard(schoolCode = schoolCode, targetGrade = targetGrade)

    private fun syncFromCloudInBackground() {
        engineScope.launch(Dispatchers.IO) {
            val endpoint = buildSyncUrl(schoolCode, studentId) ?: return@launch
            runCatching {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                try {
                    if (conn.responseCode in 200..299) {
                        val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        if (raw.isNotBlank() && raw != "null") {
                            val remote = parseProgressJson(raw)
                            val merged = mergeProgress(_progress.value, remote)
                            _progress.value = merged
                            saveLocalProgress(appContext, schoolCode, studentId, merged)
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }
    }

    private fun syncToCloudInBackground(state: ReadingProgressState) {
        engineScope.launch(Dispatchers.IO) {
            pushProgressToCloudBlocking(
                state = state,
                schoolCode = schoolCode,
                studentId = studentId,
                studentName = studentName,
                studentGrade = studentGrade
            )
        }
    }

    fun release() {
        isReleased = true
        segmentJob?.cancel()
        segmentJob = null
        speakingTimeoutJob?.cancel()
        speakingTimeoutJob = null
        pendingTtsContinuations.values.forEach { cont ->
            runCatching { if (cont.isActive) cont.cancel() }
        }
        pendingTtsContinuations.clear()
        inFlightDownloads.clear()
        stopActiveMediaPlayer()
        runCatching {
            tts?.setOnUtteranceProgressListener(null)
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        isTtsReady = false
        pendingSpeech = null
        _isSpeaking.value = false
        runCatching {
            toneGenerator?.release()
        }
        engineScope.cancel()
    }

    companion object {
        private const val ENABLE_ROBOTIC_TTS_FALLBACK = false
        private const val CHILD_SPEECH_RATE = 0.86f
        private const val CHILD_SPEECH_PITCH = 1.22f
        private const val SEQUENCE_PAUSE_MS = 120L
        private const val SYLLABLE_PAUSE_MS = 180L
        private const val MIN_VALID_MP3_BYTES = 128L
        private const val AUDIO_CACHE_DIR = "reading_audio"
        private const val PREFS_NAME = "reading_oasis_progress_prefs"
        private const val BASE_RTDB_URL = "https://madrasati-app-d51dd-default-rtdb.firebaseio.com"

        /**
         * رابط خادم الحزمة الصوتية الاختياري للتحميل التلقائي لمرة واحدة (فارغ افتراضياً للعمل الكامل دون إنترنت).
         */
        const val remoteVoiceBaseUrl: String = ""

        private val WHITESPACE_REGEX = Regex("\\s+")
        private val PUNCTUATION_SPLIT_REGEX = Regex("[،:.!؟]+")

        private val SUCCESS_ENCOURAGEMENTS: List<String> = listOf(
            "ممتاز يا بطل!",
            "أحسنت يا بطل!",
            "رائع جداً!",
            "أنت بطل القراءة!"
        )

        private val TRY_AGAIN_ENCOURAGEMENTS: List<String> = listOf(
            "حاول مرة أخرى يا بطل!",
            "أنت قريب، جرّب مجدداً!"
        )

        /**
         * تنظيف النص بالصيغة الحرفية الموحّدة لحساب بصمة الصوت.
         */
        private fun normalizeTextForAudio(text: String): String =
            text.trim().replace(WHITESPACE_REGEX, " ")

        /**
         * تقسيم النص المركّب عند علامات الترقيم العربية والأساسية لتشغيل كل جزء عبر ملفه الصوتي.
         */
        private fun splitByPunctuation(cleanedText: String): List<String> {
            if (!PUNCTUATION_SPLIT_REGEX.containsMatchIn(cleanedText)) return emptyList()
            return cleanedText
                .split(PUNCTUATION_SPLIT_REGEX)
                .map { normalizeTextForAudio(it) }
                .filter { it.isNotEmpty() }
        }

        /**
         * دالة توليد اسم الملف الصوتي الموحّد:
         * تنظّف النص عبر text.trim().replace(Regex("\\s+"), " ")
         * وتحسب بصمة MD5 بترميز UTF-8 وتأخذ أول 12 حرفاً صغيراً مع الامتداد ".mp3".
         */
        fun audioFileNameForText(text: String): String {
            val normalized = normalizeTextForAudio(text)
            val digest = MessageDigest.getInstance("MD5")
                .digest(normalized.toByteArray(Charsets.UTF_8))
            val hexBuilder = StringBuilder(digest.size * 2)
            for (byte in digest) {
                val unsigned = byte.toInt() and 0xFF
                hexBuilder.append(HEX_CHARS[unsigned ushr 4])
                hexBuilder.append(HEX_CHARS[unsigned and 0x0F])
            }
            return "${hexBuilder.substring(0, 12)}.mp3"
        }

        private val HEX_CHARS: CharArray = "0123456789abcdef".toCharArray()

        /**
         * خريطة أسماء ملفات MP3 القياسية (res/raw أو reading_audio/) للمقاطع والكلمات العراقية
         * لتسهيل ربط الأصوات البشرية المسجلة مستقبلاً بسلاسة.
         */
        private val knownAudioKeyMap: Map<String, String> = mapOf(
            "دا" to "reading_syl_da",
            "دو" to "reading_syl_do",
            "دي" to "reading_syl_di",
            "دَ" to "reading_syl_d_fatha",
            "دُ" to "reading_syl_d_damma",
            "دِ" to "reading_syl_d_kasra",
            "را" to "reading_syl_ra",
            "رو" to "reading_syl_ro",
            "ري" to "reading_syl_ri",
            "رَ" to "reading_syl_r_fatha",
            "وَ" to "reading_syl_w_fatha",
            "با" to "reading_syl_ba",
            "بو" to "reading_syl_bo",
            "بي" to "reading_syl_bi",
            "بَ" to "reading_syl_b_fatha",
            "ما" to "reading_syl_ma",
            "مو" to "reading_syl_mo",
            "مي" to "reading_syl_mi",
            "مَ" to "reading_syl_m_fatha",
            "نا" to "reading_syl_na",
            "نو" to "reading_syl_no",
            "ني" to "reading_syl_ni",
            "قا" to "reading_syl_qa",
            "قو" to "reading_syl_qo",
            "قي" to "reading_syl_qi",
            "قَ" to "reading_syl_q_fatha",
            "زا" to "reading_syl_za",
            "زو" to "reading_syl_zo",
            "زي" to "reading_syl_zi",
            "زَ" to "reading_syl_z_fatha",
            "دادا" to "reading_word_dada",
            "دار" to "reading_word_dar",
            "دور" to "reading_word_door",
            "بابا" to "reading_word_baba",
            "باب" to "reading_word_bab",
            "ماما" to "reading_word_mama",
            "نام" to "reading_word_nam",
            "نور" to "reading_word_noor",
            "نوري" to "reading_word_noori",
            "داري" to "reading_word_dari",
            "بوق" to "reading_word_booq",
            "نديم" to "reading_word_nadeem",
            "بقر" to "reading_word_baqar",
            "وز" to "reading_word_waz",
            "زيد" to "reading_word_zaid",
            "زورق" to "reading_word_zawraq"
        )

        fun mapTextToAudioKey(text: String): String? {
            val trimmed = text.trim()
            return knownAudioKeyMap[trimmed]
        }

        private fun sanitizeSegment(raw: String, fallback: String): String {
            val cleaned = raw.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_")
            return cleaned.ifBlank { fallback }
        }

        private fun prefsKey(schoolCode: String, studentId: String): String {
            return "reading_${sanitizeSegment(schoolCode, "default_school")}_${sanitizeSegment(studentId, "guest")}"
        }

        private fun buildSyncUrl(schoolCode: String, studentId: String): String? {
            return runCatching {
                val sCode = URLEncoder.encode(sanitizeSegment(schoolCode, "default_school"), "UTF-8")
                val sId = URLEncoder.encode(sanitizeSegment(studentId, "guest"), "UTF-8")
                "$BASE_RTDB_URL/schools/$sCode/reading_progress/$sId.json"
            }.getOrNull()
        }

        private fun buildSchoolLeaderboardUrl(schoolCode: String): String? {
            return runCatching {
                val sCode = URLEncoder.encode(sanitizeSegment(schoolCode, "default_school"), "UTF-8")
                "$BASE_RTDB_URL/schools/$sCode/reading_progress.json"
            }.getOrNull()
        }

        private fun pushProgressToCloudBlocking(
            state: ReadingProgressState,
            schoolCode: String,
            studentId: String,
            studentName: String,
            studentGrade: String
        ) {
            val endpoint = buildSyncUrl(schoolCode, studentId) ?: return
            runCatching {
                val payload = serializeProgressJson(
                    state = state,
                    schoolCode = schoolCode,
                    studentId = studentId,
                    studentName = studentName,
                    studentGrade = studentGrade
                )
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5500
                    readTimeout = 5500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                try {
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(payload)
                        writer.flush()
                    }
                    conn.responseCode
                } finally {
                    conn.disconnect()
                }
            }
        }

        /**
         * قائمة الصفوف الابتدائية المعتمدة في ساحة أبطال الصف.
         */
        val PRIMARY_GRADES_LIST: List<String> = listOf(
            "الأول الابتدائي",
            "الثاني الابتدائي",
            "الثالث الابتدائي",
            "الرابع الابتدائي",
            "الخامس الابتدائي",
            "السادس الابتدائي"
        )

        /**
         * تطبيع اسم الصف بمرونة عالية (مثل "الأول" أو "الصف الأول الابتدائي" أو "1" -> "الأول الابتدائي").
         */
        fun normalizePrimaryGrade(rawGrade: String): String {
            val cleaned = rawGrade
                .replace("الصف", "")
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .trim()
            return when {
                cleaned.contains("اول") || cleaned.contains("1") || cleaned.contains("١") -> PRIMARY_GRADES_LIST[0]
                cleaned.contains("ثاني") || cleaned.contains("2") || cleaned.contains("٢") -> PRIMARY_GRADES_LIST[1]
                cleaned.contains("ثالث") || cleaned.contains("3") || cleaned.contains("٣") -> PRIMARY_GRADES_LIST[2]
                cleaned.contains("رابع") || cleaned.contains("4") || cleaned.contains("٤") -> PRIMARY_GRADES_LIST[3]
                cleaned.contains("خامس") || cleaned.contains("5") || cleaned.contains("٥") -> PRIMARY_GRADES_LIST[4]
                cleaned.contains("سادس") || cleaned.contains("6") || cleaned.contains("٦") -> PRIMARY_GRADES_LIST[5]
                else -> PRIMARY_GRADES_LIST[0]
            }
        }

        private fun matchesGradeFlexibly(recordGrade: String, targetGrade: String): Boolean {
            if (recordGrade.isBlank()) return false
            return normalizePrimaryGrade(recordGrade) == normalizePrimaryGrade(targetGrade)
        }

        /**
         * جلب لوحة شرف أبطال الصف من العقدة المعزولة schools/{schoolCode}/reading_progress.json
         * وفلترتها بدقة حسب الصف وترتيب جميع الطلبة تنازلياً مع إسناد التسلسل الحقيقي لكل طالب.
         */
        suspend fun fetchGradeLeaderboard(
            schoolCode: String,
            targetGrade: String
        ): LeaderboardFetchResult = withContext(Dispatchers.IO) {
            val endpoint = buildSchoolLeaderboardUrl(schoolCode)
                ?: return@withContext LeaderboardFetchResult.Error("تعذّر إعداد رابط ساحة الأبطال")

            runCatching {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        return@withContext LeaderboardFetchResult.Error("تعذّر الاتصال بساحة الأبطال حالياً")
                    }
                    val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    if (raw.isBlank() || raw.trim() == "null") {
                        return@withContext LeaderboardFetchResult.Success(emptyList())
                    }

                    val root = JSONObject(raw)
                    val rawRecords = mutableListOf<ReadingLeaderboardEntry>()
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val item = root.optJSONObject(key) ?: continue
                        val recGrade = item.optString("grade", "")
                        if (!matchesGradeFlexibly(recGrade, targetGrade)) continue

                        val parsed = parseProgressJson(item.toString())
                        val stars = maxOf(item.optInt("totalStars", 0), parsed.totalStars)
                        val completed = maxOf(item.optInt("completedLessons", 0), parsed.completedLessonsCount)
                        val name = item.optString("studentName", "").ifBlank { "بطل القراءة" }
                        val sId = item.optString("studentId", key).ifBlank { key }
                        val avatarId = ReadingAvatarCatalog.byId(item.optString("avatarId", parsed.avatarId)).id
                        val lastUp = maxOf(item.optLong("lastUpdated", 0L), item.optLong("updatedAt", 0L))
                        val rankInfo = resolveReadingHeroTitle(stars)

                        rawRecords.add(
                            ReadingLeaderboardEntry(
                                rank = 0,
                                studentId = sId,
                                studentName = name,
                                grade = normalizePrimaryGrade(recGrade),
                                totalStars = stars,
                                completedLessons = completed,
                                avatarId = avatarId,
                                level = rankInfo.level,
                                heroTitle = rankInfo.title,
                                nextLevelStars = rankInfo.nextLevelStars,
                                lastUpdated = lastUp
                            )
                        )
                    }

                    val sorted = rawRecords
                        .sortedWith(
                            compareByDescending<ReadingLeaderboardEntry> { it.totalStars }
                                .thenByDescending { it.completedLessons }
                                .thenBy { it.studentName }
                        )
                        .mapIndexed { idx, entry -> entry.copy(rank = idx + 1) }

                    LeaderboardFetchResult.Success(sorted)
                } finally {
                    conn.disconnect()
                }
            }.getOrElse {
                LeaderboardFetchResult.Error("تحقق من اتصالك بالإنترنت ثم اضغط تحديث لعرض أبطال الصف")
            }
        }

        fun loadCachedProgress(
            context: Context,
            schoolCode: String,
            studentId: String
        ): ReadingProgressState {
            return runCatching {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val raw = prefs.getString(prefsKey(schoolCode, studentId), null)
                if (raw.isNullOrBlank()) ReadingProgressState() else parseProgressJson(raw)
            }.getOrDefault(ReadingProgressState())
        }

        fun loadCachedTotalStars(
            context: Context,
            schoolCode: String,
            studentId: String
        ): Int = loadCachedProgress(context, schoolCode, studentId).totalStars

        private fun saveLocalProgress(
            context: Context,
            schoolCode: String,
            studentId: String,
            state: ReadingProgressState
        ) {
            runCatching {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val json = serializeProgressJson(state, schoolCode, studentId, "", "")
                prefs.edit().putString(prefsKey(schoolCode, studentId), json).apply()
            }
        }

        private fun serializeProgressJson(
            state: ReadingProgressState,
            schoolCode: String,
            studentId: String,
            studentName: String,
            studentGrade: String = ""
        ): String {
            val root = JSONObject()
            root.put("schoolCode", schoolCode)
            root.put("studentId", studentId)
            if (studentName.isNotBlank()) {
                root.put("studentName", studentName)
            }
            if (studentGrade.isNotBlank()) {
                root.put("grade", normalizePrimaryGrade(studentGrade))
            }
            root.put("totalStars", state.totalStars)
            root.put("completedLessons", state.completedLessonsCount)
            root.put("bonusStars", state.bonusStars)
            root.put("avatarId", ReadingAvatarCatalog.byId(state.avatarId).id)
            root.put("updatedAt", state.updatedAt)
            root.put("lastUpdated", state.updatedAt)

            val starsObj = JSONObject()
            state.lessonStars.forEach { (k, v) -> starsObj.put(k, v) }
            root.put("lessonStars", starsObj)

            val pillarsObj = JSONObject()
            state.completedPillars.forEach { (k, set) ->
                val arr = JSONArray()
                set.forEach { arr.put(it) }
                pillarsObj.put(k, arr)
            }
            root.put("completedPillars", pillarsObj)
            return root.toString()
        }

        private fun parseProgressJson(raw: String): ReadingProgressState {
            val root = JSONObject(raw)
            val starsMap = mutableMapOf<String, Int>()
            val starsObj = root.optJSONObject("lessonStars")
            if (starsObj != null) {
                val keys = starsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    starsMap[key] = starsObj.optInt(key, 0).coerceIn(0, 3)
                }
            }
            val pillarsMap = mutableMapOf<String, Set<String>>()
            val pillarsObj = root.optJSONObject("completedPillars")
            if (pillarsObj != null) {
                val keys = pillarsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = pillarsObj.optJSONArray(key)
                    if (arr != null) {
                        val items = mutableSetOf<String>()
                        for (i in 0 until arr.length()) {
                            val item = arr.optString(i, "")
                            if (item.isNotBlank()) items.add(item)
                        }
                        pillarsMap[key] = items
                    }
                }
            }
            val parsedAvatar = ReadingAvatarCatalog.byId(
                root.optString("avatarId", ReadingAvatarCatalog.DEFAULT_AVATAR_ID)
            ).id
            return ReadingProgressState(
                lessonStars = starsMap,
                completedPillars = pillarsMap,
                bonusStars = root.optInt("bonusStars", 0).coerceAtLeast(0),
                avatarId = parsedAvatar,
                updatedAt = maxOf(root.optLong("lastUpdated", 0L), root.optLong("updatedAt", 0L))
            )
        }

        private fun mergeProgress(
            local: ReadingProgressState,
            remote: ReadingProgressState
        ): ReadingProgressState {
            val mergedStars = local.lessonStars.toMutableMap()
            remote.lessonStars.forEach { (lessonId, stars) ->
                mergedStars[lessonId] = maxOf(mergedStars[lessonId] ?: 0, stars.coerceIn(0, 3))
            }
            val mergedPillars = local.completedPillars.toMutableMap()
            remote.completedPillars.forEach { (lessonId, set) ->
                mergedPillars[lessonId] = (mergedPillars[lessonId].orEmpty() + set)
            }
            val chosenAvatar = if (remote.updatedAt > local.updatedAt && remote.avatarId.isNotBlank()) {
                remote.avatarId
            } else {
                local.avatarId
            }
            return ReadingProgressState(
                lessonStars = mergedStars,
                completedPillars = mergedPillars,
                bonusStars = maxOf(local.bonusStars, remote.bonusStars),
                avatarId = ReadingAvatarCatalog.byId(chosenAvatar).id,
                updatedAt = maxOf(local.updatedAt, remote.updatedAt)
            )
        }
    }
}
