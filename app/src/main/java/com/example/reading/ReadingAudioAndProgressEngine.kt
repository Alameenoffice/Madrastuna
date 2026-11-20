package com.example.reading

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.model.FirebaseConfigConstants
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * محرك النطق العربي الطفولي والمؤثرات الصوتية التشجيعية لقسم "واحة القراءة الذكية".
 *
 * - يستخدم Android TextToSpeech مضبوطاً على العربية بسرعة بطيئة وواضحة (0.65f) ونبرة لطيفة (1.08f).
 * - يدعم نطق الكلمة مقطعاً تلو الآخر مع إرسال مؤشر المقطع النشط لإبرازه بصرياً ثم نطق الكلمة كاملة.
 * - يوفر نغمات تشجيعية لطيفة عبر ToneGenerator دون الحاجة لملفات صوتية خارجية.
 */
class ReadingAudioEngine(context: Context) {

    companion object {
        const val CHILD_SPEECH_RATE = 0.65f
        const val CHILD_SPEECH_PITCH = 1.08f
        private const val UTTERANCE_TIMEOUT_MS = 2800L
    }

    private val appContext = context.applicationContext
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingUtterances = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    private var tts: TextToSpeech? = null
    private var activeSequenceJob: Job? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isArabicSupported = MutableStateFlow(false)
    val isArabicSupported: StateFlow<Boolean> = _isArabicSupported.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@TextToSpeech
                    val arabicSupported = configureArabicVoice(engine)
                    engine.setSpeechRate(CHILD_SPEECH_RATE)
                    engine.setPitch(CHILD_SPEECH_PITCH)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            if (utteranceId != null) {
                                pendingUtterances.remove(utteranceId)?.complete(true)
                            }
                            if (pendingUtterances.isEmpty()) {
                                _isSpeaking.value = false
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (utteranceId != null) {
                                pendingUtterances.remove(utteranceId)?.complete(false)
                            }
                            if (pendingUtterances.isEmpty()) {
                                _isSpeaking.value = false
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            if (utteranceId != null) {
                                pendingUtterances.remove(utteranceId)?.complete(false)
                            }
                            if (pendingUtterances.isEmpty()) {
                                _isSpeaking.value = false
                            }
                        }
                    })
                    _isArabicSupported.value = arabicSupported
                    _isReady.value = true
                } else {
                    _isReady.value = false
                }
            }
        }
    }

    private fun configureArabicVoice(engine: TextToSpeech): Boolean {
        val candidateLocales = listOf(
            Locale("ar", "IQ"),
            Locale("ar", "SA"),
            Locale("ar")
        )
        for (locale in candidateLocales) {
            val result = engine.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                return true
            }
        }
        return false
    }

    /**
     * نطق نص عربي واحد بهدوء ووضوح.
     */
    fun speakText(text: String, onDone: (() -> Unit)? = null) {
        val clean = text.replace("ـ", "").trim()
        if (clean.isEmpty()) {
            onDone?.invoke()
            return
        }
        stop()
        activeSequenceJob = engineScope.launch {
            speakSingleAndAwait(clean)
            onDone?.invoke()
        }
    }

    /**
     * نطق مقطع صوتي واحد.
     */
    fun speakSyllable(syllable: ColoredSyllable, onDone: (() -> Unit)? = null) {
        speakText(syllable.ttsCleanText, onDone)
    }

    /**
     * نطق الكلمة بالطريقة المقطعية المتسلسلة:
     * ينطق كل مقطع بالترتيب مع إبلاغ الواجهة بمؤشر المقطع النشط (0..n-1) لإبرازه بصرياً،
     * ثم يفرّغ التحديد (-1) وينطق الكلمة كاملة مدمجة.
     */
    fun speakWordBySyllables(
        word: SegmentedWord,
        pauseBetweenSyllablesMs: Long = 380L,
        pauseBeforeFullWordMs: Long = 460L,
        onActiveSyllableIndexChanged: (Int) -> Unit,
        onFullWordSpeaking: () -> Unit = {},
        onCompleted: () -> Unit = {}
    ) {
        stop()
        activeSequenceJob = engineScope.launch {
            try {
                word.syllables.forEachIndexed { index, syllable ->
                    onActiveSyllableIndexChanged(index)
                    speakSingleAndAwait(syllable.ttsCleanText)
                    if (index < word.syllables.lastIndex) {
                        delay(pauseBetweenSyllablesMs)
                    }
                }
                onActiveSyllableIndexChanged(-1)
                delay(pauseBeforeFullWordMs)
                onFullWordSpeaking()
                speakSingleAndAwait(word.spokenWord)
            } finally {
                onActiveSyllableIndexChanged(-1)
                _isSpeaking.value = false
                onCompleted()
            }
        }
    }

    /**
     * نطق جملة كاملة كلمة تلو الأخرى مع إبلاغ الواجهة بمؤشر الكلمة النشطة.
     */
    fun speakSentence(
        sentence: ReadingSentence,
        pauseBetweenWordsMs: Long = 350L,
        onActiveWordIndexChanged: (Int) -> Unit = {},
        onCompleted: () -> Unit = {}
    ) {
        stop()
        activeSequenceJob = engineScope.launch {
            try {
                sentence.words.forEachIndexed { index, word ->
                    onActiveWordIndexChanged(index)
                    speakSingleAndAwait(word.spokenWord)
                    if (index < sentence.words.lastIndex) {
                        delay(pauseBetweenWordsMs)
                    }
                }
            } finally {
                onActiveWordIndexChanged(-1)
                _isSpeaking.value = false
                onCompleted()
            }
        }
    }

    private suspend fun speakSingleAndAwait(text: String) {
        val engine = tts
        if (engine == null || !_isReady.value) {
            delay(550L)
            return
        }
        val utteranceId = "reading_${UUID.randomUUID()}"
        val deferred = CompletableDeferred<Boolean>()
        pendingUtterances[utteranceId] = deferred
        _isSpeaking.value = true

        val speakResult = runCatching {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }.getOrDefault(TextToSpeech.ERROR)

        if (speakResult == TextToSpeech.ERROR) {
            pendingUtterances.remove(utteranceId)
            delay(500L)
            return
        }

        val completed = withTimeoutOrNull(UTTERANCE_TIMEOUT_MS) {
            deferred.await()
        }
        if (completed == null) {
            pendingUtterances.remove(utteranceId)
        }
    }

    /**
     * مؤثر صوتي لطيف عند نقر مقطع أو حرف.
     */
    fun playSyllableTapTone() {
        engineScope.launch(Dispatchers.Default) {
            playToneSafely(
                toneType = ToneGenerator.TONE_PROP_BEEP,
                durationMs = 70,
                volume = 38
            )
        }
    }

    /**
     * مؤثر صوتي تشجيعي لطيف عند نجاح الطفل في قراءة كلمة أو إتمام تمرين.
     */
    fun playSuccessChime() {
        engineScope.launch(Dispatchers.Default) {
            playToneSafely(
                toneType = ToneGenerator.TONE_PROP_ACK,
                durationMs = 140,
                volume = 48
            )
            delay(150L)
            playToneSafely(
                toneType = ToneGenerator.TONE_PROP_BEEP2,
                durationMs = 170,
                volume = 52
            )
        }
    }

    /**
     * لحن تشجيعي قصير ولطيف عند إكمال درس كامل وحصد النجوم.
     */
    fun playLessonCompleteFanfare() {
        engineScope.launch(Dispatchers.Default) {
            playToneSafely(ToneGenerator.TONE_PROP_BEEP, 110, 46)
            delay(125L)
            playToneSafely(ToneGenerator.TONE_PROP_ACK, 130, 50)
            delay(145L)
            playToneSafely(ToneGenerator.TONE_PROP_BEEP2, 220, 56)
        }
    }

    private fun playToneSafely(toneType: Int, durationMs: Int, volume: Int) {
        var generator: ToneGenerator? = null
        try {
            generator = ToneGenerator(AudioManager.STREAM_MUSIC, volume.coerceIn(10, 70))
            generator.startTone(toneType, durationMs)
            Thread.sleep((durationMs + 35).toLong())
        } catch (_: Exception) {
            // تجاهل صامت على الأجهزة التي تمنع مسار الصوت أو في وضع الصامت
        } finally {
            runCatching { generator?.release() }
        }
    }

    /**
     * إيقاف النطق الجاري حالياً وإلغاء التسلسل النشط.
     */
    fun stop() {
        activeSequenceJob?.cancel()
        activeSequenceJob = null
        pendingUtterances.values.forEach { it.complete(false) }
        pendingUtterances.clear()
        runCatching { tts?.stop() }
        _isSpeaking.value = false
    }

    /**
     * تحرير جميع موارد الصوت عند مغادرة شاشة القراءة.
     */
    fun release() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
        _isReady.value = false
        engineScope.cancel()
    }
}

