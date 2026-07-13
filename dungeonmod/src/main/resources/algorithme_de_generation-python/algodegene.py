import pygame
import random
import sys

# Configuration de la grille logique du donjon
GRID_SIZE = 16  
MARGIN = 40
LEGEND_HEIGHT = 160  # Espace réservé en bas pour l'affichage des informations

# Codes couleur et labels d'affichage (M1 = impasse, M2 = passage rectiligne)
ROOM_STYLES = {
    'D':        ((0, 153, 255), "D"),       # Départ (Impasse de départ)
    'C':        ((110, 110, 110), "C"),     # Couloir (ligne droite)
    'I2':       ((204, 153, 0), "I2"),      # Virage (2 voies, crée le labyrinthe)
    'I3':       ((230, 92, 0), "I3"),       # Intersection à 3 voies (limité à 3 maximum)
    'M1':       ((255, 51, 51), "M1"),      # Mob en cul-de-sac (sans passage)
    'M2':       ((255, 128, 0), "M2"),      # Mob de garde (passage rectiligne)
    'Prison':   ((153, 51, 153), "Pris"),   # Prison (toujours unique et cul-de-sac)
    'loot':     ((255, 215, 0), "Loot"),    # Trésor (au minimum 1 garanti)
    'cul':      ((60, 60, 60), "Cul"),       # Cul-de-sac vide
    'porte':    ((0, 204, 102), "Port")     # Sortie / Porte (impasse)
}

