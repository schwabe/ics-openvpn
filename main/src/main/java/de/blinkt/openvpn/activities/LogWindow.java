package de.blinkt.openvpn.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.fragments.LogFragment;

/**
 * Created by arne on 13.10.13.
 */
public class LogWindow extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_window);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new LogFragment())
                    .commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
