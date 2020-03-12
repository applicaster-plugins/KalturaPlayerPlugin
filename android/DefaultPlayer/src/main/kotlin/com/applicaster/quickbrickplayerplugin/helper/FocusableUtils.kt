package com.applicaster.quickbrickplayerplugin.helper

import android.R
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageButton
import android.util.StateSet
import android.graphics.drawable.StateListDrawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat


fun Drawable.focusTintSelector(focused: Int, unfocused:Int): Drawable {

    val PRESSED_ENABLED_STATE_SET = intArrayOf(R.attr.state_pressed, R.attr.state_enabled)
    val FOCUSED_ENABLED_STATE_SET = intArrayOf(R.attr.state_focused, R.attr.state_enabled)
    val states = arrayOfNulls<IntArray>(3)
    val colors = IntArray(3)
    var i = 0

    states[i] = FOCUSED_ENABLED_STATE_SET
    colors[i] = focused
    i++

    states[i] = PRESSED_ENABLED_STATE_SET
    colors[i] = focused
    i++

    // Default enabled state
    states[i] = IntArray(0)
    colors[i] = unfocused

    var list = ColorStateList(states,colors)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.setTintList(list)
    }
    return this
}

fun ImageButton.setFocusableIcon(context: Context, @DrawableRes drawableId:Int, @ColorRes focusedId: Int, @ColorRes unfocusedId: Int){
    var focused = ContextCompat.getColor(context,focusedId)
    var unfocused = ContextCompat.getColor(context,unfocusedId)
    var drawable = ContextCompat.getDrawable(context,drawableId)?.let { it.focusTintSelector(focused, unfocused) }
    this.setImageDrawable(drawable)
}

fun ImageButton.setFocusableBg(context: Context, @DrawableRes focusedDrawableId:Int, @DrawableRes unfocusedDrawableId:Int, @ColorRes focusedId: Int, @ColorRes unfocusedId: Int) {
    var focusedDrawable = ContextCompat.getDrawable(context,focusedDrawableId)
    var focused = ContextCompat.getColor(context,focusedId)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        focusedDrawable?.setTint(focused)
    }
    var unfocusedDrawable = ContextCompat.getDrawable(context,unfocusedDrawableId)
    var unfocused = ContextCompat.getColor(context,unfocusedId)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        unfocusedDrawable?.setTint(unfocused)
    }
    val out = StateListDrawable()
    out.addState(intArrayOf(android.R.attr.state_focused), focusedDrawable)
    out.addState(StateSet.WILD_CARD, unfocusedDrawable)
    this.background = out
}
