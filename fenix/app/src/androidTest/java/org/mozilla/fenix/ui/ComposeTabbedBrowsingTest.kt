/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.android.material.bottomsheet.BottomSheetBehavior
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.RetryTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.clickSnackbarButton
import org.mozilla.fenix.helpers.TestHelper.closeApp
import org.mozilla.fenix.helpers.TestHelper.restartApp
import org.mozilla.fenix.helpers.TestHelper.verifyKeyboardVisibility
import org.mozilla.fenix.helpers.TestHelper.verifySnackBarText
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.notificationShade

/**
 *  Tests for verifying basic functionality of tabbed browsing
 *
 *  Including:
 *  - Opening a tab
 *  - Opening a private tab
 *  - Verifying tab list
 *  - Closing all tabs
 *  - Close tab
 *  - Swipe to close tab (temporarily disabled)
 *  - Undo close tab
 *  - Close private tabs persistent notification
 *  - Empty tab tray state
 *  - Tab tray details
 *  - Shortcut context menu navigation
 */

class ComposeTabbedBrowsingTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer

    @get:Rule(order = 0)
    val composeTestRule =
        AndroidComposeTestRule(
            HomeActivityIntentTestRule.withDefaultSettingsOverrides(
                skipOnboarding = true,
                tabsTrayRewriteEnabled = true,
            ),
        ) { it.activity }

    @Rule(order = 1)
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun openNewTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.waitForIdle()
            verifyTabCounter("1")
        }.openComposeTabDrawer(composeTestRule) {
            verifyNormalBrowsingButtonIsSelected()
            verifyExistingOpenTabs("Test_Page_1")
            closeTab()
        }
        homeScreen {
        }.openComposeTabDrawer(composeTestRule) {
            verifyNoOpenTabsInNormalBrowsing()
        }.openNewTab {
        }.submitQuery(defaultWebPage.url.toString()) {
            verifyPageContent(defaultWebPage.content)
            verifyTabCounter("1")
        }.openComposeTabDrawer(composeTestRule) {
            verifyNormalBrowsingButtonIsSelected()
            verifyExistingOpenTabs("Test_Page_1")
        }
    }

    @Test
    fun openNewPrivateTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.togglePrivateBrowsingMode()
        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.waitForIdle()
            verifyTabCounter("1")
        }.openComposeTabDrawer(composeTestRule) {
            verifyPrivateTabsList()
            verifyPrivateBrowsingButtonIsSelected()
        }.toggleToNormalTabs {
            verifyNoOpenTabsInNormalBrowsing()
        }.toggleToPrivateTabs {
            verifyPrivateTabsList()
        }
    }

    @Test
    fun closeAllTabsTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyNormalTabsList()
        }.openThreeDotMenu {
            verifyCloseAllTabsButton()
            verifyShareAllTabsButton()
            verifySelectTabsButton()
        }.closeAllTabs {
            verifyTabCounter("0")
        }

        // Repeat for Private Tabs
        homeScreen {
        }.togglePrivateBrowsingMode()

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyPrivateTabsList()
        }.openThreeDotMenu {
            verifyCloseAllTabsButton()
        }.closeAllTabs {
            verifyTabCounter("0")
        }
    }

    @Test
    fun closeTabTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            closeTab()
        }
        homeScreen {
            verifyTabCounter("0")
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            swipeTabRight("Test_Page_1")
        }
        homeScreen {
            verifyTabCounter("0")
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            swipeTabLeft("Test_Page_1")
        }
        homeScreen {
            verifyTabCounter("0")
        }
    }

    @Test
    fun verifyUndoSnackBarTest() {
        // disabling these features because they interfere with the snackbar visibility
        composeTestRule.activityRule.applySettingsExceptions {
            it.isPocketEnabled = false
            it.isRecentTabsFeatureEnabled = false
            it.isRecentlyVisitedFeatureEnabled = false
        }

        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            closeTab()
            verifySnackBarText("Tab closed")
            clickSnackbarButton("UNDO")
        }

        browserScreen {
            verifyTabCounter("1")
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
        }
    }

    @Test
    fun closePrivateTabTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen { }.togglePrivateBrowsingMode()
        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            closeTab()
        }
        homeScreen {
            verifyTabCounter("0")
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            swipeTabRight("Test_Page_1")
        }
        homeScreen {
            verifyTabCounter("0")
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            swipeTabLeft("Test_Page_1")
        }
        homeScreen {
            verifyTabCounter("0")
        }
    }

    @Test
    fun verifyPrivateTabUndoSnackBarTest() {
        val genericURL = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen { }.togglePrivateBrowsingMode()
        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
            verifyPageContent(genericURL.content)
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            closeTab()
            verifySnackBarText("Private tab closed")
            clickSnackbarButton("UNDO")
        }

        browserScreen {
            verifyPageContent(genericURL.content)
            verifyTabCounter("1")
        }.openComposeTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            verifyPrivateBrowsingButtonIsSelected()
        }
    }

    @SmokeTest
    @Test
    fun verifyCloseAllPrivateTabsNotificationTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.togglePrivateBrowsingMode()

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.openNotification()
        }

        notificationShade {
            verifyPrivateTabsNotification()
        }.clickClosePrivateTabsNotification {
            verifyHomeScreen()
        }
    }

    @Test
    fun verifyTabTrayNotShowingStateHalfExpanded() {
        homeScreen {
        }.openComposeTabDrawer(composeTestRule) {
            verifyNoOpenTabsInNormalBrowsing()
            // With no tabs opened the state should be STATE_COLLAPSED.
            verifyTabsTrayBehaviorState(BottomSheetBehavior.STATE_COLLAPSED)
            // Need to ensure the halfExpandedRatio is very small so that when in STATE_HALF_EXPANDED
            // the tabTray will actually have a very small height (for a very short time) akin to being hidden.
            verifyMinusculeHalfExpandedRatio()
        }.clickTopBar {
        }.waitForTabTrayBehaviorToIdle {
            // Touching the topBar would normally advance the tabTray to the next state.
            // We don't want that.
            verifyTabsTrayBehaviorState(BottomSheetBehavior.STATE_COLLAPSED)
        }.advanceToHalfExpandedState {
        }.waitForTabTrayBehaviorToIdle {
            // TabTray should not be displayed in STATE_HALF_EXPANDED.
            // When advancing to this state it should immediately be hidden.
            verifyTabTrayIsClosed()
        }
    }

    @Test
    fun verifyEmptyTabTray() {
        homeScreen {
        }.openComposeTabDrawer(composeTestRule) {
            verifyNormalBrowsingButtonIsSelected()
            verifyPrivateBrowsingButtonIsSelected(false)
            verifySyncedTabsButtonIsSelected(false)
            verifyNoOpenTabsInNormalBrowsing()
            verifyFab()
            verifyThreeDotButton()
        }.openThreeDotMenu {
            verifyTabSettingsButton()
            verifyRecentlyClosedTabsButton()
        }
    }

    @Test
    fun emptyTabsTrayViewPrivateBrowsingTest() {
        homeScreen {
        }.openComposeTabDrawer(composeTestRule) {
        }.toggleToPrivateTabs {
            verifyNormalBrowsingButtonIsSelected(false)
            verifyPrivateBrowsingButtonIsSelected(true)
            verifySyncedTabsButtonIsSelected(false)
            verifyNoOpenTabsInPrivateBrowsing()
            verifyFab()
            verifyThreeDotButton()
        }.openThreeDotMenu {
            verifyTabSettingsButton()
            verifyRecentlyClosedTabsButton()
        }
    }

    @Test
    fun verifyOpenTabDetails() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openComposeTabDrawer(composeTestRule) {
            verifyNormalBrowsingButtonIsSelected()
            verifyPrivateBrowsingButtonIsSelected(isSelected = false)
            verifySyncedTabsButtonIsSelected(isSelected = false)
            verifyThreeDotButton()
            verifyNormalTabCounter()
            verifyNormalTabsList()
            verifyFab()
            verifyTabThumbnail()
            verifyExistingOpenTabs(defaultWebPage.title)
            verifyTabCloseButton()
        }.openTab(defaultWebPage.title) {
            verifyUrl(defaultWebPage.url.toString())
            verifyTabCounter("1")
        }
    }

    @Test
    fun verifyContextMenuShortcuts() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabButtonShortcutsMenu {
            verifyTabButtonShortcutMenuItems()
        }.closeTabFromShortcutsMenu {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabButtonShortcutsMenu {
        }.openNewPrivateTabFromShortcutsMenu {
            verifyKeyboardVisibility()
            verifySearchBarPlaceholder("Search or enter address")
            // dismiss search dialog
        }.dismissSearchBar {
            verifyCommonMythsLink()
            verifyNavigationToolbar()
        }
        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabButtonShortcutsMenu {
        }.openTabFromShortcutsMenu {
            verifyKeyboardVisibility()
            verifySearchBarPlaceholder("Search or enter address")
            // dismiss search dialog
        }.dismissSearchBar {
            verifyHomeWordmark()
            verifyNavigationToolbar()
        }
    }

    @Test
    fun verifySyncedTabsWhenUserIsNotSignedInTest() {
        navigationToolbar {
        }.openComposeTabDrawer(composeTestRule) {
            verifySyncedTabsButtonIsSelected(isSelected = false)
        }.toggleToSyncedTabs {
            verifySyncedTabsButtonIsSelected(isSelected = true)
            verifySyncedTabsListWhenUserIsNotSignedIn()
        }.clickSignInToSyncButton {
            verifyTurnOnSyncMenu()
        }
    }

    @Test
    fun privateModeStaysAsDefaultAfterRestartTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.goToHomescreen {
        }.togglePrivateBrowsingMode()

        closeApp(composeTestRule.activityRule)
        restartApp(composeTestRule.activityRule)

        homeScreen {
            verifyPrivateBrowsingHomeScreen()
        }.openComposeTabDrawer(composeTestRule) {
        }.toggleToNormalTabs {
            verifyExistingOpenTabs(defaultWebPage.title)
        }
    }

    @SmokeTest
    @Test
    fun privateTabsDoNotPersistAfterClosingAppTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        homeScreen {
        }.togglePrivateBrowsingMode()

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.openComposeTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(secondWebPage.url.toString()) {
        }
        closeApp(composeTestRule.activityRule)
        restartApp(composeTestRule.activityRule)
        homeScreen {
            verifyPrivateBrowsingHomeScreen()
        }.openComposeTabDrawer(composeTestRule) {
            verifyNoOpenTabsInPrivateBrowsing()
        }
    }
}
