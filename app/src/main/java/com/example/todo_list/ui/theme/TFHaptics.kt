package com.example.todo_list.ui.theme

import android.content.Context
import com.example.todo_list.utils.HapticManager
import com.example.todo_list.utils.HapticType

/**
 * TaskFlow Semantic Haptic Feedback (Apple iOS HIG Specification).
 * Reference: TASKFLOW_DESIGN_SYSTEM.md Section 8
 */
object TFHaptics {
    /**
     * Selection: Segmented change, picker tick, drag snap, chip select.
     */
    fun selection(context: Context?) {
        HapticManager.perform(context, HapticType.TICK)
    }

    /**
     * Light: Button press, tab change, sheet detent snap.
     */
    fun light(context: Context?) {
        HapticManager.perform(context, HapticType.CLICK)
    }

    /**
     * Medium: Task complete, toggle, long-press lift, swipe threshold.
     */
    fun medium(context: Context?) {
        HapticManager.perform(context, HapticType.SUCCESS)
    }

    /**
     * Strong: Destructive confirmation, wrong PIN, medal unlock.
     */
    fun strong(context: Context?) {
        HapticManager.perform(context, HapticType.ERROR)
    }
}
