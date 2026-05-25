"""Скрытие типовой рекламы перед скриншотом Playwright."""

from __future__ import annotations

ADBLOCK_CSS = """
iframe[src*="doubleclick"],
iframe[src*="googlesyndication"],
iframe[src*="ad."],
ins.adsbygoogle,
[id*="ad-container" i],
[id*="ad_container" i],
[class*="ad-banner" i],
[class*="ad_banner" i],
[class*="advertisement" i],
[class*="banner-ad" i],
div[data-ad],
aside[data-ad],
.cookie-banner,
#cookie-notice,
.gdpr-banner {
  display: none !important;
  visibility: hidden !important;
  height: 0 !important;
  max-height: 0 !important;
  overflow: hidden !important;
}
"""

_REMOVE_AD_IFRAMES_JS = """() => {
  const bad = /doubleclick|googlesyndication|adfox|adservice|adsystem|adnxs|taboola|outbrain/i;
  document.querySelectorAll('iframe').forEach((el) => {
    const src = (el.getAttribute('src') || '').toLowerCase();
    if (bad.test(src)) el.remove();
  });
}"""


async def inject_adblock(page) -> None:
    try:
        await page.add_style_tag(content=ADBLOCK_CSS)
        await page.evaluate(_REMOVE_AD_IFRAMES_JS)
    except Exception:
        pass
