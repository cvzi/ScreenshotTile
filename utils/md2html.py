import markdown

with open("FEATURES.md", "r", encoding="utf-8") as f:
    md = f.read()

html = markdown.markdown(md)

with open("features.html", "w", encoding="utf-8") as f:
    f.write(html)
