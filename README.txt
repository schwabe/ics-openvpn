This is my first Android project, so some things may be done in a completely stupid way.

See  the file todo.txt for ideas/not yet implemented features (and the bug tracker).

Build instraction:

Checkout google breakcode:

svn co http://google-breakpad.googlecode.com/svn/trunk/ google-breakpad

- Install sdk
- Install ndk

Do ./build-native.sh in the root directory of the project.

Use eclipse with android plugins to build the project.

Optional: Copy minivpn from lib/ to assets (if you want your own compiled version)




Starting a VPN by name from an external app:

public class StartOpenVPNActivity extends Activity {
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

