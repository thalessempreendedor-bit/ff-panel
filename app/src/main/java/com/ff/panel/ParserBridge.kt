package com.ff.panel

object ParserBridge {
    @Volatile var onUpdate: (() -> Unit)? = null
    fun notifyUpdate() { onUpdate?.invoke() }
}
