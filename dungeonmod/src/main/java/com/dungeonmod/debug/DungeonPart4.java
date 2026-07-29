package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.DungeonAlgo.RoomIds;
import com.dungeonmod.debug.DungeonAlgo.RoomPools;
import com.dungeonmod.debug.DungeonAlgo.Theme;
import com.dungeonmod.debug.DungeonAlgo.TreeResult;

import java.util.*;

/**
 * Génération/analyse de la quatrième partie du donjon : étage de la Centrale,
 * arbres P4, village gobelin, chapelle/crypte, prison centrale, Marchand Noir,
 * PuitDJ et loot de rattrapage.
 *
 * Extraction mécanique depuis DungeonAlgo : déplacer le code sans modifier
 * volontairement le comportement.
 */
final class DungeonPart4 {
    private DungeonPart4() {}


    private static final int P4_MAX_IJ3 = 2;
    private static final int P4_MAX_IJ4 = 1;

    private static String pickC(Random rng) {
        return RoomPools.CORRIDORS_P1_P2.get(rng.nextInt(3));
    }

    private static String pickCJ(Random rng) {
        return RoomPools.CORRIDORS_P3_P4.get(rng.nextInt(3));
    }

    private static boolean isStraight(Point p1, Point p2) {
        return p1.x() == p2.x() || p1.y() == p2.y();
    }

    private static void putSpecial(DungeonLabelState labelState, Map<Point, String> workingLabels,
                                   Point point, String label) {
        labelState.putSpecial(point, label);
        workingLabels.put(point, label);
    }

    private static void putGeneric(DungeonLabelState labelState, Map<Point, String> workingLabels,
                                   Point point, Theme theme, Set<Point> neighbors, Random rng) {
        labelState.setTheme(point, theme);
        workingLabels.put(point, DungeonLabels.labelForNeighbors(neighbors, theme, rng));
    }

    private static void putGenericWorking(DungeonLabelState labelState, Map<Point, String> workingLabels,
                                          Point point, Theme theme, String workingLabel) {
        labelState.setTheme(point, theme);
        workingLabels.put(point, workingLabel);
    }

