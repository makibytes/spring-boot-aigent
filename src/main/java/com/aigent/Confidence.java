package com.aigent;

/**
 * The AI's confidence level in its implementation, used in {@link AiNote}.
 *
 * <ul>
 *   <li>{@link #HIGH}   — all spec cases are clearly covered; human can do a quick review.
 *   <li>{@link #MEDIUM} — minor uncertainty or untested edge cases remain; worth inspecting.
 *   <li>{@link #LOW}    — significant assumptions or guessing involved; human must review carefully.
 * </ul>
 */
public enum Confidence {
    HIGH,
    MEDIUM,
    LOW
}
