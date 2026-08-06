# SargO OpenVPN integracija

Šis dokumentas aprašo `feature/sargo-integration` šakos `sargo` produkto skonio (flavor)
realizaciją, skirtą valdyti `ics-openvpn` per SargO MDM.

## Bendra struktūra

SargO specifinis kodas izoliuotas `main/src/sargo/` kataloge:

- `AndroidManifest.xml` — SargO įėjimo taškas, config broadcast receiver ir UI manifesto įrašai.
- `java/de/blinkt/openvpn/sargo/` — Android komponentai (launcher activity, config receiver).
- `java/pro/sargo/openvpn/` — VPN konfigūracijos importo ir valdymo logika.
- `res/values/strings.xml` — SargO flavor programos pavadinimas.

## SargO MDM biblioteka

AAR gaunamas iš atskiro `SargO/sargo/launcher/lib/` projekto:

```bash
cd /Users/eduardas/Documents/SargO/sargo/launcher
sh gradlew :lib:assembleRelease
```

Sukurtas failas perkeliamas į šį projektą:

```bash
cp /Users/eduardas/Documents/SargO/sargo/launcher/lib/build/outputs/aar/lib-release.aar \
   /Users/eduardas/Documents/SargO_openVPN/ics-openvpn/main/src/sargo/libs/sargo-mdm-lib-release.aar
```

`main/build.gradle.kts` priklausomybė rodo į release AAR:

```kotlin
sargoImplementation(files("src/sargo/libs/sargo-mdm-lib-release.aar"))
```

## Build komandos

```bash
# Debug buildas
./gradlew :main:assembleSargoOvpn23Debug

# Release buildas (reikia release signing nustatymų)
./gradlew :main:assembleSargoOvpn23Release

# Unit testai
./gradlew test
```

## Release signing

Release buildas pasirašomas testiniu raktu, kurio duomenys saugomi
`~/.gradle/gradle.properties`:

```properties
keystoreFile=/Users/eduardas/.sargo-release.keystore
keystorePassword=sargorelease
keystoreAlias=sargoreleasekey
keystoreAliasPassword=sargorelease
```

Keystore sukurtas komanda:

```bash
keytool -genkey -v \
  -keystore /Users/eduardas/.sargo-release.keystore \
  -alias sargoreleasekey \
  -keyalg RSA -keysize 2048 -validity 36500 \
  -storepass sargorelease -keypass sargorelease \
  -dname "CN=SargO Release Test, OU=SargO, O=SargO, L=Vilnius, C=LT"
```

> **Dėmesio:** šis raktas yra testinis. Realioje gamyboje naudoti CI aplinkos
> paslaptis (GitHub Actions secrets) ir atskirą gamybos raktą.

## SargO konfigūracijos raktai

SargO serverio pusėje per application configuration siunčiami šie raktaivirtės:

| Raktas | Tipas | Paskirtis |
|---|---|---|
| `vpn_name` | tekstas | Profilio pavadinimas (privalomas). |
| `vpn_config` | tekstas / URI | `.ovpn` kelias arba `content://` URI. |
| `vpn_config_content` | tekstas | Inline `.ovpn` turinys; turi pirmenybę prieš `vpn_config`. |
| `connect` | `0` / `1` | Ar prisijungti iškart po importo. |
| `always_on` | `0` / `1` | Ar nustatyti always-on VPN. |
| `remove` | tekstas | Kableliais atskirti šalinamų profilių pavadinimai. |
| `remove_all` | `0` / `1` | Ar pašalinti visus kitus profilius. |
| `disconnect_on_config_change` | `0` / `1` | Ar atjungti aktyvų VPN prieš taikant naują konfigą. |
| `allow_user_disconnect` | `0` / `1` | Ar vartotojas gali atsijungti nuo VPN. |
| `auto_reconnect` | `0` / `1` | Ar automatiškai atstatyti ryšį po tinklo pokyčių. |
| `vpn_username` | tekstas | OpenVPN vartotojo vardas (optional). |
| `vpn_password` | tekstas | OpenVPN slaptažodis (optional). |
| `log_level` | `0`–`11` | OpenVPN `verb` lygis (optional). |

## Papildomų laukų elgsena

### `vpn_username` / `vpn_password`

`IOpenVPNAPIService.addNewVPNProfile` neturi tiesioginių `username`/`password`
parametrų. Todėl prieš importą konfigas apdorojamas `OpenVpnConfigPostProcessor`:

- Jei nurodytas `vpn_username`, iš konfigo pašalinamos visos egzistuojančios
  `auth-user-pass` eilutės ir įterpiamas inline blokas:

  ```
  <auth-user-pass>
  username
  password
  </auth-user-pass>
  ```

- `auth-user-pass-verify` ir panašios direktyvos lieka nepaliestos.
- Jei `vpn_username` nenurodytas, konfigas paliekamas toks, koks yra.

### `log_level`

`IOpenVPNAPIService` neturi metodo log lygiui keisti. `OpenVpnConfigPostProcessor`
pašalina esamas `verb N` eilutes ir įterpia `verb <log_level>` konfigo pabaigoje.
OpenVPN apdoroja paskutinę `verb` direktyvą, todėl injektuota reikšmė nustato
realų log lygį. Jei `log_level` nenurodytas, konfigo `verb` direktyvos nekeičiamos.

### `allow_user_disconnect=false`

Kai `allow_user_disconnect=0` ir `always_on` nėra aiškiai išjungtas (`0`),
`SargoVpnController` automatiškai įjungia always-on VPN su lockdown režimu.
Tai neleidžia vartotojui atjungti VPN per Android nustatymus. Greitų nustatymų
plytelė (`OpenVPNTileService`) ir `MainActivity` atsijungimo mygtukas lieka
standartiniai, bet Android always-on lockdown užkerta kelią faktiniam
atsijungimui.

### `auto_reconnect=true`

Kai `auto_reconnect=1`, `OpenVpnConfigPostProcessor` į konfigą prideda
`persist-tun` ir `persist-key` direktyvas (jei jų dar nėra). Jos leidžia
OpenVPN core po ryšio nutrūkimo atkurti tunelį be profilio perkrovimo.
Tikslesnis reconnect elgesys priklauso nuo OpenVPN core ir serverio pusės
`keepalive` / `ping-restart` nustatymų.

## End-to-end testas

Atliktas emuliatoriuje:

1. SargO serveryje sukonfigūruota VPN programa su inline `.ovpn` turiniu.
2. Įrenginys gavo config push, `SargoConfigReceiver` paleido `SargoLauncherActivity`.
3. `SargoVpnController` importavo profilį, prisijungė ir įjungė always-on VPN.
4. Patikrinta, kad `Settings → Network & Internet → VPN → Always-on` rodo
   aktyvų SargO OpenVPN profilį.
5. Išjungus emuliatoriaus Wi-Fi ir vėl įjungus, VPN atsistatė automatiškai
   (su `auto_reconnect=1`).

## Apribojimai

- Pakeitimai lieka `main/src/sargo/` ir `main/build.gradle.kts`.
- `main/src/main/cpp/`, `main/src/main/java/core/` ir `MainActivity.kt` esminė
  logika nekeičiama.
- Nenaudojamas `AsyncTask` ir `Environment.getExternalStorageDirectory()`.
- Admin komponentas gaunamas per `SargoMDM.getAdminComponent(context)`.
