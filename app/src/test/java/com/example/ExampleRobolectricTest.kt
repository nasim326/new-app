package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.util.InterestCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Budget Tracker", appName)
    }

    @Test
    fun `calculate fixed deposit compound interest`() {
        val estimate = InterestCalculator.calculateFdMaturity(
            principal = 10000.0,
            annualRatePercent = 8.0,
            openingDate = System.currentTimeMillis(),
            maturityDate = System.currentTimeMillis() + (365L * 24 * 3600 * 1000),
            compoundingFrequency = "ANNUALLY"
        )
        assertTrue("Estimated interest should be positive", estimate.estimatedInterest > 790.0)
        assertTrue("Maturity amount should exceed principal", estimate.maturityAmount > 10000.0)
    }

    @Test
    fun `launch MainActivity scenario`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }
}
