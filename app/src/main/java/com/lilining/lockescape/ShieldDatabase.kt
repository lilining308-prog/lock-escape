/*
 * Copyright (C) 2026 liyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.lilining.lockescape

data class ProtectedTargetEntity(
    val packageName: String,
    val kind: String,
    val label: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ShieldEventEntity(
    val id: Long = 0,
    val packageName: String,
    val label: String,
    val source: String,
    val action: String,
    val result: String,
    val occurredAt: Long = System.currentTimeMillis()
)
