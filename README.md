OpenVPN for personalDNSfilter
=============
This is a fork of the famous OpenVPN for Android project (https://github.com/schwabe/ics-openvpn) for integrating OpenVPN on Android with a locally running personalDNSfilter on non-rooted Android.
The use case is to connect to a real remote (Open) VPN server while using locally running personalDNSfilter for filtering.

For integrating with personalDNSfilter (minimal Version 1.50.38), select the Option "DNS Proxy Mode" within the personalDNSfilter advanced settings. In OpenVPN for personalDNSfilter, configure own DNS Server "10.10.10.10" within the "IP and DNS" configuration section. Without this special DNS, OpenVPN for personalDNSfilter will behave exactly as the original OpenVPN for Android application.

Download
--------
Current Version is based on OpenVPN for Android 0.7.21 and includes StreamCapture 0.0.3 for redirecting the DNS traffic to personalDNSfilter.
Download: https://www.zenz-solutions.de/personaldnsfilter/getOpenVPNpDNSf.php.

Footnotes
-----------
Please note that OpenVPN used by this project is under GPLv2. 
Same rules and licenses apply as for the original OpenVPN for Android project under https://github.com/schwabe/ics-openvpn.
