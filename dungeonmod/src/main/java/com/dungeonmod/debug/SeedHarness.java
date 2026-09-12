package com.dungeonmod.debug;

import java.util.*;

import com.dungeonmod.debug.DungeonAlgo.DungeonResult;
import com.dungeonmod.debug.DungeonAlgo.Point;

/**
 * HARNAIS DE RÉGRESSION — qualité automatique de l'algo de génération.
 *
 * IMPORTANT — sémantique des seeds :
 *   L'algo ({@link DungeonAlgo#generateDungeon}) rejette en interne les layouts
 *   invalides (retry jusqu'à 100× en mode joueur seed=0, 20× en mode seed fixe).
 *   Seules les seeds qui SORTENT de l'algo (champ {@code DungeonResult.seed} /
 *   {@link DungeonAlgo#getLastSeed()}) sont livrées au joueur.
 *
 *   Tester la plage séquentielle 1..N est donc trompeur : la plupart de ces
 *   valeurs d'entrée sont rejetées ou n'atteignent jamais le joueur. Le mode
 *   par défaut échantillonne donc comme le joueur (seed=0) et valide le
 *   résultat réellement produit.
 *
 * Pour chaque donjon testé :
 *   1. génération non nulle
 *   2. cohérence labels <-> adjacence (DungeonAlgo.validateStructure) sur les 2 étages
 *   3. connexité des 2 étages (BFS : tout nœud étiqueté doit être atteignable)
 *   4. garanties gameplay (Prison, loot, Ogre, Centrale, PorteGob, MarchandNoir, PuitDJ...)
 *
 * Usage : java com.dungeonmod.debug.SeedHarness [-n count] [-v] [-range start] [-seed S]
 *   -n     : nombre de donjons joueur à échantillonner (défaut 100)
 *   -v     : affiche chaque seed testée
 *   -range : (opt-in debug) teste aussi la plage séquentielle start .. start+n-1
 *            en entrée de generateDungeon — NE reflète PAS ce que le joueur reçoit
 *   -seed  : teste une seed de sortie précise (répétable, ex. seed dorée)
 * Les seeds dorées (régressions historiques = seeds de SORTIE) sont TOUJOURS testées.
 * Code de sortie : 0 si tout passe, 1 sinon (chainable en CI).
 *
 * NOTE : classe pure — aucune dépendance Minecraft (comme DungeonAlgo/DungeonViz).
 */
public class SeedHarness {

    /**
     * Seeds dorées : seeds de SORTIE (dr.seed / getLastSeed()) ayant produit un bug.
     * Chaque bug identifié devient un cas de régression PERMANENT.
     * Ajouter ici la seed de SORTIE de tout futur bug d'algo découvert
     * (pas une valeur d'entrée arbitraire 1..N).
     */
    private static final long[] GOLDEN_SEEDS = {
        224237267600147L,   // couloir à 3-4 connexions (raccords P2/P3 partagés)
        827324799543570426L,// virage IJ2 à 3 connexions au sud de la Centrale (P4)
        227471353010315L    // virage IJ2 à 4 connexions au sud (fusion des 2 arbres sud adjacents)
    };

    /** Même garde que /teste : relance des batches complets si generateDungeon(0) revient null. */
    private static final int MAX_PLAYER_BATCHES = 100;

    private record PlayerGeneration(DungeonResult result, int batches) {}

