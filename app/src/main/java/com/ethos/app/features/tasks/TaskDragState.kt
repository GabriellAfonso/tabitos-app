package com.ethos.app.features.tasks

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/** UI-only draft order; storage changes only when the user releases the dragged card. */
@Stable
internal class TaskDragState(val list: LazyListState, private val original: List<String>) {
    var order by mutableStateOf(original)
        private set
    var draggedId by mutableStateOf<String?>(null)
        private set
    private var top by mutableFloatStateOf(0f)
    private var height = 0
    private var pointerY = 0f

    fun start(y: Float): Boolean {
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key in original && y >= it.offset && y < it.offset + it.size
        } ?: return false
        draggedId = item.key as String
        top = item.offset.toFloat()
        height = item.size
        pointerY = y
        return true
    }

    fun drag(delta: Float) {
        if (draggedId == null) return
        top += delta
        pointerY += delta
        place()
    }

    private fun place() {
        val id = draggedId ?: return
        val center = top + height / 2f
        val from = order.indexOf(id)
        val target = list.layoutInfo.visibleItemsInfo.firstOrNull {
            val to = order.indexOf(it.key)
            to >= 0 && it.key != id && center >= it.offset && center < it.offset + it.size &&
                ((to > from && center > it.offset + it.size / 2f) ||
                    (to < from && center < it.offset + it.size / 2f))
        } ?: return
        order = order.toMutableList().apply {
            removeAt(from)
            add(order.indexOf(target.key), id)
        }
    }

    suspend fun autoScroll(edge: Float) {
        while (coroutineContext.isActive && draggedId != null) {
            withFrameNanos { }
            val info = list.layoutInfo
            val speed = when {
                pointerY < info.viewportStartOffset + edge ->
                    -((info.viewportStartOffset + edge - pointerY) / edge).coerceIn(0f, 1f) * edge / 5
                pointerY > info.viewportEndOffset - edge ->
                    ((pointerY - info.viewportEndOffset + edge) / edge).coerceIn(0f, 1f) * edge / 5
                else -> 0f
            }
            if (speed != 0f) {
                list.scrollBy(speed)
                place()
            }
        }
    }

    fun finish(commit: (List<String>) -> Unit) {
        val changed = draggedId != null && order != original
        draggedId = null
        if (changed) commit(order) else order = original
    }

    fun cancel() {
        draggedId = null
        order = original
    }

    fun translation(id: String): Float = if (draggedId == id) {
        top - (list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }?.offset ?: top.toInt())
    } else 0f
}

internal fun Modifier.taskDragGestures(
    state: TaskDragState, enabled: Boolean, onStart: () -> Unit, onCommit: (List<String>) -> Unit,
): Modifier = if (!enabled) this else pointerInput(state) {
    detectDragGesturesAfterLongPress(
        onDragStart = { if (state.start(it.y)) onStart() },
        onDragCancel = state::cancel,
        onDragEnd = { state.finish(onCommit) },
        onDrag = { change, amount ->
            if (state.draggedId != null) {
                change.consume()
                state.drag(amount.y)
            }
        },
    )
}

internal fun Modifier.taskDragPlacement(state: TaskDragState, id: String): Modifier =
    zIndex(if (state.draggedId == id) 1f else 0f).graphicsLayer {
        translationY = state.translation(id)
        shadowElevation = if (state.draggedId == id) 8f else 0f
        scaleX = if (state.draggedId == id) 1.02f else 1f
        scaleY = scaleX
    }
