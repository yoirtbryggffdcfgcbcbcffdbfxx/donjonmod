import nbtlib, gzip, sys
for name, path in [
    ("PorteGob", "porte_gobelin.nbt"),
    ("Chapelle2", "salle_chapelle_2.nbt"),
    ("Chapelle1", "salle_chapelle_1.nbt"),
]:
    f = gzip.open(f"C:/Users/antoi/Downloads/salles/{path}")
    nbt = nbtlib.File.parse(f)
    size = list(nbt["size"])
    print(f"{name} ({path}): size={size}")
    purple = 0
    for b in nbt["blocks"]:
        s = int(b["state"])
        if s < len(nbt["palette"]):
            pn = str(nbt["palette"][s].get("Name", ""))
            if "purple_wool" in pn:
                purple += 1
                pos = list(b["pos"])
                face = ""
                if pos[0] == 0: face = "OUEST"
                elif pos[0] == size[0]-1: face = "EST"
                elif pos[2] == 0: face = "NORD"
                elif pos[2] == size[2]-1: face = "SUD"
                print(f"  PURPLE at {pos} -> {face}")
            if "light_blue_wool" in pn:
                pos = list(b["pos"])
                face = ""
                if pos[0] == 0: face = "OUEST"
                elif pos[0] == size[0]-1: face = "EST"
                elif pos[2] == 0: face = "NORD"
                elif pos[2] == size[2]-1: face = "SUD"
                print(f"  BLUE at {pos} -> {face}")
    if purple == 0:
        print("  NO PURPLE WOOL FOUND!")
    print()
