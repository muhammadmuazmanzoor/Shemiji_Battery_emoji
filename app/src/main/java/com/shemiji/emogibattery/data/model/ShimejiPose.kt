package com.shemiji.emogibattery.data.model

data class ShimejiPose(
    val id: String,
    val name: String,
    val row: Int,
    val column: Int,
)

val ShimejiPoses = listOf(
    ShimejiPose("idle", "Idle", row = 0, column = 0),
    ShimejiPose("walking", "Walking", row = 1, column = 0),
    ShimejiPose("running", "Running", row = 2, column = 0),
    ShimejiPose("jumping", "Jumping", row = 3, column = 0),
    ShimejiPose("airborne", "Airborne", row = 4, column = 0),
    ShimejiPose("climbing", "Climbing", row = 5, column = 0),
    ShimejiPose("ceiling_walk", "Ceiling Walk", row = 6, column = 0),
    ShimejiPose("falling", "Falling", row = 7, column = 0),
    ShimejiPose("bouncing", "Bouncing", row = 7, column = 2),
)
