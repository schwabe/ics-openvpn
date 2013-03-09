This is my first Android project, so some things may be done in a completely stupid way.

See  the file todo.txt for ideas/not yet implemented features (and the bug tracker).

Build instraction:

Checkout google breakcode:

svn co http://google-breakpad.googlecode.com/svn/trunk/ google-breakpad

- Install sdk
- Install ndk (Version 8d gives strange linker errors, use 8b for now)

Do ./build-native.sh in the root directory of the project.

Use eclipse with android plugins to build the project.

Optional: Copy minivpn from lib/ to assets (if you want your own compiled version)


FAQ

Q: How is the OpenVPN version different from normal OpenVPN

A: OpenVPN for Android uses a OpenVPN 2.3 master branch + Android patches + dual stack client patches. 
   A git repository of the OpenVPN source code and changes is under: 
   https://github.com/schwabe/openvpn/tree/android_2.3rc1%2Bds

Q: what is minivpn?

A: minivpn is only a executable thats links against libopenvpn which is the normal openvpn build as
   library. It is done this way so the Android Play/Store apk will treat the library as normal library
   and update it on updates of the application. Also the application does not need to take care of 
   keeping minivpn up to date because it contains no code. For almost all intents and purposes
   minivpn + libopenvpn.so is the same as the normal openvpn binary

Q: How to a VPN by name from an external app

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

