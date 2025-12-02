package com.bytedance.xhsdemo.utils

import android.content.Context
import android.widget.Toast

// 全局 Toast 工具：避免多次点击产生多个 Toast 排队
object ToastUtils {
    // 记录当前正在显示的 Toast
    private var currentToast: Toast? = null

    fun show(context: Context, message: String) {
        // 若已有 Toast，在展示新 Toast 前取消，避免叠加
        currentToast?.cancel()
        // 使用 ApplicationContext 创建 Toast，避免持有 Activity 引用导致泄漏
        currentToast = Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT)
        currentToast?.show()
    }

    // 主动取消当前 Toast
    fun cancel() {
        currentToast?.cancel()
        currentToast = null
    }
}
