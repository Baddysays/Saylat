package com.baddysays.saylat.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.baddysays.saylat.BuildConfig
import com.baddysays.saylat.data.AppUpdateInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

sealed class UpdateCheckResult {
    data class UpToDate(val serverVersionCode: Int, val serverVersionName: String) : UpdateCheckResult()
    data class Available(val info: AppUpdateInfo) : UpdateCheckResult()
}

class AppUpdateManager(private val context: Context) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val updateAdapter = moshi.adapter(AppUpdateInfo::class.java)

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Проверка обновления через GitHub (releases/update.json), не через ваш VPS. */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val info = fetchGithubUpdateInfo()
        if (info.version_code > BuildConfig.VERSION_CODE) {
            UpdateCheckResult.Available(info)
        } else {
            UpdateCheckResult.UpToDate(
                serverVersionCode = info.version_code,
                serverVersionName = info.version_name,
            )
        }
    }

    private fun fetchGithubUpdateInfo(): AppUpdateInfo {
        val url = BuildConfig.GITHUB_UPDATE_JSON.trim()
        if (url.isBlank()) {
            throw IllegalStateException("Не задан URL метаданных обновления (GitHub)")
        }
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub update.json: HTTP ${response.code}")
            }
            val body = response.body?.string()?.trim().orEmpty()
            if (body.isBlank()) throw IllegalStateException("Пустой update.json на GitHub")
            val info = updateAdapter.fromJson(body)
                ?: throw IllegalStateException("Не удалось разобрать update.json")
            if (info.apk_url.isBlank()) {
                throw IllegalStateException("В update.json нет apk_url")
            }
            return info
        }
    }

    suspend fun downloadAndInstall(apkUrl: String): String = withContext(Dispatchers.IO) {
        if (!canRequestPackageInstalls()) {
            openUnknownSourcesSettings()
            throw IllegalStateException(
                "Разрешите установку из этого приложения в настройках Android, затем нажмите снова",
            )
        }

        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outFile = File(dir, "saylat.apk")
        if (outFile.exists()) outFile.delete()

        val request = Request.Builder().url(apkUrl).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Не удалось скачать APK: HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Пустой ответ")
            body.byteStream().use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        if (outFile.length() < 10_000) {
            outFile.delete()
            throw IllegalStateException("Скачанный файл слишком маленький — проверьте релиз на GitHub")
        }

        installApk(outFile)
        "Установщик открыт. Подтвердите обновление Saylat."
    }

    private fun installApk(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun canRequestPackageInstalls(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    private fun openUnknownSourcesSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