    public static void main(String[] args) {
        int count = 100;
        boolean verbose = false;
        boolean useRange = false;
        long rangeStart = 1;
        List<Long> extraSeeds = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-n" -> count = Integer.parseInt(args[++i]);
                case "-v" -> verbose = true;
                case "-range" -> {
                    useRange = true;
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        rangeStart = Long.parseLong(args[++i]);
                    }
                }
                case "-s", "-seed" -> extraSeeds.add(Long.parseLong(args[++i]));
                // Compat ancienne CLI : -s <start> sans -range était "plage séquentielle".
                // On l'ignore volontairement ici (remplacé par l'échantillonnage joueur) ;
                // utiliser -range <start> pour l'ancien comportement debug.
                default -> System.out.println("Argument ignoré : " + args[i]);
            }
        }

        List<String> failures = new ArrayList<>();
        int total = 0;
        int genNull = 0;
        long t0 = System.currentTimeMillis();
        DungeonFailureLog.reset();

        // 1. Seeds dorées (régressions historiques = seeds de SORTIE) — toujours testées
        System.out.println("== Seeds dorées (régressions historiques, seeds de SORTIE) ==");
        for (long seed : GOLDEN_SEEDS) {
            total++;
            List<String> probs = testOutputSeed(seed, verbose);
            failures.addAll(probs);
            for (String p : probs) if (p.contains("GENERATION NULLE")) genNull++;
        }

        // 1b. Seeds explicites (-seed)
        if (!extraSeeds.isEmpty()) {
            System.out.println("== Seeds explicites (-seed) ==");
            for (long seed : extraSeeds) {
                total++;
                List<String> probs = testOutputSeed(seed, verbose);
                failures.addAll(probs);
                for (String p : probs) if (p.contains("GENERATION NULLE")) genNull++;
            }
        }

        // 2. Échantillonnage mode JOUEUR (seed=0) — ce que le joueur reçoit réellement
        System.out.println("== Echantillonnage joueur : " + count + " generation(s) (seed=0, comme /teste) ==");
        int playerOk = 0;
        int playerBatchTotal = 0;
        int playerBatchMax = 0;
        int playerBatchRetries = 0;
        for (int i = 0; i < count; i++) {
            total++;
            PlayerGeneration pg = generatePlayerLikeDungeon();
            DungeonResult dr = pg.result();
            playerBatchTotal += pg.batches();
            playerBatchMax = Math.max(playerBatchMax, pg.batches());
            playerBatchRetries += Math.max(0, pg.batches() - 1);
            if (dr == null) {
                genNull++;
                failures.add("joueur #" + (i + 1) + " : GENERATION NULLE (l'algo n'a rien pu livrer au joueur)");
                if (verbose) System.out.println("  joueur #" + (i + 1) + " : GENERATION NULLE");
            } else {
                long outSeed = dr.seed != 0 ? dr.seed : DungeonAlgo.getLastSeed();
                List<String> probs = validateResult(dr, outSeed, "joueur#" + (i + 1) + "/seed " + outSeed);
                failures.addAll(probs);
                if (probs.isEmpty()) playerOk++;
                if (verbose) {
                    if (probs.isEmpty()) {
                        System.out.println("  joueur #" + (i + 1) + " seed " + outSeed
                                + " : OK (batchs=" + pg.batches() + ")");
                    } else {
                        for (String p : probs) System.out.println("  " + p);
                    }
                }
            }
            if ((i + 1) % 25 == 0) {
                long dt = System.currentTimeMillis() - t0;
                double avgBatches = playerBatchTotal / (double) (i + 1);
                System.out.println("  ... " + (i + 1) + "/" + count + " joueur, "
                        + playerOk + " OK, " + failures.size() + " probleme(s), batch moy="
                        + String.format(java.util.Locale.ROOT, "%.2f", avgBatches)
                        + ", batch max=" + playerBatchMax + ", " + dt + " ms");
            }
        }

        if (!AGG_FAILS.isEmpty()) {
            java.util.List<java.util.Map.Entry<String, Integer>> top = new java.util.ArrayList<>(AGG_FAILS.entrySet());
            top.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            int tot = top.stream().mapToInt(java.util.Map.Entry::getValue).sum();
            StringBuilder sb = new StringBuilder();
            for (var e : top) sb.append(e.getKey()).append('=').append(e.getValue())
                    .append(String.format(java.util.Locale.ROOT, " (%.1f%%)", 100.0 * e.getValue() / tot)).append(", ");
            System.out.println("[Harnais] pertes par tentative (" + tot + ") : " + sb);
        }

        // 3. Plage séquentielle (opt-in debug uniquement — ne reflète PAS le joueur)
        if (useRange) {
            System.out.println("== DEBUG plage entree : " + rangeStart + " .. "
                    + (rangeStart + count - 1) + " (NE sont PAS des seeds joueur) ==");
            for (int i = 0; i < count; i++) {
                long seed = rangeStart + i;
                total++;
                List<String> probs = testInputSeed(seed, verbose);
                failures.addAll(probs);
                for (String p : probs) if (p.contains("GENERATION NULLE")) genNull++;
            }
        }

        System.out.println();
        if (count > 0) {
            double avgBatches = playerBatchTotal / (double) count;
            System.out.println("=== BATCHS JOUEUR (/teste) ===");
            System.out.println("batch moyen=" + String.format(java.util.Locale.ROOT, "%.2f", avgBatches)
                    + ", batch max=" + playerBatchMax
                    + ", retries batch=" + playerBatchRetries
                    + " sur " + count + " echantillon(s)");
        }
        DungeonFailureLog.printSummary("résumé test_algo");

        System.out.println();
        System.out.println("=== RÉSUMÉ ===");
        long dtTotal = System.currentTimeMillis() - t0;
        if (failures.isEmpty()) {
            System.out.println("SUCCESS : " + total + "/" + total + " tests OK"
                    + " (" + playerOk + "/" + count + " echantillons joueur),"
                    + " aucun probleme, en " + dtTotal + " ms.");
            System.exit(0);
        }

        System.out.println("FAILURE : " + failures.size() + " probleme(s) sur " + total + " tests"
                + " (" + genNull + " generations nulles, " + playerOk + "/" + count
                + " echantillons joueur OK), en " + dtTotal + " ms :");
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

    /** Agrégat des causes de rejet par tentative, sur tout l'échantillon joueur. */
    private static final java.util.Map<String, Integer> AGG_FAILS = new java.util.LinkedHashMap<>();

    private static PlayerGeneration generatePlayerLikeDungeon() {
        for (int batch = 1; batch <= MAX_PLAYER_BATCHES; batch++) {
            DungeonResult dr = DungeonAlgo.generateDungeon(0);
            DungeonAlgo.FAIL_STAGES.forEach((k, v) -> AGG_FAILS.merge(k, v, Integer::sum));
            if (dr != null) return new PlayerGeneration(dr, batch);
        }
        return new PlayerGeneration(null, MAX_PLAYER_BATCHES);
    }

    /**
     * Teste une seed de SORTIE : on force generateDungeon(seed) et on valide
     * le layout produit (reproductibilité d'un bug joueur).
     */
    private static List<String> testOutputSeed(long seed, boolean verbose) {
        DungeonAlgo.resetFailStages();
        DungeonResult dr = DungeonAlgo.generateDungeon(seed);
        if (dr == null) {
            List<String> problems = new ArrayList<>();
            String stages = DungeonAlgo.FAIL_STAGES.toString();
            problems.add("seed " + seed + " : GENERATION NULLE " + stages);
            if (verbose) System.out.println("  seed " + seed + " : GENERATION NULLE " + stages);
            return problems;
        }
        // La seed réellement utilisée peut être seed+outer (retry interne).
        // On valide le résultat tel quel et on affiche les deux si différentes.
        long outSeed = dr.seed != 0 ? dr.seed : seed;
        String tag = (outSeed == seed) ? ("seed " + seed) : ("seed " + seed + "→" + outSeed);
        List<String> problems = validateResult(dr, outSeed, tag);
        if (verbose) {
            if (problems.isEmpty()) System.out.println("  " + tag + " : OK");
            else for (String p : problems) System.out.println("  " + p);
        }
        return problems;
    }

    /**
     * Ancien comportement : seed d'ENTRÉE séquentielle. Conservé uniquement
     * pour -range (debug). Beaucoup de null / layouts non-joueur sont attendus.
     */
    private static List<String> testInputSeed(long seed, boolean verbose) {
        return testOutputSeed(seed, verbose);
    }

    /** Valide un DungeonResult déjà généré (chemin joueur ou seed forcée). */
    private static List<String> validateResult(DungeonResult dr, long seed, String tag) {
        List<String> problems = new ArrayList<>();

        // 1. Cohérence labels <-> adjacence, PAR COUCHE (extraite du graphe 3D unifié)
        problems.addAll(prefix(tag, DungeonAlgo.validateStructure(dr.labelsAt(0), dr.adjAt(0), "ETAGE 0")));
        if (dr.topLabels != null && dr.p4Adj != null) {
            problems.addAll(prefix(tag, DungeonAlgo.validateStructure(dr.labelsAt(1), dr.adjAt(1), "ETAGE 1")));
        } else {
            problems.add(tag + " : P4 absente (topLabels ou p4Adj null)");
        }

        // 2. Connexité BFS par couche
        checkConnectivity(problems, dr.labelsAt(0), dr.adjAt(0), "ETAGE 0", tag);
        checkConnectivity(problems, dr.labelsAt(1), dr.adjAt(1), "ETAGE 1", tag);
        // 2b. Connexité GLOBALE : les couches sont reliées par l'escalier vertical du hub
        checkConnectivity(problems, dr.unifiedLabels, dr.unifiedAdj, "GRAPHE 3D", tag);
        // 2c. Intégrité structurelle (orthogonalité, symétrie, bornes, labels sans nœud)
        checkGraphIntegrity(problems, dr.labelsAt(0), dr.adjAt(0), "ETAGE 0", tag);
        checkGraphIntegrity(problems, dr.labelsAt(1), dr.adjAt(1), "ETAGE 1", tag);
        checkGraphIntegrity(problems, dr.unifiedLabels, dr.unifiedAdj, "GRAPHE 3D", tag);

        // 3. Garanties gameplay
        checkGuarantees(problems, dr, tag);

        // 4. Règles d'espacement (conversation 3) : monstres entre eux, isolement Ogre,
        //    culs-de-sac génériques jamais après une ligne droite.
        checkSpacingRules(problems, dr.labelsAt(0), dr.adjAt(0), "ETAGE 0", tag, true);
        if (dr.topLabels != null && dr.p4Adj != null) {
            checkSpacingRules(problems, dr.labelsAt(1), dr.adjAt(1), "ETAGE 1", tag, false);
        }

        return problems;
    }

    /**
     * Règles d'espacement (cahier des charges) :
     *  - deux salles monstre (M1-M5, MJ1-MJ5) toujours à distance >= MONSTER_MIN_DIST
     *    (au moins 2 salles neutres entre elles) ;
     *  - toute salle monstre à distance >= OGRE_MIN_MONSTER_DIST de l'Ogre (3 salles min) ;
     *  - un cul-de-sac générique (cul/culDJ/CDG) jamais après une ligne droite : son parent
     *    à 2 voisins doit être un VIRAGE, pas un couloir droit (intersection = toujours OK).
     */
    private static void checkSpacingRules(List<String> problems, Map<Point, RoomType> labels,
                                          Map<Point, Set<Point>> adj, String scope, String tag, boolean checkOgre) {
        if (labels == null || labels.isEmpty() || adj == null) return;

        List<Point> monsters = new ArrayList<>();
        Point ogre = null;
        for (var e : labels.entrySet()) {
            if (e.getValue().isMonster()) monsters.add(e.getKey());
            if (e.getValue() == RoomType.OGRE) ogre = e.getKey();
        }
        for (int i = 0; i < monsters.size(); i++) {
            Map<Point, Integer> d = DungeonAlgo.bfsDistances(adj, monsters.get(i));
            for (int j = i + 1; j < monsters.size(); j++) {
                int dd = d.getOrDefault(monsters.get(j), Integer.MAX_VALUE);
                if (dd < DungeonAlgo.MONSTER_MIN_DIST) {
                    problems.add(tag + " : " + scope + " monstres trop proches (dist " + dd
                            + " < " + DungeonAlgo.MONSTER_MIN_DIST + ") : "
                            + labels.get(monsters.get(i)).id + " @(" + monsters.get(i).key() + ") <-> "
                            + labels.get(monsters.get(j)).id + " @(" + monsters.get(j).key() + ")");
                }
            }
            if (checkOgre && ogre != null) {
                int dd = d.getOrDefault(ogre, Integer.MAX_VALUE);
                if (dd < DungeonAlgo.OGRE_MIN_MONSTER_DIST) {
                    problems.add(tag + " : " + scope + " monstre trop proche de l'Ogre (dist " + dd
                            + " < " + DungeonAlgo.OGRE_MIN_MONSTER_DIST + ") : "
                            + labels.get(monsters.get(i)).id + " @(" + monsters.get(i).key() + ")");
                }
            }
        }

        for (var e : labels.entrySet()) {
            if (!DungeonAlgo.isGenericDeadEnd(e.getValue())) continue;
            Set<Point> nb = adj.getOrDefault(e.getKey(), Set.of());
            if (nb.size() != 1) continue;
            Point parent = nb.iterator().next();
            Set<Point> pAdj = adj.getOrDefault(parent, Set.of());
            if (pAdj.size() == 2 && DungeonAlgo.shapeOf(pAdj) == DungeonAlgo.Shape.STRAIGHT) {
                problems.add(tag + " : " + scope + " cul-de-sac après une ligne droite @("
                        + e.getKey().key() + "), parent @(" + parent.key() + ")=" + labels.get(parent).id);
            }
        }
    }

    private static List<String> prefix(String tag, List<String> problems) {
        List<String> out = new ArrayList<>();
        for (String p : problems) out.add(tag + " : " + p);
        return out;
    }

    /**
     * INTEGRITE STRUCTURELLE du graphe. Detecte le type d'erreur qui n'etait pas
     * couvert avant (mauvais placement de hub) :
     *  - arete NON ORTHOGONALE : distance de Manhattan 3D != 1 (donc diagonale, ou saut de couche) ;
     *  - arete NON SYMETRIQUE, arete NULLE ;
     *  - noeud HORS BORNES ;
     *  - label SANS NOEUD dans l'adjacence.
     */
    private static void checkGraphIntegrity(List<String> problems, Map<Point, RoomType> labels,
                                            Map<Point, Set<Point>> adj, String scope, String tag) {
        if (adj == null) return;
        for (var e : adj.entrySet()) {
            Point a = e.getKey();
            if (a.isOutOfBounds()) problems.add(tag + " : " + scope + " noeud HORS BORNES " + a.key());
            for (Point b : e.getValue()) {
                if (b == null) { problems.add(tag + " : " + scope + " arete NULLE depuis " + a.key()); continue; }
                if (!adj.getOrDefault(b, Collections.emptySet()).contains(a)) {
                    problems.add(tag + " : " + scope + " arete NON SYMETRIQUE " + a.key() + " -> " + b.key());
                }
                int dist = Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y()) + Math.abs(a.level() - b.level());
                if (dist != 1) {
                    problems.add(tag + " : " + scope + " arete NON ORTHOGONALE " + a.key()
                            + " -> " + b.key() + " (dist=" + dist + ")");
                }
            }
        }
        if (labels != null) {
            for (var e : labels.entrySet()) {
                if (!adj.containsKey(e.getKey())) {
                    problems.add(tag + " : " + scope + " label "
                            + (e.getValue() != null ? e.getValue().id : "?") + " SANS NOEUD @ " + e.getKey().key());
                }
            }
        }
    }

    /**
     * BFS depuis un nœud quelconque : tout nœud ÉTIQUETÉ doit être atteignable.
     * Sur l'étage 1, la Centrale est une salle physique 2×2 dont les 4 cellules
     * hub ne sont pas forcément maillées dans p4Adj (chaque sortie s'accroche à
     * une cellule différente). On les traite comme un seul super-nœud pour le BFS,
     * sinon le graphe paraît à tort en plusieurs composantes.
     */
    private static void checkConnectivity(List<String> problems, Map<Point, RoomType> labels,
                                          Map<Point, Set<Point>> adj, String scope, String tag) {
        if (labels == null || labels.isEmpty() || adj == null) return;

        Set<Point> hub = new HashSet<>();
        if ("ETAGE 1".equals(scope)) {
            for (var e : labels.entrySet()) {
                if (e.getValue() == RoomType.CENTRALE) {
                    int hx = e.getKey().x(), hz = e.getKey().y(), lvl = e.getKey().level();
                    for (int dx = 0; dx <= 1; dx++)
                        for (int dz = 0; dz <= 1; dz++)
                            hub.add(new Point(hx + dx, hz + dz, lvl));
                    break;
                }
            }
        }

        Point start = labels.keySet().iterator().next();
        Set<Point> visited = new HashSet<>();
        Deque<Point> stack = new ArrayDeque<>();
        stack.push(start);
        visited.add(start);
        while (!stack.isEmpty()) {
            Point p = stack.pop();
            // Voisins graphiques +, si on est dans le hub 2×2, toutes les cellules hub
            List<Point> expand = new ArrayList<>(adj.getOrDefault(p, Collections.emptySet()));
            if (hub.contains(p)) expand.addAll(hub);
            for (Point nb : expand) {
                if (visited.add(nb)) stack.push(nb);
            }
        }
        List<String> unreachable = new ArrayList<>();
        for (Point p : labels.keySet()) {
            if (!visited.contains(p)) unreachable.add("(" + p.key() + ")=" + labels.get(p));
        }
        if (!unreachable.isEmpty()) {
            problems.add(tag + " : " + scope + " INCONNEXE, " + unreachable.size()
                    + " noeud(s) inatteignable(s) : ["
                    + String.join(", ", unreachable.subList(0, Math.min(4, unreachable.size())))
                    + (unreachable.size() > 4 ? ", ..." : "") + "]");
        }
    }

    /** Invariants gameplay qui DOIVENT tenir quel que soit le layout généré. */
    private static void checkGuarantees(List<String> problems, DungeonResult dr, String tag) {
        Set<RoomType> l0 = new HashSet<>(dr.labels.values());
        Set<RoomType> l1 = dr.topLabels == null ? Set.of() : new HashSet<>(dr.topLabels.values());

        // Étage 0 (P1-P3)
        if (!l0.contains(RoomType.PRISON)) problems.add(tag + " : ETAGE 0 sans Prison");
        if (l0.stream().noneMatch(v -> v != null && (v == RoomType.LOOT_1 || v.isDjLoot()))) {
            problems.add(tag + " : ETAGE 0 sans aucun loot");
        }
        if (!l0.contains(RoomType.OGRE)) problems.add(tag + " : ETAGE 0 sans Ogre");
        if (!l0.contains(RoomType.CENTRALE)) problems.add(tag + " : ETAGE 0 sans Centrale");
        // M5 : une avant porte1 (chemin taverne) + une en P2 (couloir droit) = 2 attendues.
        long nbM5 = dr.labels.values().stream().filter(v -> v == RoomType.MONSTER_5).count();
        if (nbM5 < 2) problems.add(tag + " : ETAGE 0 avec seulement " + nbM5 + " M5 (2 attendues)");

        // Étage 1 (P4)
        if (l1.isEmpty()) return; // déjà signalé comme P4 absente
        if (!l1.contains(RoomType.CENTRALE)) problems.add(tag + " : ETAGE 1 sans Centrale");
        if (!l1.contains(RoomType.GOBLIN_DOOR)) problems.add(tag + " : ETAGE 1 sans PorteGob");
        if (!l1.contains(RoomType.BLACK_MARKET)) problems.add(tag + " : ETAGE 1 sans MarchandNoir");
        if (!l1.contains(RoomType.WELL_DJ)) problems.add(tag + " : ETAGE 1 sans PuitDJ");
        if (l1.stream().noneMatch(v -> v != null && v.isDjLoot())) {
            problems.add(tag + " : ETAGE 1 sans aucun lootdj");
        }
    }
}
