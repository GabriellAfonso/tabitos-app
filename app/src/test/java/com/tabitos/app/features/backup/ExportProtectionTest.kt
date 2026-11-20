package com.tabitos.app.features.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportProtectionTest {
    @Test fun plainCopyNeedsNoPassword() {
        val choice = ExportProtection(protect = false, password = "ignored", confirmation = "")
        assertTrue(choice.ready)
        assertNull(choice.password())
    }

    @Test fun protectedCopyNeedsTheSamePasswordTwice() {
        assertFalse(ExportProtection(true, "", "").ready)
        val mismatch = ExportProtection(true, "rotina", "rotin")
        assertFalse(mismatch.ready)
        assertTrue(mismatch.showsMismatch)
        val typing = ExportProtection(true, "rotina", "")
        assertFalse(typing.showsMismatch)
        val ready = ExportProtection(true, "rotina", "rotina")
        assertTrue(ready.ready)
        assertArrayEquals("rotina".toCharArray(), ready.password())
    }
}
