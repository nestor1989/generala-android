"""Genera las dos salidas del juego a partir de web/game.html:
   - dist/artifact.html : fragmento para el prototipo publicado (Google Fonts)
   - app/src/main/assets/index.html + fonts/ : versión offline para la app
"""
import os, shutil, re
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = open(os.path.join(ROOT, "web", "game.html"), encoding="utf-8").read()
GF = re.search(r'<link rel="stylesheet" href="https://fonts.googleapis.com[^>]*>', src).group(0)

os.makedirs(os.path.join(ROOT, "dist"), exist_ok=True)
open(os.path.join(ROOT, "dist", "artifact.html"), "w", encoding="utf-8").write(src)

FONTS = [("Karla", 400, "karla-latin-400-normal.woff2"), ("Karla", 500, "karla-latin-500-normal.woff2"),
         ("Karla", 700, "karla-latin-700-normal.woff2"), ("Fraunces", 500, "fraunces-latin-500-normal.woff2"),
         ("Fraunces", 600, "fraunces-latin-600-normal.woff2"), ("Fraunces", 700, "fraunces-latin-700-normal.woff2")]
faces = "".join("@font-face{font-family:'%s';font-style:normal;font-weight:%d;font-display:swap;src:url(fonts/%s) format('woff2');}\n" % f for f in FONTS)
local = "<style>\n" + faces + """html,body{height:100%;}
body{-webkit-tap-highlight-color:transparent;-webkit-user-select:none;user-select:none;overscroll-behavior:none;}
input{-webkit-user-select:text;user-select:text;}
</style>"""
body = src.replace(GF, local)
title = re.search(r"<title>.*?</title>", body).group(0)
body = body.replace(title, "", 1)
meta = re.search(r'<meta name="color-scheme"[^>]*>', body)
if meta:
    body = body.replace(meta.group(0), "", 1)
    title = title + "\n" + meta.group(0)
page = ('<!doctype html>\n<html lang="es">\n<head>\n<meta charset="utf-8">\n'
        '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">\n'
        + title + '\n</head>\n<body>\n' + body + '\n</body>\n</html>\n')
assets = os.path.join(ROOT, "app", "src", "main", "assets")
os.makedirs(os.path.join(assets, "fonts"), exist_ok=True)
open(os.path.join(assets, "index.html"), "w", encoding="utf-8").write(page)
fontsrc = os.path.join(ROOT, "tools", "fonts")
for _, _, fn in FONTS:
    shutil.copy(os.path.join(fontsrc, fn), os.path.join(assets, "fonts", fn))
print("ok:", len(page), "bytes")
