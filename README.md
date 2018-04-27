OpenVPN for Android -EduVPN fork
=============
[![Build Status](https://travis-ci.org/schwabe/ics-openvpn.svg?branch=master)](https://travis-ci.org/schwabe/ics-openvpn)

The EduVPN changes are:

* [Explicitly import support-v4 library](https://github.com/eduvpn/ics-openvpn/commit/b5eb68ea8749314342cd454cdda7766dbb36977c): Support-v4 was removed as a transitional dependency, so we have to import it explicitly now for the parent activity fix.
* [Librarify module](https://github.com/eduvpn/ics-openvpn/commit/a7529a8093db723b2dd1809adcde84700d9dd7c4): This converts the submodule to a library with as less changes as possible.
* [Change activity which opens when tapping notification](https://github.com/eduvpn/ics-openvpn/commit/5d1778af0b5e43faaa24e2356c5383f5fd457dfe): Opens a different activity when tapping on the notification
* [Ability to get IPv4 and IPv6 addresses from the service](https://github.com/eduvpn/ics-openvpn/commit/ec9097df75af7be93beaea0e82ab544420d0569e): Allows us to get the IPv4 and IPv6 addresses.
* [Fix parent activity problem](https://github.com/eduvpn/ics-openvpn/commit/6ba82fdccb86ccd61b11c92d527ac7014f9ff06f): Opens the correct parent activity from the log window.

Description
------------
With the new VPNService of Android API level 14+ (Ice Cream Sandwich) it is possible to create a VPN service that does not need root access. This project is a port of OpenVPN.

<a href="https://f-droid.org/repository/browse/?fdid=de.blinkt.openvpn" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=de.blinkt.openvpn" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>

Developing
---------------
If you want to develop on ics-openvpn please read the [doc/README.txt](https://github.com/schwabe/ics-openvpn/blob/master/doc/README.txt) *before* opening issues or emailing me. 

Also please note that before contributing to the project that I would like to retain my ability to relicense the project for different third parties and therefore probably need a contributer's agreement from any contributing party. To get started, [sign the Contributor License Agreement](https://www.clahub.com/agreements/schwabe/ics-openvpn).

You can help
------------
Even if you are no programmer you can help by translating the OpenVPN client into your native language. [Crowdin provides a free service for non commercial open source projects](http://crowdin.net/project/ics-openvpn/invite) (Fixing/completing existing translations is very welcome as well)

FAQ
-----
You can find the FAQ here (same as in app): http://ics-openvpn.blinkt.de/FAQ.html

Note to administrators
------------------------

You make your life and that of your users easier if you embed the certificates into the .ovpn file. You or the users can mail the .ovpn as a attachment to the phone and directly import and use it. Also downloading and importing the file works. The MIME Type should be application/x-openvpn-profile. 

Inline files are supported since OpenVPN 2.1rc1 and documented in the  [OpenVPN 2.3 man page](https://community.openvpn.net/openvpn/wiki/Openvpn23ManPage) (under INLINE FILE SUPPORT) 

(Using inline certifaces can also make your life on non-Android platforms easier since you only have one file.)

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
Please note that OpenVPN used by this project is under GPLv2. 

If you cannot or do not want to use the Play Store you can [download the apk files directly](http://plai.de/android/).

If you want to donate you can donate to [arne-paypal@rfc2549.org via paypal](https://www.paypal.com/cgi-bin/webscr?hosted_button_id=R2M6ZP9AF25LS&cmd=_s-xclick), or alternatively if you believe in fancy Internet money you can use Bitcoin: 1EVWVqpVQFhoFE6gKaqSkfvSNdmLAjcQ9z 

The old official or main repository was a Mercurial (hg) repository at http://code.google.com/p/ics-openvpn/source/

The new Git repository is now at GitHub under https://github.com/schwabe/ics-openvpn

Please read the doc/README before asking questions or starting development.
