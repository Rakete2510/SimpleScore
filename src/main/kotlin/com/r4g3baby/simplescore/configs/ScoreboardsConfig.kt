package com.r4g3baby.simplescore.configs

import com.r4g3baby.simplescore.scoreboard.models.*
import com.r4g3baby.simplescore.utils.configs.ConfigFile
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.Plugin

class ScoreboardsConfig(plugin: Plugin) : ConfigFile(plugin, "scoreboards") {
    private val conditionsConfig = ConditionsConfig(plugin)
    val conditions get() = conditionsConfig.conditions
    val scoreboards = HashMap<String, Scoreboard>()

    init {
        for (scoreboard in config.getKeys(false).filter { !scoreboards.containsKey(it.lowercase()) }) {
            if (config.isConfigurationSection(scoreboard)) {
                val scoreboardSec = config.getConfigurationSection(scoreboard)
                val updateTime = scoreboardSec.getInt("updateTime", 20)

                val titles = ScoreFrames()
                when {
                    scoreboardSec.isList("titles") -> {
                        scoreboardSec.getList("titles").forEach { frame ->
                            val parsed = parseFrame(frame, updateTime)?.also { titles.addFrame(it) }
                            if (parsed == null) plugin.logger.warning(
                                "Invalid frame value for titles scoreboard: $scoreboard, value: $frame."
                            )
                        }
                    }
                    scoreboardSec.isString("titles") -> {
                        titles.addFrame(scoreboardSec.getString("titles"), updateTime)
                    }
                    else -> {
                        val titlesValue = scoreboardSec.get("titles")
                        plugin.logger.warning(
                            "Invalid titles value for scoreboard: $scoreboard, value: $titlesValue."
                        )
                    }
                }

                val scores = ArrayList<BoardScore>()
                if (scoreboardSec.isConfigurationSection("scores")) {
                    val scoresSec = scoreboardSec.getConfigurationSection("scores")
                    scoresSec.getKeys(false).mapNotNull { it.toIntOrNull() }.forEach { score ->
                        when {
                            scoresSec.isConfigurationSection(score.toString()) -> {
                                val scoreSec = scoresSec.getConfigurationSection(score.toString())
                                val scoreFrames = ScoreFrames()
                                when {
                                    scoreSec.isList("frames") -> {
                                        scoreSec.getList("frames").forEach { line ->
                                            val parsed = parseFrame(line, updateTime)?.also { scoreFrames.addFrame(it) }
                                            if (parsed == null) plugin.logger.warning(
                                                "Invalid frame value for scoreboard: $scoreboard, score: $score, value: $line."
                                            )
                                        }
                                    }
                                    scoreSec.isString("frames") -> {
                                        scoreFrames.addFrame(scoreSec.getString("frames"), updateTime)
                                    }
                                    else -> {
                                        val framesValue = scoreSec.get("frames")
                                        plugin.logger.warning(
                                            "Invalid frames value for scoreboard: $scoreboard, score: $score, value: $framesValue."
                                        )
                                    }
                                }
                                BoardScore(score, scoreFrames, getConditions(scoreSec))
                            }
                            scoresSec.isList(score.toString()) -> {
                                val scoreFrames = ScoreFrames()
                                scoresSec.getList(score.toString()).forEach { frame ->
                                    val parsed = parseFrame(frame, updateTime)?.also { scoreFrames.addFrame(it) }
                                    if (parsed == null) plugin.logger.warning(
                                        "Invalid frame value for scoreboard: $scoreboard, score: $score, value: $frame."
                                    )
                                }
                                BoardScore(score, scoreFrames)
                            }
                            scoresSec.isString(score.toString()) -> {
                                BoardScore(score, ScoreFrames().apply {
                                    addFrame(scoresSec.getString(score.toString()), updateTime)
                                })
                            }
                            else -> {
                                val scoreValue = scoresSec.get(score.toString())
                                plugin.logger.warning(
                                    "Invalid score value for scoreboard: $scoreboard, score: $score, value: $scoreValue."
                                )
                                null
                            }
                        }?.also { scores.add(it) }
                    }
                }

                scoreboards[scoreboard.lowercase()] = Scoreboard(
                    scoreboard, titles, scores, getConditions(scoreboardSec)
                )
            }
        }
    }

    private fun parseFrame(frame: Any?, updateTime: Int): ScoreFrame? {
        return when (frame) {
            is String -> ScoreFrame(frame, updateTime)
            is Map<*, *> -> ScoreFrame(frame["text"] as String, frame.getOrDefault("time", updateTime) as Int)
            else -> null
        }
    }

    private fun getConditions(section: ConfigurationSection): List<Condition> {
        return section.getStringList("conditions").mapNotNull { conditions[it.lowercase()] }
    }
}