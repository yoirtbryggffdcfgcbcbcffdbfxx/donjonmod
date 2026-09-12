package com.dungeonmod.debug;

import com.dungeonmod.debug.DungeonAlgo.Point;
import com.dungeonmod.debug.RoomType.Size3D;

import java.util.*;

/**
 * REGISTRE D'OCCUPATION 3D, partage entre les phases de generation.
 *
 * Une phase peut y reserver un volume (empreinte XZ x couches) que les phases
 * suivantes doivent respecter, y compris sur d'autres couches. Aujourd'hui le
 * hub multi-couches (Centrale 2x2x2) y est enregistre ; une future salle 2x12x4
 * le sera nativement via {@link DungeonRoomPlacement#volume}.
 *
 * Note : les phases au sol (P1-P3) utilisent deja l'adjacence comme occupation
 * implicite ; ce registre sert surtout a la protection INTER-COUCHES (P4).
 */
final class DungeonOccupancy {
    private final Set<Point> reserved = new HashSet<>();

    void reserve(Collection<Point> cells) { if (cells != null) reserved.addAll(cells); }

    /** Reserve tout le volume 3D d'une salle (empreinte x size.y couches). */
    void reserve(Point anchor, Size3D size) { reserve(DungeonRoomPlacement.volume(anchor, size)); }

    boolean contains(Point p) { return reserved.contains(p); }

    /** Vrai si l'empreinte XZ de la salle (couche de l'ancre) est libre. */
    boolean isFootprintFree(Point anchor, Size3D size) {
        for (Point p : DungeonRoomPlacement.footprint(anchor, size)) if (reserved.contains(p)) return false;
        return true;
    }

    /** Cellules reservees (lecture seule), utilisables comme `blocked` par les phases. */
    Set<Point> cells() { return Collections.unmodifiableSet(reserved); }

    int size() { return reserved.size(); }
}
