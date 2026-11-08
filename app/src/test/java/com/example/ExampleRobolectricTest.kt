package com.example

import com.example.model.ArabicTextNormalizer
import com.example.model.GradingRules
import com.example.model.SchoolStage
import com.example.model.SchoolType
import com.example.model.SubjectGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun `verify school stage and grade calculation`() {
        assertEquals(6, SchoolStage.PRIMARY.defaultClasses.size)
        assertEquals("مدرسة أهلية", SchoolType.PRIVATE.arabicLabel)

        val normalizedDigits = ArabicTextNormalizer.normalizeDigitsToAscii("٢٠٢٦-١١-٠٨")
        assertEquals("2026-11-08", normalizedDigits)

        val sampleGrade = SubjectGrade(
            id = "g1",
            studentId = "s1",
            studentName = "يوسف",
            gradeClass = "الأول المتوسط",
            subjectName = "الرياضيات",
            dailyScore = 96,
            month1Score = 94,
            month2Score = 98,
            midYearScore = 96,
            finalExamScore = 98
        )
        assertEquals(100, GradingRules.defaultMaxScoreForClass("الأول المتوسط"))
        assertEquals(96, sampleGrade.annualEffortScore)
        assertEquals(97, sampleGrade.finalAverage)
        assertEquals("ممتاز", sampleGrade.ratingLabel)
        assertTrue(sampleGrade.finalAverage >= 50)
    }
}
