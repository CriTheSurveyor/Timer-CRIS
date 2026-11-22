package com.cri.tabata

import org.json.JSONObject
import java.net.URL

data class Update(val build: Int, val apkUrl: String)

/** The latest GitHub release if it is newer than [current]; null when up to date or offline. */
fun latestUpdate(current: Int): Update? = runCatching {
    val conn = URL("https://api.github.com/repos/CriTheSurveyor/Timer-CRIS/releases/latest").openConnection()
    conn.connectTimeout = 5000
    conn.readTimeout = 5000
    val o = JSONObject(conn.getInputStream().bufferedReader().use { it.readText() })
    val build = o.getString("tag_name").removePrefix("build-").toInt()
    val assets = o.getJSONArray("assets")
    val apk = (0 until assets.length()).map { assets.getJSONObject(it).getString("browser_download_url") }
        .first { it.endsWith(".apk") }
    Update(build, apk).takeIf { build > current }
}.getOrNull()
