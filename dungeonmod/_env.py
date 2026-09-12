import glob
import os
import shutil
import subprocess
import sys

IS_WINDOWS = os.name == "nt"
EXE = ".exe" if IS_WINDOWS else ""

HOME_JDK_PATTERNS = (
    "~/.jdks/*21*",
    "~/jdk-21*",
    "~/jdk*21*",
    "~/.sdkman/candidates/java/*21*",
    "~/.local/share/mise/installs/java/*21*",
    "~/.asdf/installs/java/*21*",
)


def _jdk_home_ok(home):
    javac = os.path.join(home, "bin", "javac" + EXE)
    if not os.path.isfile(javac):
        return False
    try:
        r = subprocess.run([javac, "-version"], capture_output=True, text=True)
    except OSError:
        return False
    return "javac 21" in ((r.stdout or "") + (r.stderr or ""))


def find_jdk21():
    candidates = []
    jh = os.environ.get("JAVA_HOME")
    if jh:
        candidates.append(jh)
    if IS_WINDOWS:
        for root in (
            r"C:\Program Files\Java",
            r"C:\Program Files\Eclipse Adoptium",
            r"C:\Program Files\Microsoft",
            r"C:\Program Files\Zulu",
        ):
            candidates += glob.glob(os.path.join(root, "*21*"))
    elif sys.platform == "darwin":
        candidates += glob.glob("/Library/Java/JavaVirtualMachines/*21*/Contents/Home")
        candidates += glob.glob("/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home")
        candidates += glob.glob("/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home")
    else:
        candidates += sorted(glob.glob("/usr/lib/jvm/*21*"))
    for pattern in HOME_JDK_PATTERNS:
        candidates += sorted(glob.glob(os.path.expanduser(pattern)))
    for c in candidates:
        if _jdk_home_ok(c):
            return c
    return None


def java_command():
    jh = find_jdk21()
    if jh:
        exe = os.path.join(jh, "bin", "java" + EXE)
        if os.path.isfile(exe):
            return exe
    return "java"


def java_available():
    return find_jdk21() is not None or shutil.which("java") is not None


def _gradlew_path():
    name = "gradlew.bat" if IS_WINDOWS else "gradlew"
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), name)
    if not IS_WINDOWS:
        try:
            os.chmod(path, os.stat(path).st_mode | 0o111)
        except OSError:
            pass
    return path


def gradle_env():
    env = os.environ.copy()
    jh = find_jdk21()
    if jh:
        env["JAVA_HOME"] = jh
        env["PATH"] = os.path.join(jh, "bin") + os.pathsep + env.get("PATH", "")
    return env


def run_gradle(*tasks, **kwargs):
    if not java_available():
        print("ATTENTION : aucun Java detecte dans cet environnement.")
        print("  - Installe le JDK 21 :  sudo apt install openjdk-21-jdk-headless")
        print("  - Ou definis JAVA_HOME vers un JDK 21 avant de lancer ce script.")
        print("  - Si tu passes par un IDE (Zed, VS Code...), ouvre-le depuis un")
        print("    terminal normal : certains IDE ne voient pas /usr/lib/jvm.")
    cmd = [_gradlew_path(), *tasks]
    return subprocess.run(cmd, env=gradle_env(), **kwargs)


def pause(message="Appuie sur Entree pour fermer..."):
    if sys.stdin.isatty():
        input(message)
