package com.injectbuddy.android.screenshot

import app.cash.paparazzi.Paparazzi
import com.injectbuddy.android.feature.calculator.CalculatorContent
import com.injectbuddy.android.feature.calculator.CyclePlotterContent
import com.injectbuddy.android.feature.calculator.SaveStatus
import com.injectbuddy.android.feature.calculator.samplePeptideState
import com.injectbuddy.android.feature.calculator.samplePlotterCompounds
import com.injectbuddy.android.feature.calculator.sampleTrtState
import org.junit.Rule
import org.junit.Test

/**
 * Headless Paparazzi snapshots of the stateless calculator content in the real theme. Each
 * case feeds a sample state + no-op lambdas, so nothing touches a ViewModel, ServiceLocator,
 * or the network. Snapshot PNGs are uploaded by CI and reviewed visually.
 */
class CalculatorScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE)

    @Test
    fun trtCalculator_light() = paparazzi.themed(dark = false) {
        val state = sampleTrtState()
        CalculatorContent(
            spec = state.spec,
            inputs = state.inputs,
            result = state.result,
            saveStatus = SaveStatus.Idle,
            onInput = { _, _ -> },
            onSave = {},
            onBack = {},
        )
    }

    @Test
    fun trtCalculator_dark() = paparazzi.themed(dark = true) {
        val state = sampleTrtState()
        CalculatorContent(
            spec = state.spec,
            inputs = state.inputs,
            result = state.result,
            saveStatus = SaveStatus.Idle,
            onInput = { _, _ -> },
            onSave = {},
            onBack = {},
        )
    }

    @Test
    fun peptideCalculator_light() = paparazzi.themed(dark = false) {
        val state = samplePeptideState()
        CalculatorContent(
            spec = state.spec,
            inputs = state.inputs,
            result = state.result,
            saveStatus = SaveStatus.Idle,
            onInput = { _, _ -> },
            onSave = {},
            onBack = {},
        )
    }

    @Test
    fun cyclePlotter_light() = paparazzi.themed(dark = false) {
        CyclePlotterContent(
            compounds = samplePlotterCompounds(),
            onAddCompound = {},
            onChangeCompound = { _, _ -> },
            onRemoveCompound = {},
            onBack = {},
        )
    }
}
