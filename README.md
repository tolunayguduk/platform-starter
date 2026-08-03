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
#    process-resources önce çalışmalı: flyway:migrate migration dosyalarını target/classes'tan
#    (classpath) okur, doğrudan goal çağrısı bunu otomatik kopyalamaz - atlarsanız "No migrations
#    found" uyarısıyla hiçbir tablo oluşturulmadan sessizce geçer.
mvn -pl module-user process-resources flyway:migrate

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
| Alertmanager *(observability profile)* | http://localhost:9093 | — |

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

**Yedekleme:** `mysql-backup` servisi, `platform_user`/`platform_post` şemalarının
`mysqldump` çıktısını her 6 saatte bir (`BACKUP_INTERVAL_SECONDS`) `./backups/mysql/`
altına, 14 gün saklama süresiyle (`BACKUP_RETENTION_DAYS`) yazar. Bilerek isimli bir Docker
volume'ü değil, host'un kendi dosya sistemini kullanır — `docker compose down -v` (ya da
Docker'ın veri dizininin tamamen kaybolması, bu projede bir kez zaten yaşandı) named
volume'leri siler ama host'taki `./backups/` klasörüne dokunmaz. Geri yüklemek için:
`gunzip < backups/mysql/platform_user-20260101-000000.sql.gz | mysql -uroot -p`.

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
├── module-user                  (GDPR-kategorili user tabloları + rol/yetki matrisi; şema: platform_user)
├── module-post                  (henüz boş iskelet - bkz. "Yeni bir modül eklemek"; şema: platform_post)
└── platform-app                 (çalıştırılabilir Spring Boot uygulaması, saf REST API)

platform-web                     (Maven reactor'ın DIŞINDA, ayrı bir React/Vite SPA - bkz. aşağı)
```

Her `platform-*` starter bağımsız bir Maven modülü — micro mimariye geçişte bunları bir
internal artifact repository'ye (`mvn deploy`) yayınlayıp, her mikroservisin `pom.xml`'ine normal
bir dependency olarak eklemeniz yeterli. Kod tekrarı sıfır.

Aynı mantık veritabanı seviyesinde de var: her `module-*` **kendi MySQL şemasına** sahip
(`module-user` → `platform_user`, `module-post` → `platform_post`, ...), hepsi bugün aynı MySQL
sunucusuna/bağlantısına gitse bile. Hiçbir tablo şema sınırını geçmez, hiçbir modül başka bir
modülün tablosuna JOIN atmaz - bu sayede bir modül günün birinde kendi DataSource'una (hatta kendi
DB sunucusuna) taşınırken veri taşıma projesi gerekmez, sadece bağlantı bilgisi değişir. Bkz.
"Yeni bir modül eklemek" ve `docker/mysql/init-schemas.sql`.

`platform-app` artık hiç HTML render etmiyor — sadece `/api/auth/*` (login/register/refresh/logout)
ve `/api/me`, `/api/me/ui-permissions` gibi JSON endpoint'leri sunan bir REST API. Tüm arayüz
`platform-web/` altındaki ayrı React uygulamasında yaşıyor; ikisi arasındaki tek bağ HTTP + Bearer
token (bkz. "Permission-aware UI" satırı).

## Katmanlı paket yapısı

`module-user`, `platform-app`'in controller'ları ve `platform-security-starter`'daki Keycloak
entegrasyon sınıfları (`integration/keycloak`) **feature-by-package değil, layer-by-package**
düzenlenir:

```
entity/           @Entity sınıfları - hiç logic yok
repository/        Spring Data JPA interface'leri
constant/          enum'lar (uygulama genelinde kullanılan sabitler)
controller/
  model/           Controller'ın DTO'ları (HTTP request/response body'leri)
  XxxController          interface - Spring MVC anotasyonları burada (@RequestMapping,
                          @GetMapping, @RequestBody, @PathVariable, @AuthenticationPrincipal...)
  XxxControllerImpl      @RestController, interface'i implemente eder, logic içermez
service/
  model/           Service'in Entity'den farklı ihtiyaç duyduğu request/response modelleri
  XxxService             interface
  XxxServiceImpl         @Service, gerçek iş mantığı burada
mapper/            MapStruct interface'leri (@Mapper(componentModel = "spring")) - katmanlar
                   arası model dönüşümü (Controller DTO ↔ Service model ↔ Entity)
integration/       harici sistemlere (Keycloak gibi) entegrasyon - sıfır logic, sadece API çağrısı
  <sistem>/model/        o sistemin kendi request/response modelleri
  XxxClient              interface
  XxxClientImpl          zero-logic implementasyon
util/              saf statik yardımcı sınıflar (örn. KeycloakRoleMapper)
```

Kurallar:
- Her Service/Integration/Controller sınıfı bir `Xxx` arayüzünden türer, implementasyonu `XxxImpl`
  adını taşır. Repository ve Mapper bunun dışında — onları sırasıyla Spring Data JPA ve MapStruct
  kendisi üretir.
- Controller'da logic olmaz; validasyon/hesaplama mantığı Service'e taşınır.
- Bir katman bir alt katmanı çağırırken, alt katmanın kendi request/response modelini (varsa
  Mapper ile, basit primitive parametrelerde doğrudan) kendisi hazırlar.

MapStruct'ın annotation processor'ı `mvn`'in klasik classpath taramasıyla otomatik devreye
girmiyor — `module-user`, `platform-app` ve gelecekte mapper ekleyecek her modülün
`maven-compiler-plugin` konfigürasyonunda `annotationProcessorPaths` açıkça tanımlı olmalı
(mevcut pom'lara bakın).

Diğer `platform-*-starter` kütüphaneleri (error/cache/observability/audit) saf auto-configuration
kütüphaneleri olduğu için (controller/entity yok) bu yapıya zorlanmadı.

## Veri şeması: hangi veri nerede tutuluyor

Dört ayrı depo var, her biri farklı bir sorumluluk taşıyor. **Kural: Keycloak kimlik ve rol
yönetiminin TEK doğruluk kaynağıdır — MySQL'de artık ne bir kimlik cache'i (`user_core`), ne de
bir rol tablosu var.** username/email/ad-soyad her zaman canlı olarak Keycloak'tan okunur/yazılır
(`KeycloakAdminClient`); kullanıcı↔rol ataması da sadece Keycloak'ta tutulur ve JWT'nin
`realm_access.roles` claim'inden ya da Admin API'den okunur. Uygulamanın kendi kodunda tek bir
statik rol adı bile yok — admin panelinin düzenleyebileceği rol listesi
(`KeycloakAdminClient.listRealmRoles()`) her seferinde doğrudan Keycloak realm'inden çekilir.

### 1) Keycloak (realm: `platform`) — kimlik + rol yönetimi, tek doğruluk kaynağı

| Veri | Nerede | Not |
|---|---|---|
| `username`, `email`, `firstName`, `lastName`, `enabled`, `createdTimestamp` | Keycloak user kaydı | `KeycloakAdminClient` üzerinden okunur/yazılır — MySQL'de hiçbir kopyası yok |
| Şifre (hash) | Keycloak credential store | Uygulama koduna **hiçbir zaman** ulaşmaz — `KeycloakTokenClient` şifreyi Keycloak'a iletir, hiçbir yerde saklamaz |
| `sub` (Keycloak user id) | Keycloak user id (UUID) | Uygulamadaki TEK kullanıcı tanımlayıcısı — `user_profile`/`user_contact`/`user_consent`'in PK/FK'si bu id, gerçek bir foreign key değil (Keycloak dışarıda) |
| Realm rolleri: `ADMIN`, `MANAGER`, `USER`, `AUDITOR`, ... | Keycloak realm role tanımları + JWT `realm_access.roles` claim'i | **Rol tanımı ve kullanıcı-rol ataması sadece burada var** — `KeycloakRoleMapper.isApplicationRole` JWT'den okurken Keycloak'ın kendi bookkeeping rollerini (`offline_access`, `default-roles-*`) eler; admin panelinin rol listesi `KeycloakAdminClient.listRealmRoles()` ile aynı realm'den canlı çekilir |
| Organizasyonlar | Keycloak Group'lar (`platform` realminde, alt-grup yok — bkz. `KeycloakAdminClient.createGroup`/`listGroups`) | Ayrı bir "organization" tablosu **yok** — her organizasyon bir Keycloak Group, üyelik de Group membership'i. `description` (Hakkımızda), `coverImageUrl`, `logoImageUrl`, `requiresApproval` gibi ek alanlar Group'un native alanı olmadığı için attribute olarak tutulur (`KeycloakAdminClientImpl.mergeGroupAttributes` - read-modify-write, tek attribute set etmek diğerlerini silmesin diye) |
| SSO session / refresh token durumu | Keycloak'ın kendi internal veritabanı | Uygulamanın hiçbir tablosunda karşılığı yok |
| Sosyal login federasyonu (Google/GitHub/Facebook) | Keycloak identity provider config | `docker/keycloak/realm-platform.json` → `identityProviders` (şu an `enabled: false`) |

### 2) MySQL (`platform_user` şeması) — sadece bu uygulamanın sahip olduğu veri: GDPR kategorileri, yetki matrisi, audit

Aşağıdaki tablolar `module-user`'a ait ve `platform_user` şemasında yaşıyor - `module-post` (ya da
eklenecek başka bir `module-*`) kendi tablolarını asla bu şemaya değil, kendi şemasına
(`platform_post`) yazar. Bkz. az önceki not.

Kimlik için ayrı bir tablo **yok** — `user_core` kaldırıldı (V10 migration). Aşağıdaki tablolar
kullanıcıyı doğrudan Keycloak'ın `sub` id'siyle (`keycloak_user_id` kolonu) referanslar.

**GDPR kategorileri** — her biri ayrı tabloda, birbirinden bağımsız export/silinebilsin diye:

| Tablo | Kolonlar | Kategori |
|---|---|---|
| `user_profile` | `keycloak_user_id` (PK), `full_name`, `birth_date`, `avatar_url`, `locale`, `deleted_at` | Temel profil — `deleted_at` right-to-erasure soft-delete alanı |
| `user_contact` | `keycloak_user_id` (PK), `phone_number`, `alternate_email`, `address_line`, `city`, `country` | İletişim bilgisi |
| `user_consent` | `id`, `keycloak_user_id`, `consent_type`, `legal_basis`, `purpose`, `granted_at`, `revoked_at`, `ip_address` | Onay kayıtları (GDPR Art. 6 legal basis) — hangi verinin hangi gerekçeyle toplandığının kanıtı |

**Yetki matrisi** — rol↔izin ilişkisi (kullanıcı↔rol değil, o sadece Keycloak'ta):

| Tablo | Kolonlar | Not |
|---|---|---|
| `permission` | `id`, `key` (`resource:action` formatı), `ui_policy` (`HIDE_IF_DENIED` / `DISABLE_IF_DENIED`) | Güvenlik sınırı değil, UI davranış ipucu |
| `role_permission` | `id`, `role_name`, `permission_id`, `access_level` (`GRANTED` / `VISIBLE_DENIED`) | `role_name` artık lokal bir tabloya değil, doğrudan Keycloak realm rol ismine karşılık gelir — ayrı bir `role` tablosu yok. Gerçek yetki kontrolü (`MatrixPermissionEvaluator`) sadece `GRANTED` satırlarına bakar |

**Audit / tamper-evidence** — `platform-audit-starter` (Hibernate Envers) tarafından otomatik yönetilir:

| Tablo | Kolonlar | Not |
|---|---|---|
| `platform_rev_info` | `id`, `timestamp`, `username`, `client_ip`, `trace_id`, `record_hash`, `previous_hash` | Her değişiklik revizyonu — kim, nereden, ne zaman + zincir hash (DB'de doğrudan satır değiştirmeyi tespit eder) |
| `user_profile_aud`, `user_contact_aud`, `user_consent_aud`, `role_permission_aud` | İlgili tablonun tüm kolonları + `rev`, `revtype` (0=ADD/1=MOD/2=DEL) | `@Audited` işaretli her entity'nin tam geçmişi — kim ne zaman neyi değiştirdi |

**Organizasyon yönetimi** — kimlik değil, Keycloak Group üyeliğinin üstüne inşa edilen yerel
workflow/yetkilendirme verisi (`role_state` ile aynı kategori — Keycloak'ın hiç bilmediği, bu
uygulamaya özgü local policy). `@Audited` değiller (bkz. kendi entity yorumları):

| Tablo | Kolonlar | Not |
|---|---|---|
| `organization_membership_request` | `id`, `organization_id`, `keycloak_user_id`, `request_type` (`INVITE`/`JOIN_REQUEST`), `status` (`PENDING`/`APPROVED`/`REJECTED`), `initiated_by_keycloak_user_id`, `created_at`, `resolved_at` | Keycloak'ta "member / not-member" dışında bir "pending" kavramı yok — bekleyen davet/katılım talepleri burada tutulur. Bir talep `APPROVED` olduğu an gerçek Keycloak grup üyeliği o anda verilir (`OrganizationMembershipService` self-service tarafı: davet kabul/katılım talebi; `AdminOrganizationService` yönetici tarafı: davet gönderme/talep onaylama) |
| `organization_manager` | `id`, `organization_id`, `keycloak_user_id`, `granted_at`, `granted_by_keycloak_user_id` | Bir organizasyonu kimin yönetebileceğinin **tek** kaynağı — herhangi bir Keycloak rolüyle (örn. `MANAGER`) hiçbir ilgisi yok. Sadece bu tabloda satırı olan kullanıcı o organizasyonu yönetir; bir organizasyona üye olmak, onu yönetebilmek anlamına gelmez (bkz. `AdminAccessScopeService.resolve` — `organizationGroupIds` bu tablodan gelir, Keycloak grup üyeliğinden değil) |

### 3) Redis — sadece türetilmiş veri (cache), hiç kişisel veri yok

| Cache adı | İçerik | TTL |
|---|---|---|
| `role-permissions` | Rol adı → `GRANTED` izin listesi (`RolePermissionLookupService`) | 300s (`application.yml` → `platform.cache.ttls`) |
| `role-permissions-visible-denied` | Rol adı → `VISIBLE_DENIED` izin listesi | `default-ttl` 120s |
| `role-permissions-hidden` | Rol adı → `HIDDEN` izin listesi | `default-ttl` 120s |

`role_permission` tablosu değiştiğinde (`RolePermissionLookupService`) her üç cache de `@CacheEvict(allEntries = true)` ile tamamen temizlenir.

### 4) Vault — sır (secret) yönetimi, kullanıcı verisi değil

`DB_PASSWORD`, `KEYCLOAK_CLIENT_SECRET`, `KEYCLOAK_ADMIN_CLIENT_SECRET` gibi bağlantı sırları
`secret/platform-app` altında tutulur (`spring.config.import: vault://`). Bu bir kullanıcı/iş
verisi deposu değil, sadece uygulamanın diğer depolara bağlanmak için kullandığı kimlik
bilgilerinin merkezi saklama yeri.

## Konuşulan kararların kod karşılığı

| Karar | Nerede |
|---|---|
| Modüler/izole yapı, micro-ready | `platform-*` starter'lar + `module-user` ayrımı |
| OAuth2 (Google/GitHub/Facebook) + Keycloak altyapısı | `platform-security-starter` — `issuer-uri` tek değişen satır |
| Config-driven her şey | `application.yml` (`platform.cache.ttls.*` gibi), DB-driven `role_permission` matrisi |
| GDPR kategorize user data | `module-user/.../entity/{UserProfile,UserContact,UserConsent}` - ayrı tablolar, kimlik Keycloak'ta |
| Yetki matrisi, DB'den yönetilebilir; roller Keycloak'ta | `module-user/.../entity/{Permission,RolePermission}` (`RolePermission.roleName` → Keycloak realm rolü) |
| Permission-aware UI (hide/disable/enable) | `UiPermissionsController` (`/api/me/ui-permissions`) + `platform-web/src/components/PermissionButton.tsx` |
| Envers + tamper-evident audit | `platform-audit-starter` (`PlatformRevisionListener` - hash chain) |
| Finans-grade hata yönetimi | `platform-error-starter` (Business/Technical/Security exception ayrımı) |
| Redis cache, annotation ile | `platform-cache-starter` (`@Cacheable("role-permissions")`) |
| Health check (liveness/readiness) | `application.yml` → `management.endpoint.health.probes.enabled=true` |
| Keycloak çağrılarında circuit breaker + retry + timeout | `platform-security-starter/.../KeycloakAdminClientImpl` (`@CircuitBreaker`/`@Retry`, instance: `keycloakAdmin`) + `application.yml` → `resilience4j.*`, `spring.http.client.*` |
| MySQL otomatik yedekleme | `docker-compose.yml` → `mysql-backup` servisi, `docker/mysql/backup.sh` — bkz. "Yedekleme" notu yukarıda |
| Alerting (servis down, 5xx oranı, JVM heap, circuit breaker durumu) | `docker-compose.yml` → `alertmanager` servisi (`observability` profile), `docker/prometheus/alert-rules.yml` |
| Vault ile secret yönetimi | `application.yml` → `spring.config.import: vault://`, `docker-compose.yml` → `vault` servisi |
| Maven | Tüm proje Maven multi-module |
| Katmanlı paket yapısı, MapStruct ile model dönüşümü | `module-user`, `platform-app`, `platform-security-starter/integration` — bkz. "Katmanlı paket yapısı" bölümü |
| Çoklu-kiracı organizasyon yapısı (Keycloak Group = organizasyon) | `AdminOrganizationService` (yönetim), `OrganizationDirectoryService` (herkese açık gözatma), `KeycloakAdminClient.createGroup`/`getGroup`/`mergeGroupAttributes` |
| Organizasyon yönetim yetkisi role değil, açık bir tabloya bağlı (rol sadece platform genelini belirler) | `module-user/.../entity/OrganizationManager`, `AdminAccessScopeService.resolve` — `RoleScope` artık sadece `NONE`/`PLATFORM` |
| Davet + kendiliğinden katılım, ikisi de organizasyon bazında onaya tabi olabilir | `module-user/.../entity/OrganizationMembershipRequest`, `OrganizationMembershipService` (kullanıcı tarafı: kabul/red/katıl/ayrıl), `AdminOrganizationService` (yönetici tarafı: davet gönder/talep onayla-reddet) |
| Her organizasyonun ve her kullanıcının herkese açık bir profil sayfası | `OrganizationDirectoryController`/`UserDirectoryController` (`/api/organizations/{id}`, `/api/users/{id}`), `platform-web/src/pages/organizations`, `platform-web/src/pages/users` |

## Bilinçli olarak eksik bırakılanlar (sıradaki adımlar)

- **Sosyal login (Google/GitHub/Facebook)**: `realm-platform.json` içine `identityProviders`
  olarak eklendi (`enabled: false`, placeholder client-id/secret ile) — Google/GitHub/Facebook
  Developer Console'larından gerçek OAuth uygulaması oluşturup `REPLACE_WITH_*` alanlarını
  doldurduktan sonra `enabled: true` yapman yeterli, başka bir şey değişmiyor.
- **Idempotency key altyapısı, maker-checker pattern, outbox pattern**: henüz kod yok,
  ilk `payment`/işlem modülü yazılırken `platform-error` ve `platform-audit` üzerine inşa edilecek.
- **Rate limiting (Bucket4j), field-level encryption, TDE**: henüz eklenmedi.
- **DB kullanıcı yetkilerini `_AUD` tablolarında INSERT-only yapma**: `V4` migration'ının
  sonunda örnek SQL var, gerçek DBA script'i olarak ayrıca çalıştırılmalı.
- **`platform-web`'de refresh token'ın `localStorage`'da tutulması**: bilinçli bir
  basitleştirme - bir demo/starter için yeterli, ama XSS'e karşı üretimde httpOnly cookie +
  BFF (backend-for-frontend) pattern'i ya da en azından refresh token'ı memory-only tutup
  sayfa yenilemede yeniden login isteme değerlendirilmeli.
- **Otomatik test yok**: ne backend'de (JUnit) ne frontend'de (Vitest/Playwright) tek bir test
  dosyası bulunmuyor - bugüne kadarki tüm doğrulama elle (curl / tarayıcı) yapıldı. Çoklu-kiracı
  izolasyonu gibi güvenlik açısından kritik davranışların kalıcı bir regresyon güvencesi yok.
- **Login'de rate limiting yok**: `KeycloakTokenClient`'ın parola-grant çağrısına kaba kuvvet
  (brute-force) koruması eklenmedi.

## Yeni bir modül eklemek

`module-post` tam olarak bu adımların canlı örneği - şu an boş bir iskelet (`pom.xml` + paket
yapısı, hiç entity/migration yok), "içini doldurmak" bundan sonraki adımlar.

1. `module-<isim>` adında yeni bir Maven modülü aç, `platform-parent`'a `<parent>` yap
2. İhtiyaç duyduğu `platform-*` starter'ları dependency olarak ekle; MapStruct kullanacaksan
   `mapstruct`/`mapstruct-processor` dependency'lerini ve `annotationProcessorPaths` konfigürasyonunu
   da ekle (yukarıdaki "Katmanlı paket yapısı" bölümündeki not)
3. Kendi MySQL şemasını aç: `platform_<isim>` (bkz. `docker/mysql/init-schemas.sql`'e bir
   `CREATE DATABASE IF NOT EXISTS platform_<isim>` + `GRANT` satırı ekle) - migration'larını asla
   `platform_user`'a değil, kendi şemasına yaz. Yerel geliştirmede hâlâ tek `DataSource`/tek
   Spring Boot süreci üzerinden gidiyoruz; şema ayrımı, ileride bu modülü kendi DataSource'una (ya
   da kendi DB sunucusuna) taşırken veri taşıma projesi gerekmemesi için
4. Yukarıdaki "Katmanlı paket yapısı"nı izle: `entity/repository/constant/controller(+model)/
   service(+model)/mapper`
5. Entity'lerini `@Audited` işaretle (otomatik olarak Envers'e dahil olur)
6. Kritik endpoint'lerini `@PreAuthorize("hasPermission('<resource>', '<action>')")` ile koru
7. `platform-app/pom.xml`'e yeni modülü dependency olarak ekle
