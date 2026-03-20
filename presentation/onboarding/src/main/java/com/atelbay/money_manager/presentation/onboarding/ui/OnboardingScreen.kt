package com.atelbay.money_manager.presentation.onboarding.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atelbay.money_manager.core.ui.theme.MoneyManagerTheme
import com.atelbay.money_manager.core.ui.theme.AppStrings
import com.atelbay.money_manager.core.ui.theme.OutfitFontFamily

// T003: Color constants
private val OnboardingGreen = Color(0xFF3D8A5A)
private val OnboardingGreenLight = Color(0xFF3D8A5A).copy(alpha = 0.07f)
private val OnboardingBackground = Color(0xFFF5F4F1)
private val OnboardingTextPrimary = Color(0xFF1A1918)
private val OnboardingTextSecondary = Color(0xFF6D6C6A)
private val OnboardingSkipColor = Color(0xFF9C9B99)
private val OnboardingDotInactive = Color(0xFFD4D3D0)

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onPageChanged: (Int) -> Unit,
    onNextClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { 3 },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(page)
        }
    }

    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }

    // T008: Root background + updated padding/spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OnboardingBackground)
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
            .testTag("onboarding:screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        val s = MoneyManagerTheme.strings
        val localizedPages = remember(s) { localizedOnboardingPages(s) }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding:pager"),
        ) { page ->
            OnboardingPageContent(
                page = localizedPages[page],
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // T008: 32dp spacer between pager and indicator
        Spacer(modifier = Modifier.height(32.dp))

        PageIndicator(
            pageCount = localizedPages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.testTag("onboarding:indicator"),
        )

        Spacer(modifier = Modifier.weight(1f))

        val isLastPage = pagerState.currentPage == localizedPages.size - 1

        // T005: Redesigned main action button
        Button(
            onClick = onNextClick,
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth()
                .testTag("onboarding:nextButton"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OnboardingGreen,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = if (isLastPage) s.start else s.next,
                style = TextStyle(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                ),
            )
        }

        // T008: 12dp spacer between button and skip text
        Spacer(modifier = Modifier.height(12.dp))

        // T006: Skip as plain Text with clickable modifier
        if (!isLastPage) {
            Text(
                text = s.skip,
                style = TextStyle(
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                color = OnboardingSkipColor,
                modifier = Modifier
                    .clickable(onClick = onSkipClick)
                    .testTag("onboarding:skipButton"),
            )
        }
    }
}

// T004: Redesigned OnboardingPageContent
@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 160dp circular container with OnboardingGreenLight background
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(OnboardingGreenLight),
        ) {
            Icon(
                imageVector = iconForPage(page.icon),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = OnboardingGreen,
            )
        }

        // 32dp between icon container and title
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                letterSpacing = (-0.5).sp,
            ),
            color = OnboardingTextPrimary,
            textAlign = TextAlign.Center,
        )

        // 12dp between title and description
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.description,
            style = TextStyle(
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.5.sp,
            ),
            color = OnboardingTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
    }
}

// T007: Updated PageIndicator colors
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pageCount) { index ->
            val color = animateColorAsState(
                targetValue = if (index == currentPage) {
                    OnboardingGreen
                } else {
                    OnboardingDotInactive
                },
                label = "indicator_color",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color.value),
            )
        }
    }
}

private fun iconForPage(name: String): ImageVector = when (name) {
    "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
    "bar_chart" -> Icons.Default.BarChart
    "insights" -> Icons.Default.Insights
    else -> Icons.Default.AccountBalanceWallet
}

private fun localizedOnboardingPages(s: AppStrings): List<OnboardingPage> = listOf(
    OnboardingPage(title = s.onboardingPage1Title, description = s.onboardingPage1Desc, icon = "account_balance_wallet"),
    OnboardingPage(title = s.onboardingPage2Title, description = s.onboardingPage2Desc, icon = "bar_chart"),
    OnboardingPage(title = s.onboardingPage3Title, description = s.onboardingPage3Desc, icon = "insights"),
)

// T009: Preview with new design
@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    MoneyManagerTheme(dynamicColor = false) {
        OnboardingScreen(
            state = OnboardingState(),
            onPageChanged = {},
            onNextClick = {},
            onSkipClick = {},
        )
    }
}
