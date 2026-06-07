import json
from pathlib import Path

from fastapi import HTTPException
from fastapi.responses import FileResponse

from .config import settings
from .models import AppUpdateInfo

_RELEASES_DIR = Path(__file__).resolve().parent.parent / "releases"
_APK_NAME = "saylat.apk"
_META_NAME = "apk-meta.json"


def _load_apk_meta() -> dict | None:
    path = _RELEASES_DIR / _META_NAME
    if not path.is_file():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return None


def resolve_app_version() -> tuple[int, str, str]:
    """Single source for /health and /api/app/update (apk-meta.json overrides .env)."""
    meta = _load_apk_meta()
    version_code = int(meta["version_code"]) if meta and "version_code" in meta else settings.app_version_code
    version_name = str(meta.get("version_name", settings.app_version_name)) if meta else settings.app_version_name
    release_notes = str(meta.get("release_notes", settings.app_release_notes)) if meta else settings.app_release_notes
    return version_code, version_name, release_notes


def get_update_info(base_url: str) -> AppUpdateInfo:
    base = base_url.rstrip("/")
    version_code, version_name, release_notes = resolve_app_version()
    return AppUpdateInfo(
        version_code=version_code,
        version_name=version_name,
        apk_url=f"{base}/app/download/saylat.apk",
        release_notes=release_notes,
        mandatory=settings.app_update_mandatory,
    )


def apk_file_response() -> FileResponse:
    path = _RELEASES_DIR / _APK_NAME
    if not path.is_file():
        raise HTTPException(
            status_code=404,
            detail="APK ещё не выложен на сервер. Соберите debug/release и скопируйте в server/releases/saylat.apk",
        )
    return FileResponse(
        path,
        media_type="application/vnd.android.package-archive",
        filename=_APK_NAME,
    )
