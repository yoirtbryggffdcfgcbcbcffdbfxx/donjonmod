package com.dungeonmod.client.dialogue;

import java.util.List;

public class GaspardDialogue {
    public static final List<String> FIRST_MEETING = List.of(
        "Écoute attentivement, minuscule voyageur.",
        "Je parcours ces cavernes depuis bien plus longtemps que toi", 
        "alors évite de me regarder comme si tu avais quelque chose à m'apprendre."
    );
    public static final List<String> STANDARD_PROMPT = List.of(
        "Si tu veux des conseils, je peux t'en donner. Mais avant ça, offre-moi une bière gamin."
    );

    public static final List<String> REFUSAL_BIERE_PERIMEE = List.of(
        "Pour qui me prends-tu, petit idiot ?",
        "Je ne bois pas une bière qui a déjà dépassé sa propre date de survie."
    );

    public static final List<List<String>> CONSEILS = List.of(
        List.of(
            "...",
            "...",
            "... Ah !",
            "Voilà un conseil qui pourrait t'éviter une mort ridicule. Au détour d'un couloir, tu risques de croiser le Cyclope.",
            "Sache que ce n'est pas une créature si dangereuse. Il possède un seul œil, tandis que toi tu en possèdes deux.",
            "Cela signifie que tu as deux fois plus de chances de voir venir ses attaques.",
            "Ne me remercie pas. Ce genre de réflexion demande un certain talent.",
            "Adieu."
        ),
        List.of(
            "...",
            "...",
            "... Ah !",
            "Voilà quelque chose d'intéressant. Certains aventuriers racontent que ces cavernes cachent un temple oublié dans lequel repose un ange.",
            "Si tu viens à le rencontrer, fuis.",
            "Ce qu'il garde est probablement une chose que le monde aurait mieux fait de ne jamais connaître.",
            "Ne me remercie pas, gamin.",
            "Adieu."
        ),
        List.of(
            "...",
            "...",
            "... Ah !",
            "Voilà un renseignement qui pourrait t'être utile. Tu connais sûrement la légende de la Fontaine de Jouvence.",
            "Certains prétendent que son eau coule quelque part dans ces grottes.",
            "Elle promet la jeunesse éternelle.",
            "Mais retiens bien ceci : tu n'es pas le seul à chercher ce qui possède de la valeur.",
            "Si tu rencontres une créature dans ces profondeurs, ne lui parle jamais comme à une amie.",
            "Les monstres sont rarement assez honnêtes pour prévenir avant de te dévorer.",
            "Ne me remercie pas, gamin.",
            "Adieu."
        )
    );
}
