ATTENTION
================
Before doing anything please read the first FAQ point in this file, espicially if 
you planing to build commercial software from this client. Also make sure you
understand the licenses of the code. OpenVPN for Android is GPL licensed. You
_CANNOT_ build a closed sourced custom UI application without acquiring a different
(paid) license for UI code.

The use of the AIDL API to control OpenVPN for Android from an external app is 
not subject to the license. The remoteExample project is licensed under the Apache 2.0 license.

When in doubt mail me about it.

See  the file todo.txt for ideas/not yet implemented features (and the bug tracker).

Build instructions:

- Install sdk
- Install ndk (Latest version should work as long as you use gcc)
- Make sure that ndk-build is in your build path.

Fetch the git submodules (the default urls for the submodules use ssh,
setup your own github ssh key or change the url to http in .gitmodules):

  git submodule init
  git submodule update

Do cd main;./misc/build-native.(sh|bat) in the main directory of the project.
After that build the project using "gradle build" (Or use Android Studio). 
The project is converted to gradle and building with Eclipse is no longer supported.

Alternatively, if the NDK build fails for some reason pre-built libraries can be downloaded
(e.g. from plai.de/android) and placed under main/ovpnlibs/assets/(no)pie_openvpn.{ABI} 
and main/ovpnlibs/jniLibs/{ABI}/*.so

FAQ

Q: Why are you not answering my questions about modifying ics-openvpn/why do not help build my app on top 
   of ics-openvpn? I thought this is open source.

A: There are many people building/wanting to build commercial VPN clients on top of my of my client. These
   client often do not even honour the license my app or the license of OpenVPN. Even if these modified
   software does honour the license I don't like doing unpaid work/giving advise for free to commerical
   software developers. 
   
   If you have a legitimate non commercial open source project I will gladly help you but please understand
   my initial reservations.
   

Q: How is the OpenVPN version different from normal OpenVPN

A: OpenVPN for Android uses a OpenVPN 2.4 master branch + dual stack client patches. 
   A git repository of the OpenVPN source code and changes is under: 
   https://github.com/schwabe/openvpn/

Q: what is minivpn?

A: minivpn is only a executable thats links against libopenvpn which is the normal openvpn build as
   library. It is done this way so the Android Play/Store apk will treat the library as normal library
   and update it on updates of the application. Also the application does not need to take care of 
   keeping minivpn up to date because it contains no code. For almost all intents and purposes
   minivpn + libopenvpn.so is the same as the normal openvpn binary

Q: How do I start a VPN by name from an external app?

A: public class StartOpenVPNActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
    	final String EXTRA_NAME = "de.blinkt.openvpn.shortcutProfileName";

        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
		shortcutIntent.setClassName("de.blinkt.openvpn", "de.blinkt.openvpn.LaunchVPN");
		shortcutIntent.putExtra(EXTRA_NAME,"upb ssl");
		startActivity(shortcutIntent);
    }
}

or from the shell:

am start -a android.intent.action.MAIN -n de.blinkt.openvpn/.LaunchVPN -e de.blinkt.openvpn.shortcutProfileName Home

Q: How to control the app from an external app?

A: There is an AIDL interface. See src/de/blinkt/openvpn/api/IOpenVPNAPIService.aidl. See the normal Android documentation how to use AIDL. 
   See also the example project under remoteExample.
   