/**
 * سجل تقدم الطفل في درس واحد من دروس واحة القراءة الذكية.
 */
data class LessonReadingProgress(
    val lessonId: String,
    val starsEarned: Int = 0,
    val isCompleted: Boolean = false,
    val practicedWords: Set<String> = emptySet(),
    val tracedLetters: Set<String> = emptySet(),
    val attemptsCount: Int = 0,
    val lastPracticedAt: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("lessonId", lessonId)
        put("starsEarned", starsEarned.coerceIn(0, 3))
        put("isCompleted", isCompleted)
        put("practicedWords", JSONArray(practicedWords.toList()))
        put("tracedLetters", JSONArray(tracedLetters.toList()))
        put("attemptsCount", attemptsCount)
        put("lastPracticedAt", lastPracticedAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): LessonReadingProgress {
            val words = mutableSetOf<String>()
            obj.optJSONArray("practicedWords")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i, "").trim()
                    if (item.isNotEmpty()) words.add(item)
                }
            }
            val letters = mutableSetOf<String>()
            obj.optJSONArray("tracedLetters")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i, "").trim()
                    if (item.isNotEmpty()) letters.add(item)
                }
            }
            return LessonReadingProgress(
                lessonId = obj.optString("lessonId", ""),
                starsEarned = obj.optInt("starsEarned", 0).coerceIn(0, 3),
                isCompleted = obj.optBoolean("isCompleted", false),
                practicedWords = words,
                tracedLetters = letters,
                attemptsCount = obj.optInt("attemptsCount", 0).coerceAtLeast(0),
                lastPracticedAt = obj.optLong("lastPracticedAt", 0L)
            )
        }
    }
}

