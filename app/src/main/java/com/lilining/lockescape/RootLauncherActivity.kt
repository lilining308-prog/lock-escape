/*
 * Copyright (C) 2026 liyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.lilining.lockescape

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class RootLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.saveExecutionMode(this, RootLauncherDefaults.executionMode)
        startActivity(Intent(this, EscapeActivity::class.java))
        finish()
    }
}
