package com.amowpuzzle.aitricker.domain.model

enum class MovementRule {
    /** Original levels: the corridor in front of the arrowhead must be empty. */
    HEAD_CORRIDOR,

    /** Every segment must be able to translate in the exit direction without contact. */
    SWEPT_TRANSLATION
}
