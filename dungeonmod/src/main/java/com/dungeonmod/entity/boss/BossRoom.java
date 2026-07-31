package com.dungeonmod.entity.boss;

/**
 * Zone rectangulaire (AABB 2D sur XZ) dans laquelle un boss est ancré.
 * Le boss reste dans cette salle, sa orientation par défaut y est figée,
 * et la visibilité de la bossbar dépend de l'appartenance du joueur à cette zone.
 *
 * @param minX coin X minimum (inclus)
 * @param maxX coin X maximum (inclus)
 * @param minZ coin Z minimum (inclus)
 * @param maxZ coin Z maximum (inclus)
 * @param facingYaw orientation par défaut en degrés (0 = sud, 90 = ouest, etc.)
 */
public record BossRoom(int minX, int maxX, int minZ, int maxZ, float facingYaw) {

    public boolean contains(int x, int z) {
        return x >= minX - 2 && x <= maxX + 2 && z >= minZ - 2 && z <= maxZ + 2;
    }

    public int centerX() { return (minX + maxX) / 2; }
    public int centerZ() { return (minZ + maxZ) / 2; }

    public boolean isDefined() { return maxX > 0; }
}
