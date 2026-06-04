package com.injectbuddy.android.data.repo

import com.injectbuddy.android.data.model.Cycle
import com.injectbuddy.android.data.model.CycleItem
import com.injectbuddy.android.domain.CycleRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/**
 * Supabase-backed cycle loader. Mirrors lib/cycles.ts exactly: two RLS-scoped queries
 * (cycles ordered active-first then newest; items ordered by sort_order then created_at),
 * then group items under their cycle in memory. A single round trip per table beats N+1.
 */
class SupabaseCycleRepository(
    private val supabase: SupabaseClient,
) : CycleRepository {

    override suspend fun listCycles(): Result<List<Cycle>> = runCatching {
        val cycles = supabase.from("cycles")
            .select(Columns.ALL) {
                // Active cycles first, then newest — same two-key ordering as the web.
                order("is_active", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Cycle>()

        val items = supabase.from("cycle_items")
            .select(Columns.ALL) {
                order("sort_order", Order.ASCENDING)
                order("created_at", Order.ASCENDING)
            }
            .decodeList<CycleItem>()

        // Group items by cycle_id, preserving the query order within each group.
        val byCycle: Map<String, List<CycleItem>> = items.groupBy { it.cycleId }

        cycles.map { cycle -> cycle.copy(items = byCycle[cycle.id].orEmpty()) }
    }
}
