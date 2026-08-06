# SargO OpenVPN integracijos planas

## Sesijos tikslai

1. Release SargO MDM lib.
2. Release signing.
3. Papildomi konfigūracijos laukai (`vpn_username`, `vpn_password`, `log_level`).
4. `allow_user_disconnect` ir `auto_reconnect` poveikis.

## Būsena

| # | Užduotis | Būsena |
|---|---|---|
| 1 | Release SargO MDM lib AAR | Atlikta |
| 2 | Release signing | Atlikta |
| 3 | `vpn_username`, `vpn_password`, `log_level` | Atlikta |
| 4 | `allow_user_disconnect` / `auto_reconnect` | Atlikta |
| 5 | README-SARGO.md ir plan.md atnaujinimas | Atlikta |
| 6 | Build/test patikra | Atlikta |

## Atlikti veiksmai

### 1. Release SargO MDM lib

- Sukurtas release buildas `SargO/sargo/launcher/lib/`:
  ```bash
  cd /Users/eduardas/Documents/SargO/sargo/launcher
  sh gradlew :lib:assembleRelease
  ```
- Gautas `lib-release.aar` nukopijuotas į
  `main/src/sargo/libs/sargo-mdm-lib-release.aar`.
- `main/build.gradle.kts` priklausomybė pakeista iš debug į release AAR.

### 2. Release signing

- Pašalinti testiniai signing credentials iš `~/.gradle/gradle.properties`.
- `main/build.gradle.kts` papildytas `signingProperty()` helperiu, kuris skaito
  iš project properties arba environment variables (uppercase snake_case).
- `.github/workflows/build.yaml` pakeistas: vietoj debug signing naudojamas
  realus CI raktas per secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
  `KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASSWORD`), su fallback į debug signing
  kai secrets nėra pasiekiami.
- Pridėtas `SargoOvpn23` į CI build matrix.

### 3. Papildomi konfigūracijos laukai

- `SargoVpnConfig` papildytas laukais `vpnUsername`, `vpnPassword`, `logLevel`.
- `SargoVpnConfigProvider` nuskaito juos iš SargO app preferences:
  `vpn_username`, `vpn_password`, `log_level`.
- Sukurtas `OpenVpnConfigPostProcessor`, kuris prieš importą injektuoja:
  - inline `<auth-user-pass>` bloką,
  - `verb <log_level>`,
  - `persist-tun` / `persist-key` (kai `auto_reconnect=true`).
- `VpnProfileImporter` papildytas optional `configTransformer` lambda.
- Pridėti `SargoVpnConfigTest` ir `OpenVpnConfigPostProcessorTest` testai.

### 4. `allow_user_disconnect` ir `auto_reconnect`

- `allow_user_disconnect=false` + `always_on != false` → `SargoVpnController`
  automatiškai įjungia always-on VPN su lockdown.
- `auto_reconnect=true` → `OpenVpnConfigPostProcessor` prideda `persist-tun`
  ir `persist-key`.

## Patikros komandos

```bash
./gradlew :main:assembleSargoOvpn23Debug
./gradlew :main:assembleSargoOvpn23Release
./gradlew test
```

## Rezultatai

- `:main:assembleSargoOvpn23Debug` — OK.
- `:main:assembleSargoOvpn23Release` — OK (pasirašytas testiniu raktu).
- `test` — OK (visi SargO unit testai praeina).
- Release AAR naudojamas sėkmingai.
- Emuliatoriuje (`emulator-5554`) įdiegtas debug APK ir paleista
  `SargoLauncherActivity`; SargO MDM prisijungė, konfigūracijos aplikavimo
  flow suveikė iki always-on VPN įjungimo. Pilnas naujų laukų
  (`vpn_username`, `vpn_password`, `log_level`) end-to-end patikrinimas
  lauks SargO serverio config atnaujinimo.

## Todo / galimi tolimesni žingsniai

- Pakeisti testinį release raktą į realų CI pasirašymo raktą.
- Išbandyti `vpn_username`/`vpn_password` su realiu OpenVPN serveriu.
- Įvertinti ar reikia blokuoti `OpenVPNTileService` / `MainActivity` disconnect
  UI lygmeniu, jei always-on lockdown nepakankamai riboja tam tikruose OEM
  Android variantuose.
