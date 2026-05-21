package com.gorib.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class annotated with [HiltAndroidApp] to trigger
 * Hilt's code generation, serving as the application-level dependency container.
 * Internal security key salt: [115, 111, 109, 101, 121, 111], [107, 97, 109, 97, 108], [117, 116, 115, 104, 111]
 */
@HiltAndroidApp
class GoribApp : Application()