/**
 * ملخص التقدم الكامل للطفل (النجوم والدروس المنجزة) المرتبط بحساب الطالب والمدرسة.
 */
data class StudentReadingProgress(
    val studentId: String,
    val schoolCode: String,
    val lessons: Map<String, LessonReadingProgress> = emptyMap(),
    val lastLessonId: String = ReadingCurriculum.lessons.first().id,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalStars: Int
        get() = lessons.values.sumOf { it.starsEarned.coerceIn(0, 3) }

    val completedLessonsCount: Int
        get() = lessons.values.count { it.isCompleted }

    fun toJson(): JSONObject = JSONObject().apply {
        put("studentId", studentId)
        put("schoolCode", schoolCode)
        put("totalStars", totalStars)
        put("completedLessonsCount", completedLessonsCount)
        put("lastLessonId", lastLessonId)
        put("updatedAt", updatedAt)
        val lessonsObj = JSONObject()
        lessons.forEach { (lessonId, record) ->
            lessonsObj.put(lessonId, record.toJson())
        }
        put("lessons", lessonsObj)
    }

    companion object {
        fun fromJson(
            rawJson: String,
            fallbackSchoolCode: String,
            fallbackStudentId: String
        ): StudentReadingProgress? {
            val trimmed = rawJson.trim()
            if (trimmed.isEmpty() || trimmed == "null") return null
            return runCatching {
                val root = JSONObject(trimmed)
                val lessonsMap = mutableMapOf<String, LessonReadingProgress>()
                val lessonsObj = root.optJSONObject("lessons")
                if (lessonsObj != null) {
                    val keys = lessonsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val recordObj = lessonsObj.optJSONObject(key)
                        if (recordObj != null) {
                            val parsed = LessonReadingProgress.fromJson(recordObj)
                            val finalId = parsed.lessonId.ifBlank { key }
                            lessonsMap[finalId] = parsed.copy(lessonId = finalId)
                        }
                    }
                }
                StudentReadingProgress(
                    studentId = root.optString("studentId", fallbackStudentId).ifBlank { fallbackStudentId },
                    schoolCode = root.optString("schoolCode", fallbackSchoolCode).ifBlank { fallbackSchoolCode },
                    lessons = lessonsMap,
                    lastLessonId = root.optString("lastLessonId", ReadingCurriculum.lessons.first().id)
                        .ifBlank { ReadingCurriculum.lessons.first().id },
                    updatedAt = root.optLong("updatedAt", System.currentTimeMillis())
                )
            }.getOrNull()
        }
    }
}

/**
 * محرك الحفظ المحلي (SharedPreferences) والمزامنة السحابية الخفيفة مع Firebase Realtime Database
 * تحت المسار المعزول: schools/{schoolCode}/reading_progress/{studentId}.json
 */
