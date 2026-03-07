package com.atelbay.money_manager.navigation

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PendingNavigationManagerTest {

    private lateinit var manager: PendingNavigationManager

    @Before
    fun setup() {
        manager = PendingNavigationManager()
    }

    @Test
    fun `initial state is null`() = runTest {
        assertNull(manager.pendingAction.value)
    }

    @Test
    fun `enqueue sets pending action`() = runTest {
        val action = NavigationAction.OpenImport("content://pdf")
        manager.enqueue(action)
        assertEquals(action, manager.pendingAction.value)
    }

    @Test
    fun `consume clears pending action`() = runTest {
        manager.enqueue(NavigationAction.OpenImport("content://pdf"))
        manager.consume()
        assertNull(manager.pendingAction.value)
    }

    @Test
    fun `enqueue emits new action to collectors`() = runTest {
        val action = NavigationAction.OpenImport("content://pdf")
        manager.pendingAction.test {
            awaitItem() // initial null
            manager.enqueue(action)
            assertEquals(action, awaitItem())
            cancel()
        }
    }

    @Test
    fun `consume emits null to collectors`() = runTest {
        manager.enqueue(NavigationAction.OpenImport("content://pdf"))
        manager.pendingAction.test {
            awaitItem() // current action
            manager.consume()
            assertNull(awaitItem())
            cancel()
        }
    }

    @Test
    fun `second enqueue overwrites first`() = runTest {
        val first = NavigationAction.OpenImport("content://pdf1")
        val second = NavigationAction.OpenImport("content://pdf2")
        manager.enqueue(first)
        manager.enqueue(second)
        assertEquals(second, manager.pendingAction.value)
    }
}
