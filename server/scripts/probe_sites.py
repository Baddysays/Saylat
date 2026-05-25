import re
import urllib.request

UA = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"


def fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    return urllib.request.urlopen(req, timeout=25).read().decode("utf-8", "replace")


html = fetch("https://m.vk.com/feed")
print("vk len", len(html))
for pat in [
    r'href="(/wall[^"]+)"',
    r'class="[^"]*PostContentWrapper[^"]*"',
    r'"text"\s*:\s*"([^"]{30,120})"',
]:
    m = re.findall(pat, html)
    print("vk", pat[:40], len(m), m[:2])

h2 = fetch("https://pikabu.ru/")
print("pikabu len", len(h2))
stories = re.findall(r'href="(https://pikabu\.ru/story/[^"]+)"', h2)
print("pikabu stories", len(set(stories)), list(set(stories))[:3])
hrefs = re.findall(r'href="(/story/[^"]+)"', h2)
print("pikabu rel", len(set(hrefs)), list(set(hrefs))[:3])

# pikabu story
sid = list(set(stories))[0].split("?")[0] if stories else ""
if sid:
    shtml = fetch(sid)
    og = re.search(r'property="og:description" content="([^"]*)"', shtml)
    print("story og desc", (og.group(1)[:80] if og else None))

# dzen article
try:
    d = fetch("https://dzen.ru/a/ZWJldGl0bGU")
except Exception as e:
    print("dzen err", e)

# vk public
try:
    vk = fetch("https://m.vk.com/durov")
    print("vk durov len", len(vk), "wall" in vk)
    print(re.findall(r'href="(/wall[^"]+)"', vk)[:3])
except Exception as e:
    print("vk public", e)
