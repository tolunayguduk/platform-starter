# Platform Starter

Modüler Spring Boot monolit iskeleti — micro mimariye hızlı geçiş için tasarlandı.
Bu README, konuştuğumuz tüm mimari kararların kod tarafında nerede karşılık bulduğunu özetliyor.

## Hızlı Başlangıç

Sıfırdan sistemi ayağa kaldırmak için sırasıyla:

```bash
# 1) Projeyi aç, kök dizine geç
cd platform-starter

# 2) Temel altyapı bileşenlerini ayağa kaldır (MySQL, Redis, Keycloak, Vault)
docker compose up -d

# 2b) (opsiyonel) Gözlemlenebilirlik gerekiyorsa Prometheus + Grafana'yı da ekle
#     Varsayılanda kapalıdır - sürekli arkada CPU harcamasın diye ayrı bir profile alındı.
docker compose --profile observability up -d

# 3) Servislerin sağlıklı (healthy) duruma geçmesini bekle
docker compose ps

# 4) Veritabanı şemasını oluştur (Flyway migration'ları)
mvn -pl module-user flyway:migrate

# 5) Vault dev sunucusuna DB şifresini yükle
#    Token, VAULT_DEV_ROOT_TOKEN_ID ile aynı olmalı (.env yoksa varsayılan: "root")
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=root
vault kv put secret/platform-app DB_PASSWORD=changeme

# 6) Projeyi derle (tüm modülleri sırayla build eder)
mvn clean install -DskipTests

# 7) Backend'i (REST API) çalıştır - VAULT_TOKEN, adım 5'teki ile aynı olmalı
export VAULT_TOKEN=root
mvn -pl platform-app spring-boot:run

# 8) Frontend'i (React SPA) ayrı bir terminalde çalıştır
cd platform-web
cp .env.example .env   # varsayılan API adresi: http://localhost:8081
npm install
npm run dev
```

> `.env` dosyasında `VAULT_DEV_ROOT_TOKEN_ID` değerini değiştirdiyseniz, yukarıdaki
> her iki `VAULT_TOKEN` değerini de (adım 5 ve 7) aynı değere güncelleyin - aksi
> halde uygulama `403 invalid token` hatasıyla Vault'a bağlanamaz.

**Erişim adresleri:**
| Servis | URL | Giriş (varsayılan) |
|---|---|---|
| Frontend (React SPA, `platform-web`) | http://localhost:5173 | — (`/register`'dan kayıt ol) |
| Backend (REST API, `platform-app`) | http://localhost:8081 | — (token ile, bkz. `/api/auth/login`) |
| Keycloak admin | http://localhost:8080 | admin / admin |
| Vault UI | http://localhost:8200 | token: root (`.env`'deki `VAULT_DEV_ROOT_TOKEN_ID`) |
| Grafana *(observability profile)* | http://localhost:3000 | admin / admin |
| Prometheus *(observability profile)* | http://localhost:9090 | — |

Yukarıdaki admin / admin, root gibi değerler sadece **varsayılan**dır ve `.env` dosyasıyla
kendi şifrelerinizle değiştirilebilir — aşağıdaki "Admin kullanıcı adı ve şifrelerini
özelleştirme" bölümüne bakın.

**Durdurmak için:**
```bash
mvn -pl platform-app spring-boot:run    # Ctrl+C ile durdur
docker compose down            # container'ları durdur, veriyi (volume) korur
docker compose down -v         # container'ları durdur VE tüm veriyi siler (dikkat)

# observability profile'ı ayrıca açtıysan onu da kapatmak için:
docker compose --profile observability down
```

## Admin kullanıcı adı ve şifrelerini özelleştirme

Keycloak, Vault ve Grafana bu proje tarafından `docker-compose.yml` üzerinden ayağa
kaldırılıyor; varsayılan admin kullanıcı adı/şifreleri de orada tanımlı (`admin`/`admin`,
Vault için `root` token gibi). Kendi şifrelerinizi belirlemek için bu araçların kendi
arayüzlerine gitmenize gerek yok — `docker-compose.yml`'i hiç değiştirmeden, sadece bir
`.env` dosyasıyla üzerine yazabilirsiniz:

```bash
# 1) Şablonu kopyalayın
cp .env.example .env

# 2) .env içindeki "change-me-..." değerlerini kendi şifrelerinizle değiştirin

# 3) (Zaten `docker compose up -d` çalıştırdıysanız) servisleri .env değerleriyle
#    sıfırdan başlatın - aşağıdaki "ilk kurulumda etkili" notuna bakın
docker compose down -v
docker compose up -d
```

`docker compose`, aynı dizindeki `.env` dosyasını otomatik okur ve `docker-compose.yml`
içindeki `${DEĞİŞKEN:-varsayılan}` yer tutucularını doldurur. `.env` dosyası `.gitignore`
ile git'e eklenmez — şifreleriniz repoya sızmaz. `.env` dosyası yoksa (siz bu adımı hiç
yapmasanız da) her şey eski varsayılan değerlerle çalışmaya devam eder.

