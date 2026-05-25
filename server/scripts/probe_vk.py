import re
import urllib.request

UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
url = "https://m.vk.com/durov"
req = urllib.request.Request(url, headers={"User-Agent": UA})
html = urllib.request.urlopen(req, timeout=25).read().decode("utf-8", "replace")
open(r"C:\Users\Admin\Projects\thin-browser\server\scripts\vk_durov.html", "w", encoding="utf-8").write(html[:80000])
print("len", len(html))
for pat in [r"wall_post", r"Post--", r"feed_row", r'"text":"', r"op_post"]:
    print(pat, len(re.findall(pat, html, re.I)))
