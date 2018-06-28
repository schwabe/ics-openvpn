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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.nicecert).setOnClickListener((v) ->
        {
            Intent intent = new Intent();
            intent.putExtra(KeyChain.EXTRA_KEY_ALIAS, "mynicecert");
            setResult(RESULT_OK);
        });
    }
}
