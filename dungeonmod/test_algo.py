import os, subprocess, sys
from _env import java_command, pause

# Harnais de regression : echantillonne N donjons MODE JOUEUR (seed=0) et verifie
# automatiquement coherence labels <-> adjacence, connexite, garanties gameplay.
#
# IMPORTANT : on ne teste PAS la plage 1..N en entree. L'algo rejette deja les
# mauvaises tentatives en interne ; seules les seeds de SORTIE (dr.seed) sont
# livrees au joueur. C'est celles-la qu'on valide.
#
# Usage :
#   python test_algo.py [nb] [v]            # nb echantillons joueur (defaut 100)
#   python test_algo.py 50 v                # idem + detail par seed
#   python test_algo.py --seed 12345        # rejoue une seed de SORTIE precise
#   python test_algo.py --range 1           # DEBUG : ancienne plage sequentielle
#                                           # (ne reflete PAS ce que le joueur recoit)
#
# ATTENTION : utilise build/classes/java/main -> lancer clean_build.py d'abord !
base = os.path.dirname(__file__)
classes = os.path.join(base, "build", "classes", "java", "main")

if not os.path.isdir(classes):
    print("Pas de classes compilees ! Lance d'abord : python clean_build.py")
    pause()
    sys.exit(1)

cmd = [java_command(), "-cp", classes, "com.dungeonmod.debug.SeedHarness"]

# Parse args souple pour rester compatible avec l'ancienne CLI
#   ancienne : test_algo.py [nb] [seedDepart] [v]
#   nouvelle : test_algo.py [nb] [v]  |  --seed S  |  --range [start]
args = sys.argv[1:]
i = 0
positional = []
while i < len(args):
    a = args[i]
    if a in ("-v", "v", "--verbose"):
        cmd.append("-v")
    elif a in ("-n", "--n") and i + 1 < len(args):
        cmd += ["-n", args[i + 1]]; i += 1
    elif a in ("-s", "--seed", "-seed") and i + 1 < len(args):
        cmd += ["-seed", args[i + 1]]; i += 1
    elif a in ("--range", "-range"):
        cmd.append("-range")
        if i + 1 < len(args) and not args[i + 1].startswith("-") and args[i + 1] not in ("v",):
            # start optionnel
            try:
                int(args[i + 1])
                cmd.append(args[i + 1]); i += 1
            except ValueError:
                pass
    elif a.startswith("-"):
        print(f"Argument ignore : {a}")
    else:
        positional.append(a)
    i += 1

# Positionnels : 1er = nb, 2e (si present et numerique) etait seedDepart —
# on l'ignore volontairement (plus de plage 1..N par defaut). Pour forcer
# l'ancien comportement : --range <start>.
if positional:
    cmd += ["-n", str(positional[0])]
if len(positional) > 1:
    print(f"NOTE : seedDepart={positional[1]} ignoree (le harnais echantillonne")
    print("       des seeds JOUEUR via seed=0). Pour l'ancienne plage debug :")
    print(f"       python test_algo.py {positional[0]} --range {positional[1]}")
    print("       Pour rejouer une seed de SORTIE :")
    print(f"       python test_algo.py --seed {positional[1]}")

r = subprocess.run(cmd, capture_output=True, text=True)
print(r.stdout)
if r.stderr:
    print(r.stderr)

if r.returncode != 0:
    pause("ECHEC harnais (voir details ci-dessus). Appuie sur Entree pour fermer...")
    sys.exit(1)
