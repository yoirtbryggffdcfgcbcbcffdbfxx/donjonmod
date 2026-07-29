package com.dungeonmod.debug;

import java.util.*;

import com.dungeonmod.debug.DungeonAlgo.DungeonResult;
import com.dungeonmod.debug.DungeonAlgo.Point;

/**
 * HARNAIS DE RÉGRESSION PAR SEEDS — qualité automatique de l'algo de génération.
 *
 * Pour chaque seed testée :
 *   1. génération non nulle
 *   2. cohérence labels <-> adjacence (DungeonAlgo.validateStructure) sur les 2 étages
 *   3. connexité des 2 étages (BFS : tout nœud étiqueté doit être atteignable)
 *   4. garanties gameplay (Prison, loot, Ogre, Centrale, PorteGob, MarchandNoir, PuitDJ...)
 *
 * Usage : java com.dungeonmod.debug.SeedHarness [-n count] [-s startSeed] [-v]
 *   -n : nombre de seeds dans la plage séquentielle (défaut 100)
 *   -s : première seed de la plage (défaut 1)
 *   -v : affiche chaque seed testée
 * Les seeds dorées (régressions historiques) sont TOUJOURS testées en plus.
 * Code de sortie : 0 si tout passe, 1 sinon (chainable en CI).
 *
 * NOTE : classe pure — aucune dépendance Minecraft (comme DungeonAlgo/DungeonViz).
 */
public class SeedHarness {

    /**
     * Seeds dorées : chaque bug identifié devient un cas de régression PERMANENT.
     * Ajouter ici la seed de tout futur bug d'algo découvert.
     */
    private static final long[] GOLDEN_SEEDS = {
        224237267600147L,   // couloir à 3-4 connexions (raccords P2/P3 partagés)
        827324799543570426L,// virage IJ2 à 3 connexions au sud de la Centrale (P4)
        227471353010315L    // virage IJ2 à 4 connexions au sud (fusion des 2 arbres sud adjacents)
    };

