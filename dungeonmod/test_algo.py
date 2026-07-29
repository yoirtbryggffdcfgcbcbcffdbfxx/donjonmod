import os, subprocess, sys

# Harnais de regression par seeds : genere N donjons et verifie automatiquement
# coherence labels <-> adjacence, connexite, garanties gameplay.
# Usage :  python test_algo.py [nbSeeds] [seedDepart] [v]
#   defaut : 100 seeds a partir de 1.  'v' = detail par seed.
# ATTENTION : utilise build/classes/java/main -> lancer clean_build.py d'abord !
base = os.path.dirname(__file__)
classes = os.path.join(base, "build", "classes", "java", "main")

n = sys.argv[1] if len(sys.argv) > 1 else "100"
s = sys.argv[2] if len(sys.argv) > 2 else "1"
verbose = "-v" in sys.argv[3:]

if not os.path.isdir(classes):
    print("Pas de classes compilees ! Lance d'abord : python clean_build.py")
    input("Appuie sur Entree pour fermer...")
    sys.exit(1)

cmd = ["java", "-cp", classes, "com.dungeonmod.debug.SeedHarness", "-n", str(n), "-s", str(s)]
if verbose:
    cmd.append("-v")

r = subprocess.run(cmd, capture_output=True, text=True)
print(r.stdout)
if r.stderr:
    print(r.stderr)

if r.returncode != 0:
    input("ECHEC harnais (voir details ci-dessus). Appuie sur Entree pour fermer...")
    sys.exit(1)
