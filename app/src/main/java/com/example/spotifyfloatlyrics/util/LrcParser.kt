package com.example.spotifyfloatlyrics.util

import com.example.spotifyfloatlyrics.model.TimedLine

object LrcParser {
    private val regex = Regex("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{1,3}))?]\\s*(.*)")

    fun parse(lrc: String): List<TimedLine> {
        return lrc.lines().mapNotNull { line ->
            val match = regex.matchEntire(line.trim()) ?: return@mapNotNull null
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val fracRaw = match.groupValues[3]
            val frac = when (fracRaw.length) {
                1 -> fracRaw.toLong() * 100
                2 -> fracRaw.toLong() * 10
                3 -> fracRaw.toLong()
                else -> 0L
            }
            val text = match.groupValues[4].ifBlank { "..." }
            TimedLine(timeMs = min * 60_000 + sec * 1000 + frac, text = text)
        }.sortedBy { it.timeMs }
    }
}
