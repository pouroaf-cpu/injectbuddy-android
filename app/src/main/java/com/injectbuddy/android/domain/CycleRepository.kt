package com.injectbuddy.android.domain

import com.injectbuddy.android.data.model.Cycle

/**
 * Cycle-planner data: the user's cycles with their items grouped (mirrors
 * Injectbuddy/lib/cycles.ts). Active cycles sort first, then newest.
 */
interface CycleRepository {
    suspend fun listCycles(): Result<List<Cycle>>
}
