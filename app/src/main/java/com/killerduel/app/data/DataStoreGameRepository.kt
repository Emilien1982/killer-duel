package com.killerduel.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.killerduel.app.core.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "killer_duel")

private val json = Json { ignoreUnknownKeys = true }

/** Au-delà, l'historique ne sert plus l'adversaire par rejeu et alourdit chaque écriture. */
private const val MAX_SESSIONS_PER_DIFFICULTY = 50

private object Keys {
    val STATS = stringPreferencesKey("stats_by_difficulty")
    val DUEL_STATS = stringPreferencesKey("duel_stats")
    val IN_PROGRESS = stringPreferencesKey("in_progress_game")
    val SESSIONS = stringPreferencesKey("training_sessions")
    val SETTINGS = stringPreferencesKey("game_settings")
    val DAILY_WINS = stringPreferencesKey("daily_wins")
}

/**
 * Persistance sur DataStore Preferences : chaque agrégat est stocké comme un
 * unique document JSON, ce qui garde les mises à jour atomiques (un `edit`,
 * une valeur cohérente) sans multiplier les clés.
 *
 * Les Map sont indexées par [Difficulty.name] plutôt que par l'enum : un niveau
 * renommé ou retiré ne rend alors illisible que son entrée, pas tout le document.
 */
class DataStoreGameRepository(private val context: Context) : GameRepository {

    override val stats: Flow<Map<Difficulty, DifficultyStats>> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it.readStats() }

    override val duelStats: Flow<DuelStats> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { decode<DuelStats>(it[Keys.DUEL_STATS]) ?: DuelStats() }

    override val settings: Flow<GameSettings> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { decode<GameSettings>(it[Keys.SETTINGS]) ?: GameSettings() }

    override suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        context.dataStore.edit { prefs ->
            val current = decode<GameSettings>(prefs[Keys.SETTINGS]) ?: GameSettings()
            prefs[Keys.SETTINGS] = json.encodeToString(transform(current))
        }
    }

    override val dailyWins: Flow<Set<String>> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { decode<Set<String>>(it[Keys.DAILY_WINS]).orEmpty() }

    override suspend fun recordDailyWin(date: String) {
        context.dataStore.edit { prefs ->
            val current = decode<Set<String>>(prefs[Keys.DAILY_WINS]).orEmpty()
            prefs[Keys.DAILY_WINS] = json.encodeToString(current + date)
        }
    }

    override suspend fun recordTrainingSession(session: RecordedSession) {
        context.dataStore.edit { prefs ->
            val history = prefs.readHistory()
            val forDifficulty = (history[session.difficulty.name].orEmpty() + session)
                .sortedByDescending { it.completedAtEpochMillis }
                .take(MAX_SESSIONS_PER_DIFFICULTY)
            prefs[Keys.SESSIONS] =
                json.encodeToString(history + (session.difficulty.name to forDifficulty))

            prefs.mutateStats(session.difficulty) { current ->
                current.copy(
                    completed = current.completed + 1,
                    totalMillis = current.totalMillis + session.durationMillis,
                    // bestMillis vaut 0 tant qu'aucune partie n'est terminée : ce zéro
                    // ne doit pas l'emporter dans le minimum.
                    bestMillis = if (current.bestMillis == 0L) session.durationMillis
                    else minOf(current.bestMillis, session.durationMillis)
                )
            }
        }
    }

    override suspend fun recordGameStarted(difficulty: Difficulty) {
        context.dataStore.edit { prefs ->
            prefs.mutateStats(difficulty) { it.copy(played = it.played + 1) }
        }
    }

    override suspend fun recordDuel(won: Boolean) {
        context.dataStore.edit { prefs ->
            val current = decode<DuelStats>(prefs[Keys.DUEL_STATS]) ?: DuelStats()
            val updated = current.copy(
                played = current.played + 1,
                won = current.won + if (won) 1 else 0
            )
            prefs[Keys.DUEL_STATS] = json.encodeToString(updated)
        }
    }

    override suspend fun recentSessions(difficulty: Difficulty, limit: Int): List<RecordedSession> =
        preferences().readHistory()[difficulty.name].orEmpty()
            .sortedByDescending { it.completedAtEpochMillis }
            .take(limit)

    override suspend fun saveInProgress(game: SavedGame?) {
        context.dataStore.edit { prefs ->
            if (game == null) prefs.remove(Keys.IN_PROGRESS)
            else prefs[Keys.IN_PROGRESS] = json.encodeToString(game)
        }
    }

    override suspend fun loadInProgress(): SavedGame? =
        decode<SavedGame>(preferences()[Keys.IN_PROGRESS])

    private suspend fun preferences(): Preferences =
        context.dataStore.data.catch { emit(emptyPreferences()) }.first()
}

/** Complète la Map lue pour que les quatre niveaux soient toujours présents. */
private fun Preferences.readStats(): Map<Difficulty, DifficultyStats> {
    val stored = decode<Map<String, DifficultyStats>>(this[Keys.STATS]).orEmpty()
    return Difficulty.entries.associateWith { stored[it.name] ?: DifficultyStats() }
}

private fun Preferences.readHistory(): Map<String, List<RecordedSession>> =
    decode<Map<String, List<RecordedSession>>>(this[Keys.SESSIONS]).orEmpty()

private fun MutablePreferences.mutateStats(
    difficulty: Difficulty,
    transform: (DifficultyStats) -> DifficultyStats
) {
    val stored = decode<Map<String, DifficultyStats>>(this[Keys.STATS]).orEmpty()
    val updated = transform(stored[difficulty.name] ?: DifficultyStats())
    this[Keys.STATS] = json.encodeToString(stored + (difficulty.name to updated))
}

/** Une valeur corrompue est traitée comme une valeur absente. */
private inline fun <reified T> decode(raw: String?): T? =
    raw?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }
