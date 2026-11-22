package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.CmdMode
import com.example.domain.OcsRuleValidator
import com.example.domain.ProtocolRegistry
import com.example.domain.RunFlagMode
import com.example.domain.SonicContrastPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OCS Runtime", appName)
    }

    @Test
    fun `rule 01 rejects SuperGrok Leader alias`() {
        val warnings = OcsRuleValidator.validateObjectiveInput("Delegate to SuperGrok Leader")
        assertTrue(warnings.isNotEmpty())
        assertTrue(warnings.first().contains("RULE-01"))
    }

    @Test
    fun `rule 09 computes dual axis structural coherence accurately`() {
        val score = OcsRuleValidator.computeStructuralCoherence(
            invokedAtomicAgentCount = 14,
            stepsDone = 20,
            undeclaredDriftCount = 0
        )
        assertEquals(1.0, score, 0.001)
        assertEquals(14, ProtocolRegistry.agents.size)
        assertEquals(20, ProtocolRegistry.steps.size)
        assertEquals(9, ProtocolRegistry.tasPrimitives.size)
    }

    @Test
    fun `rule 10 triggers consent gate on energy 4, adaptive tempo, or Path D`() {
        assertFalse(OcsRuleValidator.requiresSonicConsentGate(3, false, SonicContrastPath.PATH_B_PULSE))
        assertTrue(OcsRuleValidator.requiresSonicConsentGate(4, false, SonicContrastPath.PATH_B_PULSE))
        assertTrue(OcsRuleValidator.requiresSonicConsentGate(2, true, SonicContrastPath.PATH_A_AMBIENT))
        assertTrue(OcsRuleValidator.requiresSonicConsentGate(2, false, SonicContrastPath.PATH_D_FULL_BLOOM))
    }

    @Test
    fun `mode projection maps Predictive and Hybrid into 4-value run_flags subset`() {
        assertEquals(RunFlagMode.Swarm, CmdMode.Predictive.projectToRunFlagMode(RunFlagMode.Swarm))
        assertEquals(RunFlagMode.Fluid, CmdMode.Hybrid.projectToRunFlagMode(RunFlagMode.Fluid))
        assertEquals(RunFlagMode.Strict, CmdMode.Strict.projectToRunFlagMode())
    }
}