**Hangi değişken hangi servisi kontrol ediyor:**

| Servis | Değişken(ler) | Kalıcı mı? |
|---|---|---|
| Keycloak | `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` | Evet (`keycloak-data` volume) — sadece **ilk kurulumda** etkili |
| MySQL | `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD` | Evet (`mysql-data` volume) — sadece **ilk kurulumda** etkili |
| Vault | `VAULT_DEV_ROOT_TOKEN_ID` | Hayır (dev mode, in-memory) — her başlatmada yeniden okunur |
| Grafana | `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` | Hayır (volume yok) — her başlatmada yeniden okunur |

**"Sadece ilk kurulumda etkili" ne demek?** Keycloak ve MySQL, kullanıcı/şifre bilgisini
kendi kalıcı volume'lerine (diske) yazıyor; `docker compose up -d` her çalıştığında bu ortam
değişkenlerini tekrar okumuyor, sadece volume ilk defa oluşturulurken (veritabanı/realm henüz
yokken) kullanıyor. Zaten kurulu bir ortamda `.env`'i değiştirip şifreyi güncellemek için:
- ya volume'ü silip sıfırdan kurun: `docker compose down -v && docker compose up -d`
  (**dikkat: bu MySQL'deki ve Keycloak'taki tüm verinizi siler**, projenin kendi Flyway
  migration'ları ve `realm-platform.json` importu ile her şey otomatik yeniden kurulur)
- ya da ilgili aracın kendi arayüzünden şifreyi değiştirin (Keycloak: admin console →
  Users → admin → Credentials; MySQL: `ALTER USER 'root'@'%' IDENTIFIED BY '...'`)

Vault ve Grafana'nın kalıcı depolaması olmadığı için (dev/demo amaçlı), `.env`'deki değer
her `docker compose up -d`'de hemen geçerli olur — volume silmeye gerek yok.

**Önemli — bu şifreler uygulamanın kendi kullanıcı girişinden farklı:**
- Keycloak admin şifresi sadece Keycloak'ın **yönetim konsoluna** (realm/kullanıcı/rol
  yönetimi) giriş içindir; uygulamanın kendi `/login` ekranından giren kullanıcılarla hiçbir
  ilgisi yoktur.
