package com.takat.finanzas.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per guide the user has finished or dismissed (see ui/tutorial/TutorialCatalog). No row means
 * never seen: that's what makes the welcome guide auto-start on a fresh install.
 */
@Entity(tableName = "tutorial_progress")
data class TutorialProgressEntity(
    @PrimaryKey val tutorialId: String,
    /** Null when the guide was skipped rather than finished. */
    val completedAt: Long? = null,
    val skipped: Boolean = false
)
