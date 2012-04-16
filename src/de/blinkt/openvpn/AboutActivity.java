package de.blinkt.openvpn;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class AboutActivity extends Activity implements OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        
        //findViewById(R.layout.about).setOnClickListener(this);
    }

	@Override
	public void onClick(View v) {
		finish();
	}
}
