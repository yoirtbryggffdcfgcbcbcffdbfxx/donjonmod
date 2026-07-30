package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DungeonViz {

    private static final Map<RoomType, String> ROOM_COLORS = new LinkedHashMap<>();
    private static final Map<RoomType, String> ROOM_LABELS = new LinkedHashMap<>();
    static {
        put(RoomType.START, "#2E7D32", "Depart");
        put(RoomType.C1, "#78909C", "Couloir"); put(RoomType.C2, "#78909C", "Couloir"); put(RoomType.C3, "#78909C", "Couloir");
        put(RoomType.I2, "#4FC3F7", "Virage"); put(RoomType.I3, "#29B6F6", "Intersec 3"); put(RoomType.I4, "#0288D1", "Intersec 4");
        put(RoomType.CUL, "#616161", "Cul-de-sac"); put(RoomType.MONSTER_1, "#E53935", "Monstre 1"); put(RoomType.MONSTER_2, "#C62828", "Monstre 2");
        put(RoomType.MONSTER_5, "#B71C1C", "Monstre 5"); put(RoomType.PRISON, "#B71C1C", "Prison"); put(RoomType.LOOT_1, "#FFD600", "Loot");
        put(RoomType.FOUNTAIN, "#42A5F5", "Fontaine"); put(RoomType.WELL, "#0D47A1", "Puits");
        put(RoomType.DOOR_1, "#757575", "Porte"); put(RoomType.DOOR_2, "#757575", "Porte 2"); put(RoomType.DOOR_3, "#757575", "Porte 3");
        put(RoomType.TAVERN_1, "#8D6E63", "Taverne 1"); put(RoomType.TAVERN_2, "#8D6E63", "Taverne 2"); put(RoomType.TAVERN_3, "#8D6E63", "Taverne 3"); put(RoomType.TAVERN_4, "#8D6E63", "Taverne 4");
        put(RoomType.CJ1, "#A1887F", "Couloir Donjon"); put(RoomType.CJ2, "#A1887F", "Couloir Donjon"); put(RoomType.CJ3, "#A1887F", "Couloir Donjon");
        put(RoomType.IJ2, "#7E57C2", "Virage Donjon"); put(RoomType.IJ3, "#7E57C2", "Intersec Donjon"); put(RoomType.IJ4, "#7E57C2", "Intersec Donjon");
        put(RoomType.MONSTER_DJ_1, "#D32F2F", "Monstre Donjon"); put(RoomType.MONSTER_DJ_2, "#D32F2F", "Monstre Donjon");
        put(RoomType.LOOT_DJ_1, "#FFC107", "Loot Donjon"); put(RoomType.LOOT_DJ_2, "#FFC107", "Loot Donjon");
        put(RoomType.CAMP_1, "#FF8A65", "Camp 1"); put(RoomType.CAMP_2, "#FF8A65", "Camp 2"); put(RoomType.CAMP_3, "#FF8A65", "Camp 3"); put(RoomType.CAMP_4, "#FF8A65", "Camp 4");
        put(RoomType.BIB_1, "#9C27B0", "Biblio 1"); put(RoomType.BIB_2, "#9C27B0", "Biblio 2"); put(RoomType.SHOP, "#FFD700", "Shop");
        put(RoomType.CUL_DJ, "#616161", "Cul-de-sac DJ"); put(RoomType.MONSTER_3, "#E57373", "Monstre 3"); put(RoomType.MONSTER_4, "#EF5350", "Monstre 4");
        put(RoomType.OGRE, "#880E4F", "Ogre"); put(RoomType.MONSTER_DJ_3, "#E91E63", "Monstre DJ 3"); put(RoomType.MONSTER_DJ_4, "#AD1457", "Monstre DJ 4"); put(RoomType.MONSTER_DJ_5, "#880E4F", "Monstre DJ 5");
        put(RoomType.WELL_DJ, "#1565C0", "Puit Donjon"); put(RoomType.GARDEN, "#66BB6A", "Jardin"); put(RoomType.LOOT_DJ_3, "#FFB300", "Loot Donjon 3");
        put(RoomType.STATUE, "#A1887F", "Statue"); put(RoomType.CENTRALE, "#1B5E20", "Centrale"); put(RoomType.BLACK_MARKET, "#FF8C00", "Marchand Noir");
        put(RoomType.CHAPEL_1, "#D4A574", "Chapelle Entree"); put(RoomType.CHAPEL_2, "#D4A574", "Chapelle Fond");
        put(RoomType.CRYPT_1, "#5D4037", "Crypte Entree"); put(RoomType.CRYPT_2, "#4E342E", "Crypte Fond");
        put(RoomType.PRISON_C1, "#B71C1C", "Prison C1"); put(RoomType.PRISON_C2, "#C62828", "Prison C2"); put(RoomType.PRISON_C3, "#C62828", "Prison C3"); put(RoomType.PRISON_C4, "#D32F2F", "Prison C4");
        put(RoomType.GOBLIN_DOOR, "#8D6E63", "Porte Gob"); put(RoomType.CG1, "#A1887F", "Couloir Gob");
        put(RoomType.GI2, "#7E57C2", "Intersec Gob"); put(RoomType.GI3, "#7E57C2", "Intersec Gob"); put(RoomType.GI4, "#7E57C2", "Intersec Gob");
        put(RoomType.GOBLIN_WELL, "#1565C0", "Puit Gob"); put(RoomType.GOBLIN_MARCH, "#FF8A65", "Marchand Gob"); put(RoomType.GOBLIN_TREASURE, "#FFD700", "Tresor Gob");
        put(RoomType.GOBLIN_ARMORY, "#B71C1C", "Armure Gob"); put(RoomType.CDG, "#616161", "Cul Sac Gob");
        put(RoomType.GOBLIN_HOUSE_1, "#8BC34A", "Maison Gob"); put(RoomType.GOBLIN_HOUSE_2, "#8BC34A", "Maison Gob"); put(RoomType.GOBLIN_HOUSE_3, "#8BC34A", "Maison Gob");
    }
    private static void put(RoomType t, String c, String l) { ROOM_COLORS.put(t, c); ROOM_LABELS.put(t, l); }

    private static String colorOf(RoomType t) { return ROOM_COLORS.getOrDefault(t, "#555"); }
    private static String labelOf(RoomType t) { String n = ROOM_LABELS.get(t); return n != null ? (n.length() > 5 ? n.substring(0, 5) : n) : t.id; }
    private static String nameOf(RoomType t) { return ROOM_LABELS.getOrDefault(t, t.id); }

    public static void renderToHtml(Map<Point, Set<Point>> adj, Map<Point, RoomType> labels,
                                     Point startPoint, long seed, String filePath) throws IOException {
        renderToHtml(adj, labels, null, null, startPoint, seed, filePath);
    }

    public static void renderToHtml(Map<Point, Set<Point>> adj, Map<Point, RoomType> labels,
                                     Map<Point, Set<Point>> p4Adj, Map<Point, RoomType> topLabels,
                                     Point startPoint, long seed, String filePath) throws IOException {
        if (labels == null || labels.isEmpty()) return;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (Point key : labels.keySet()) { int x = key.x(), z = key.y(); if (x < minX) minX = x; if (x > maxX) maxX = x; if (z < minZ) minZ = z; if (z > maxZ) maxZ = z; }
        int gridW = maxX - minX + 1, gridH = maxZ - minZ + 1, cellSize = 80, cellGap = 3;
        int svgW = gridW * cellSize + 40, svgH = gridH * cellSize + 40;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(svgW).append("' height='").append(svgH).append("'>\n  <rect width='100%' height='100%' fill='#1a1a2e'/>\n");
        Set<String> drawn = new HashSet<>();
        for (var entry : adj.entrySet()) {
            Point k1 = entry.getKey(); if (!labels.containsKey(k1)) continue;
            int x1 = k1.x() - minX, z1 = k1.y() - minZ, cx1 = x1 * cellSize + cellSize / 2 + 20, cy1 = z1 * cellSize + cellSize / 2 + 20;
            if (labels.get(k1) == RoomType.CENTRALE) { cx1 += 40; cy1 += 40; }
            for (Point k2 : entry.getValue()) {
                if (!labels.containsKey(k2)) continue;
                String edge = k1.key() + "|" + k2.key(), rev = k2.key() + "|" + k1.key();
                if (drawn.contains(edge) || drawn.contains(rev)) continue; drawn.add(edge);
                int x2 = k2.x() - minX, z2 = k2.y() - minZ, cx2 = x2 * cellSize + cellSize / 2 + 20, cy2 = z2 * cellSize + cellSize / 2 + 20;
                if (labels.get(k2) == RoomType.CENTRALE) { cx2 += 40; cy2 += 40; }
                svg.append("  <line x1='").append(cx1).append("' y1='").append(cy1).append("' x2='").append(cx2).append("' y2='").append(cy2).append("' stroke='#444' stroke-width='10' stroke-linecap='round'/>\n");
            }
        }
        for (var entry : labels.entrySet()) {
            Point key = entry.getKey(); RoomType type = entry.getValue();
            int gx = key.x() - minX, gz = key.y() - minZ, x = gx * cellSize + 20 + cellGap, z = gz * cellSize + 20 + cellGap;
            int mult = type == RoomType.CENTRALE ? 2 : 1, w = cellSize * mult - cellGap * 2;
            boolean isStart = key.equals(startPoint);
            String tooltip = esc(type.id) + " - " + esc(nameOf(type)) + " (" + key.key() + ")";
            svg.append("  <g><title>").append(tooltip).append("</title>")
               .append("<rect x='").append(x).append("' y='").append(z).append("' width='").append(w).append("' height='").append(w)
               .append("' fill='").append(colorOf(type)).append("' rx='4' stroke='").append(isStart ? "#FFD600" : "#333")
               .append("' stroke-width='").append(isStart ? "3" : "1").append("'/>")
               .append("<text x='").append(x + w / 2).append("' y='").append(z + w / 2 + 3)
               .append("' text-anchor='middle' font-size='").append(mult > 1 ? "12" : "8").append("' fill='#fff' font-family='sans-serif' font-weight='bold' pointer-events='none'>")
               .append(esc(mult > 1 ? "Centrale" : labelOf(type))).append("</text></g>\n");
        }
        svg.append("</svg>");
        StringBuilder sbCounts = new StringBuilder(); Map<RoomType, Integer> counts = new HashMap<>();
        for (RoomType v : labels.values()) counts.merge(v, 1, Integer::sum);
        for (var e : counts.entrySet()) sbCounts.append("<span style='margin:0 8px;color:#aaa'>").append(esc(nameOf(e.getKey()))).append(": <b>").append(e.getValue()).append("</b></span>");
        StringBuilder legend = new StringBuilder(); Set<String> seenC = new LinkedHashSet<>();
        for (var e : ROOM_COLORS.entrySet()) {
            String ck = e.getValue() + "|" + nameOf(e.getKey());
            if (!seenC.add(ck)) continue;
            legend.append("<tr><td style='background:").append(e.getValue()).append(";width:16px;height:16px;border-radius:3px'></td><td style='color:#ccc;padding:2px 8px'>").append(esc(nameOf(e.getKey()))).append("</td></tr>\n");
        }
        String html = "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'><title>Donjon - Seed " + seed + "</title>"
            + "<style>body{background:#0d0d1a;font-family:sans-serif;margin:20px;color:#eee}h2{color:#FFD600;margin-bottom:5px}.seed{color:#888;font-size:14px;margin-bottom:15px}"
            + ".map{background:#1a1a2e;display:inline-block;padding:10px;border-radius:8px}table.legend{display:inline-block;vertical-align:top;margin-left:30px;border-collapse:collapse}"
            + "table.legend td{padding:2px 4px;font-size:13px}.counts{margin:15px 0;line-height:1.8}</style></head><body>"
            + "<h2>&#9879; Donjon - Visualisation</h2><div class='seed'>Seed: <b>" + seed + "</b> | Salles: <b>" + labels.size() + "</b> | " + gridW + "x" + gridH + "</div>"
            + "<div class='counts'>" + sbCounts + "</div><div class='map' style='margin-top:15px'>" + svg + "</div>"
            + "<table class='legend'><tr><th colspan='2' style='color:#FFD600;text-align:left;padding-bottom:6px'>Legende</th></tr>" + legend + "</table></body></html>";
        Files.writeString(Path.of(filePath), html);
    }

    private static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    public static void main(String[] args) {
        long seed = 0; String output = "dungeon_viz.html";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-o") && i + 1 < args.length) output = args[i + 1];
            else if (args[i].equals("-s") && i + 1 < args.length) seed = Long.parseLong(args[i + 1]);
            else if (seed == 0 && Character.isDigit(args[i].charAt(0))) seed = Long.parseLong(args[i]);
        }
        int attempts = 0; DungeonFailureLog.reset();
        while (true) {
            attempts++; DungeonAlgo.DungeonResult result = DungeonAlgo.generateDungeon(seed);
            if (result != null) {
                try {
                    renderToHtml(result.adj, result.labels, result.p4Adj, result.topLabels, result.startPoint, DungeonAlgo.getLastSeed(), output);
                    System.out.println("Dungeon genere avec la seed " + DungeonAlgo.getLastSeed() + " (tentative " + attempts + ")");
                    System.out.println("Salles: " + result.labels.size()); System.out.println("Visualisation: " + Path.of(output).toAbsolutePath());
                    List<String> probs = DungeonAlgo.validateStructure(result.labels, result.adj, "ETAGE 0");
                    if (result.topLabels != null && result.p4Adj != null) probs.addAll(DungeonAlgo.validateStructure(result.topLabels, result.p4Adj, "ETAGE 1"));
                    if (probs.isEmpty()) System.out.println("Validation structure : OK, 0 incoherence");
                    else { System.out.println("Validation structure : " + probs.size() + " incoherence(s) !"); for (String p : probs) System.out.println("  - " + p); }
                    DungeonFailureLog.printSummary("resume ouvrir_donjon");
                } catch (IOException e) { System.err.println("Erreur ecriture: " + e.getMessage()); }
                break;
            }
            if (seed != 0) { System.out.println("Seed " + seed + " invalide, essai avec seed aleatoire"); seed = 0; }
        }
    }
}
