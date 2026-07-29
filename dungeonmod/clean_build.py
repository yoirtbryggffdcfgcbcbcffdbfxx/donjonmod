import subprocess, os, sys, time

# CLEAN BUILD complet : supprime TOUT le cache de compilation et recompile.
# A lancer APRES CHAQUE PULL (sinon ouvrir_donjon.py / test_algo.py tournent
# sur les vieilles classes et affichent des bugs deja corriges !)
base = os.path.dirname(os.path.abspath(__file__))
os.chdir(base)

print("=== CLEAN BUILD (gradlew clean build) ===")
t0 = time.time()
r = subprocess.run(["gradlew.bat", "clean", "build"])
dt = time.time() - t0

if r.returncode != 0:
    print("\nECHEC du build ({:.1f}s). Corrige les erreurs ci-dessus.".format(dt))
    print("(Si l'erreur vient d'un commit recent : GitHub Desktop > History > clic droit > Revert)")
    input("Appuie sur Entree pour fermer...")
    sys.exit(1)

print("\nBuild OK en {:.1f}s.".format(dt))
print("Classes fraiches : build/classes/java/main")
print("Tu peux maintenant lancer : ouvrir_donjon.py, test_algo.py, run_client.py")
