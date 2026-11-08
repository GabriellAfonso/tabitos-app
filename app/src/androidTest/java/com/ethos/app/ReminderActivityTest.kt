package com.ethos.app

import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.features.tasks.TasksViewModel
import com.ethos.app.features.reminders.ReminderViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReminderActivityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun editorIsTop(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.text?.toString() == "O lembrete não altera o prazo nem a meta do item.") return true
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
            compose.onNodeWithText("Adicionar ou editar lembrete").performScrollTo().performClick()
            compose.activityRule.scenario.recreate()
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            compose.waitUntil(15000) { editorIsTop(automation.rootInActiveWindow) }
            assertTrue(editorIsTop(automation.rootInActiveWindow))
            compose.onNodeWithText(compose.activity.getString(R.string.cancel)).performClick()
            compose.onNodeWithText("Nenhum lembrete configurado para este item.").assertIsDisplayed()
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
