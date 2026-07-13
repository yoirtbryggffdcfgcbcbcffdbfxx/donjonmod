import subprocess, os
os.chdir(os.path.dirname(os.path.abspath(__file__)))
subprocess.run(["gradlew.bat", "runClient"])