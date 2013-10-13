package de.blinkt.openvpn;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by arne on 13.10.13.
 */
public class LogWindow extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_window);
    }
}
