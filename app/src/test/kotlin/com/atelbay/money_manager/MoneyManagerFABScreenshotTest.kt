package com.atelbay.money_manager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import org.robolectric.RobolectricTestRunner
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.atelbay.money_manager.core.ui.components.MoneyManagerFAB
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "xxhdpi")
class MoneyManagerFABScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureFloatingActionButton() {
        composeTestRule.setContent {
            MoneyManagerTheme(dynamicColor = false) {
                MoneyManagerFAB(
                    onClick = {},
                    icon = Icons.Default.Add,
                    contentDescription = "Add"
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/fab_screenshot.png")
    }
}
