# Деплой лендинга v3 на saylat.baddysays.ru (aaPanel / Apache)
param(
    [string]$ServerHost = "",
    [string]$User = "root",
    [string]$RemoteRoot = "/www/wwwroot/saylat.baddysays.ru"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$EnvFile = Join-Path $Root "saylat.deploy.env"
if (-not $ServerHost -and (Test-Path $EnvFile)) {
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*SAYLAT_DEPLOY_HOST\s*=\s*(.+)\s*$') { $ServerHost = $Matches[1].Trim() }
        if ($_ -match '^\s*SAYLAT_DEPLOY_USER\s*=\s*(.+)\s*$') { $User = $Matches[1].Trim() }
    }
}
if (-not $ServerHost) {
    Write-Host "Укажите хост: .\scripts\deploy-saylat-website.ps1 -ServerHost '157.22.202.235'"
    exit 1
}

& (Join-Path $PSScriptRoot "build-saylat-website.ps1")
$Dist = Join-Path $Root "website\dist"
$VhostSsl = Join-Path $Root "website\apache\saylat.baddysays.ru.conf"
$VhostHttp = Join-Path $Root "website\apache\saylat.baddysays.ru-http.conf"
$Remote = "${User}@${ServerHost}"
$CertPath = "/www/server/panel/vhost/cert/saylat.baddysays.ru/fullchain.pem"

ssh $Remote "mkdir -p $RemoteRoot"
scp -r "$Dist\*" "${Remote}:${RemoteRoot}/"
ssh $Remote "chmod -R a+rX $RemoteRoot/assets $RemoteRoot/privacy 2>/dev/null; chmod a+r $RemoteRoot/site-legal.css 2>/dev/null"

$hasCert = ssh $Remote "test -f $CertPath && echo yes || echo no"
if ($hasCert.Trim() -eq "yes") {
    scp $VhostSsl "${Remote}:/www/server/panel/vhost/apache/saylat.baddysays.ru.conf"
} else {
    scp $VhostHttp "${Remote}:/www/server/panel/vhost/apache/saylat.baddysays.ru.conf"
}

ssh $Remote "/www/server/apache/bin/apachectl -t && /www/server/apache/bin/apachectl graceful"

if (-not (ssh $Remote "test -f $CertPath")) {
    ssh $Remote "/root/.acme.sh/acme.sh --issue -d saylat.baddysays.ru -w $RemoteRoot --server letsencrypt --force"
    ssh $Remote "/root/.acme.sh/acme.sh --install-cert -d saylat.baddysays.ru --key-file /www/server/panel/vhost/cert/saylat.baddysays.ru/privkey.pem --fullchain-file /www/server/panel/vhost/cert/saylat.baddysays.ru/fullchain.pem"
}

$hasCertAfter = ssh $Remote "test -f $CertPath && echo yes || echo no"
if ($hasCertAfter.Trim() -eq "yes") {
    scp $VhostSsl "${Remote}:/www/server/panel/vhost/apache/saylat.baddysays.ru.conf"
}

ssh $Remote "/www/server/apache/bin/apachectl -t && /www/server/apache/bin/apachectl graceful"

Write-Host ""
Write-Host "Лендинг: https://saylat.baddysays.ru/"
Write-Host "Файлы:   $RemoteRoot"