    private static void labelTreeNodes(DungeonLabelState labelState, Map<Point, Set<Point>> tr,
                                       Map<Point, String> topLabels, Point startPoint, Random rng) {
        List<Point> lf = new ArrayList<>(), co = new ArrayList<>(), i3 = new ArrayList<>(), i4 = new ArrayList<>();
        for (Point k : tr.keySet()) {
            int d = tr.get(k).size();
            if (d == 1) lf.add(k); else if (d == 2) co.add(k); else if (d == 3) i3.add(k); else if (d == 4) i4.add(k);
        }
        // Racine : si elle n'a qu'un voisin dans 'tr', l'arête vers l'exit du hub (absente de 'tr')
        // la rendra droite dans 'adj' — pickCJ direct est donc correct et évite un re-pick futur.
        if (tr.get(startPoint).size() == 1) putGenericWorking(labelState, topLabels, startPoint, Theme.DJ, pickCJ(rng));
        else putGeneric(labelState, topLabels, startPoint, Theme.DJ, tr.get(startPoint), rng);
        for (Point k : i3) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.DJ, tr.get(k), rng);
        for (Point k : i4) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.DJ, tr.get(k), rng);
        for (Point k : co) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.DJ, tr.get(k), rng);
        for (Point k : lf) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.DJ, tr.get(k), rng);
        // Règle spéciale : un couloir droit rattaché perpendiculairement à une intersection 4
        // devient un virage (visuellement, on tourne en entrant/sortant de l'intersection).
        for (Point ip : i4) {
            for (Point nb : tr.get(ip)) {
                String lbl = topLabels.get(nb);
                if (lbl == null || !(lbl.equals("CJ1") || lbl.equals("CJ2") || lbl.equals("CJ3"))) continue;
                List<Point> nAd = new ArrayList<>(tr.get(nb));
                if (nAd.size() != 2) continue;
                Point a0 = nAd.get(0);
                Point a1 = nAd.get(1);
                if ((a1.x() - a0.x()) * (nb.x() - ip.x()) + (a1.y() - a0.y()) * (nb.y() - ip.y()) == 0) {
                    putGenericWorking(labelState, topLabels, nb, Theme.DJ, RoomIds.CORRIDOR_TURN_J);
                }
            }
        }
    }

    private static boolean placeGoblinVillage(DungeonLabelState labelState, Map<Point, Set<Point>> adj,
                                               Map<Point, String> topLabels, Map<Point, Set<Point>> gt,
                                               Set<Point> globalOccupied, Set<Point> goblinCells, Random rng) {
        Point gLeaf = null; Point firstGob = null; Point parent = null;
        for (Point testLeaf : new ArrayList<>(gt.keySet())) {
            if (topLabels.get(testLeaf) == null || !topLabels.get(testLeaf).equals(RoomIds.DEAD_END_DJ)) continue;
            parent = gt.get(testLeaf).iterator().next();
            int gdx = testLeaf.x() - parent.x(), gdy = testLeaf.y() - parent.y();
            Point target = testLeaf.move(gdx, gdy);
            if (!target.isOutOfBounds() && !globalOccupied.contains(target)) {
                firstGob = target; gLeaf = testLeaf; break;
            }
        }
        if (gLeaf == null || firstGob == null) return false;

        putSpecial(labelState, topLabels, gLeaf, RoomIds.GOBLIN_DOOR);
        Map<Point, Set<Point>> ga = new HashMap<>();
        ga.put(gLeaf, new HashSet<>());
        globalOccupied.add(firstGob); ga.put(firstGob, new HashSet<>());
        ga.get(gLeaf).add(firstGob); ga.get(firstGob).add(gLeaf);

        TreeResult gobRaw = DungeonTreeBuilder.generateRawTree(20, 25, 3, 1, 2, firstGob, new HashSet<>(globalOccupied), DungeonAlgo.MAX_COLINEAR_RUN, rng);
        if (gobRaw.adj.size() < 6) return false;

        for (var e : gobRaw.adj.entrySet()) {
            ga.putIfAbsent(e.getKey(), new HashSet<>());
            ga.get(e.getKey()).addAll(e.getValue());
        }

        if (ga.get(gLeaf).size() > 2) {
            Point keepNb = null;
            for (Point nb : ga.get(gLeaf)) { if (!nb.equals(parent)) { keepNb = nb; break; } }
            for (Point nb : new ArrayList<>(ga.get(gLeaf))) {
                if (!nb.equals(parent) && !nb.equals(keepNb)) { ga.get(nb).remove(gLeaf); ga.get(gLeaf).remove(nb); }
            }
        }
        globalOccupied.addAll(ga.keySet());
        labelState.setTheme(ga.keySet(), Theme.GOBLIN);

        List<Point> gl = new ArrayList<>(), gco = new ArrayList<>(), gi3 = new ArrayList<>(), gi4 = new ArrayList<>();
        for (Point k : ga.keySet()) {
            if (k.equals(gLeaf)) continue;
            int d = ga.get(k).size();
            if (d == 1) gl.add(k); else if (d == 2) gco.add(k); else if (d == 3) gi3.add(k); else if (d == 4) gi4.add(k);
        }
        for (Point k : gi3) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.GOBLIN, ga.get(k), rng);
        for (Point k : gi4) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.GOBLIN, ga.get(k), rng);

        for (Point k : gco) {
            List<Point> nb2 = new ArrayList<>(ga.get(k));
            if (isStraight(nb2.get(0), nb2.get(1))) { putSpecial(labelState, topLabels, k, RoomIds.GOBLIN_WELL); break; }
        }

        Collections.shuffle(gl, rng);
        int gli = 0;
        if (gli < gl.size()) { putSpecial(labelState, topLabels, gl.get(gli), RoomIds.GOBLIN_MARCH); gli++; }
        if (gli < gl.size()) { putSpecial(labelState, topLabels, gl.get(gli), RoomIds.GOBLIN_ARMORY); gli++; }
        if (gli < gl.size()) { putSpecial(labelState, topLabels, gl.get(gli), RoomIds.GOBLIN_TREASURE); gli++; }

        for (Point k : gco) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.GOBLIN, ga.get(k), rng);
        for (Point k : gl) if (!topLabels.containsKey(k)) putGeneric(labelState, topLabels, k, Theme.GOBLIN, ga.get(k), rng);

        List<Point> hl = new ArrayList<>();
        for (Point k : ga.keySet()) {
            if (k.equals(gLeaf)) continue;
            String v = topLabels.get(k);
            if (v != null && (v.equals(RoomIds.GOBLIN_CORRIDOR) || v.equals(RoomIds.GOBLIN_TURN) || v.equals(RoomIds.GOBLIN_DEAD_END) || v.equals(RoomIds.GOBLIN_I3) || v.equals(RoomIds.GOBLIN_I4))) hl.add(k);
        }
        Collections.shuffle(hl, rng);
        Set<Point> hs = new HashSet<>();
        int hMax = 3 + rng.nextInt(3);
        for (Point k : hl) {
            if (hs.size() >= hMax) break;
            int dk = ga.get(k).size();
            if (dk == 2) {
                List<Point> nb2 = new ArrayList<>(ga.get(k));
                if (isStraight(nb2.get(0), nb2.get(1))) continue;
            }
            boolean treeAdj = ga.getOrDefault(k, Set.of()).stream().anyMatch(hs::contains);
            if (treeAdj) continue;
            boolean adjSpecial = ga.getOrDefault(k, Set.of()).stream().anyMatch(nb -> {
                String vl = topLabels.get(nb);
                return vl != null && (vl.equals(RoomIds.GOBLIN_WELL) || vl.equals(RoomIds.GOBLIN_MARCH) || vl.equals(RoomIds.GOBLIN_ARMORY) || vl.equals(RoomIds.GOBLIN_TREASURE));
            });
            if (adjSpecial) continue;
            hs.add(k);
        }
        for (Point k : hs) {
            int d = ga.get(k).size();
            if (d == 3) putSpecial(labelState, topLabels, k, RoomIds.GOBLIN_HOUSE_3);
            else if (d == 2) {
                List<Point> nb2 = new ArrayList<>(ga.get(k));
                if (!isStraight(nb2.get(0), nb2.get(1))) putSpecial(labelState, topLabels, k, RoomIds.GOBLIN_HOUSE_2);
            } else if (d == 1) putSpecial(labelState, topLabels, k, RoomIds.GOBLIN_HOUSE_1);
        }
        for (var e : ga.entrySet()) { adj.putIfAbsent(e.getKey(), new HashSet<>()); adj.get(e.getKey()).addAll(e.getValue()); goblinCells.add(e.getKey()); }
        goblinCells.add(gLeaf);
        return true;
    }

    private static void placeChapelAndCrypt(DungeonLabelState labelState, Map<Point, Set<Point>> adj,
                                             Map<Point, String> topLabels, Map<Point, Set<Point>> ct,
                                             Point cs, Set<Point> globalOccupied, Random rng) {
        for (Point k : ct.keySet()) {
            String vl = topLabels.get(k);
            if (ct.get(k).size() != 1 || k.equals(cs) || vl == null || !vl.equals(RoomIds.DEAD_END_DJ)) continue;
            Point mb = ct.get(k).iterator().next();
            Point e = k.move(k.x() - mb.x(), k.y() - mb.y());
            if (e.isOutOfBounds() || globalOccupied.contains(e)) continue;

            globalOccupied.add(e);
            adj.put(e, new HashSet<>());
            adj.get(k).add(e); adj.get(e).add(k);
            putSpecial(labelState, topLabels, k, RoomIds.CHAPEL_1); putSpecial(labelState, topLabels, e, RoomIds.CHAPEL_2);

            int dir = (k.x() - mb.x() == 1) ? 0 : (k.x() - mb.x() == -1) ? 2 : (k.y() - mb.y() == 1) ? 1 : 3;
            int pathLen = 3 + rng.nextInt(3);
            int turnAt = 1 + rng.nextInt(pathLen - 1);
            int turnDir = rng.nextBoolean() ? 1 : -1;
            Point pv = e; Point curr = e;

            for (int s = 0; s < pathLen; s++) {
                if (s == turnAt) dir = (dir + turnDir + 4) % 4;
                Point ncx = curr.move(DungeonAlgo.DIR_OFFSET[dir]);
                if (ncx.isOutOfBounds() || globalOccupied.contains(ncx)) break;
                globalOccupied.add(ncx); adj.put(ncx, new HashSet<>());
                adj.get(pv).add(ncx); adj.get(ncx).add(pv);
                // Chemin chapelle -> crypte : couloirs thème P1/P2 (C/I2), demande du dev.
                putGenericWorking(labelState, topLabels, ncx, Theme.P12, s == turnAt - 1 ? RoomIds.CORRIDOR_TURN : pickC(rng));
                pv = ncx; curr = ncx;
            }
            for (int s = 0; s < 2; s++) {
                Point ncx = curr.move(DungeonAlgo.DIR_OFFSET[dir]);
                if (ncx.isOutOfBounds() || globalOccupied.contains(ncx)) break;
                globalOccupied.add(ncx); adj.put(ncx, new HashSet<>());
                adj.get(pv).add(ncx); adj.get(ncx).add(pv);
                putSpecial(labelState, topLabels, ncx, s == 0 ? RoomIds.CRYPT_1 : RoomIds.CRYPT_2);
                pv = ncx; curr = ncx;
            }
            break;
        }
    }

    private static void placePrisonBlock(DungeonLabelState labelState, Map<Point, Set<Point>> adj,
                                          Map<Point, String> topLabels, Map<Point, Set<Point>> pt,
                                          Point ps, Set<Point> globalOccupied) {
        for (Point pk : pt.keySet()) {
            String vp = topLabels.get(pk);
            if (pt.get(pk).size() != 1 || pk.equals(ps) || vp == null || !vp.equals(RoomIds.DEAD_END_DJ)) continue;
            Point np = pt.get(pk).iterator().next();
            int dx = pk.x() - np.x(), dy = pk.y() - np.y();
            int d = dx == 1 ? 0 : dx == -1 ? 2 : dy == 1 ? 1 : 3;

            DungeonCompositeRooms.Placement prison = DungeonCompositeRooms.plan(
                    adj, pk, dx, dy, DungeonCompositeRooms.PRISON_CENTRAL, Set.of(pk));
            if (prison == null) continue;
            boolean occupied = false;
            for (Point cell : prison.occupiedCells()) {
                if (!cell.equals(pk) && globalOccupied.contains(cell)) { occupied = true; break; }
            }
            if (occupied) continue;

            DungeonCompositeRooms.place(adj, null, prison);
            globalOccupied.addAll(prison.occupiedCells());
            for (var e : prison.labelPoints().entrySet()) {
                putSpecial(labelState, topLabels, e.getValue(), e.getKey());
            }
            break;
        }
    }

    /** Reconstruit les labels finaux depuis la map de travail P4. */
    private static void rebuildFinalLabels(Map<Point, String> topLabels, Map<Point, Set<Point>> adj, Random rng) {
        DungeonLabelState labelState = new DungeonLabelState();
        labelState.absorbLabels(topLabels);
        topLabels.clear();
        topLabels.putAll(labelState.buildLabels(adj, rng));
    }


    static boolean generatePart4Tree(Map<Point, Set<Point>> adj,
                                              Map<Point, String> topLabels,
                                              int hx, int hz, String missingLootType, Random rng) {
        DungeonLabelState labelState = new DungeonLabelState();
        labelState.absorbLabels(topLabels);
        Point[] p4Exits = {
            new Point(hx, hz + 2),
            new Point(hx + 1, hz + 2),
            new Point(hx - 1, hz),
            new Point(hx + 2, hz),
            new Point(hx + 1, hz - 1)
        };
        List<Point> p4List = new ArrayList<>(Arrays.asList(p4Exits));
        Collections.shuffle(p4List, rng);

        Set<Point> globalOccupied = new HashSet<>();
        for (int x = hx; x <= hx + 1; x++) {
            for (int z = hz; z <= hz + 1; z++) {
                Point hp = new Point(x, z);
                globalOccupied.add(hp);
                adj.putIfAbsent(hp, new HashSet<>());
            }
        }

        List<Point> cjKeys = new ArrayList<>(), exitKeys = new ArrayList<>();
        List<int[]> cjDirs = new ArrayList<>();
        for (Point pp : p4List) {
            int adx = 0, ady = 0; Point hk = null;
            if (pp.x() == hx && pp.y() == hz+2) { adx = 0; ady = 1; hk = new Point(hx, hz+1); }
            else if (pp.x() == hx+1 && pp.y() == hz+2) { adx = 0; ady = 1; hk = new Point(hx+1, hz+1); }
            else if (pp.x() == hx-1 && pp.y() == hz) { adx = -1; ady = 0; hk = new Point(hx, hz); }
            else if (pp.x() == hx+2 && pp.y() == hz) { adx = 1; ady = 0; hk = new Point(hx+1, hz); }
            else if (pp.x() == hx+1 && pp.y() == hz-1) { adx = 0; ady = -1; hk = new Point(hx+1, hz); }
            if (hk == null) continue;

            Point cjPt = pp.move(adx, ady);
            if (!cjPt.isOutOfBounds() && !globalOccupied.contains(cjPt)) {
                adj.putIfAbsent(pp, new HashSet<>()); adj.putIfAbsent(cjPt, new HashSet<>());
                adj.get(pp).add(hk); adj.get(hk).add(pp);
                adj.get(pp).add(cjPt); adj.get(cjPt).add(pp);
                globalOccupied.add(pp); globalOccupied.add(cjPt);
                labelState.setTheme(pp, Theme.DJ);
                labelState.setTheme(cjPt, Theme.DJ);
                cjKeys.add(cjPt); cjDirs.add(new int[]{adx, ady}); exitKeys.add(pp);
            }
        }

        // RÉSERVATION DES CHAÎNES INITIALES : les 2 premiers segments (f1, f2) de chaque
        // arbre sont posés par la croissance SANS vérifier globalOccupied — on les réserve
        // donc AVANT qu'aucun arbre ne pousse. Sinon, un arbre construit tôt (ex. sortie
        // sud-ouest) pouvait coloniser les cellules f1/f2 de l'arbre adjacent (sortie
        // sud-est) pas encore bâti → cellules partagées par deux arbres, voisinages
        // fusionnés au merge addAll, et labels calculés sur la vue PARTIELLE d'un seul
        // arbre (les fameux faux virages à 3-4 connexions, toujours au sud).
        Map<Point, List<Point>> reservedChains = new HashMap<>();
        for (int ti = 0; ti < cjKeys.size(); ti++) {
            Point cp = cjKeys.get(ti);
            int rdx = cjDirs.get(ti)[0], rdy = cjDirs.get(ti)[1];
            List<Point> chain = new ArrayList<>();
            Point f1 = cp.move(rdx, rdy);
            if (!f1.isOutOfBounds() && !globalOccupied.contains(f1)) {
                chain.add(f1);
                globalOccupied.add(f1);
                Point f2 = f1.move(rdx, rdy);
                if (!f2.isOutOfBounds() && !globalOccupied.contains(f2)) {
                    chain.add(f2);
                    globalOccupied.add(f2);
                }
            }
            reservedChains.put(cp, chain);
        }

        List<Map<Point, Set<Point>>> allTrees = new ArrayList<>();
        List<Point> allStarts = new ArrayList<>();
        for (int ti = 0; ti < 5 && ti < cjKeys.size(); ti++) {
            Point startPoint = cjKeys.get(ti);
            int adx = cjDirs.get(ti)[0], ady = cjDirs.get(ti)[1];
            Map<Point, Set<Point>> tr = new HashMap<>();
            tr.put(startPoint, new HashSet<>());
            globalOccupied.add(startPoint);
            int ci3 = 0, ci4 = 0, target = 13 + rng.nextInt(5);

            // Chaîne initiale f1/f2 : déjà réservée (voir plus haut) → garantie libre,
            // sans vérification d'occupation ici (c'est tout l'intérêt de la réservation).
            List<Point> chain = reservedChains.getOrDefault(startPoint, List.of());
            Point prev = startPoint;
            for (Point f : chain) {
                tr.put(f, new HashSet<>());
                tr.get(prev).add(f); tr.get(f).add(prev);
                prev = f;
            }

            while (tr.size() < target) {
                List<Object[]> cands = new ArrayList<>();
                for (Point p : tr.keySet()) {
                    int d = tr.get(p).size(); if (d >= 4) continue;
                    if (d == 2 && ci3 >= P4_MAX_IJ3) continue;
                    if (d == 3 && ci4 >= P4_MAX_IJ4) continue;

                    for (int[] dir : DungeonAlgo.DIR_OFFSET) {
                        Point next = p.move(dir);
                        if (next.isOutOfBounds()) continue;
                        if (!globalOccupied.contains(next)) {
                            // Limite géométrique : max DungeonAlgo.MAX_COLINEAR_RUN segments alignés
                            // dans l'adj de l'arbre (I3 comptés dans l'axe)
                            if (DungeonConstraints.colinearRunAfterEdge(p, next, tr) > DungeonAlgo.MAX_COLINEAR_RUN) continue;
                            if (d >= 2) {
                                boolean sk = false;
                                for (Point m1 : tr.get(p)) {
                                    for (Point m2 : tr.get(p)) {
                                        if (m1.equals(m2)) continue;
                                        if (p.x() - m1.x() == m2.x() - p.x() && p.y() - m1.y() == m2.y() - p.y()) {
                                            Set<Point> a1 = tr.get(m1), a2 = tr.get(m2);
                                            if ((a1 != null && a1.size() >= 3) || (a2 != null && a2.size() >= 3)) { sk = true; break; }
                                        }
                                    }
                                    if (sk) break;
                                }
                                if (sk) continue;
                            }
                            if (d == 1) {
                                Point op = tr.get(p).iterator().next();
                                if (p.x() - op.x() == next.x() - p.x() && p.y() - op.y() == next.y() - p.y()) {
                                    Point bk = op.move(-(p.x() - op.x()), -(p.y() - op.y()));
                                    if (tr.containsKey(bk) && tr.get(bk).contains(op)) continue;
                                }
                            }
                            cands.add(new Object[]{p, next, dir});
                        }
                    }
                }
                if (cands.isEmpty()) break;
                List<Object[]> w = new ArrayList<>();
                for (Object[] c : cands) {
                    int[] cd = (int[]) c[2];
                    int wt = (cd[0] == adx && cd[1] == ady) ? 20 : 1;
                    for (int i = 0; i < wt; i++) w.add(c);
                }
                if (w.isEmpty()) break;
                Object[] ch = w.get(rng.nextInt(w.size()));
                Point src = (Point) ch[0], dst = (Point) ch[1];
                globalOccupied.add(dst); tr.put(dst, new HashSet<>());
                tr.get(src).add(dst); tr.get(dst).add(src);
                int nd = tr.get(src).size(); if (nd == 3) ci3++; else if (nd == 4) ci4++;
            }
            if (tr.size() < 6) continue;
            allTrees.add(tr); allStarts.add(startPoint);

            labelTreeNodes(labelState, tr, topLabels, startPoint, rng);

            for (var e : tr.entrySet()) {
                adj.putIfAbsent(e.getKey(), new HashSet<>());
                adj.get(e.getKey()).addAll(e.getValue());
            }
        }

        // Garde anti-fusion : aucune cellule ne doit appartenir à deux arbres distincts
        // (impossible depuis la réservation des chaînes ; si un futur changement la
        // réintroduisait, on préfère un retry propre à un graphe fusionné silencieux).
        Set<Point> seenTreeCells = new HashSet<>();
        for (Map<Point, Set<Point>> t : allTrees) {
            for (Point k : t.keySet()) {
                if (!seenTreeCells.add(k)) return false;
            }
        }

        // Rattrapage : les exits et racines d'arbres voient leur degré réel dans 'adj'
        // (le hub et l'arête racine ne sont pas dans les maps d'arbres locales).
        for (Point ek : exitKeys) {
            if (topLabels.containsKey(ek)) continue;
            Set<Point> skn = adj.get(ek); if (skn == null) continue;
            putGeneric(labelState, topLabels, ek, Theme.DJ, skn, rng);
        }

        for (Point cjk : cjKeys) {
            Set<Point> cn = adj.get(cjk); if (cn == null) continue;
            putGeneric(labelState, topLabels, cjk, Theme.DJ, cn, rng);
        }

        if (allTrees.size() < 5) return false;

        List<Integer> idxs = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
        Collections.shuffle(idxs, rng);
        int gIdx = idxs.get(0), cIdx = idxs.get(1), pIdx = idxs.get(2);
        Set<Point> goblinCells = new HashSet<>();

        if (!placeGoblinVillage(labelState, adj, topLabels, allTrees.get(gIdx), globalOccupied, goblinCells, rng)) return false;
        placeChapelAndCrypt(labelState, adj, topLabels, allTrees.get(cIdx), allStarts.get(cIdx), globalOccupied, rng);
        placePrisonBlock(labelState, adj, topLabels, allTrees.get(pIdx), allStarts.get(pIdx), globalOccupied);

        // PASSE DE COHÉRENCE FINALE : la topologie P4 est désormais figée (arbres, exits,
        // village gobelin, chapelle, prison). On re-déduit chaque label structurel générique
        // (CJ/IJ/culDJ + CG/GI/CDG) de l'adjacence RÉELLE. Corrige notamment les nœuds classés
        // "virage" qui ont gagné un 3e voisin via une greffe post-classification.
        DungeonLabels.reclassifyGeneric(topLabels, adj, rng);

        java.util.function.Predicate<Point> isHubExit = kp ->
            (kp.x() == hx-1 && kp.y() == hz) || (kp.x() == hx+2 && kp.y() == hz) ||
            (kp.x() == hx+1 && kp.y() == hz-1) || (kp.x() == hx && kp.y() == hz+2) ||
            (kp.x() == hx+1 && kp.y() == hz+2);

        int mjT = 5 + rng.nextInt(6);
        Map<Point, Integer> treeForNode = new HashMap<>();
        for (int ti = 0; ti < allTrees.size(); ti++) for (Point k : allTrees.get(ti).keySet()) treeForNode.put(k, ti);
        Set<Point> mjPlacedKeys = new HashSet<>();
        Map<Integer, Set<String>> mjTypesOnTree = new HashMap<>();
        for (int ti = 0; ti < allTrees.size(); ti++) mjTypesOnTree.put(ti, new HashSet<>());

        for (int attempt = 0; attempt < 50 && mjPlacedKeys.size() < mjT; attempt++) {
            Point best = null; String bestType = null;
            for (Point k : topLabels.keySet()) {
                if (mjPlacedKeys.contains(k) || goblinCells.contains(k) || isHubExit.test(k)) continue;
                if (mjPlacedKeys.size() >= mjT) break;
                String v = topLabels.get(k); if (v == null) continue;

                boolean isLeaf = RoomIds.DEAD_END_DJ.equals(v);
                boolean isCorr = v.startsWith("CJ") || v.startsWith("CG");
                if (!isLeaf && !isCorr) continue;

                // ESPACEMENT MONSTRES (règle dure, conversation 3) : distance
                // >= DungeonAlgo.MONSTER_MIN_DIST entre salles MJ de l'étage 1.
                if (!DungeonConstraints.isFarFromAll(adj, k, mjPlacedKeys, DungeonAlgo.MONSTER_MIN_DIST)) continue;

                int ti = treeForNode.getOrDefault(k, -1);
                Set<String> usedOnTree = mjTypesOnTree.getOrDefault(ti, new HashSet<>());
                List<String> pool = isLeaf ? RoomPools.LEAF_MONSTERS_P3_P4 : RoomPools.CORRIDOR_MONSTERS_P3_P4;
                String availType = null;
                for (String t : pool) if (!usedOnTree.contains(t)) { availType = t; break; }
                if (availType == null) continue;
                best = k; bestType = availType; break;
            }
            if (best != null) {
                putSpecial(labelState, topLabels, best, bestType);
                mjPlacedKeys.add(best);
                int ti = treeForNode.getOrDefault(best, -1);
                if (ti >= 0) mjTypesOnTree.get(ti).add(bestType);
            }
        }

        for (var e : new ArrayList<>(topLabels.entrySet())) {
            if (e.getValue() != null && e.getValue().equals(RoomIds.DEAD_END_DJ) && !isHubExit.test(e.getKey())) {
                putSpecial(labelState, topLabels, e.getKey(), RoomIds.BLACK_MARKET); break;
            }
        }

        if (missingLootType != null) {
            boolean lootCorr = missingLootType.equals("Lootdj2");
            Point lk = null;
            for (var e : topLabels.entrySet()) {
                String v = e.getValue(); Point k = e.getKey();
                if (v == null || goblinCells.contains(k) || isHubExit.test(k)) continue;
                if (lootCorr && (v.startsWith("CJ") || v.startsWith("CG"))) { lk = k; break; }
                if (!lootCorr && v.equals(RoomIds.DEAD_END_DJ)) { lk = k; break; }
            }
            if (lk != null) putSpecial(labelState, topLabels, lk, missingLootType);
        }

        Point puitDJKey = null;
        for (var e : topLabels.entrySet()) {
            String v = e.getValue(); Point k = e.getKey();
            if (v == null || goblinCells.contains(k) || isHubExit.test(k)) continue;
            if (v.startsWith("CJ") || v.startsWith("CG")) { puitDJKey = k; break; }
        }
        if (puitDJKey != null) putSpecial(labelState, topLabels, puitDJKey, RoomIds.WELL_DJ);

        // Règle cul-de-sac étage 1 (conversation 3) : pas de culDJ/CDG au bout d'une
        // ligne droite. Échec => rejet (false => retry amont, 15 tentatives).
        if (!DungeonConstraints.enforceDeadEndAfterTurn(topLabels, adj, rng)) return false;

        // Reconstruction finale : les génériques P4 sont redéduits de l'adj réelle.
        rebuildFinalLabels(topLabels, adj, rng);

        // Validation finale
        boolean hc1 = topLabels.containsValue(RoomIds.CHAPEL_1) && topLabels.containsValue(RoomIds.CRYPT_1);
        boolean hpr = topLabels.values().stream().anyMatch(v -> v != null && v.startsWith("PrisonC"));
        boolean hpg = topLabels.containsValue(RoomIds.GOBLIN_DOOR);
        boolean hmn = topLabels.containsValue(RoomIds.BLACK_MARKET);
        boolean hlt = topLabels.containsValue("Lootdj1") || topLabels.containsValue("Lootdj2") || topLabels.containsValue("Lootdj3");
        boolean hPuitDJ = topLabels.containsValue(RoomIds.WELL_DJ);
        int gbc = (int) topLabels.values().stream().filter(v -> v != null && (v.equals(RoomIds.GOBLIN_WELL) || v.equals(RoomIds.GOBLIN_MARCH) || v.equals(RoomIds.GOBLIN_ARMORY) || v.equals(RoomIds.GOBLIN_TREASURE))).count();
        boolean hmg = topLabels.values().stream().anyMatch(v -> v != null && v.startsWith("MG"));

        return hc1 && hpr && hpg && hmn && hlt && hPuitDJ && hmg && gbc >= 2 && mjPlacedKeys.size() >= 5;
    }
}
