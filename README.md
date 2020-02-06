OpenVPN for personalDNSfilter
=============
This is a fork of the famous OpenVPN for Android project (https://github.com/schwabe/ics-openvpn) for integrating OpenVPN on Android with a locally running personalDNSfilter.
The use case is to connect to a real remote (Open) VPN server while using locally running personalDNSfilter for filtering.

For integrating with personalDNSfilter, configure own DNS Server "10.10.10.10" within the "IP and DNS" configuration section of OpemVPN for personalDNSfilter. Without this special DNS, OpenVPN for personalDNSfilter will behave exactly as the original OpenVPN for Android application. 


Footnotes
-----------
Please note that OpenVPN used by this project is under GPLv2. 
Same rules and licenses apply as for the original OpenVPN for Android project under https://github.com/schwabe/ics-openvpn.
