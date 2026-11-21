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
import android.speech.tts.Voice
import java.io.File
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
 * محرك الصوت الطفولي المحسّن:
 * 1) طبقة تشغيل صوت بشري حقيقي عبر MediaPlayer (تبحث في الكاش المحلي و res/raw أولاً).
 * 2) محرك TextToSpeech عربي ينتقي تلقائياً أفضل صوت عربي عالي الجودة من tts.voices
 *    بسرعة طبيعية مريحة (0.86f) ونبرة طفولية مبهجة ودافئة (1.22f).
 * 3) حفظ محلي فوري للنجوم مع مزامنة خلفية خفيفة إلى Firebase RTDB.
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
    private var activeMediaPlayer: MediaPlayer? = null

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
                    selectBestArabicVoice(engine)
                    engine.setSpeechRate(CHILD_SPEECH_RATE)
                    engine.setPitch(CHILD_SPEECH_PITCH)
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
     * يشغّل ملف MP3 حقيقي مطابق للمقطع/الكلمة من الكاش المحلي أو res/raw إن وُجد،
     * وإلا يستخدم محرك TextToSpeech المحسّن فوراً.
     */
    fun speak(text: String, audioKey: String? = null) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        segmentJob?.cancel()
        stopActiveMediaPlayer()

        val resolvedKey = audioKey?.takeIf { it.isNotBlank() } ?: mapTextToAudioKey(cleaned)
        if (tryPlayRecordedAudio(resolvedKey)) {
            return
        }

        if (!isTtsReady) {
            pendingSpeech = cleaned
            return
        }
        runCatching {
            _isSpeaking.value = true
            tts?.setSpeechRate(CHILD_SPEECH_RATE)
            tts?.setPitch(CHILD_SPEECH_PITCH)
            tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "reading_utterance_${System.currentTimeMillis()}")
            engineScope.launch {
                val estimatedMs = (cleaned.length * 90L).coerceIn(550L, 2200L)
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
        stopActiveMediaPlayer()
        segmentJob = engineScope.launch {
            _isSpeaking.value = true
            onWholeWordActive(false)
            word.syllables.forEachIndexed { index, syllable ->
                onActiveSyllableIndex(index)
                speakSingleSegmentImmediate(syllable.spokenText)
                delay(720L)
            }
            onActiveSyllableIndex(-1)
            onWholeWordActive(true)
            speakSingleSegmentImmediate(word.spokenWord)
            delay(900L)
            onWholeWordActive(false)
            _isSpeaking.value = false
            onFinished()
        }
    }

    private fun speakSingleSegmentImmediate(text: String) {
        val key = mapTextToAudioKey(text)
        if (tryPlayRecordedAudio(key)) return
        if (!isTtsReady) return
        runCatching {
            tts?.setSpeechRate(CHILD_SPEECH_RATE)
            tts?.setPitch(CHILD_SPEECH_PITCH)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "seg_${System.currentTimeMillis()}")
        }
    }

    /**
     * يبحث عن ملف صوتي MP3 مسجّل في الذاكرة المحلية أو في res/raw ويشغّله عبر MediaPlayer.
     */
    private fun tryPlayRecordedAudio(audioKey: String?): Boolean {
        if (audioKey.isNullOrBlank()) return false
        return runCatching {
            // 1) البحث في ذاكرة الكاش/ملفات التطبيق المحلية (reading_audio/<key>.mp3)
            val candidateFiles = listOf(
                File(File(appContext.filesDir, AUDIO_CACHE_DIR), "$audioKey.mp3"),
                File(File(appContext.cacheDir, AUDIO_CACHE_DIR), "$audioKey.mp3")
            )
            val localFile = candidateFiles.firstOrNull { it.exists() && it.length() > 128L }
            if (localFile != null) {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(localFile.absolutePath)
                    setOnCompletionListener { mp ->
                        _isSpeaking.value = false
                        runCatching { mp.release() }
                        if (activeMediaPlayer === mp) activeMediaPlayer = null
                    }
                    setOnErrorListener { mp, _, _ ->
                        _isSpeaking.value = false
                        runCatching { mp.release() }
                        if (activeMediaPlayer === mp) activeMediaPlayer = null
                        true
                    }
                    prepare()
                    start()
                }
                activeMediaPlayer = player
                _isSpeaking.value = true
                return true
            }

            // 2) البحث في res/raw عن مورد صوتي مطابق للاسم (مثلاً raw/reading_da.mp3)
            val rawResId = appContext.resources.getIdentifier(audioKey, "raw", appContext.packageName)
            if (rawResId != 0) {
                val player = MediaPlayer.create(appContext, rawResId)
                if (player != null) {
                    player.setOnCompletionListener { mp ->
                        _isSpeaking.value = false
                        runCatching { mp.release() }
                        if (activeMediaPlayer === mp) activeMediaPlayer = null
                    }
                    player.setOnErrorListener { mp, _, _ ->
                        _isSpeaking.value = false
                        runCatching { mp.release() }
                        if (activeMediaPlayer === mp) activeMediaPlayer = null
                        true
                    }
                    activeMediaPlayer = player
                    _isSpeaking.value = true
                    player.start()
                    return true
                }
            }
            false
        }.getOrDefault(false)
    }

    private fun stopActiveMediaPlayer() {
        runCatching {
            activeMediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        activeMediaPlayer = null
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
        stopActiveMediaPlayer()
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
        private const val CHILD_SPEECH_RATE = 0.86f
        private const val CHILD_SPEECH_PITCH = 1.22f
        private const val AUDIO_CACHE_DIR = "reading_audio"
        private const val PREFS_NAME = "reading_oasis_progress_prefs"
        private const val BASE_RTDB_URL = "https://madrasati-app-d51dd-default-rtdb.firebaseio.com"

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
