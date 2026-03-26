package com.atelbay.money_manager.core.parser

import javax.inject.Inject

class RegexValidator @Inject constructor() {

    /**
     * Heuristic check for ReDoS-vulnerable patterns.
     * Returns `true` if the pattern appears safe, `false` if it matches
     * known dangerous constructs (nested quantifiers, overlapping alternations,
     * adjacent overlapping quantified groups).
     */
    fun isReDoSSafe(pattern: String): Boolean = getReDoSViolation(pattern) == null

    /**
     * Returns a human-readable description of the ReDoS vulnerability found
     * in [pattern], or `null` if the pattern appears safe.
     */
    fun getReDoSViolation(pattern: String): String? {
        findNestedQuantifier(pattern)?.let { fragment ->
            return "Nested quantifier detected: '$fragment'. A quantified group containing +/* must not itself be followed by a quantifier. Flatten the repetition instead."
        }
        findOverlappingAlternation(pattern)?.let { fragment ->
            return "Overlapping alternation detected: '$fragment'. Both branches match the same input."
        }
        findAdjacentOverlappingGroups(pattern)?.let { fragment ->
            return "Adjacent overlapping groups detected: '$fragment'. Consecutive identical character classes with quantifiers cause catastrophic backtracking."
        }
        return null
    }

    /**
     * Detects nested quantifiers like `(a+)+`, `(a*)*`, `(a+)*`, `(a*)+`.
     * Only flags groups followed by `+`, `*`, or `{` — NOT `?` (0-or-1 is safe).
     * Returns the matched fragment or null.
     */
    private fun findNestedQuantifier(pattern: String): String? {
        // Use [^()]* (not [^)]*) to respect nested groups — only check quantifiers
        // at the top level of each group, not inside nested subgroups.
        val nested = Regex("""\([^()]*[+*][^()]*\)[+*]|\([^()]*[+*][^()]*\)\{""")
        return nested.find(pattern)?.value
    }

    /**
     * Detects overlapping alternations like `(a|a)*` where both branches
     * of an alternation are identical. Returns the matched fragment or null.
     */
    private fun findOverlappingAlternation(pattern: String): String? {
        val alternation = Regex("""\(([^()]+)\|(\1)\)""")
        return alternation.find(pattern)?.value
    }

    /**
     * Detects adjacent overlapping quantified groups like `\d+\d+`, `\w+\w+`,
     * `[a-z]+[a-z]+`. Returns the matched fragment or null.
     */
    private fun findAdjacentOverlappingGroups(pattern: String): String? {
        val shorthand = Regex("""(\\[dDwWsS])[+*](\\[dDwWsS])[+*]""")
        shorthand.find(pattern)?.let { match ->
            if (match.groupValues[1] == match.groupValues[2]) return match.value
        }

        val bracketClass = Regex("""(\[[^\]]+\])[+*](\[[^\]]+\])[+*]""")
        bracketClass.find(pattern)?.let { match ->
            if (match.groupValues[1] == match.groupValues[2]) return match.value
        }

        return null
    }
}