- `MYSQL_USER`/`MYSQL_PASSWORD`'ı değiştirirseniz, uygulamanın buna bağlanabilmesi için
  `DB_USERNAME`/`DB_PASSWORD` ortam değişkenlerini (ya da Vault'taki `DB_PASSWORD`
  secret'ını — yukarıdaki adım 5) da aynı değere güncellemeniz gerekir.
- `VAULT_DEV_ROOT_TOKEN_ID`'yi değiştirirseniz, uygulamanın Vault'a bağlanabilmesi için
  `VAULT_TOKEN` ortam değişkenini de aynı değere ayarlamanız gerekir.

### Vault nedir, ne amaçla kullanılır?

[HashiCorp Vault](https://www.vaultproject.io/), sırları (şifre, API key, sertifika vb.)
merkezi ve güvenli bir yerde tutmak için kullanılan bir araç. Amaç: **DB şifresi gibi
hassas bilgilerin kod içine veya `application.yml`'e yazılmaması.**

- Bu projede uygulama açılırken (`application.yml` → `spring.config.import: vault://`)
  Vault'a bağlanıp `DB_PASSWORD` gibi secret'ları oradan çekiyor (bkz. adım 5).
- Vault olmadan yapılsaydı, DB şifresi `application.yml` içine düz metin olarak yazılır ve
  git'e commit'lenirdi — bu bir güvenlik riski olurdu.
- Burada kullanılan **dev mode**, sadece local geliştirme içindir: veriler bellekte tutulur,
  `unseal` (kilit açma) adımı otomatiktir, container yeniden başlayınca sıfırlanır. Gerçek
  (staging/prod) ortamda Vault, diske yazan kalıcı bir storage backend ve manuel/otomatik
  unseal süreciyle çalıştırılır.
- Token, Vault'a "ben yetkiliyim" demenin yolu — şifre yerine geçer. Dev mode'da bu token
  `VAULT_DEV_ROOT_TOKEN_ID` ile belirlenir (bkz. yukarıdaki tablo).

## Modül yapısı

```
platform-parent                  (root POM - build/versiyon ayarları)
├── platform-bom                 (dependencyManagement - tek versiyon kaynağı)
├── platform-error-starter       (AppException hiyerarşisi + GlobalExceptionHandler)
├── platform-audit-starter       (Envers + hash-chain tamper-evidence)
├── platform-security-starter    (Keycloak JWT resource server + MatrixPermissionEvaluator)
├── platform-observability-starter (correlation id / MDC, Actuator, Prometheus)
├── platform-cache-starter       (Redis + config-driven TTL, @Cacheable hazır)
├── module-user                  (GDPR-kategorili user tabloları + rol/yetki matrisi)
└── platform-app                 (çalıştırılabilir Spring Boot uygulaması, saf REST API)

platform-web                     (Maven reactor'ın DIŞINDA, ayrı bir React/Vite SPA - bkz. aşağı)
```

Her `platform-*` starter bağımsız bir Maven modülü — micro mimariye geçişte bunları bir
internal artifact repository'ye (`mvn deploy`) yayınlayıp, her mikroservisin `pom.xml`'ine normal
bir dependency olarak eklemeniz yeterli. Kod tekrarı sıfır.

`platform-app` artık hiç HTML render etmiyor — sadece `/api/auth/*` (login/register/refresh/logout)
ve `/api/me`, `/api/me/ui-permissions` gibi JSON endpoint'leri sunan bir REST API. Tüm arayüz
`platform-web/` altındaki ayrı React uygulamasında yaşıyor; ikisi arasındaki tek bağ HTTP + Bearer
token (bkz. "Permission-aware UI" satırı).

## Konuşulan kararların kod karşılığı

| Karar | Nerede |
|---|---|
| Modüler/izole yapı, micro-ready | `platform-*` starter'lar + `module-user` ayrımı |
| OAuth2 (Google/GitHub/Facebook) + Keycloak altyapısı | `platform-security-starter` — `issuer-uri` tek değişen satır |
| Config-driven her şey | `application.yml` (`platform.cache.ttls.*` gibi), DB-driven `role_permission` matrisi |
| GDPR kategorize user data | `module-user/.../profile/*` (UserCore, UserProfile, UserContact, UserConsent - ayrı tablolar) |
| Rol/yetki matrisi, DB'den yönetilebilir | `module-user/.../authz/*` (`Role`, `Permission`, `RolePermission`) |
| Permission-aware UI (hide/disable/enable) | `UiPermissionsController` (`/api/me/ui-permissions`) + `platform-web/src/components/PermissionButton.tsx` |
| Envers + tamper-evident audit | `platform-audit-starter` (`PlatformRevisionListener` - hash chain) |
| Finans-grade hata yönetimi | `platform-error-starter` (Business/Technical/Security exception ayrımı) |
| Redis cache, annotation ile | `platform-cache-starter` (`@Cacheable("role-permissions")`) |
| Health check (liveness/readiness) | `application.yml` → `management.endpoint.health.probes.enabled=true` |
| Vault ile secret yönetimi | `application.yml` → `spring.config.import: vault://`, `docker-compose.yml` → `vault` servisi |
| Maven | Tüm proje Maven multi-module |

## Bilinçli olarak eksik bırakılanlar (sıradaki adımlar)

- **Sosyal login (Google/GitHub/Facebook)**: `realm-platform.json` içine `identityProviders`
  olarak eklendi (`enabled: false`, placeholder client-id/secret ile) — Google/GitHub/Facebook
  Developer Console'larından gerçek OAuth uygulaması oluşturup `REPLACE_WITH_*` alanlarını
  doldurduktan sonra `enabled: true` yapman yeterli, başka bir şey değişmiyor.
- **Diğer `_AUD` tabloları**: `V4__envers_audit_tables.sql` içinde sadece `user_core_AUD`
  örnek olarak yazıldı; `user_profile_AUD`, `user_contact_AUD`, `role_permission_AUD` aynı
  pattern'le eklenmeli.
- **Idempotency key altyapısı, maker-checker pattern, outbox pattern**: henüz kod yok,
  ilk `payment`/işlem modülü yazılırken `platform-error` ve `platform-audit` üzerine inşa edilecek.
- **Rate limiting (Bucket4j), field-level encryption, TDE**: henüz eklenmedi.
- **DB kullanıcı yetkilerini `_AUD` tablolarında INSERT-only yapma**: `V4` migration'ının
  sonunda örnek SQL var, gerçek DBA script'i olarak ayrıca çalıştırılmalı.
- **`platform-web`'de refresh token'ın `localStorage`'da tutulması**: bilinçli bir
  basitleştirme - bir demo/starter için yeterli, ama XSS'e karşı üretimde httpOnly cookie +
  BFF (backend-for-frontend) pattern'i ya da en azından refresh token'ı memory-only tutup
  sayfa yenilemede yeniden login isteme değerlendirilmeli.

## Yeni bir modül eklemek

1. `module-<isim>` adında yeni bir Maven modülü aç, `platform-parent`'a `<parent>` yap
2. İhtiyaç duyduğu `platform-*` starter'ları dependency olarak ekle
3. Entity'lerini `@Audited` işaretle (otomatik olarak Envers'e dahil olur)
4. Kritik endpoint'lerini `@PreAuthorize("hasPermission('<resource>', '<action>')")` ile koru
5. `platform-app/pom.xml`'e yeni modülü dependency olarak ekle
