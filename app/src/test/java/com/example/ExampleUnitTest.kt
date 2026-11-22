package com.example

import com.example.domain.CmdMode
import com.example.domain.OcsRuleValidator
import com.example.domain.ProtocolRegistry
import com.example.domain.RunFlagMode
import com.example.domain.SonicContrastPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun rule01_rejectsDeprecatedSuperGrokLeaderAlias() {
        val warnings = OcsRuleValidator.validateObjectiveInput("Delegate to SuperGrok Leader")
        assertTrue(warnings.isNotEmpty())
        assertTrue(warnings.first().contains("RULE-01"))
    }

    @Test
    fun rule09_computesDualAxisStructuralCoherenceAndPreservesRosterBounds() {
        val score = OcsRuleValidator.computeStructuralCoherence(
            invokedAtomicAgentCount = 14,
            stepsDone = 20,
            undeclaredDriftCount = 0
        )
        assertEquals(1.0, score, 0.001)
        assertEquals(14, ProtocolRegistry.agents.size)
        assertEquals(20, ProtocolRegistry.steps.size)
        assertEquals(9, ProtocolRegistry.tasPrimitives.size)
        assertEquals(20, ProtocolRegistry.openDecisions.size)
    }

    @Test
    fun rule10_triggersConsentGateOnEnergy4AdaptiveTempoOrPathD() {
        assertFalse(OcsRuleValidator.requiresSonicConsentGate(3, false, SonicContrastPath.PATH_B_PULSE))
        assertTrue(OcsRuleValidator.requiresSonicConsentGate(4, false, SonicContrastPath.PATH_B_PULSE))
        assertTrue(OcsRuleValidator.requiresSonicConsentGate(2, true, SonicContrastPath.PATH_A_AMBIENT))
        assertTrue(OcsRuleValidator.requiresSonicConsentGate(2, false, SonicContrastPath.PATH_D_FULL_BLOOM))
    }

    @Test
    fun modeProjection_mapsPredictiveAndHybridIntoFourValueSubset() {
        assertEquals(RunFlagMode.Swarm, CmdMode.Predictive.projectToRunFlagMode(RunFlagMode.Swarm))
        assertEquals(RunFlagMode.Fluid, CmdMode.Hybrid.projectToRunFlagMode(RunFlagMode.Fluid))
        assertEquals(RunFlagMode.Strict, CmdMode.Strict.projectToRunFlagMode())
    }

    @Test
    fun format09_validatesLyria3PromptEnvelopeOrdering() {
        val valid = """
            ⫻prompt/obsidian_bloom
            ⫻role/sonic_architect
            ⫻objective/spatial pulse
            ⫻context/wide obsidian hall
            ⫻constraints/micro_timing +5ms
            ⫻task/compose stem
            ⫻output-format/lyria3 brief
            ⫻evaluation/sonic axis open until wet-lift
        """.trimIndent()
        assertTrue(OcsRuleValidator.validateFormat09Envelope(valid).isEmpty())
    }
}
