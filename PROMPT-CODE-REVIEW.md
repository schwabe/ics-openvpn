# Prompt — SargO OpenVPN integration code review

Naudoti naujoje švarioje sesijoje. Kalba: lietuvių.

## Kontekstas

Projektas `Sargo-project/Sargo_openVPN` yra forkas nuo `schwabe/ics-openvpn`.
Daromas branch: `feature/sargo-integration`.

SargO integracija izoliuota `main/src/sargo/` kataloge ir `main/build.gradle.kts`.
Nauji failai: `README-SARGO.md`, `plan.md`.

## Sesijos tikslas

Atlikti kodo peržiūrą (code review) SargO OpenVPN integracijos pakeitimams.
Įvertinti:
- Ar implementacija teisinga ir funkcionali.
- Ar atitinka `plan.md` uždavinius.
- Ar laikomasi gerųjų Android / Kotlin / Gradle praktikų.
- Ar nėra saugumo spragų (secrets handling, permissions, input validation).
- Ar pakeitimai nepažeidžia apribojimų (žr. žemiau).

## Peržiūrimi failai

```
main/build.gradle.kts
main/src/sargo/AndroidManifest.xml
main/src/sargo/java/de/blinkt/openvpn/sargo/SargoConfigReceiver.kt
main/src/sargo/java/de/blinkt/openvpn/sargo/SargoLauncherActivity.kt
main/src/sargo/java/pro/sargo/openvpn/AlwaysOnVpnManager.kt
main/src/sargo/java/pro/sargo/openvpn/OpenVpnConfigPostProcessor.kt
main/src/sargo/java/pro/sargo/openvpn/SargoVpnConfig.kt
main/src/sargo/java/pro/sargo/openvpn/SargoVpnConfigProvider.kt
main/src/sargo/java/pro/sargo/openvpn/SargoVpnController.kt
main/src/sargo/java/pro/sargo/openvpn/VpnProfileImporter.kt
main/src/sargo/res/values/strings.xml
main/src/testSargo/java/pro/sargo/openvpn/OpenVpnConfigPostProcessorTest.kt
main/src/testSargo/java/pro/sargo/openvpn/SargoVpnConfigTest.kt
main/src/testSargo/java/pro/sargo/openvpn/VpnProfileImporterTest.kt
main/src/sargo/libs/sargo-mdm-lib-release.aar
.github/workflows/build.yaml
README-SARGO.md
plan.md
```

## Įvertinimo kriterijai

1. **Atitiktis planui**
   - Ar padaryti visi `plan.md` § Būsena punktai?
   - Ar `vpn_username`, `vpn_password`, `log_level` nuskaitomi ir integruoti?
   - Ar `allow_user_disconnect=false` ir `auto_reconnect=true` turi apibrėžtą elgseną?

2. **Teisingumas**
   - Ar `OpenVpnConfigPostProcessor` teisingai apdoroja configą (auth-user-pass, verb, persist-tun)?
   - Ar `SargoVpnController` teisingai valdo always-on VPN ir lockdown?
   - Ar content:// URI importas veikia be `Environment.getExternalStorageDirectory()`?

3. **Gerosios praktikos**
   - Ar kodas yra modulinis, testuojamas, be perteklinės logikos?
   - Ar nėra deprecated API (`AsyncTask`, etc.)?
   - Ar `IOpenVPNAPIService` naudojamas teisingai?

4. **Saugumas**
   - Ar `vpn_password` nėra logginamas arba saugomas nešifruotai ilgą laiką?
   - Ar signing secrets nepatenka į git / logs?
   - Ar CI workflow saugiai naudoja secrets (env vars, ne command args)?
   - Ar `allowUserDisconnect=false` tikrai užkerta kelią vartotojo atsijungimui?
   - Ar SargO admin component gaunamas tik per `SargoMDM.getAdminComponent()`?
   - Ar nėra hardcoded `pro.sargo.launcher` / AdminReceiver?

5. **Apribojimų laikymasis**
   - Pakeitimai lieka `main/src/sargo/` arba `main/build.gradle.kts`.
   - Nepakeista `main/src/main/cpp/`, `main/src/main/java/core/`, `MainActivity.kt` esminė logika.
   - Nenaudojamas `AsyncTask` ir `Environment.getExternalStorageDirectory()`.
   - Nehardkoduojamas `pro.sargo.launcher` / AdminReceiver.

## Išvedimo formatas

Pateik struktūruotą ataskaitą:

```
## Santrauka
- Bendras įvertinimas: [OK / Reikia pataisymų / Kritiška]
- Surasta problemų: N
- Rekomendacijų: M

## Rasti trūkumai
1. **[Svarba]** Trumpas aprašymas.
   - Failas: eilutė(-ės)
   - Kodėl problema
   - Pataisymo pasiūlymas (konkretus kodas)

## Saugumo pastabos
...

## Rekomendacijos
...

## Teigiami aspektai
...
```

Jei viskas tvarkinga, pabaik sakiniu: "Kritinių problemų nerasta."

## Papildomi veiksmai

Jei aptinki problemas, pasiūlyk jas ištaisyti šioje sesijoje arba įtraukti į `plan.md` TODO sąrašą.
