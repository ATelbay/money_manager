package com.atelbay.money_manager.core.parser

import javax.inject.Inject

class RegexValidator @Inject constructor() {

    /**
     * Heuristic check for ReDoS-vulnerable patterns.
     * Returns `true` if the pattern appears safe, `false` if it matches
     * known dangerous constructs (nested quantifiers, overlapping alternations,
     * adjacent overlapping quantified groups).
     */
    fun isReDoSSafe(pattern: String): Boolean {
        return !hasNestedQuantifiers(pattern) &&
            !hasOverlappingAlternations(pattern) &&
            !hasAdjacentOverlappingGroups(pattern)
    }

    /**
     * Detects nested quantifiers like `(a+)+`, `(a*)*`, `(a+)*`, `(a*)+`.
     * A group whose body contains a quantifier, followed by another quantifier.
     */
    private fun hasNestedQuantifiers(pattern: String): Boolean {
        // Match a parenthesized group containing +, *, or {n,m} inside,
        // followed by an outer quantifier (+, *, ?, {).
        val nested = Regex("""\([^)]*[+*][^)]*\)[+*?]|\([^)]*[+*][^)]*\)\{""")
        return nested.containsMatchIn(pattern)
    }

    /**
     * Detects overlapping alternations like `(a|a)*` where both branches
     * of an alternation are identical.
     */
    private fun hasOverlappingAlternations(pattern: String): Boolean {
        val alternation = Regex("""\(([^()]+)\|(\1)\)""")
        return alternation.containsMatchIn(pattern)
    }

    /**
     * Detects adjacent overlapping quantified groups like `\d+\d+`, `\w+\w+`,
     * `[a-z]+[a-z]+` — consecutive identical character classes both with quantifiers.
     */
    private fun hasAdjacentOverlappingGroups(pattern: String): Boolean {
        // Shorthand classes: \d, \w, \s and their uppercase variants
        val shorthand = Regex("""(\\[dDwWsS])[+*](\\[dDwWsS])[+*]""")
        if (shorthand.find(pattern)?.let { match ->
                match.groupValues[1] == match.groupValues[2]
            } == true
        ) return true

        // Bracket character classes: [a-z]+[a-z]+ etc.
        val bracketClass = Regex("""(\[[^\]]+\])[+*](\[[^\]]+\])[+*]""")
        if (bracketClass.find(pattern)?.let { match ->
                match.groupValues[1] == match.groupValues[2]
            } == true
        ) return true

        return false
    }
}
