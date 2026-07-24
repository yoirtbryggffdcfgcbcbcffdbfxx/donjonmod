package com.dungeonmod.client.dialogue;

import java.util.List;

public class BarmanDialogue {
    public static final List<String> FIRST_MEETING = List.of(
        "Oh ! Un nouveau visage. Ça faisait longtemps que personne",
        "n'était revenu vivant du donjon.", 
        "Installe-toi si tu veux reprendre ton souffle.",
        "Ici, les monstres restent dehors... enfin, en principe."
    );
    public static final List<String> STANDARD_PROMPT = List.of(
        "Alors, qu'est-ce que je te sers ?"
    );
    public static final List<String> PURCHASE_SINGLE = List.of(
        "Une bière bien fraîche! Fais attention à ne pas tout boire avant le prochain combat..."
    );
    public static final List<String> PURCHASE_MULTIPLE = List.of(
        "Des bières bien fraiches! Fais attention à ne pas tout boire avant le prochain combat..."
    );
    public static final List<String> NOT_ENOUGH_DENIERS = List.of(
        "Désolée... ici les tonneaux ne se remplissent pas tout seuls. Reviens quand tu auras des sous."
    );
    public static final List<String> RETURN_1 = List.of(
        "Ah, te voilà encore. Je commençais à croire que le donjon avait enfin eu raison de toi."
    );
    public static final List<String> RETURN_2 = List.of(
        "Tu finis presque par être un habitué. C'est plutôt bon signe."
    );
}
