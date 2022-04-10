OpenVPN for Android
=============
![build status](https://github.com/schwabe/ics-openvpn/actions/workflows/build.yaml/badge.svg)


Description
------------
In Android 4.0 (Ice Cream Sandwich) a VPN service can be made without root access, using the VPNService of API level 14+. \
This project is a port of OpenVPN.

<a href="https://f-droid.org/repository/browse/?fdid=de.blinkt.openvpn" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=de.blinkt.openvpn" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>

Developing
----------
Read the [doc/README.txt](https://github.com/schwabe/ics-openvpn/blob/master/doc/README.txt) *before* opening issues or e-mailing. 

To retain my ability to re-license the project for different third-parties I probably need a [contributor agreement](https://www.clahub.com/agreements/schwabe/ics-openvpn) from any contributing party.

Translations
------------
Help [translate](https://hosted.weblate.org/projects/openvpn-for-android/) the app to your language.
<a href="https://hosted.weblate.org/engage/openvpn-for-android/">
<img src="https://hosted.weblate.org/widgets/openvpn-for-android/-/horizontal-blue.svg" alt="Oversettelsesstatus" />
</a>

FAQ
-----
You can find the FAQ here (same as in app): https://ics-openvpn.blinkt.de/FAQ.html

Controlling from external apps
------------------------------

The AIDL API offers real control. (More about this in the developing section.) \
Due to high demand also acitvies to start/stop, pause/resume (like a user would with the notification) exists:
  
 - `de.blinkt.openvpn.api.DisconnectVPN`
 - `de.blinkt.openvpn.api.ConnectVPN`
 - `de.blinkt.openvpn.api.PauseVPN`
 - `de.blinkt.openvpn.api.ResumeVPN`

They use `de.blinkt.openvpn.api.profileName` as extra addition for the name of the VPN profile…

Note to administrators
------------------------

Make your life and that of your users easier by embedding the certificates into the .ovpn file. \
You or the users can mail the .ovpn as a attachment to the phone and directly import and use it. Also downloading and importing the file works. \
The media type should be "application/x-openvpn-profile".

Inline files are supported since OpenVPN 2.1rc1 and documented in the  [OpenVPN 2.3 man page](https://community.openvpn.net/openvpn/wiki/Openvpn23ManPage) (under INLINE FILE SUPPORT).

(Using inline certificates can also make your life on non-Android platforms easier since you only have one file.)

For example `ca mycafile.pem` becomes
```
  <ca>
  -----BEGIN CERTIFICATE-----
  MIIHPTCCBSWgAwIBAgIBADANBgkqhkiG9w0BAQQFADB5MRAwDgYDVQQKEwdSb290
  [...]
  -----END CERTIFICATE-----
  </ca>
```
Footnotes
-----------
Please note that OpenVPN used by this project is under GPLv2+.

If you cannot or do not want to use the Play Store you can [download the APK files directly](http://plai.de/android/).

If you want to be a hero, donate to [arne-paypal@rfc2549.org via paypal](https://www.paypal.com/cgi-bin/webscr?hosted_button_id=R2M6ZP9AF25LS&cmd=_s-xclick), or alternatively if you believe in fancy Internet money (or not) you can use Bitcoin: 1EVWVqpVQFhoFE6gKaqSkfvSNdmLAjcQ9z 

The old official or main repository was a Mercurial (hg) repository at http://code.google.com/p/ics-openvpn/source/

The new Git repository is now at GitHub under https://github.com/schwabe/ics-openvpn

Please read the doc/README before asking questions or starting development.
