package com.example.reading.qiraati

import androidx.compose.ui.graphics.Color

/**
 * المستودع المركزي القابل للتوسّع لكتاب "قراءتي الجديدة — الصف الأول الابتدائي".
 *
 * لإضافة أي وحدة جديدة مستقبلاً (مثل الوحدة الثانية: مدرستي):
 * 1. أنشئ ملف بيانات واحد (مثل Unit2MadrasatiData.kt) يعرّف `val unit: QiraatiUnit`.
 * 2. استبدل عنصر الوحدة القادمة هنا بـ `Unit2MadrasatiData.unit` دون تعديل أي ملف آخر.
 */
object NewQiraatiRepository {

    const val BOOK_TITLE: String = "قراءتي الجديدة"
    const val BOOK_SUBTITLE: String = "الصف الأول الابتدائي — المنهج العراقي"

    private val upcomingUnitsCatalog: List<QiraatiUnit> = emptyList()

    /**
     * جميع وحدات كتاب "قراءتي الجديدة — الصف الأول الابتدائي" مكتملة بالوحدات الأربع:
     * (الأولى: أسرتي، الثانية: مدرستي، الثالثة: وطني، الرابعة: صحتي وبيئتي).
     */
    val allUnits: List<QiraatiUnit> = listOf(
        Unit1UsratyData.unit,
        Unit2MadrasatiData.unit,
        Unit3WataniData.unit,
        Unit4SihatiWaBiatiData.unit
    ) + upcomingUnitsCatalog

    /** الوحدات المتوفرة والجاهزة للتعلّم حالياً. */
    val availableUnits: List<QiraatiUnit>
        get() = allUnits.filter { it.isAvailable }

    /** الوحدات القادمة (قريباً) إن وُجدت في الإصدارات المستقبلية. */
    val upcomingUnits: List<QiraatiUnit>
        get() = allUnits.filterNot { it.isAvailable }

    /** جميع دروس الحروف المتوفرة حالياً عبر الوحدات المفعّلة. */
    val allAvailableLessons: List<QiraatiLetterLesson>
        get() = availableUnits.flatMap { it.letterLessons }

    fun getUnitById(unitId: String): QiraatiUnit? =
        allUnits.firstOrNull { it.id == unitId }

    fun getUnitByNumber(unitNumber: Int): QiraatiUnit? =
        allUnits.firstOrNull { it.unitNumber == unitNumber }

    fun getLessonById(lessonId: String): QiraatiLetterLesson? =
        allAvailableLessons.firstOrNull { it.id == lessonId }
}
