package com.cri.tabata

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Workout(
    val id: Long = System.currentTimeMillis(),
    val name: String = "Nuovo allenamento",
    val prepare: Int = 10,
    val work: Int = 20,
    val rest: Int = 10,
    val cycles: Int = 8,
    val sets: Int = 1,
    val setRest: Int = 60,
    val cooldown: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name)
        .put("prepare", prepare).put("work", work).put("rest", rest).put("cycles", cycles)
        .put("sets", sets).put("setRest", setRest).put("cooldown", cooldown)

    companion object {
        fun fromJson(o: JSONObject) = Workout(
            o.getLong("id"), o.getString("name"), o.optInt("prepare", 10), o.optInt("work", 20),
            o.optInt("rest", 10), o.optInt("cycles", 8), o.optInt("sets", 1),
            o.optInt("setRest", 60), o.optInt("cooldown", 0),
        )
    }
}

enum class Kind(val label: String) {
    PREPARE("Preparazione"), WORK("Lavoro"), REST("Riposo"), SET_REST("Recupero"), COOLDOWN("Defaticamento")
}

data class Phase(val kind: Kind, val seconds: Int, val cycle: Int, val set: Int)

/** The full timeline: no rest after the last cycle of a set, no recovery after the last set. */
fun Workout.phases(): List<Phase> = buildList {
    if (prepare > 0) add(Phase(Kind.PREPARE, prepare, 0, 1))
    for (s in 1..sets) {
        for (c in 1..cycles) {
            if (work > 0) add(Phase(Kind.WORK, work, c, s))
            if (rest > 0 && c < cycles) add(Phase(Kind.REST, rest, c, s))
        }
        if (setRest > 0 && s < sets) add(Phase(Kind.SET_REST, setRest, cycles, s))
    }
    if (cooldown > 0) add(Phase(Kind.COOLDOWN, cooldown, cycles, sets))
}

fun Workout.totalSeconds() = phases().sumOf { it.seconds }

fun fmt(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("workouts", Context.MODE_PRIVATE)

    fun load(): List<Workout> {
        val raw = prefs.getString("list", null) ?: return listOf(Workout(id = 1, name = "Tabata classico"))
        return try {
            val a = JSONArray(raw)
            List(a.length()) { Workout.fromJson(a.getJSONObject(it)) }
        } catch (e: Exception) {
            // keep the unreadable data instead of overwriting it on the next save
            prefs.edit().putString("list_corrupt_${System.currentTimeMillis()}", raw).apply()
            emptyList()
        }
    }

    fun save(list: List<Workout>) =
        prefs.edit().putString("list", JSONArray(list.map { it.toJson() }).toString()).apply()
}
