package com.example.reading

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
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
import org.json.JSONArray
import org.json.JSONObject

data class ReadingProgressState(
    val lessonStars: Map<String, Int> = emptyMap(),
    val completedPillars: Map<String, Set<String>> = emptyMap(),
    val bonusStars: Int = 0,
    val updatedAt: Long = 0L
) {
    val totalStars: Int
        get() = lessonStars.values.sum() + bonusStars

    fun starsForLesson(lessonId: String): Int = (lessonStars[lessonId] ?: 0).coerceIn(0, 3)
}

/**
 * محرك الصوت العربي والتقطيع المتزامن وحفظ النجوم (محلياً أولاً مع مزامنة سحابية خفيفة).
 */
class ReadingAudioAndProgressEngine(
    context: Context,
    private val schoolCode: String,
    private val studentId: String,
    private val studentName: String
) {
    private val appContext = context.applicationContext
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var tts: TextToSpeech? = null
    @Volatile
    private var isTtsReady = false
    private var pendingSpeech: String? = null
    private var segmentJob: Job? = null

    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 78)
    }.getOrNull()

    private val _progress = MutableStateFlow(loadCachedProgress(appContext, schoolCode, studentId))
    val progress: StateFlow<ReadingProgressState> = _progress.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        initTextToSpeech()
        syncFromCloudInBackground()
    }

    private fun initTextToSpeech() {
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@TextToSpeech
                    val arabicResult = engine.setLanguage(Locale("ar"))
                    if (arabicResult == TextToSpeech.LANG_MISSING_DATA ||
                        arabicResult == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        engine.setLanguage(Locale("ar", "IQ"))
                    }
                    engine.setSpeechRate(0.65f)
                    engine.setPitch(1.08f)
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

    fun speak(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        segmentJob?.cancel()
        if (!isTtsReady) {
            pendingSpeech = cleaned
            return
        }
        runCatching {
            _isSpeaking.value = true
            tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "reading_utterance_${System.currentTimeMillis()}")
            engineScope.launch {
                val estimatedMs = (cleaned.length * 115L).coerceIn(700L, 2600L)
                delay(estimatedMs)
                _isSpeaking.value = false
            }
        }
    }

    /**
     * نطق متسلسل للمقاطع مع إبلاغ الواجهة برقم المقطع النشط لإضاءته وقفزه بالتزامن مع الصوت،
     * ثم نطق الكلمة كاملة في النهاية.
     */
    fun speakSegmentedWord(
        word: SegmentedWord,
        onActiveSyllableIndex: (Int) -> Unit,
        onWholeWordActive: (Boolean) -> Unit = {},
        onFinished: () -> Unit = {}
    ) {
        segmentJob?.cancel()
        segmentJob = engineScope.launch {
            _isSpeaking.value = true
            onWholeWordActive(false)
            word.syllables.forEachIndexed { index, syllable ->
                onActiveSyllableIndex(index)
                speakRawImmediate(syllable.spokenText)
                delay(860L)
            }
            onActiveSyllableIndex(-1)
            onWholeWordActive(true)
            speakRawImmediate(word.spokenWord)
            delay(1050L)
            onWholeWordActive(false)
            _isSpeaking.value = false
            onFinished()
        }
    }

    private fun speakRawImmediate(text: String) {
        if (!isTtsReady) return
        runCatching {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "seg_${System.currentTimeMillis()}")
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
            updatedAt = System.currentTimeMillis()
        )
        _progress.value = updated
        saveLocalProgress(appContext, schoolCode, studentId, updated)
        syncToCloudInBackground(updated)
    }

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
            val endpoint = buildSyncUrl(schoolCode, studentId) ?: return@launch
            runCatching {
                val payload = serializeProgressJson(state, schoolCode, studentId, studentName)
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 5000
                    readTimeout = 5000
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
    }

    fun release() {
        segmentJob?.cancel()
        engineScope.cancel()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        runCatching {
            toneGenerator?.release()
        }
    }

    companion object {
        private const val PREFS_NAME = "reading_oasis_progress_prefs"
        private const val BASE_RTDB_URL = "https://madrasati-app-d51dd-default-rtdb.firebaseio.com"

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
                val json = serializeProgressJson(state, schoolCode, studentId, "")
                prefs.edit().putString(prefsKey(schoolCode, studentId), json).apply()
            }
        }

        private fun serializeProgressJson(
            state: ReadingProgressState,
            schoolCode: String,
            studentId: String,
            studentName: String
        ): String {
            val root = JSONObject()
            root.put("schoolCode", schoolCode)
            root.put("studentId", studentId)
            if (studentName.isNotBlank()) {
                root.put("studentName", studentName)
            }
            root.put("totalStars", state.totalStars)
            root.put("bonusStars", state.bonusStars)
            root.put("updatedAt", state.updatedAt)

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
            return ReadingProgressState(
                lessonStars = starsMap,
                completedPillars = pillarsMap,
                bonusStars = root.optInt("bonusStars", 0).coerceAtLeast(0),
                updatedAt = root.optLong("updatedAt", 0L)
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
            return ReadingProgressState(
                lessonStars = mergedStars,
                completedPillars = mergedPillars,
                bonusStars = maxOf(local.bonusStars, remote.bonusStars),
                updatedAt = maxOf(local.updatedAt, remote.updatedAt)
            )
        }
    }
}
