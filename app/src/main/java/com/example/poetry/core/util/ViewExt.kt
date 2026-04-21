package com.example.poetry.core.util

import android.content.Context
import android.util.TypedValue

fun Context.dp(value: Int): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()
