from pathlib import Path

from fastapi import HTTPException
from fastapi.responses import FileResponse

from .config import settings
from .models import AppUpdateInfo

_RELEASES_DIR = Path(__file__).resolve().parent.parent / "releases"
_APK_NAME = "saylat.apk"


def get_update_info(base_url: str) -> AppUpdateInfo:
    base = base_url.rstrip("/")
    return AppUpdateInfo(
        version_code=settings.app_version_code,
        version_name=settings.app_version_name,
        apk_url=f"{base}/app/download/saylat.apk",
        release_notes=settings.app_release_notes,
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
