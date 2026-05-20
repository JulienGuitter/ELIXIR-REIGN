package com.mjm.elixir_reign.shared.logic

enum class EntityType {
    BARBARIAN,
    ARCHER,
    GIANT,
    BARRACKS,
    ELEXIR_PUMP,
    DARCKELEXIR_PUMP,
    GOLD_MINE,
    ARCHER_TOWER,
    TOWN_HALL
}

enum class BuildingState {
    IDLE,
    TRAINING_UNIT,
    MINING,
    DESTROYED
}

enum class DirectionType {
    UP_LEFT,
    UP,
    UP_RIGHT,
    RIGHT,
    DOWN_RIGHT,
    DOWN,
    DOWN_LEFT,
    LEFT
}

enum class ActionType {
    RUN,
    ATTACK
}
