import os
from _env import run_gradle

os.chdir(os.path.dirname(os.path.abspath(__file__)))
run_gradle("runClient")
