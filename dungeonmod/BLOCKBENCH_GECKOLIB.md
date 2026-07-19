# Pipeline Blockbench → GeckoLib

## Principe
Ce projet utilise **GeckoLib** pour charger des modèles 3D créés dans **Blockbench** et les animer sur des entités customs (mobs).  
Le fichier source est un `.bbmodel` (Blockbench). Un script Python le convertit en fichiers utilisables par GeckoLib.

---

## Fichiers générés pour chaque modèle

| Fichier | Rôle |
|---------|------|
| `geo/<nom>.geo.json` | Géométrie 3D du modèle (os + cubes + UV par face) |
| `animations/<nom>.animation.json` | Animations (walk, pose, attaque, etc.) |
| `textures/entity/<texture>.png` | Texture extraite du .bbmodel |

---

## Utilisation

```bash
python .opencode/convert_new_model.py \
  src/main/resources/assets/dungeonmod/textures/models/modele.bbmodel \
  nom_du_modele
```

Exemple réel :
```bash
python .opencode/convert_new_model.py \
  "src/main/resources/assets/dungeonmod/textures/models/animation_de_lancer_de_pierre_cyclop.bbmodel" \
  cyclops
```

---

## Structure Java à créer pour chaque modèle

### 1. Entity — `OgreEntity.java`
- Étend `PathAwareEntity` et implémente `GeoEntity`
- Enregistre le `EntityType` (dimensions, tracking range, etc.)
- Définit les animations dans `registerControllers()` :
  ```java
  registrar.add(new AnimationController<>(this, "walk", 10, state -> {
      double hSq = this.getVelocity().x * this.getVelocity().x + this.getVelocity().z * this.getVelocity().z;
      if (hSq > 0.0001 || state.isMoving())
          return state.setAndContinue(RawAnimation.begin().thenLoop("animation.<nom>.walk"));
      return PlayState.STOP;
  }));
  ```
- Définit les attributs (HP, dégâts, vitesse) dans `registerAttributes()`
- Définit les IA goals dans `initGoals()`

### 2. Model — `OgreModel.java`
- Étend `GeoModel<OgreEntity>`
- Pointe vers `geo/<nom>.geo.json`, `textures/entity/<texture>.png`, `animations/<nom>.animation.json`

### 3. Renderer — `OgreRenderer.java`
- Étend `GeoEntityRenderer<OgreEntity>`
- Passe le model au super constructeur
- Optionnel : `withScale(<taille>)` pour agrandir/rétrécir

---

## Règles ABSOLUES (NE JAMAIS MODIFIER)

### Structure du `.geo.json`
Les faces UV doivent être encapsulées dans un objet `"uv"` :
```json
{
  "origin": [x, y, z],
  "size": [w, h, d],
  "uv": {
    "north": { "uv": [u, v], "uv_size": [w, h] },
    "east":  { "uv": [u, v], "uv_size": [w, h] },
    "south": { "uv": [u, v], "uv_size": [w, h] },
    "west":  { "uv": [u, v], "uv_size": [w, h] },
    "up":    { "uv": [u, v], "uv_size": [w, h] },
    "down":  { "uv": [u, v], "uv_size": [w, h] }
  }
}
```
→ **Les directions ne doivent JAMAIS être à la racine du cube.** Elles doivent TOUJOURS être dans `"uv": { ... }`.  
→ Le champ s'appelle `uv_size` (snake_case), pas `uvSize`.  
→ Un cube sans per-face UV utilise `"uv": [u, v]` directement.

### Format des animations
- `format_version`: `"1.8.0"`
- Structure : `"animations": { "animation.<nom>.<anim>": { "loop": true/false, "animation_length": ..., "bones": {...} } }`
- Les noms d'os dans l'animation DOIVENT correspondre exactement aux noms d'os dans le `.geo.json`.

### Nom des fichiers
- Les textures ne doivent JAMAIS contenir d'espaces → remplacer par `_`.
- Les identifiants Minecraft n'acceptent que `[a-z0-9/._-]`.

### Dépendances
- `build.gradle` : ajouter le maven `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/` et la dépendance `software.bernie.geckolib:geckolib-fabric-1.21.4:4.8.5`.
- `fabric.mod.json` : ajouter `"geckolib": ">=4.8.5"` dans `depends`.
- `gradle.properties` : utiliser `fabric_version=0.119.4+1.21.4` minimum (bug de race condition corrigé).

---

## Script de conversion (`convert_new_model.py`)
Situé dans `.opencode/convert_new_model.py`.  
**NE PAS MODIFIER** sa logique de génération UV (structure, noms de champs, encapsulation).  
Seule la partie extraction texture (noms de fichiers) peut être adaptée.

Le script :
1. Extrait la texture embedded du .bbmodel
2. Lit les éléments (cubes) et les animations
3. Génère le `.geo.json` avec les UV par face encapsulés dans `"uv": { ... }`
4. Génère le `.animation.json` avec toutes les animations du .bbmodel
5. Assainit les noms de fichiers (espaces → underscores)

---

## Dépannage

| Problème | Cause | Solution |
|----------|-------|----------|
| Freeze au démarrage | Race condition Fabric API | Mettre à jour Fabric API ≥ 0.119.4 |
| `InvalidIdentifierException` | Espace dans le nom de texture | Remplacer par `_` |
| Texture blanche/décalée | UV par face mal formatés | Vérifier `"uv": {...}` encapsulé correctement |
| Animation non jouée | Nom d'os différent entre geo et animation | Vérifier que les noms correspondent |
| Modèle invisible | Fichier geo.json manquant ou mauvais chemin | Vérifier `OgreModel.java` |
