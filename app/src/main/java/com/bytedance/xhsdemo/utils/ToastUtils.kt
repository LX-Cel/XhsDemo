package com.bytedance.xhsdemo.utils

import android.content.Context
import android.widget.Toast

object ToastUtils {
    private var currentToast: Toast? = null

    fun show(context: Context, message: String) {
        // Cancel the existing toast if any
        currentToast?.cancel()
        // Create and show the new toast
        currentToast = Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT)
        currentToast?.show()
    }

    fun cancel() {
        currentToast?.cancel()
        currentToast = null
    }
}
