@echo off
REM ============================================================================
REM  README.md -> "Hizli Baslangic" adimlarini tek seferde calistirir:
REM  docker compose up -d -> saglik kontrolu -> flyway:migrate -> vault kv put
REM  -> mvn clean install -> backend (platform-app) ve frontend (platform-web)
REM  ayri pencerelerde baslatilir.
REM
REM  Kullanim:  start-platform.bat            (sadece temel altyapi)
REM             start-platform.bat observability  (+ Prometheus/Grafana da acilir)
REM ============================================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo.
echo === Platform Starter - Hizli Baslangic ===
echo.

REM --- Gerekli araclar yuklu mu? ---
where docker >nul 2>&1
if errorlevel 1 (
    echo [HATA] "docker" bulunamadi. Docker Desktop kurulu ve PATH'te olmali.
    goto :FAIL
)
where mvn >nul 2>&1
if errorlevel 1 (
    echo [HATA] "mvn" bulunamadi. Maven kurulu ve PATH'te olmali.
    goto :FAIL
)
where npm >nul 2>&1
if errorlevel 1 (
    echo [HATA] "npm" bulunamadi. Node.js kurulu ve PATH'te olmali.
    goto :FAIL
)
where curl >nul 2>&1
if errorlevel 1 (
    echo [HATA] "curl" bulunamadi. Windows 10/11 ile birlikte gelir; PATH'i kontrol edin.
    goto :FAIL
)

REM --- .env varsa ozel sifreleri/kullanici adlarini oradan al, yoksa README varsayilanlari ---
set "VAULT_TOKEN=root"
set "DB_USERNAME=platform_app"
set "DB_PASSWORD=changeme"

if exist ".env" (
    echo [BILGI] ".env" bulundu, ozel degerler oradan okunacak.
    for /f "usebackq tokens=1,2 delims==" %%A in (".env") do (
        if /i "%%A"=="VAULT_DEV_ROOT_TOKEN_ID" set "VAULT_TOKEN=%%B"
        if /i "%%A"=="MYSQL_USER" set "DB_USERNAME=%%B"
        if /i "%%A"=="MYSQL_PASSWORD" set "DB_PASSWORD=%%B"
    )
) else (
    echo [BILGI] ".env" yok, README varsayilanlari kullanilacak: root / platform_app / changeme.
)

REM --- 2) Altyapi container'lari ---
echo.
echo [1/6] docker compose up -d (MySQL, Redis, Keycloak, Vault)...
docker compose up -d
if errorlevel 1 goto :FAIL

if /i "%~1"=="observability" (
    echo [BILGI] Prometheus + Grafana da aciliyor - observability profile...
    docker compose --profile observability up -d
)

REM --- 3) Servislerin hazir olmasini bekle ---
echo.
echo [2/6] Servislerin saglikli olmasi bekleniyor...

set /a RETRIES=0
:WAIT_MYSQL
set "MYSQL_HEALTH="
for /f "delims=" %%H in ('docker inspect --format="{{.State.Health.Status}}" platform-mysql 2^>nul') do set "MYSQL_HEALTH=%%H"
if "!MYSQL_HEALTH!"=="healthy" goto :MYSQL_READY
set /a RETRIES+=1
if !RETRIES! GEQ 30 (
    echo [HATA] MySQL 90 saniye icinde saglikli duruma gecmedi. "docker compose logs mysql" ile kontrol edin.
    goto :FAIL
)
echo   MySQL bekleniyor... (!RETRIES!/30^)
ping -n 4 127.0.0.1 >nul
goto :WAIT_MYSQL
:MYSQL_READY
echo   MySQL hazir.

set /a RETRIES=0
:WAIT_REDIS
set "REDIS_HEALTH="
for /f "delims=" %%H in ('docker inspect --format="{{.State.Health.Status}}" platform-redis 2^>nul') do set "REDIS_HEALTH=%%H"
if "!REDIS_HEALTH!"=="healthy" goto :REDIS_READY
set /a RETRIES+=1
if !RETRIES! GEQ 30 (
    echo [HATA] Redis 90 saniye icinde saglikli duruma gecmedi. "docker compose logs redis" ile kontrol edin.
    goto :FAIL
)
echo   Redis bekleniyor... (!RETRIES!/30^)
ping -n 4 127.0.0.1 >nul
goto :WAIT_REDIS
:REDIS_READY
echo   Redis hazir.

set /a RETRIES=0
:WAIT_KEYCLOAK
set "KC_STATUS="
for /f %%C in ('curl -s -o nul -w "%%{http_code}" http://localhost:8080/realms/platform') do set "KC_STATUS=%%C"
if "!KC_STATUS!"=="200" goto :KEYCLOAK_READY
set /a RETRIES+=1
if !RETRIES! GEQ 40 (
    echo [HATA] Keycloak 2 dakika icinde hazir olmadi. "docker compose logs keycloak" ile kontrol edin.
    goto :FAIL
)
echo   Keycloak bekleniyor... (!RETRIES!/40^)
ping -n 4 127.0.0.1 >nul
goto :WAIT_KEYCLOAK
:KEYCLOAK_READY
echo   Keycloak hazir.

