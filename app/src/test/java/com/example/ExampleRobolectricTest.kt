package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.SchoolType
import com.example.model.SubjectGrade
import com.example.viewmodel.SchoolViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify school setup and grade calculation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مدرستي", appName)

        val viewModel = SchoolViewModel(context as Application)
        viewModel.setupSchoolAndAdmin(
            schoolName = "مدرستي النموذجية",
            address = "الموصل - زمار",
            schoolType = SchoolType.PRIVATE,
            logoBase64 = "",
            adminFullName = "المدير العام",
            adminUsername = "admin",
            adminPassword = "123"
        )
        assertTrue(viewModel.schoolConfig.value.isSetupCompleted)
        assertEquals("الموصل - زمار", viewModel.schoolConfig.value.address)

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
        assertEquals(96, sampleGrade.annualEffortScore)
        assertEquals(97, sampleGrade.finalAverage)
        assertEquals("ممتاز", sampleGrade.ratingLabel)
    }
}
