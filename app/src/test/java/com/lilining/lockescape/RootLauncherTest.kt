package com.lilining.lockescape

import org.junit.Assert.assertEquals
import org.junit.Test

class RootLauncherTest {
    @Test
    fun rootLauncherStartsInRootMode() {
        assertEquals(ShellExecutor.Mode.ROOT, RootLauncherDefaults.executionMode)
    }
}
