package com.dailychallenge.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history")
private val KEY_RECORDS = stringPreferencesKey("records")
private val gson = Gson()
private val type = object : TypeToken<List<DayRecord>>() {}.type

class HistoryDataSource(private val context: Context) {

    @Volatile
    private var cachedJson: String? = null
    @Volatile
    private var cachedRecords: List<DayRecord> = emptyList()

    private fun parse(json: String): List<DayRecord> {
        if (json == cachedJson) return cachedRecords
        val result = try {
            (gson.fromJson(json, type) as? List<DayRecord>) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        cachedJson = json
        cachedRecords = result
        return result
    }

    val records: Flow<List<DayRecord>> = context.historyDataStore.data.map { prefs ->
        parse(prefs[KEY_RECORDS] ?: "[]")
    }

    suspend fun addOrUpdateRecord(record: DayRecord) {
        context.historyDataStore.edit { prefs ->
            val list = parse(prefs[KEY_RECORDS] ?: "[]").toMutableList()
            val index = list.indexOfFirst { it.date == record.date }
            if (index >= 0) list[index] = record else list.add(record)
            list.sortBy { it.date }
            val json = gson.toJson(list)
            prefs[KEY_RECORDS] = json
            cachedJson = json
            cachedRecords = list
        }
    }

    suspend fun removeRecord(date: String) {
        context.historyDataStore.edit { prefs ->
            val list = parse(prefs[KEY_RECORDS] ?: "[]").toMutableList()
            list.removeAll { it.date == date }
            val json = gson.toJson(list)
            prefs[KEY_RECORDS] = json
            cachedJson = json
            cachedRecords = list
        }
    }

    suspend fun getRecords(): List<DayRecord> = records.first()

    suspend fun setRecords(list: List<DayRecord>) {
        context.historyDataStore.edit { prefs ->
            val sorted = list.sortedBy { it.date }
            val json = gson.toJson(sorted)
            prefs[KEY_RECORDS] = json
            cachedJson = json
            cachedRecords = sorted
        }
    }
}
