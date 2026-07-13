import os, subprocess, webbrowser, sys, random

base = os.path.dirname(__file__)
classes = os.path.join(base, "build", "classes", "java", "main")
html = os.path.join(base, "dungeon_viz.html")

seed = sys.argv[1] if len(sys.argv) > 1 else str(random.randint(0, 2**60))

r = subprocess.run(
    ["java", "-cp", classes, "com.dungeonmod.debug.DungeonViz", "-s", seed, "-o", html],
    capture_output=True, text=True
)
print(r.stdout)
if r.returncode != 0:
    input("Erreur. Appuie sur Entree pour fermer...")
    sys.exit(1)

webbrowser.open("file://" + os.path.abspath(html))
