/*
 * Copyright (c) 2012-2020 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.content.Context;
import android.content.Intent;

import de.blinkt.openvpn.activities.InternalWebView;

public class VariantConfig {
    /** Return the normal webview or internal webview depending what is available */
    static Intent getOpenUrlIntent(Context c, boolean external) {
        if (external)
            return new Intent(Intent.ACTION_VIEW);
        else
            return new Intent(c, InternalWebView.class);
    }
}
