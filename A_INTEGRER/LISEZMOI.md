# 📥 A_INTEGRER — Zone de dépôt pour l'agent (Arena)

Dépose ici tes fichiers bruts (textures, modèles de boss…), puis dis à l'agent en chat :
« **j'ai déposé X dans A_INTEGRER, intègre-le** ».
L'agent lit ce dossier, place chaque fichier au bon endroit dans le mod, crée les fichiers
JSON / models / code nécessaires, puis **archive ce qu'il a intégré dans `_integre/`**.

## Règles pour que tout se passe bien

1. **Noms en minuscules, snake_case** : `casque_givre.png`, `cerbere_animation.json` ✅
   (pas d'espaces, pas d'accents, pas de majuscules)
2. **Un fichier = un nom parlant** : le nom du fichier sert d'identifiant (`id`) dans le mod.
3. **Dis ce que tu veux dans le chat** : le fichier seul ne dit pas tout !
   Exemple : « j'ai mis `casque_givre.png` dans `items/` → en faire un casque +2 armure ».

## Les sous-dossiers

| Dossier | Quoi y déposer | Format attendu |
|---|---|---|
| `items/` | Icônes d'items (main, casque, nourriture…) | PNG **16×16** |
| `armures/` | Textures portées des armures | PNG **64×64** (calque vanilla) |
| `boss/` | 1 sous-dossier par boss : `boss/cerbere/` | `cerbere_geo.json`, `cerbere_animation.json`, texture PNG **64×64** (Blockbench/GeckoLib) |
| `divers/` | Structures `.nbt`, sons, GUI, tout le reste | — |

## Ce que fait l'agent après ton dépôt

- déplace les fichiers vers les bons chemins (`assets/dungeonmod/textures/…`, `geckolib/…`…) ;
- crée le JSON d'equipment / item-model / définition d'item si besoin ;
- met à jour le code (`ModItems.java`, entité…) si tu le demandes ;
- archive ta source dans `_integre/` avec la date pour traçabilité ;
- te dit exactement quoi tester en jeu après ton `Pull`.

> 💡 Le dossier `_integre/` est géré par l'agent — n'y touche pas.