def generate_raw_tree():
    """Génère un arbre aléatoire de taille >= 20 en limitant strictement les intersections I3 à 3."""
    start_pos = (GRID_SIZE // 2, GRID_SIZE // 2)
    visited = {start_pos}
    adj = {start_pos: set()}
    all_nodes = [start_pos]
    
    # On force la salle de départ à n'avoir qu'une seule connexion sortante (degré 1)
    dx, dy = random.choice([(-1,0), (1,0), (0,-1), (0,1)])
    first_child = (start_pos[0] + dx, start_pos[1] + dy)
    
    visited.add(first_child)
    all_nodes.append(first_child)
    adj[first_child] = set()
    adj[start_pos].add(first_child)
    adj[first_child].add(start_pos)
    
    target_size = random.randint(21, 25)
    
    # Un maximum de 3 intersections de type I3 dans tout le donjon pour un effet labyrinthe
    MAX_I3 = 3 
    
    while len(all_nodes) < target_size:
        candidates = []
        
        # Compter les intersections I3 actuellement présentes dans le graphe
        current_I3_count = sum(1 for node, neighbors in adj.items() if len(neighbors) == 3)
        
        for node in all_nodes:
            # Le Départ (D) est déjà connecté et doit rester de degré 1 (impasse de départ)
            if node == start_pos:
                continue
                
            # Si le parent a déjà 2 connexions, lui en rajouter une 3ème va créer un I3.
            # On ne l'autorise que si on n'a pas dépassé la limite de MAX_I3.
            if len(adj[node]) == 2:
                if current_I3_count >= MAX_I3:
                    continue
            elif len(adj[node]) >= 3:
                continue  # Pas d'I4 (limite absolue à 3 raccordements)
                
            x, y = node
            for ndx, ndy in [(-1,0), (1,0), (0,-1), (0,1)]:
                nx, ny = x + ndx, y + ndy
                if 0 <= nx < GRID_SIZE and 0 <= ny < GRID_SIZE:
                    if (nx, ny) not in visited:
                        candidates.append((node, (nx, ny)))
        if not candidates:
            break
        
        parent, child = random.choice(candidates)
        visited.add(child)
        all_nodes.append(child)
        adj[child] = set()
        adj[parent].add(child)
        adj[child].add(parent)
        
    return start_pos, adj

def analyze_and_label(start_pos, adj):
    """Analyse la structure et applique l'étiquetage des salles."""
    labels = {}
    
    # 1. Identifier les culs-de-sac (degré 1). On exclut le départ.
    leaves = [node for node, neighbors in adj.items() if len(neighbors) == 1 and node != start_pos]
    if len(leaves) < 4:
        return None  # Rejeter pour s'assurer d'avoir au moins 4 impasses
    
    random.shuffle(leaves)
    
    # Règle : La Prison (impasse unique) doit être derrière un M2 (colinéaire / sans tourner)
    # On cherche un candidat Prison dont le voisin (M2) forme une ligne droite avec sa propre entrée
    prison_info = None
    for leaf in leaves:
        parent = list(adj[leaf])[0]
        # Le parent doit être de degré 2 pour agir comme passage rectiligne
        if len(adj[parent]) == 2:
            grandparent = list(adj[parent] - {leaf})[0]
            # Vecteur 1 (grand-parent -> parent) et Vecteur 2 (parent -> prison)
            dx1, dy1 = parent[0] - grandparent[0], parent[1] - grandparent[1]
            dx2, dy2 = leaf[0] - parent[0], leaf[1] - parent[1]
            # Si les deux directions sont identiques (colinéarité / alignement droit)
            if dx1 == dx2 and dy1 == dy2:
                prison_info = (leaf, parent)
                break
                
    if prison_info is None:
        return None  # Rejeter si la configuration rectiligne de la prison n'est pas possible
        
    prison, prison_neighbor = prison_info
    
    # Séparer les autres feuilles pour distribuer les rôles restants
    other_leaves = [l for l in leaves if l != prison]
    porte = other_leaves[0]
    loot_garanti = other_leaves[1]  # On garantit au moins 1 salle de butin (loot)
    
    labels[start_pos] = 'D'
    labels[porte] = 'porte'
    labels[prison] = 'Prison'
    labels[prison_neighbor] = 'M2'  # Premier monstre (garde de la prison, déjà compté)
    labels[loot_garanti] = 'loot'
    
    # Gestion stricte du nombre total de monstres : tiré aléatoirement entre 2 et 3 mobs au total
    target_mobs = random.choice([2, 3])
    extra_mobs_to_place = target_mobs - 1  # Puisqu'on a déjà le garde M2 de la prison
    
    # Déterminer combien de M1 (impasse) et de M2 (couloir) additionnels on va placer
    remaining_leaves = other_leaves[2:]  # Les feuilles restantes après porte et loot_garanti
    max_possible_m1 = len(remaining_leaves)
    
    m1_count = random.randint(0, min(extra_mobs_to_place, max_possible_m1))
    m2_count = extra_mobs_to_place - m1_count
    
    # Assigner les feuilles restantes (M1 optionnels, culs-de-sac vides ou loots additionnels)
    random.shuffle(remaining_leaves)
    for i, leaf in enumerate(remaining_leaves):
        if i < m1_count:
            labels[leaf] = 'M1'
        else:
            labels[leaf] = random.choice(['cul', 'loot'])
        
    # 2. Classifier les pièces intermédiaires restantes (degré 2 et 3)
    for node, neighbors in adj.items():
        if node in labels:
            continue
            
        deg = len(neighbors)
        if deg == 2:
            n1, n2 = list(neighbors)
            if n1[0] == n2[0] or n1[1] == n2[1]:
                labels[node] = 'C'
            else:
                labels[node] = 'I2'
        elif deg == 3:
            labels[node] = 'I3'
            
    # Transformer exactement m2_count couloirs droits 'C' restants en M2 (monstres de passage)
    c_nodes = [node for node, label in labels.items() if label == 'C']
    if len(c_nodes) < m2_count:
        return None  # Rejeter s'il n'y a pas assez de couloirs droits pour les M2 requis
        
    random.shuffle(c_nodes)
    for i in range(m2_count):
        labels[c_nodes[i]] = 'M2'
            
    # 3. Validation de la contrainte : pas plus de 2 couloirs 'C' consécutifs
    remaining_c_nodes = {node for node, lbl in labels.items() if lbl == 'C'}
    for c in remaining_c_nodes:
        c_neighbors = adj[c] & remaining_c_nodes
        for cn in c_neighbors:
            cn_neighbors = (adj[cn] & remaining_c_nodes) - {c}
            if cn_neighbors:
                return None  # Détecte C - C - C -> Rejet pour générer à nouveau
                
    return labels

def generate_valid_dungeon():
    """Génère un donjon jusqu'à obtenir un tracé valide."""
    for _ in range(5000):
        start_pos, adj = generate_raw_tree()
        labels = analyze_and_label(start_pos, adj)
        if labels is not None:
            return start_pos, adj, labels
    return None

def main():
    pygame.init()
    
    # Récupération de la taille de votre écran
    info = pygame.display.Info()
    SCREEN_WIDTH = info.current_w
    SCREEN_HEIGHT = info.current_h
    
    # Initialisation du mode Plein Écran
    screen = pygame.display.set_mode((SCREEN_WIDTH, SCREEN_HEIGHT), pygame.FULLSCREEN)
    pygame.display.set_caption("Visualisation de Donjons Minecraft (Labyrinthe)")
    clock = pygame.time.Clock()
    
    # Calcul dynamique de la taille des tuiles selon l'écran pour que le rendu s'adapte
    available_w = SCREEN_WIDTH - 2 * MARGIN
    available_h = SCREEN_HEIGHT - 2 * MARGIN - LEGEND_HEIGHT
    TILE_SIZE = min(available_w // GRID_SIZE, available_h // GRID_SIZE)
    
    # Ajustement de la taille des polices selon la résolution
    font_size = max(10, int(TILE_SIZE * 0.35))
    font = pygame.font.SysFont("Arial", font_size, bold=True)
    large_font_size = max(14, int(TILE_SIZE * 0.45))
    large_font = pygame.font.SysFont("Arial", large_font_size, bold=True)
    
    dungeon = generate_valid_dungeon()
    
    running = True
    while running:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    running = False
                elif event.key == pygame.K_SPACE:
                    dungeon = generate_valid_dungeon()
                    
        screen.fill((15, 15, 15))
        
        # Centrage du rendu au milieu de l'écran
        grid_offset_x = (SCREEN_WIDTH - GRID_SIZE * TILE_SIZE) // 2
        grid_offset_y = (SCREEN_HEIGHT - LEGEND_HEIGHT - GRID_SIZE * TILE_SIZE) // 2
        
        # Dessiner la grille de fond
        for x in range(GRID_SIZE):
            for y in range(GRID_SIZE):
                rect = pygame.Rect(grid_offset_x + x * TILE_SIZE, grid_offset_y + y * TILE_SIZE, TILE_SIZE, TILE_SIZE)
                pygame.draw.rect(screen, (28, 28, 28), rect, 1)
                
        if dungeon:
            start_pos, adj, labels = dungeon
            
            # Centrage individuel du donjon généré
            min_x = min(n[0] for n in adj.keys())
            max_x = max(n[0] for n in adj.keys())
            min_y = min(n[1] for n in adj.keys())
            max_y = max(n[1] for n in adj.keys())
            
            w_box = max_x - min_x + 1
            h_box = max_y - min_y + 1
            
            offset_x = (GRID_SIZE - w_box) // 2 - min_x
            offset_y = (GRID_SIZE - h_box) // 2 - min_y
            
            def to_screen(coord):
                sx = grid_offset_x + (coord[0] + offset_x) * TILE_SIZE + TILE_SIZE // 2
                sy = grid_offset_y + (coord[1] + offset_y) * TILE_SIZE + TILE_SIZE // 2
                return sx, sy
                
            # Dessiner les corridors de connexion
            for node, neighbors in adj.items():
                p1 = to_screen(node)
                for neighbor in neighbors:
                    p2 = to_screen(neighbor)
                    pygame.draw.line(screen, (150, 150, 150), p1, p2, max(3, int(TILE_SIZE * 0.12)))
                    
            # Dessiner les salles
            for node, label in labels.items():
                sx, sy = to_screen(node)
                color, text = ROOM_STYLES[label]
                
                room_rect = pygame.Rect(sx - TILE_SIZE // 2 + 3, sy - TILE_SIZE // 2 + 3, TILE_SIZE - 6, TILE_SIZE - 6)
                pygame.draw.rect(screen, color, room_rect, border_radius=4)
                pygame.draw.rect(screen, (240, 240, 240), room_rect, 1, border_radius=4)
                
                text_color = (255, 255, 255) if color[0] < 180 or color[1] < 180 else (0, 0, 0)
                text_surf = font.render(text, True, text_color)
                text_rect = text_surf.get_rect(center=(sx, sy))
                screen.blit(text_surf, text_rect)
                
        # Zone d'information et légendes
        y_info = SCREEN_HEIGHT - LEGEND_HEIGHT + 20
        inst_surf = large_font.render("ESPACE : Générer un nouveau modèle  |  ECHAP : Quitter le Plein Écran", True, (220, 220, 220))
        inst_rect = inst_surf.get_rect(center=(SCREEN_WIDTH // 2, y_info))
        screen.blit(inst_surf, inst_rect)
        
        # Alignement de la légende
        desc_y = y_info + 40
        col_w = int(SCREEN_WIDTH * 0.11)
        start_legend_x = (SCREEN_WIDTH - (len(ROOM_STYLES) * col_w)) // 2
        
        for idx, (lbl, (color, name)) in enumerate(ROOM_STYLES.items()):
            if lbl == 'M_trans': continue  # Ne pas dupliquer "M" dans la légende
            display_name = "M (Mob)" if lbl == 'M_dead' else name
            
            bx = start_legend_x + idx * col_w
            by = desc_y
            
            pygame.draw.rect(screen, color, (bx, by, 18, 18), border_radius=3)
            pygame.draw.rect(screen, (255, 255, 255), (bx, by, 18, 18), 1, border_radius=3)
            
            lbl_surf = font.render(display_name, True, (180, 180, 180))
            screen.blit(lbl_surf, (bx + 26, by + 2))
            
        pygame.display.flip()
        clock.tick(30)
        
    pygame.quit()
    sys.exit()

if __name__ == "__main__":
    main()