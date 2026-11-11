package com.ethos.app.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ethos.app.R
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/** Springs shared by every reorderable list, so neighbours always slide the same way. */
private val slideSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)
private val placementSpring =
    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
private const val LIFTED_SCALE = 1.02f
private const val LIFTED_ELEVATION = 8f

/** Visible grip that starts a drag; 48 dp target, described for TalkBack users who explore by touch. */
@Composable
fun DragHandle(modifier: Modifier) {
    val description = stringResource(R.string.drag_to_reorder)
    Box(modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Text("⠿", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Moves the element at [from] to [to]; used by drag and by the accessibility actions. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * TalkBack alternative to dragging: "Mover para cima/baixo" actions that call [onMove] with the new index
 * (AGENTS: reordering is by drag, with an accessible alternative).
 */
@Composable
fun Modifier.reorderActions(index: Int, lastIndex: Int, enabled: Boolean, onMove: (to: Int) -> Unit): Modifier {
    val up = stringResource(R.string.move_up)
    val down = stringResource(R.string.move_down)
    return semantics {
        customActions = listOfNotNull(
            CustomAccessibilityAction(up) { if (enabled) onMove(index - 1); enabled }.takeIf { index > 0 },
            CustomAccessibilityAction(down) { if (enabled) onMove(index + 1); enabled }.takeIf { index < lastIndex },
        )
    }
}

/**
 * Drag-to-reorder for short lists inside a scrolling [Column] (dialogs, editors). The order changes only on
 * release; while dragging, the item follows the finger and the neighbours it passes slide out of the way.
 * Dragging starts from [content]'s `handle` modifier so text fields inside items keep their own gestures.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    itemKey: (T) -> Any,
    enabled: Boolean,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable (item: T, handle: Modifier) -> Unit,
) {
    val current by rememberUpdatedState(items)
    val change by rememberUpdatedState(onReorder)
    val tops = remember { mutableStateMapOf<Any, Float>() }
    val heights = remember { mutableStateMapOf<Any, Int>() }
    var dragged by remember { mutableStateOf<Any?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val gap = with(LocalDensity.current) { spacing.toPx() }
    fun from() = current.indexOfFirst { itemKey(it) == dragged }
    /** Index where the dragged item's centre would land among the others, from layout before the drag. */
    fun target(): Int {
        val id = dragged ?: return -1
        val center = (tops[id] ?: return -1) + dragY + (heights[id] ?: 0) / 2f
        return current.count { other ->
            val k = itemKey(other)
            k != id && (tops[k] ?: 0f) + (heights[k] ?: 0) / 2f < center
        }
    }
    fun finish() {
        val from = from()
        val to = target()
        dragged = null
        dragY = 0f
        if (from >= 0 && to >= 0 && from != to) change(current.moved(from, to))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.forEachIndexed { index, item ->
            val id = itemKey(item)
            key(id) {
                val lifted = dragged == id
                val from = from()
                val to = if (from >= 0) target() else -1
                val step = (heights[dragged] ?: 0) + gap
                val shift = when {
                    from < 0 || lifted -> 0f
                    index in (from + 1)..to -> -step
                    index in to until from -> step
                    else -> 0f
                }
                val slide = remember { Animatable(0f) }
                // After release the layout already holds the new order, so neighbours snap back to rest.
                LaunchedEffect(shift, dragged == null) {
                    if (dragged == null) slide.snapTo(0f) else slide.animateTo(shift, slideSpring)
                }
                val handle = if (!enabled) Modifier else Modifier.pointerInput(id) {
                    detectDragGestures(
                        onDragStart = { dragged = id; dragY = 0f },
                        onDragEnd = ::finish,
                        onDragCancel = { dragged = null; dragY = 0f },
                        onDrag = { pointer, amount -> pointer.consume(); dragY += amount.y },
                    )
                }
                Box(Modifier
                    .onGloballyPositioned { if (dragged == null) { tops[id] = it.positionInParent().y
                        heights[id] = it.size.height } }
                    .zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (lifted) dragY else slide.value
                        shadowElevation = if (lifted) LIFTED_ELEVATION else 0f
                        scaleX = if (lifted) LIFTED_SCALE else 1f
                        scaleY = scaleX
                    }
                    .reorderActions(index, items.lastIndex, enabled) { change(current.moved(index, it)) }) {
                    content(item, handle)
                }
            }
        }
    }
}

/**
 * Draft order for a [androidx.compose.foundation.lazy.LazyColumn]; storage changes only when the user
 * releases the dragged item. Neighbours animate through [reorderableItem].
 */
@Stable
class LazyReorderState(val list: LazyListState, private val original: List<String>) {
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

    /** Swaps with a neighbour once the dragged centre passes that neighbour's midpoint. */
    private fun place() {
        val id = draggedId ?: return
        val center = top + height / 2f
        val from = order.indexOf(id)
        val target = list.layoutInfo.visibleItemsInfo.firstOrNull {
            val to = order.indexOf(it.key)
            to >= 0 && it.key != id && center >= it.offset && center < it.offset + it.size &&
                ((to > from && center > it.offset + it.size / 2f) || (to < from && center < it.offset + it.size / 2f))
        } ?: return
        order = order.moved(from, order.indexOf(target.key))
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

    internal fun translation(id: String): Float = if (draggedId == id) {
        top - (list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }?.offset ?: top.toInt())
    } else 0f
}

/** Long-press anywhere on the list starts dragging the item under the finger. */
fun Modifier.lazyReorderGestures(
    state: LazyReorderState, enabled: Boolean, onStart: () -> Unit, onCommit: (List<String>) -> Unit,
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

/**
 * Item modifier: the dragged item follows the finger above the others; every other item slides to its new
 * slot with a spring instead of jumping.
 */
fun Modifier.reorderableItem(scope: LazyItemScope, state: LazyReorderState, id: String): Modifier {
    val lifted = state.draggedId == id
    return with(scope) {
        this@reorderableItem.animateItem(fadeInSpec = null, fadeOutSpec = null,
            placementSpec = if (lifted) null else placementSpring)
    }
        .zIndex(if (lifted) 1f else 0f)
        .graphicsLayer {
            translationY = state.translation(id)
            shadowElevation = if (lifted) LIFTED_ELEVATION else 0f
            scaleX = if (lifted) LIFTED_SCALE else 1f
            scaleY = scaleX
        }
}
