package com.atelbay.money_manager

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.components.MoneyManagerCard
import androidx.compose.material3.Text

class ExampleScreenshotTest {
    @Preview(showBackground = true)
    @Composable
    fun CardScreenshotPreview() {
        MoneyManagerTheme {
            MoneyManagerCard {
                Text(text = "Screenshot Test Card")
            }
        }
    }
}
