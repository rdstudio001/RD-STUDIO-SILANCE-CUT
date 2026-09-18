package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Preset
import com.example.model.SilenceAction

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thresholdDb: Float,
    val minSilenceDurationSec: Float,
    val maxSilenceDurationSec: Float?,
    val actionName: String,
    val remainingSilenceSec: Float,
    val isBuiltIn: Boolean
) {
    fun toDomain(): Preset {
        val action = try {
            SilenceAction.valueOf(actionName)
        } catch (e: Exception) {
            SilenceAction.TRUNCATE
        }
        return Preset(
            id = id,
            name = name,
            thresholdDb = thresholdDb,
            minSilenceDurationSec = minSilenceDurationSec,
            maxSilenceDurationSec = maxSilenceDurationSec,
            action = action,
            remainingSilenceSec = remainingSilenceSec,
            isBuiltIn = isBuiltIn
        )
    }

    companion object {
        fun fromDomain(preset: Preset): PresetEntity {
            return PresetEntity(
                id = preset.id,
                name = preset.name,
                thresholdDb = preset.thresholdDb,
                minSilenceDurationSec = preset.minSilenceDurationSec,
                maxSilenceDurationSec = preset.maxSilenceDurationSec,
                actionName = preset.action.name,
                remainingSilenceSec = preset.remainingSilenceSec,
                isBuiltIn = preset.isBuiltIn
            )
        }
    }
}