set /a RETRIES=0
:WAIT_VAULT
set "VAULT_STATUS="
for /f %%C in ('curl -s -o nul -w "%%{http_code}" http://localhost:8200/v1/sys/health') do set "VAULT_STATUS=%%C"
if "!VAULT_STATUS!"=="200" goto :VAULT_READY
set /a RETRIES+=1
if !RETRIES! GEQ 20 (
    echo [HATA] Vault 60 saniye icinde hazir olmadi. "docker compose logs vault" ile kontrol edin.
    goto :FAIL
)
echo   Vault bekleniyor... (!RETRIES!/20^)
ping -n 4 127.0.0.1 >nul
goto :WAIT_VAULT
:VAULT_READY
echo   Vault hazir.

REM --- 4) Veritabani semasi (Flyway) ---
echo.
echo [3/6] Flyway migration'lari calistiriliyor (kullanici: !DB_USERNAME!^)...
call mvn -q -pl module-user flyway:migrate "-Dflyway.user=!DB_USERNAME!" "-Dflyway.password=!DB_PASSWORD!"
if errorlevel 1 (
    echo [HATA] Flyway migration basarisiz. MySQL kullanici/sifresi ".env" ile uyusuyor mu kontrol edin.
    goto :FAIL
)

REM --- 5) Vault'a DB sifresini yukle ---
echo.
echo [4/6] Vault'a secret'lar yukleniyor...
set "VAULT_PAYLOAD=%TEMP%\platform_vault_payload.json"
> "%VAULT_PAYLOAD%" echo {"data":{"DB_PASSWORD":"!DB_PASSWORD!","KEYCLOAK_CLIENT_SECRET":"changeme-in-vault","KEYCLOAK_ADMIN_CLIENT_SECRET":"changeme-in-vault-admin"}}
for /f %%C in ('curl -s -o nul -w "%%{http_code}" -X POST http://localhost:8200/v1/secret/data/platform-app -H "X-Vault-Token: !VAULT_TOKEN!" -H "Content-Type: application/json" --data "@!VAULT_PAYLOAD!"') do set "VAULT_PUT_STATUS=%%C"
del "%VAULT_PAYLOAD%" >nul 2>&1
if not "!VAULT_PUT_STATUS!"=="200" (
    echo [HATA] Vault'a yazma basarisiz - HTTP durum kodu: !VAULT_PUT_STATUS!. VAULT_TOKEN ".env"'deki VAULT_DEV_ROOT_TOKEN_ID ile ayni mi?
    goto :FAIL
)

REM --- 6) Projeyi derle ---
echo.
echo [5/6] Proje derleniyor (mvn clean install -DskipTests^)...
call mvn -q clean install -DskipTests
if errorlevel 1 (
    echo [HATA] Maven build basarisiz. Yukaridaki loglara bakin.
    goto :FAIL
)

REM --- 7) Frontend bagimliliklari (ayri pencerede calistirmadan once burada kur) ---
echo.
echo [6/6] Frontend hazirlaniyor (platform-web^)...
if not exist "platform-web\.env" (
    copy /y "platform-web\.env.example" "platform-web\.env" >nul
)
pushd platform-web
call npm install
popd
if errorlevel 1 (
    echo [HATA] "npm install" basarisiz.
    goto :FAIL
)

REM --- 8) Backend ve frontend'i ayri pencerelerde baslat ---
echo.
echo Backend ve frontend ayri pencerelerde baslatiliyor...
set "VAULT_TOKEN=!VAULT_TOKEN!"
set "DB_USERNAME=!DB_USERNAME!"
set "DB_PASSWORD=!DB_PASSWORD!"
start "Platform Backend (8081)" /D "%~dp0" cmd /k mvn -pl platform-app spring-boot:run
start "Platform Frontend (5173)" /D "%~dp0platform-web" cmd /k npm run dev

echo.
echo ============================================================
echo  Her sey ayakta! Birkac saniye icinde asagidaki adresler hazir olacak:
echo.
echo    Frontend (React SPA)   : http://localhost:5173
echo    Backend  (REST API)    : http://localhost:8081
echo    Keycloak admin         : http://localhost:8080  (admin/admin, .env'de degisebilir^)
echo    Vault UI               : http://localhost:8200  (token: !VAULT_TOKEN!^)
if /i "%~1"=="observability" (
    echo    Grafana                : http://localhost:3000
    echo    Prometheus              : http://localhost:9090
)
echo.
echo  Durdurmak icin: Backend/Frontend pencerelerinde Ctrl+C, sonra "docker compose down"
echo ============================================================
goto :END

:FAIL
echo.
echo Kurulum tamamlanamadi, yukaridaki hatayi giderip tekrar calistirin.
exit /b 1

:END
endlocal