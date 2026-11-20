package com.tabitos.app.features.backup

import com.tabitos.app.core.backup.BackupInspection
import com.tabitos.app.core.backup.BackupPreview
import com.tabitos.app.core.backup.BackupRepository
import com.tabitos.app.core.backup.ReplacementRequired
import com.tabitos.app.core.tasks.domain.TaskClock
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val clock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.of(2026, 11, 19)
        override fun nowMillis() = 0L
    }
    private val vm = BackupViewModel(repository, clock)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun protectedFileKeepsPreviewPrivateUntilPasswordWorks() = runTest(dispatcher) {
        repository.protected = true
        vm.openDocument("source")
        advanceUntilIdle()
        assertEquals(BackupUiState.Password(false), vm.state.value)
        vm.unlock("wrong".toCharArray())
        advanceUntilIdle()
        assertEquals(BackupUiState.Password(true), vm.state.value)
        vm.unlock("secret".toCharArray())
        advanceUntilIdle()
        assertTrue(vm.state.value is BackupUiState.Preview)
        assertTrue(repository.confirmations.isEmpty())
    }

    @Test fun newPersonalDataRequiresConfirmationAndAllowsExportingBeforeReplacement() = runTest(dispatcher) {
        vm.openDocument("source")
        advanceUntilIdle()
        assertFalse((vm.state.value as BackupUiState.Preview).replacement)
        repository.gainedPersonalData = true
        vm.restore()
        advanceUntilIdle()
        val warning = vm.state.value as BackupUiState.Preview
        assertTrue(warning.replacement)
        vm.offerExport()
        val name = vm.prepareExport("secret".toCharArray())
        assertTrue(name!!.endsWith(".zip.enc"))
        vm.exportTo("safety")
        advanceUntilIdle()
        assertEquals("secret", repository.exportedPassword)
        vm.dismiss()
        assertSame(warning, vm.state.value)
        vm.restore()
        advanceUntilIdle()
        assertEquals(listOf(false, true), repository.confirmations)
        assertEquals(BackupUiState.Notice(BackupMessage.IMPORTED), vm.state.value)
    }

    @Test fun cancellingThePickerReturnsToPreviewWithoutRestoring() = runTest(dispatcher) {
        vm.openDocument("source")
        advanceUntilIdle()
        val preview = vm.state.value
        vm.offerExport()
        vm.prepareExport(null)
        vm.exportTo(null)
        assertSame(preview, vm.state.value)
        vm.dismiss()
        assertEquals(BackupUiState.Idle, vm.state.value)
        assertTrue(repository.confirmations.isEmpty())
    }

    private class FakeRepository : BackupRepository {
        val confirmations = mutableListOf<Boolean>()
        var exportedPassword: String? = null
        var gainedPersonalData = false
        var protected = false

        override suspend fun inspect(document: String, password: CharArray?): BackupInspection {
            if (protected && password == null) return BackupInspection.PasswordRequired
            check(!protected || password?.concatToString() == "secret")
            val preview = BackupPreview(0L, 2, 4, 1, false, byteArrayOf(), byteArrayOf())
            return BackupInspection.Ready(preview)
        }

        override suspend fun export(document: String, password: CharArray?) {
            exportedPassword = password?.concatToString()
        }

        override suspend fun restore(preview: BackupPreview, confirmedReplacement: Boolean) {
            confirmations += confirmedReplacement
            if (gainedPersonalData && !confirmedReplacement) throw ReplacementRequired()
        }
    }
}
