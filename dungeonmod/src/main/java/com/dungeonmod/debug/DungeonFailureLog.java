package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;

import java.util.*;

/**
 * Logger léger et générique pour comprendre pourquoi certains essais de génération
 * sont rejetés.
 *
 * Par défaut, il agrège les rejets et imprime seulement un résumé compact quand
 * {@link #printSummary(String)} est appelé. Pour revoir les lignes détaillées en
 * direct : lancer Java avec -Ddungeon.debug.failures.verbose=true.
 */
final class DungeonFailureLog {
    private DungeonFailureLog() {}

    private static final boolean VERBOSE = Boolean.getBoolean("dungeon.debug.failures.verbose");
    private static final int MAX_VERBOSE_EXAMPLES_PER_KEY = 3;
    private static final Map<String, Integer> COUNTS = new LinkedHashMap<>();
    private static final Map<String, String> FIRST_DETAIL = new LinkedHashMap<>();
    private static final Map<String, Integer> CELL_COUNTS = new LinkedHashMap<>();
    private static final Map<String, String> CELL_FIRST_ANCHOR = new LinkedHashMap<>();

    static void reset() {
        COUNTS.clear();
        FIRST_DETAIL.clear();
        CELL_COUNTS.clear();
        CELL_FIRST_ANCHOR.clear();
    }

    static void compositeReject(String scope, String reason, Point anchor, String detail) {
        String key = scope + "|" + reason;
        int count = COUNTS.merge(key, 1, Integer::sum);
        FIRST_DETAIL.putIfAbsent(key, "anchor=" + point(anchor)
                + (detail == null || detail.isBlank() ? "" : " — " + detail));

        String cellKey = extractCellKey(detail);
        if (cellKey != null) {
            CELL_COUNTS.merge(scope + "|" + cellKey, 1, Integer::sum);
            CELL_FIRST_ANCHOR.putIfAbsent(scope + "|" + cellKey, "anchor=" + point(anchor));
        }

        if (VERBOSE && count <= MAX_VERBOSE_EXAMPLES_PER_KEY) {
            System.out.println("[DungeonGen][composite][" + scope + "] rejet " + reason
                    + " " + FIRST_DETAIL.get(key));
        } else if (VERBOSE && count == MAX_VERBOSE_EXAMPLES_PER_KEY + 1) {
            System.out.println("[DungeonGen][composite][" + scope + "] autres rejets " + reason
                    + " masqués (déjà " + MAX_VERBOSE_EXAMPLES_PER_KEY + " exemples). ");
        }
    }

    private static String extractCellKey(String detail) {
        if (detail == null) return null;
        int localIdx = detail.indexOf("local=");
        if (localIdx < 0) return null;
        int end = detail.indexOf(' ', localIdx);
        if (end < 0) end = detail.length();
        String local = detail.substring(localIdx + 6, end);
        return local.replace("]", "").replace("[", "").replace(",", ";");
    }

    static void printSummary(String title) {
        if (COUNTS.isEmpty()) {
            System.out.println("[DungeonGen][composite] " + title + " : aucun rejet composite.");
            return;
        }
        int total = COUNTS.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("[DungeonGen][composite] " + title + " : " + total + " rejet(s) composite(s)");
        COUNTS.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> {
                    String[] parts = e.getKey().split("\\|", 2);
                    String scope = parts.length > 0 ? parts[0] : e.getKey();
                    String reason = parts.length > 1 ? parts[1] : "?";
                    System.out.println("  - " + scope + " / " + reason + " : " + e.getValue()
                            + " (ex: " + FIRST_DETAIL.getOrDefault(e.getKey(), "-") + ")");
                });
        System.out.println("[DungeonGen][composite] --- Top cellules bloquantes ---");
        CELL_COUNTS.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(15)
                .forEach(e -> {
                    String[] parts = e.getKey().split("\\|", 2);
                    String scope = parts.length > 0 ? parts[0] : e.getKey();
                    String cell = parts.length > 1 ? parts[1] : "?";
                    System.out.println("  - " + scope + " / cellule " + cell + " : " + e.getValue()
                            + " (ex: " + CELL_FIRST_ANCHOR.getOrDefault(e.getKey(), "-") + ")");
                });
    }

    private static String point(Point p) {
        return p == null ? "null" : "(" + p.key() + ")";
    }
}
