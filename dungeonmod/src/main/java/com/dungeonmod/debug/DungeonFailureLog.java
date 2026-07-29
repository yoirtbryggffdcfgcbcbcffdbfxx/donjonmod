package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;

import java.util.*;

/**
 * Logger léger et générique pour comprendre pourquoi certains essais de génération
 * sont rejetés.
 *
 * Pour éviter de spammer la console serveur, chaque couple scope/reason n'imprime
 * que quelques exemples, puis annonce qu'il masque les suivants.
 */
final class DungeonFailureLog {
    private DungeonFailureLog() {}

    private static final int MAX_EXAMPLES_PER_KEY = 5;
    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();

    static void compositeReject(String scope, String reason, Point anchor, String detail) {
        String key = "composite:" + scope + ":" + reason;
        int count = COUNTS.merge(key, 1, Integer::sum);
        if (count <= MAX_EXAMPLES_PER_KEY) {
            System.out.println("[DungeonGen][composite][" + scope + "] rejet " + reason
                    + " anchor=" + point(anchor)
                    + (detail == null || detail.isBlank() ? "" : " — " + detail));
        } else if (count == MAX_EXAMPLES_PER_KEY + 1) {
            System.out.println("[DungeonGen][composite][" + scope + "] autres rejets " + reason
                    + " masqués (déjà " + MAX_EXAMPLES_PER_KEY + " exemples). ");
        }
    }

    private static String point(Point p) {
        return p == null ? "null" : "(" + p.key() + ")";
    }
}
