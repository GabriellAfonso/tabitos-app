package com.tabitos.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.app.Notification
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.tabitos.app.core.tasks.domain.*
import com.tabitos.app.features.tasks.TasksViewModel
import com.tabitos.app.features.reminders.ReminderViewModel
import com.tabitos.app.core.reminders.platform.ReminderPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReminderActivityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun dailyReviewOpensTodayWithAllItemsFromSettings() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("filter_HABITS").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("filter_HABITS").performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_menu)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.settings)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.appearance)).assertExists()
        val remaining = 60_000L
        val notice = ReminderPlatform(compose.activity).reviewNotification(System.currentTimeMillis(), remaining)
        assertTrue(notice.flags and Notification.FLAG_ONGOING_EVENT != 0)
        if (Build.VERSION.SDK_INT >= 26) assertEquals(remaining, notice.timeoutAfter)
        notice.contentIntent.send()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("filter_ALL").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("filter_ALL").assertIsSelected()
    }

    private fun editorIsTop(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.text?.toString() == "Tipo de lembrete") return true
        if (node.className == "android.widget.Button" && node.text?.toString() in setOf("Wait", "Aguardar"))
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return (0 until node.childCount).any { editorIsTop(node.getChild(it)) }
    }

    @Test fun realReminderEditorStaysAboveTaskDetailAfterActivityRecreation() {
        lateinit var vm: TasksViewModel
        lateinit var id: String
        compose.runOnIdle {
            vm = ViewModelProvider(compose.activity)[TasksViewModel::class.java]
            vm.newTask()
            val draft = vm.state.value.draft!!
            id = draft.id
            vm.updateDraft(draft.copy(fields = draft.fields.copy(title = "Teste fictício de recriação")))
            vm.saveDraft()
        }
        compose.waitUntil(15000) { vm.state.value.snapshot.occurrences.any { it.taskId == id } &&
            vm.state.value.draft == null }
        try {
            compose.runOnIdle { vm.detail("$id:1") }
            compose.onNodeWithText("Lembretes").performScrollTo().performClick()
            compose.onNodeWithText("Novo lembrete").performScrollTo().performClick()
            compose.activityRule.scenario.recreate()
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            compose.waitUntil(15000) { editorIsTop(automation.rootInActiveWindow) }
            assertTrue(editorIsTop(automation.rootInActiveWindow))
            automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            compose.onNodeWithText(compose.activity.getString(R.string.reminder_empty)).assertIsDisplayed()
        } finally {
            compose.runOnIdle {
                ViewModelProvider(compose.activity)[ReminderViewModel::class.java].close()
                val current = ViewModelProvider(compose.activity)[TasksViewModel::class.java]
                current.action(TaskAction.Delete("$id:1", ChangeScope.ONLY_THIS))
                current.detail(null)
            }
        }
    }
}
