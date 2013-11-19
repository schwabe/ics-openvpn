package de.blinkt.openvpn;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

/**
 * Created by arne on 13.10.13.
 */
public class LogWindow extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_window);
        getActionBar().setDisplayHomeAsUpEnabled(true);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
