# OpenVPN for Android

Open Source OpenVPN client for Android.

![OpenVPN for Android][logo]

## DESCRIPTION
With the new [VPNService][vpnservice] of Android (API level 14+ / Ice Cream
Sandwich) it is now possible to create a VPN services that do not require root
privileges and hence can be used without any modifications to your device. This
project is a port of [OpenVPN][openvpn] implementing this very API.

It differentiates itself from [OpenVPN Connect][openvpnconnect] by being
completely Open Source, whereas the former is a proprietary product of OpenVPN
Technologies, Inc.

## DEVELOPERS
If you want to develop on `ics-openvpn`, please read [doc/README.txt][readme]
*before* opening any issues or mailing me. It contains not only instructions
on how to build the software, but also some remarks on re-using this code
within own projects.

## TRANSLATIONS
Translations are managed through [Crowdin][crowdin], since they provide a free
service for non commercial Open Source projects.

Fixing, extending and/or completing already existing translations is very
welcomed and can also be easily handled by non-programmers.

## FAQ
You can find the FAQ [here][faq]. It is the same version that is also embedded
into the application itself and covers questions frequently asked.

## NOTE TO ADMINISTRATORS
You will make your life and that of your users easier, if you embed the
certificates directly into the `.ovpn` file using the inline file support. You
or the users can then mail the file as an attachment to the phone and directly
import it there. Downloading and importing the file works also. The MIME-Type
should be `application/x-openvpn-profile`.

Inline files are supported since OpenVPN 2.1rc1 and are documented in the
appropriate [man page][manpage] (section INLINE FILE SUPPORT).

Using inline certificates will also make your life on non Android platforms
easier, since you have only one file, which makes it more portable.

For example `ca mycafile.pem` becomes:

    <ca>
    -----BEGIN CERTIFICATE-----
    MIIHPTCCBSWgAwIBAgIBADANBgkqhkiG9w0BAQQFADB5MRAwDgYDVQQKEwdSb290
    [...]
    -----END CERTIFICATE-----
    </ca>

## DONATIONS

Developing this applications takes time and effort. Therefore donations are
very appreciated through one of the following means:

[![PayPal donation](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif "PayPal")][paypal]

[![Flattr this git repo](http://api.flattr.com/button/flattr-badge-large.png "Flattr This!")][flattr]

Thank you very much for your support!

## FOOTNOTES
Please note that OpenVPN used in this project is licensed under [GPLv2][gplv2].

If you cannot or do not want to use the Play Store you can also download the
APK files [directly][apk].

The old official repository was based on Mercurial (hg) and residing over at
[Google code][googlecode], but has since been migrated to [GitHub][github].

[logo]: https://github.com/schwabe/ics-openvpn/blob/master/misc/icon-512.png
[vpnservice]: https://developer.android.com/reference/android/net/VpnService.html
[openvpn]: https://openvpn.net/
[readme]: https://github.com/schwabe/ics-openvpn/blob/master/doc/README.txt
[crowdin]: http://crowdin.net/project/ics-openvpn/invite
[openvpnconnect]: https://play.google.com/store/apps/details?id=net.openvpn.openvpn
[faq]: http://code.google.com/p/ics-openvpn/wiki/FAQ
[manpage]: https://community.openvpn.net/openvpn/wiki/Openvpn23ManPage
[paypal]: https://www.paypal.com/cgi-bin/webscr?hosted_button_id=R2M6ZP9AF25LS&cmd=_s-xclick
[gplv2]: https://www.gnu.org/licenses/gpl-2.0.html
[apk]: http://plai.de/android/
[googlecode]: http://code.google.com/p/ics-openvpn/source/
[github]: https://github.com/schwabe/ics-openvpn
[flattr]: https://flattr.com/submit/auto?url=https://github.com/schwabe/ics-openvpn

