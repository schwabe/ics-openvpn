/*
 * Copyright (c) 2012-2018 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.externalcertprovider;

import android.app.Activity;
import android.content.Intent;
import android.security.KeyChain;
import android.os.Bundle;

public class SelectCertificateActivity extends Activity {
    public static final String EXTRA_ALIAS = "de.blinkt.openvpn.api.KEY_ALIAS";
    public static final String EXTRA_DESCRIPTION = "de.blinkt.openvpn.api.KEY_DESCRIPTION";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.nicecert).setOnClickListener((v) ->
        {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_ALIAS, "mynicecert");
            intent.putExtra(EXTRA_DESCRIPTION, "Super secret example key!");
            setResult(RESULT_OK, intent);
            finish();
        });
    }
}
