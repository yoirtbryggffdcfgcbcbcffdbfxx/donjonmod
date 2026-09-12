import os, subprocess, webbrowser, sys, random
from _env import java_command, pause


def open_in_brave(path):
    """Ouvre le HTML dans Brave (flatpak ou natif), sinon navigateur par defaut."""
    if not path or not os.path.exists(path):
        print("Viz introuvable :", path)
        return
    url = "file://" + os.path.abspath(path)
    candidates = [
        ["brave-browser"],
        ["brave"],
        ["brave-browser-stable"],
        ["/var/lib/flatpak/exports/bin/com.brave.Browser"],
        ["flatpak", "run", "com.brave.Browser"],
    ]
    for c in candidates:
        try:
            subprocess.Popen(c + [url], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print("Ouvert dans Brave :", url)
            return
        except (FileNotFoundError, OSError):
            continue
    webbrowser.open(url)
    print("Brave introuvable -> navigateur par defaut :", url)

base = os.path.dirname(__file__)
classes = os.path.join(base, "build", "classes", "java", "main")
html = os.path.join(base, "dungeon_viz.html")

seed = sys.argv[1] if len(sys.argv) > 1 else str(random.randint(0, 2**60))

r = subprocess.run(
    [java_command(), "-cp", classes, "com.dungeonmod.debug.DungeonViz", "-s", seed, "-o", html],
    capture_output=True, text=True
)
print(r.stdout)
if r.returncode != 0:
    pause("Erreur. Appuie sur Entree pour fermer...")
    sys.exit(1)

open_in_brave(html)
