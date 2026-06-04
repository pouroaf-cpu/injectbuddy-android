package com.injectbuddy.android.domain

import com.injectbuddy.android.data.model.SavedDosage
import com.injectbuddy.android.data.model.UserProfile
import kotlinx.serialization.json.JsonObject

/**
 * Profile + saved-dosage access. Backed by Supabase Postgrest (RLS scopes every row to
 * the signed-in user), mirroring the web app's `/api/dosages` + `saved_dosages` table.
 */
interface AccountRepository {
    suspend fun getProfile(): Result<UserProfile>

    suspend fun listSavedDosages(calculatorType: String? = null): Result<List<SavedDosage>>

    /** Insert a dosage; returns the new row id. Mirrors POST /api/dosages. */
    suspend fun saveDosage(calculatorType: String, label: String?, config: JsonObject): Result<String>

    suspend fun setDosageActive(id: String, active: Boolean): Result<Unit>

    suspend fun deleteDosage(id: String): Result<Unit>
}