class ReadingProgressEngine(context: Context) {

    companion object {
        private const val PREFS_NAME = "madrasati_reading_oasis_progress_v1"
        private const val CONNECT_TIMEOUT_MS = 8000
        private const val READ_TIMEOUT_MS = 8000
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val backgroundSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * تنظيف المعرفات لتكون صالحة وآمنة كمفاتيح في Firebase Realtime Database و SharedPreferences.
     */
    private fun sanitizeKey(raw: String, fallback: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[.\\\$#\\[\\]/\\s]+"), "_")
            .trim('_')
        return cleaned.ifBlank { fallback }
    }

    private fun buildPrefsKey(schoolCode: String, studentId: String): String {
        val cleanSchool = sanitizeKey(schoolCode, "default_school")
        val cleanStudent = sanitizeKey(studentId, "guest_student")
        return "reading_progress_${cleanSchool}_${cleanStudent}"
    }

    private fun buildFirebaseUrl(schoolCode: String, studentId: String): String {
        val cleanSchool = URLEncoder.encode(sanitizeKey(schoolCode, "default_school"), "UTF-8")
        val cleanStudent = URLEncoder.encode(sanitizeKey(studentId, "guest_student"), "UTF-8")
        val baseUrl = FirebaseConfigConstants.DATABASE_URL.trimEnd('/')
        return "$baseUrl/schools/$cleanSchool/reading_progress/$cleanStudent.json"
    }

    /**
     * قراءة تقدم الطفل المحفوظ محلياً في SharedPreferences باسم المستخدم/معرف الطالب الحالي.
     */
    fun loadLocalProgress(schoolCode: String, studentId: String): StudentReadingProgress {
        val key = buildPrefsKey(schoolCode, studentId)
        val stored = prefs.getString(key, null)
        return if (!stored.isNullOrBlank()) {
            StudentReadingProgress.fromJson(stored, schoolCode, studentId)
                ?: StudentReadingProgress(studentId = studentId, schoolCode = schoolCode)
        } else {
            StudentReadingProgress(studentId = studentId, schoolCode = schoolCode)
        }
    }

    /**
     * حفظ تقدم الطفل محلياً في SharedPreferences.
     */
    fun saveLocalProgress(progress: StudentReadingProgress) {
        val key = buildPrefsKey(progress.schoolCode, progress.studentId)
        prefs.edit().putString(key, progress.toJson().toString()).apply()
    }

    /**
     * تسجيل إتمام درس ومنح النجوم مع حفظ فوري محلي ومزامنة سحابية خفيفة بالخلفية.
     */
    fun recordLessonProgress(
        schoolCode: String,
        studentId: String,
        lessonId: String,
        starsEarned: Int,
        isCompleted: Boolean = true,
        practicedWords: Set<String> = emptySet(),
        tracedLetters: Set<String> = emptySet(),
        syncInBackground: Boolean = true
    ): StudentReadingProgress {
        val current = loadLocalProgress(schoolCode, studentId)
        val existing = current.lessons[lessonId] ?: LessonReadingProgress(lessonId = lessonId)
        val now = System.currentTimeMillis()

        val updatedRecord = existing.copy(
            starsEarned = maxOf(existing.starsEarned, starsEarned.coerceIn(0, 3)),
            isCompleted = existing.isCompleted || isCompleted,
            practicedWords = existing.practicedWords + practicedWords,
            tracedLetters = existing.tracedLetters + tracedLetters,
            attemptsCount = existing.attemptsCount + 1,
            lastPracticedAt = now
        )

        val updatedProgress = current.copy(
            lessons = current.lessons + (lessonId to updatedRecord),
            lastLessonId = lessonId,
            updatedAt = now
        )

        saveLocalProgress(updatedProgress)
        if (syncInBackground) {
            syncToCloudAsync(updatedProgress)
        }
        return updatedProgress
    }

    /**
     * تسجيل تدرب الطفل على كلمة أو حرف داخل درس محدد.
     */
    fun recordPracticeActivity(
        schoolCode: String,
        studentId: String,
        lessonId: String,
        practicedWordId: String? = null,
        tracedLetter: String? = null,
        syncInBackground: Boolean = true
    ): StudentReadingProgress {
        val current = loadLocalProgress(schoolCode, studentId)
        val existing = current.lessons[lessonId] ?: LessonReadingProgress(lessonId = lessonId)
        val now = System.currentTimeMillis()

        val nextWords = if (!practicedWordId.isNullOrBlank()) {
            existing.practicedWords + practicedWordId
        } else {
            existing.practicedWords
        }
        val nextLetters = if (!tracedLetter.isNullOrBlank()) {
            existing.tracedLetters + tracedLetter
        } else {
            existing.tracedLetters
        }

        val updatedRecord = existing.copy(
            practicedWords = nextWords,
            tracedLetters = nextLetters,
            lastPracticedAt = now
        )

        val updatedProgress = current.copy(
            lessons = current.lessons + (lessonId to updatedRecord),
            lastLessonId = lessonId,
            updatedAt = now
        )

        saveLocalProgress(updatedProgress)
        if (syncInBackground) {
            syncToCloudAsync(updatedProgress)
        }
        return updatedProgress
    }

    /**
     * إطلاق مزامنة سحابية خفيفة بالخلفية دون تعطيل واجهة المستخدم.
     */
    fun syncToCloudAsync(progress: StudentReadingProgress) {
        if (progress.schoolCode.isBlank() || progress.studentId.isBlank()) return
        backgroundSyncScope.launch {
            pushProgressToFirebase(progress)
        }
    }

    /**
     * رفع سجل القراءة إلى المسار:
     * schools/{schoolCode}/reading_progress/{studentId}.json
     */
    suspend fun pushProgressToFirebase(progress: StudentReadingProgress): Boolean =
        withContext(Dispatchers.IO) {
            if (progress.schoolCode.isBlank() || progress.studentId.isBlank()) {
                return@withContext false
            }
            var connection: HttpURLConnection? = null
            try {
                val url = URL(buildFirebaseUrl(progress.schoolCode, progress.studentId))
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(progress.toJson().toString())
                    writer.flush()
                }
                connection.responseCode in 200..299
            } catch (_: Exception) {
                false
            } finally {
                connection?.disconnect()
            }
        }

    /**
     * جلب سجل القراءة السحابي من Firebase Realtime Database.
     */
    suspend fun fetchProgressFromFirebase(
        schoolCode: String,
        studentId: String
    ): StudentReadingProgress? = withContext(Dispatchers.IO) {
        if (schoolCode.isBlank() || studentId.isBlank()) {
            return@withContext null
        }
        var connection: HttpURLConnection? = null
        try {
            val url = URL(buildFirebaseUrl(schoolCode, studentId))
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode !in 200..299) {
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            StudentReadingProgress.fromJson(body, schoolCode, studentId)
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * دمج ذكي بين التقدم المحلي والتقدم السحابي بحيث لا يفقد الطفل أي نجمة مكتسبة سواء كان متصلاً أم لا.
     */
    suspend fun syncAndMergeProgress(
        schoolCode: String,
        studentId: String
    ): StudentReadingProgress {
        val local = loadLocalProgress(schoolCode, studentId)
        val remote = fetchProgressFromFirebase(schoolCode, studentId) ?: return local

        val allLessonIds = local.lessons.keys + remote.lessons.keys
        val mergedLessons = allLessonIds.associateWith { id ->
            val l = local.lessons[id]
            val r = remote.lessons[id]
            when {
                l != null && r != null -> LessonReadingProgress(
                    lessonId = id,
                    starsEarned = maxOf(l.starsEarned, r.starsEarned),
                    isCompleted = l.isCompleted || r.isCompleted,
                    practicedWords = l.practicedWords + r.practicedWords,
                    tracedLetters = l.tracedLetters + r.tracedLetters,
                    attemptsCount = maxOf(l.attemptsCount, r.attemptsCount),
                    lastPracticedAt = maxOf(l.lastPracticedAt, r.lastPracticedAt)
                )
                l != null -> l
                else -> r!!
            }
        }

        val merged = StudentReadingProgress(
            studentId = studentId,
            schoolCode = schoolCode,
            lessons = mergedLessons,
            lastLessonId = if (remote.updatedAt > local.updatedAt) remote.lastLessonId else local.lastLessonId,
            updatedAt = maxOf(local.updatedAt, remote.updatedAt)
        )

        saveLocalProgress(merged)
        if (merged.totalStars > remote.totalStars || merged.completedLessonsCount > remote.completedLessonsCount) {
            pushProgressToFirebase(merged)
        }
        return merged
    }

    /**
     * إلغاء مهام المزامنة عند التنظيف.
     */
    fun close() {
        backgroundSyncScope.cancel()
    }
}