    public static void main(String[] args) {
        int count = 100;
        long startSeed = 1;
        boolean verbose = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-n" -> count = Integer.parseInt(args[++i]);
                case "-s" -> startSeed = Long.parseLong(args[++i]);
                case "-v" -> verbose = true;
                default -> System.out.println("Argument ignoré : " + args[i]);
            }
        }

        List<String> failures = new ArrayList<>();
        int total = 0;
        long t0 = System.currentTimeMillis();

        // 1. Seeds dorées (régressions historiques) — toujours testées
        System.out.println("== Seeds dorées (régressions historiques) ==");
        for (long seed : GOLDEN_SEEDS) {
            total++;
            failures.addAll(testSeed(seed, verbose));
        }

        // 2. Plage séquentielle
        System.out.println("== Plage : " + startSeed + " .. " + (startSeed + count - 1) + " (-n " + count + ") ==");
        for (int i = 0; i < count; i++) {
            long seed = startSeed + i;
            total++;
            failures.addAll(testSeed(seed, verbose));
            if ((i + 1) % 25 == 0) {
                long dt = System.currentTimeMillis() - t0;
                System.out.println("  ... " + (i + 1) + "/" + count + " seeds, " + failures.size() + " probleme(s), " + dt + " ms");
            }
        }

        System.out.println();
        System.out.println("=== RÉSUMÉ ===");
        long dtTotal = System.currentTimeMillis() - t0;
        if (failures.isEmpty()) {
            System.out.println("SUCCESS : " + total + "/" + total + " seeds OK, aucun probleme, en " + dtTotal + " ms.");
            System.exit(0);
        }

        int genNull = 0;
        for (String f : failures) if (f.contains("GENERATION NULLE")) genNull++;
        System.out.println("FAILURE : " + failures.size() + " probleme(s) sur " + total + " seeds"
                + " (" + genNull + " generations nulles), en " + dtTotal + " ms :");
        int shown = 0;
        for (String f : failures) {
            System.out.println("  - " + f);
            if (++shown >= 40) {
                System.out.println("  ... et " + (failures.size() - shown) + " autre(s) probleme(s)");
                break;
            }
        }
        System.exit(1);
    }

    /** Teste UNE seed et retourne la liste des problèmes trouvés (vide = OK). */
    private static List<String> testSeed(long seed, boolean verbose) {
        List<String> problems = new ArrayList<>();
        DungeonResult dr = DungeonAlgo.generateDungeon(seed);
        if (dr == null) {
            problems.add("seed " + seed + " : GENERATION NULLE");
            return problems;
        }

        // 1. Cohérence labels <-> adjacence (les deux étages)
        problems.addAll(prefix(seed, DungeonAlgo.validateStructure(dr.labels, dr.adj, "ETAGE 0")));
        if (dr.topLabels != null && dr.p4Adj != null) {
            problems.addAll(prefix(seed, DungeonAlgo.validateStructure(dr.topLabels, dr.p4Adj, "ETAGE 1")));
        } else {
            problems.add("seed " + seed + " : P4 absente (topLabels ou p4Adj null)");
        }

        // 2. Connexité des deux étages
        checkConnectivity(problems, dr.labels, dr.adj, "ETAGE 0", seed);
        checkConnectivity(problems, dr.topLabels, dr.p4Adj, "ETAGE 1", seed);

        // 3. Garanties gameplay
        checkGuarantees(problems, dr, seed);

        if (verbose) {
            if (problems.isEmpty()) {
                System.out.println("seed " + seed + " : OK");
            } else {
                for (String p : problems) System.out.println("  " + p);
            }
        }
        return problems;
    }

    private static List<String> prefix(long seed, List<String> problems) {
        List<String> out = new ArrayList<>();
        for (String p : problems) out.add("seed " + seed + " : " + p);
        return out;
    }

    /** BFS depuis un nœud quelconque : tout nœud ÉTIQUETÉ doit être atteignable. */
    private static void checkConnectivity(List<String> problems, Map<Point, String> labels,
                                          Map<Point, Set<Point>> adj, String scope, long seed) {
        if (labels == null || labels.isEmpty() || adj == null) return;
        Point start = labels.keySet().iterator().next();
        Set<Point> visited = new HashSet<>();
        Deque<Point> stack = new ArrayDeque<>();
        stack.push(start);
        visited.add(start);
        while (!stack.isEmpty()) {
            Point p = stack.pop();
            for (Point nb : adj.getOrDefault(p, Collections.emptySet())) {
                if (visited.add(nb)) stack.push(nb);
            }
        }
        List<String> unreachable = new ArrayList<>();
        for (Point p : labels.keySet()) {
            if (!visited.contains(p)) unreachable.add("(" + p.key() + ")=" + labels.get(p));
        }
        if (!unreachable.isEmpty()) {
            problems.add("seed " + seed + " : " + scope + " INCONNEXE, " + unreachable.size()
                    + " noeud(s) inatteignable(s) : ["
                    + String.join(", ", unreachable.subList(0, Math.min(4, unreachable.size())))
                    + (unreachable.size() > 4 ? ", ..." : "") + "]");
        }
    }

    /** Invariants gameplay qui DOIVENT tenir quel que soit le layout généré. */
    private static void checkGuarantees(List<String> problems, DungeonResult dr, long seed) {
        Set<String> l0 = new HashSet<>(dr.labels.values());
        Set<String> l1 = dr.topLabels == null ? Set.of() : new HashSet<>(dr.topLabels.values());

        // Étage 0 (P1-P3)
        if (!l0.contains("Prison")) problems.add("seed " + seed + " : ETAGE 0 sans Prison");
        if (l0.stream().noneMatch(v -> v != null && (v.equals("Loot1") || v.startsWith("Lootdj")))) {
            problems.add("seed " + seed + " : ETAGE 0 sans aucun loot");
        }
        if (!l0.contains("Ogre")) problems.add("seed " + seed + " : ETAGE 0 sans Ogre");
        if (!l0.contains("Centrale")) problems.add("seed " + seed + " : ETAGE 0 sans Centrale");

        // Étage 1 (P4)
        if (l1.isEmpty()) return; // déjà signalé comme P4 absente
        if (!l1.contains("Centrale")) problems.add("seed " + seed + " : ETAGE 1 sans Centrale");
        if (!l1.contains("PorteGob")) problems.add("seed " + seed + " : ETAGE 1 sans PorteGob");
        if (!l1.contains("MarchandNoir")) problems.add("seed " + seed + " : ETAGE 1 sans MarchandNoir");
        if (!l1.contains("PuitDJ")) problems.add("seed " + seed + " : ETAGE 1 sans PuitDJ");
        if (l1.stream().noneMatch(v -> v != null && v.startsWith("Lootdj"))) {
            problems.add("seed " + seed + " : ETAGE 1 sans aucun lootdj");
        }
    }
}
