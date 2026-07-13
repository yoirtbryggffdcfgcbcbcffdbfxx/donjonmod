package com.dungeonmod.debug;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DungeonViz {

    // ===================== Visualization colors and labels =====================

    private static final Map<String, String> ROOM_COLORS = new LinkedHashMap<>();
    private static final Map<String, String> ROOM_LABELS = new LinkedHashMap<>();
    static {
        ROOM_COLORS.put("D",        "#2E7D32"); ROOM_LABELS.put("D",        "Depart");
        ROOM_COLORS.put("C1",       "#78909C"); ROOM_LABELS.put("C1",       "Couloir");
        ROOM_COLORS.put("C2",       "#78909C"); ROOM_LABELS.put("C2",       "Couloir");
        ROOM_COLORS.put("C3",       "#78909C"); ROOM_LABELS.put("C3",       "Couloir");
        ROOM_COLORS.put("I2",       "#4FC3F7"); ROOM_LABELS.put("I2",       "Virage");
        ROOM_COLORS.put("I3",       "#29B6F6"); ROOM_LABELS.put("I3",       "Intersec 3");
        ROOM_COLORS.put("I4",       "#0288D1"); ROOM_LABELS.put("I4",       "Intersec 4");
        ROOM_COLORS.put("cul",      "#616161"); ROOM_LABELS.put("cul",      "Cul-de-sac");
        ROOM_COLORS.put("M1",       "#E53935"); ROOM_LABELS.put("M1",       "Monstre 1");
        ROOM_COLORS.put("M2",       "#C62828"); ROOM_LABELS.put("M2",       "Monstre 2");
        ROOM_COLORS.put("Prison",   "#B71C1C"); ROOM_LABELS.put("Prison",   "Prison");
        ROOM_COLORS.put("Loot1",    "#FFD600"); ROOM_LABELS.put("Loot1",    "Loot");
        ROOM_COLORS.put("fontaine", "#42A5F5"); ROOM_LABELS.put("fontaine", "Fontaine");
        ROOM_COLORS.put("puit",     "#0D47A1"); ROOM_LABELS.put("puit",     "Puits");
        ROOM_COLORS.put("porte",    "#757575"); ROOM_LABELS.put("porte",    "Porte");
        ROOM_COLORS.put("porte2",   "#757575"); ROOM_LABELS.put("porte2",   "Porte 2");
        ROOM_COLORS.put("porte3",   "#757575"); ROOM_LABELS.put("porte3",   "Porte 3");
        ROOM_COLORS.put("T1",       "#8D6E63"); ROOM_LABELS.put("T1",       "Taverne 1");
        ROOM_COLORS.put("T2",       "#8D6E63"); ROOM_LABELS.put("T2",       "Taverne 2");
        ROOM_COLORS.put("T3",       "#8D6E63"); ROOM_LABELS.put("T3",       "Taverne 3");
        ROOM_COLORS.put("T4",       "#8D6E63"); ROOM_LABELS.put("T4",       "Taverne 4");
        ROOM_COLORS.put("CJ1",      "#A1887F"); ROOM_LABELS.put("CJ1",      "Couloir Donjon");
        ROOM_COLORS.put("CJ2",      "#A1887F"); ROOM_LABELS.put("CJ2",      "Couloir Donjon");
        ROOM_COLORS.put("CJ3",      "#A1887F"); ROOM_LABELS.put("CJ3",      "Couloir Donjon");
        ROOM_COLORS.put("IJ2",      "#7E57C2"); ROOM_LABELS.put("IJ2",      "Virage Donjon");
        ROOM_COLORS.put("IJ3",      "#7E57C2"); ROOM_LABELS.put("IJ3",      "Intersec Donjon");
        ROOM_COLORS.put("IJ4",      "#7E57C2"); ROOM_LABELS.put("IJ4",      "Intersec Donjon");
        ROOM_COLORS.put("MJ1",      "#D32F2F"); ROOM_LABELS.put("MJ1",      "Monstre Donjon");
        ROOM_COLORS.put("MJ2",      "#D32F2F"); ROOM_LABELS.put("MJ2",      "Monstre Donjon");
        ROOM_COLORS.put("Lootdj1",  "#FFC107"); ROOM_LABELS.put("Lootdj1",  "Loot Donjon");
        ROOM_COLORS.put("Lootdj2",  "#FFC107"); ROOM_LABELS.put("Lootdj2",  "Loot Donjon");
        ROOM_COLORS.put("Ca1",      "#FF8A65"); ROOM_LABELS.put("Ca1",      "Camp 1");
        ROOM_COLORS.put("Ca2",      "#FF8A65"); ROOM_LABELS.put("Ca2",      "Camp 2");
        ROOM_COLORS.put("Ca3",      "#FF8A65"); ROOM_LABELS.put("Ca3",      "Camp 3");
        ROOM_COLORS.put("Ca4",      "#FF8A65"); ROOM_LABELS.put("Ca4",      "Camp 4");
        ROOM_COLORS.put("Bib1",     "#9C27B0"); ROOM_LABELS.put("Bib1",     "Bibliotheque 1");
        ROOM_COLORS.put("Bib2",     "#9C27B0"); ROOM_LABELS.put("Bib2",     "Bibliotheque 2");
        ROOM_COLORS.put("Shop",     "#FFD700"); ROOM_LABELS.put("Shop",     "Shop");
        ROOM_COLORS.put("culDJ",    "#616161"); ROOM_LABELS.put("culDJ",    "Cul-de-sac DJ");
        ROOM_COLORS.put("M3",       "#E57373"); ROOM_LABELS.put("M3",       "Monstre 3");
        ROOM_COLORS.put("M4",       "#EF5350"); ROOM_LABELS.put("M4",       "Monstre 4");
        ROOM_COLORS.put("Ogre",     "#880E4F"); ROOM_LABELS.put("Ogre",     "Ogre");
        ROOM_COLORS.put("MJ3",      "#E91E63"); ROOM_LABELS.put("MJ3",      "Monstre Donjon 3");
        ROOM_COLORS.put("MJ4",      "#AD1457"); ROOM_LABELS.put("MJ4",      "Monstre Donjon 4");
        ROOM_COLORS.put("MJ5",      "#880E4F"); ROOM_LABELS.put("MJ5",      "Monstre Donjon 5");
        ROOM_COLORS.put("PuitDJ",   "#1565C0"); ROOM_LABELS.put("PuitDJ",   "Puit Donjon");
        ROOM_COLORS.put("Jardin",   "#66BB6A"); ROOM_LABELS.put("Jardin",   "Jardin");
        ROOM_COLORS.put("Lootdj3",  "#FFB300"); ROOM_LABELS.put("Lootdj3",  "Loot Donjon 3");
        ROOM_COLORS.put("Statue",   "#A1887F"); ROOM_LABELS.put("Statue",   "Statue");
        ROOM_COLORS.put("Centrale", "#1B5E20"); ROOM_LABELS.put("Centrale", "Centrale");
        ROOM_COLORS.put("MarchandNoir", "#FF8C00"); ROOM_LABELS.put("MarchandNoir", "Marchand Noir");
        ROOM_COLORS.put("Chapelle1", "#D4A574"); ROOM_LABELS.put("Chapelle1", "Chapelle Entree");
        ROOM_COLORS.put("Chapelle2", "#D4A574"); ROOM_LABELS.put("Chapelle2", "Chapelle Fond");
        ROOM_COLORS.put("Crypte1", "#5D4037"); ROOM_LABELS.put("Crypte1", "Crypte Entree");
        ROOM_COLORS.put("Crypte2", "#4E342E"); ROOM_LABELS.put("Crypte2", "Crypte Fond");
        ROOM_COLORS.put("PrisonC1", "#B71C1C"); ROOM_LABELS.put("PrisonC1", "Prison Centrale 1");
        ROOM_COLORS.put("PrisonC2", "#C62828"); ROOM_LABELS.put("PrisonC2", "Prison Centrale 2");
        ROOM_COLORS.put("PrisonC3", "#C62828"); ROOM_LABELS.put("PrisonC3", "Prison Centrale 3");
        ROOM_COLORS.put("PrisonC4", "#D32F2F"); ROOM_LABELS.put("PrisonC4", "Prison Centrale 4");
        ROOM_COLORS.put("PorteGob", "#8D6E63"); ROOM_LABELS.put("PorteGob", "Porte Gobelin");
        ROOM_COLORS.put("CG1", "#A1887F"); ROOM_LABELS.put("CG1", "Couloir Gobelin");
        ROOM_COLORS.put("GI2", "#7E57C2"); ROOM_LABELS.put("GI2", "Intersec Gobelin");
        ROOM_COLORS.put("GI3", "#7E57C2"); ROOM_LABELS.put("GI3", "Intersec Gobelin");
        ROOM_COLORS.put("GI4", "#7E57C2"); ROOM_LABELS.put("GI4", "Intersec Gobelin");
        ROOM_COLORS.put("PuitG", "#1565C0"); ROOM_LABELS.put("PuitG", "Puit Gobelin");
        ROOM_COLORS.put("MarchG", "#FF8A65"); ROOM_LABELS.put("MarchG", "Marchand Gobelin");
        ROOM_COLORS.put("TresorG", "#FFD700"); ROOM_LABELS.put("TresorG", "Trésorerie");
        ROOM_COLORS.put("ArmG", "#B71C1C"); ROOM_LABELS.put("ArmG", "Armurerie Gobelin");
        ROOM_COLORS.put("CDG", "#616161"); ROOM_LABELS.put("CDG", "Cul Sac Gobelin");
        ROOM_COLORS.put("MG1", "#8BC34A"); ROOM_LABELS.put("MG1", "Maison Gobelin");
        ROOM_COLORS.put("MG2", "#8BC34A"); ROOM_LABELS.put("MG2", "Maison Gobelin");
        ROOM_COLORS.put("MG3", "#8BC34A"); ROOM_LABELS.put("MG3", "Maison Gobelin");
    }

    private static String getShortLabel(String type) {
        String name = ROOM_LABELS.get(type);
        if (name == null) return type;
        return name.length() > 5 ? name.substring(0, 5) : name;
    }

    // ===================== HTML rendering =====================

    public static void renderToHtml(Map<String, Set<String>> adj, Map<String, String> labels,
                                     String startKey, long seed, String filePath) throws IOException {
        renderToHtml(adj, labels, null, null, startKey, seed, filePath);
    }

    public static void renderToHtml(Map<String, Set<String>> adj, Map<String, String> labels,
                                     Map<String, Set<String>> p4Adj,
                                     Map<String, String> topLabels,
                                     String startKey, long seed, String filePath) throws IOException {
        if (labels == null || labels.isEmpty()) return;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (String key : labels.keySet()) {
            String[] p = key.split(",");
            int x = Integer.parseInt(p[0]), z = Integer.parseInt(p[1]);
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
        }
        int gridW = maxX - minX + 1, gridH = maxZ - minZ + 1;
        int cellSize = 80, cellGap = 3;
        int svgW = gridW * cellSize + 40, svgH = gridH * cellSize + 40;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(svgW)
           .append("' height='").append(svgH).append("'>\n");
        svg.append("  <rect width='100%' height='100%' fill='#1a1a2e'/>\n");

        Set<String> drawn = new HashSet<>();
        for (var entry : adj.entrySet()) {
            String k1 = entry.getKey();
            if (!labels.containsKey(k1)) continue;
            String[] p1 = k1.split(",");
            int x1 = Integer.parseInt(p1[0]) - minX, z1 = Integer.parseInt(p1[1]) - minZ;
            int cx1 = x1 * cellSize + cellSize / 2 + 20, cy1 = z1 * cellSize + cellSize / 2 + 20;
            for (String k2 : entry.getValue()) {
                if (!labels.containsKey(k2)) continue;
                String edge = k1.compareTo(k2) < 0 ? k1 + "|" + k2 : k2 + "|" + k1;
                if (drawn.contains(edge)) continue;
                drawn.add(edge);
                String[] p2 = k2.split(",");
                int x2 = Integer.parseInt(p2[0]) - minX, z2 = Integer.parseInt(p2[1]) - minZ;
                int cx2 = x2 * cellSize + cellSize / 2 + 20, cy2 = z2 * cellSize + cellSize / 2 + 20;
                svg.append("  <line x1='").append(cx1).append("' y1='").append(cy1)
                   .append("' x2='").append(cx2).append("' y2='").append(cy2)
                   .append("' stroke='#444' stroke-width='10' stroke-linecap='round'/>\n");
            }
        }

        for (var entry : labels.entrySet()) {
            String key = entry.getKey(), type = entry.getValue();
            String[] p = key.split(",");
            int gx = Integer.parseInt(p[0]) - minX, gz = Integer.parseInt(p[1]) - minZ;
            int x = gx * cellSize + 20 + cellGap;
            int z = gz * cellSize + 20 + cellGap;
            int mult = type.equals("Centrale") ? 2 : 1;
            int w = cellSize * mult - cellGap * 2;
            String color = ROOM_COLORS.getOrDefault(type, "#555");
            String label = getShortLabel(type);
            boolean isStart = key.equals(startKey);

            String tooltip = escapeHtml(type) + " - " + escapeHtml(ROOM_LABELS.getOrDefault(type, type)) + " (" + key + ")";
            svg.append("  <g><title>").append(tooltip).append("</title>")
               .append("<rect x='").append(x).append("' y='").append(z)
               .append("' width='").append(w).append("' height='").append(w)
               .append("' fill='").append(color).append("' rx='4'")
               .append(" stroke='").append(isStart ? "#FFD600" : "#333")
               .append("' stroke-width='").append(isStart ? "3" : "1").append("'/>")
               .append("<text x='").append(x + w / 2).append("' y='").append(z + w / 2 + 3)
               .append("' text-anchor='middle' font-size='").append(mult > 1 ? "12" : "8").append("' fill='#fff'")
               .append(" font-family='sans-serif' font-weight='bold' pointer-events='none'>")
               .append(escapeHtml(mult > 1 ? "Centrale" : label)).append("</text></g>\n");
        }
        svg.append("</svg>");

        // SVG pour l'etage 1 (topLabels) - meme style que l'etage 0
        StringBuilder svgTop = new StringBuilder();
        if (topLabels != null && !topLabels.isEmpty()) {

            int tMinX = Integer.MAX_VALUE, tMaxX = Integer.MIN_VALUE;
            int tMinZ = Integer.MAX_VALUE, tMaxZ = Integer.MIN_VALUE;
            for (String key : topLabels.keySet()) {
                String[] p = key.split(",");
                int x = Integer.parseInt(p[0]), z = Integer.parseInt(p[1]);
                if (x < tMinX) tMinX = x; if (x > tMaxX) tMaxX = x;
                if (z < tMinZ) tMinZ = z; if (z > tMaxZ) tMaxZ = z;
            }
            for (String type : topLabels.values()) {
                if (type.equals("Centrale")) { tMaxX++; tMaxZ++; break; }
            }
            int tW = tMaxX - tMinX + 1, tH = tMaxZ - tMinZ + 1;
            int tSvgW = tW * cellSize + 40, tSvgH = tH * cellSize + 40;
            svgTop.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(tSvgW)
                   .append("' height='").append(tSvgH).append("'>\n");
            svgTop.append("  <rect width='100%' height='100%' fill='#1a1a2e'/>\n");
            // Connections : utilise l'adj du P4 si dispo, sinon l'adj principal filtre par topLabels
            Set<String> tDrawn = new HashSet<>();
            Map<String, Set<String>> connAdj = (p4Adj != null) ? p4Adj : adj;
            for (var entry : connAdj.entrySet()) {
                String k1 = entry.getKey();
                if (!topLabels.containsKey(k1)) continue;
                String[] p1 = k1.split(",");
                int gx1 = Integer.parseInt(p1[0]) - tMinX, gz1 = Integer.parseInt(p1[1]) - tMinZ;
                double cx1, cy1;
                if (topLabels.get(k1).equals("Centrale")) {
                    cx1 = (gx1 + 0.5) * cellSize + 20 + cellSize / 2.0;
                    cy1 = (gz1 + 0.5) * cellSize + 20 + cellSize / 2.0;
                } else {
                    cx1 = gx1 * cellSize + cellSize / 2.0 + 20;
                    cy1 = gz1 * cellSize + cellSize / 2.0 + 20;
                }
                for (String k2 : entry.getValue()) {
                    if (!topLabels.containsKey(k2)) continue;
                    String edge = k1.compareTo(k2) < 0 ? k1 + "|" + k2 : k2 + "|" + k1;
                    if (tDrawn.contains(edge)) continue;
                    tDrawn.add(edge);
                    String[] p2 = k2.split(",");
                    int gx2 = Integer.parseInt(p2[0]) - tMinX, gz2 = Integer.parseInt(p2[1]) - tMinZ;
                    double cx2, cy2;
                    if (topLabels.get(k2).equals("Centrale")) {
                        cx2 = (gx2 + 0.5) * cellSize + 20 + cellSize / 2.0;
                        cy2 = (gz2 + 0.5) * cellSize + 20 + cellSize / 2.0;
                    } else {
                        cx2 = gx2 * cellSize + cellSize / 2.0 + 20;
                        cy2 = gz2 * cellSize + cellSize / 2.0 + 20;
                    }
                    svgTop.append("  <line x1='").append((int)Math.round(cx1)).append("' y1='").append((int)Math.round(cy1))
                           .append("' x2='").append((int)Math.round(cx2)).append("' y2='").append((int)Math.round(cy2))
                           .append("' stroke='#444' stroke-width='10' stroke-linecap='round'/>\n");
                }
            }
            // Salles - meme style que l'etage 0
            for (var entry : topLabels.entrySet()) {
                String key = entry.getKey(), type = entry.getValue();
                String[] p = key.split(",");
                int gx = Integer.parseInt(p[0]) - tMinX, gz = Integer.parseInt(p[1]) - tMinZ;
                int x = gx * cellSize + 20 + cellGap;
                int z = gz * cellSize + 20 + cellGap;
                int mult = type.equals("Centrale") ? 2 : 1;
                int w = cellSize * mult - cellGap * 2;
                String color = ROOM_COLORS.getOrDefault(type, "#555");
                String label = getShortLabel(type);
                String tooltip = escapeHtml(type) + " - " + escapeHtml(ROOM_LABELS.getOrDefault(type, type)) + " (" + key + ")";
                svgTop.append("  <g><title>").append(tooltip).append("</title>")
                       .append("<rect x='").append(x).append("' y='").append(z)
                       .append("' width='").append(w).append("' height='").append(w)
                       .append("' fill='").append(color).append("' rx='4'")
                       .append(" stroke='#333' stroke-width='1'/>")
                       .append("<text x='").append(x + w / 2).append("' y='").append(z + w / 2 + 3)
                       .append("' text-anchor='middle' font-size='").append(mult > 1 ? "12" : "8").append("' fill='#fff'")
                       .append(" font-family='sans-serif' font-weight='bold' pointer-events='none'>")
                       .append(escapeHtml(mult > 1 ? "Centrale" : label)).append("</text></g>\n");
            }
            svgTop.append("</svg>");
        }

        StringBuilder legend = new StringBuilder();
        Set<String> seenColors = new LinkedHashSet<>();
        for (var entry : ROOM_COLORS.entrySet()) {
            String color = entry.getValue();
            String legKey = color + "|" + ROOM_LABELS.getOrDefault(entry.getKey(), entry.getKey());
            if (seenColors.contains(legKey)) continue;
            seenColors.add(legKey);
            legend.append("<tr><td style='background:").append(color)
                   .append(";width:16px;height:16px;border-radius:3px'></td>")
                   .append("<td style='color:#ccc;padding:2px 8px'>")
                   .append(escapeHtml(ROOM_LABELS.getOrDefault(entry.getKey(), entry.getKey()))).append("</td></tr>\n");
        }

        Map<String, Integer> counts = new HashMap<>();
        for (String v : labels.values()) counts.merge(v, 1, Integer::sum);
        StringBuilder sbCounts = new StringBuilder();
        for (var e : counts.entrySet()) {
            String name = ROOM_LABELS.getOrDefault(e.getKey(), e.getKey());
            sbCounts.append("<span style='margin:0 8px;color:#aaa'>").append(escapeHtml(name))
                    .append(": <b>").append(e.getValue()).append("</b></span>");
        }

        String html = "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>"
            + "<title>Donjon - Seed " + seed + "</title>"
            + "<style>"
            + "body{background:#0d0d1a;font-family:sans-serif;margin:20px;color:#eee}"
            + "h2{color:#FFD600;margin-bottom:5px}"
            + ".seed{color:#888;font-size:14px;margin-bottom:15px}"
            + ".map{background:#1a1a2e;display:inline-block;padding:10px;border-radius:8px}"
            + "table.legend{display:inline-block;vertical-align:top;margin-left:30px;border-collapse:collapse}"
            + "table.legend td{padding:2px 4px;font-size:13px}"
            + ".counts{margin:15px 0;line-height:1.8}"
            + "</style></head><body>"
            + "<h2>&#9879; Donjon - Visualisation</h2>"
            + "<div class='seed'>Seed: <b>" + seed + "</b> | Salles: <b>" + labels.size() + "</b>"
            + " | Dimensions: " + gridW + "x" + gridH + "</div>"
            + "<div class='counts'>" + sbCounts + "</div>"
            + "<div style='margin-top:15px'>"
            + "<button onclick=\"showFlr(0)\" id='btn0' style='background:#FFD600;padding:5px 15px;border:none;border-radius:3px;cursor:pointer;font-weight:bold'>Etage 0</button> "
            + "<button onclick=\"showFlr(1)\" id='btn1' style='background:#555;color:#fff;padding:5px 15px;border:none;border-radius:3px;cursor:pointer'>Etage 1</button>"
            + "<div id='flr0' class='map' style='margin-top:10px'>" + svg + "</div>"
            + "<div id='flr1' class='map' style='margin-top:10px;display:none'>" + (topLabels != null ? svgTop : "") + "</div>"
            + "<table class='legend'><tr><th colspan='2' style='color:#FFD600;text-align:left;padding-bottom:6px'>Legende</th></tr>"
            + legend + "</table>"
            + "</div>"
            + "<script>function showFlr(n){document.getElementById('flr0').style.display=n==0?'':'none';document.getElementById('flr1').style.display=n==1?'':'none';document.getElementById('btn0').style.background=n==0?'#FFD600':'#555';document.getElementById('btn1').style.background=n==1?'#FFD600':'#555'}</script>"
            + "</body></html>";

        Files.writeString(Path.of(filePath), html);
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ===================== Main =====================

    public static void main(String[] args) {
        long seed = 0;
        String output = "dungeon_viz.html";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-o") && i + 1 < args.length) output = args[i + 1];
            else if (args[i].equals("-s") && i + 1 < args.length) seed = Long.parseLong(args[i + 1]);
            else if (seed == 0 && Character.isDigit(args[i].charAt(0))) seed = Long.parseLong(args[i]);
        }

        int attempts = 0;
        while (true) {
            attempts++;
            DungeonAlgo.DungeonResult result = DungeonAlgo.generateDungeon(seed);
            if (result != null) {
                try {
                    renderToHtml(result.adj, result.labels, result.p4Adj, result.topLabels, result.startKey, DungeonAlgo.getLastSeed(), output);
                    System.out.println("Dungeon genere avec la seed " + DungeonAlgo.getLastSeed() + " (tentative " + attempts + ")");
                    System.out.println("Salles: " + result.labels.size());
                    System.out.println("Visualisation: " + Path.of(output).toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Erreur ecriture: " + e.getMessage());
                }
                break;
            }
            if (seed != 0) {
                System.out.println("Seed " + seed + " invalide, essai avec seed aleatoire");
                seed = 0;
            }
        }
    }
}
