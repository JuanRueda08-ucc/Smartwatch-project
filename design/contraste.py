"""Contraste WCAG de las paletas de docs/DISENO_UI.md §2, en las combinaciones que usa la interfaz."""

L = dict(background="#F6F7FB", surface="#FFFFFF", surfaceBright="#E8EAF2", onSurface="#1A1B22",
         onSurfaceVariant="#5A5D6B", primary="#2F5BD3", state1Finish="#5B3FD1", state2Prepare="#0B6BB1",
         state3Check="#8A5A00", state4Leave="#0D7548", late="#C62828", onState="#FFFFFF")
D = dict(background="#000000", surface="#1B1B1F", surfaceBright="#2A2A30", onSurface="#F2F2F5",
         onSurfaceVariant="#A8A8B3", primary="#8AB4FF", state1Finish="#B39DFF", state2Prepare="#5CC8FF",
         state3Check="#FFC857", state4Leave="#5EE6A0", late="#FF6B6B", onState="#000000")

def rgb(h): return [int(h[i:i + 2], 16) for i in (1, 3, 5)]
def hexc(c): return "#" + "".join(f"{round(v):02X}" for v in c)
def lum(h):
    def ch(v):
        v /= 255
        return v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    r, g, b = map(ch, rgb(h))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
def cr(a, b):
    la, lb = sorted([lum(a), lum(b)], reverse=True)
    return (la + 0.05) / (lb + 0.05)
def mix(c, base, p):  # color al p sobre base (color-mix)
    return hexc([p * x + (1 - p) * y for x, y in zip(rgb(c), rgb(base))])

TEXT = ["primary", "state1Finish", "state2Prepare", "state3Check", "state4Leave", "late"]
fails = []
for name, P in (("Claro", L), ("Oscuro", D)):
    print(f"\n=== {name} ===")
    rows = []
    for fg in ["onSurface", "onSurfaceVariant"] + TEXT:
        for bg in ["background", "surface", "surfaceBright"]:
            rows.append((f"{fg} / {bg}", cr(P[fg], P[bg]), 4.5))
    for c in TEXT:
        rows.append((f"onState / {c} (botón)", cr(P["onState"], P[c]), 4.5))
        tint = mix(P[c], P["surface"], 0.12)
        rows.append((f"{c} / tinte 12% de {c}", cr(P[c], tint), 4.5))
        rows.append((f"onSurface / tinte 12% de {c}", cr(P["onSurface"], tint), 4.5))
        rows.append((f"{c} (no texto) / background", cr(P[c], P["background"]), 3.0))
    rows.append(("surfaceBright (pista, bordes) / background", cr(P["surfaceBright"], P["background"]), 1.0))
    rows.append(("onSurfaceVariant (interruptor) / surfaceBright", cr(P["onSurfaceVariant"], P["surfaceBright"]), 3.0))
    for label, v, need in rows:
        mark = "OK " if v >= need else "MAL"
        if v < need: fails.append((name, label, round(v, 2), need))
        print(f"{mark} {v:5.2f}  (mín {need})  {label}")
    txt = [cr(P[k], P["background"]) for k in ["onSurface", "onSurfaceVariant"] + TEXT]
    print(f"Rango texto sobre background: {min(txt):.2f} – {max(txt):.2f}")
    print(f"onState sobre estados: mín {min(cr(P['onState'], P[c]) for c in TEXT[1:]):.2f}")

print("\nFALLAS:", fails or "ninguna")
