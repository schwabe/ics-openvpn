package de.blinkt.openvpn;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class SendDumpActivity extends Activity {
	
	protected void onStart() {
		super.onStart();
		emailMiniDumps();
		finish();
	};
	
	public void emailMiniDumps()
	{
		//need to "send multiple" to get more than one attachment
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
		emailIntent.setType("*/*");
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, 
				new String[]{"Arne Schwabe <arne@rfc2549.org>"});
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "OpenVPN Minidump");

		emailIntent.putExtra(Intent.EXTRA_TEXT, "Please describe the issue you have experienced");

		ArrayList<Uri> uris = new ArrayList<Uri>();

		File ldump = getLastestDump(this);
		if(ldump==null) {
			OpenVPN.logError("No Minidump found!");
		}
		
		uris.add(Uri.parse("content://de.blinkt.openvpn.FileProvider/" + ldump.getName()));
		uris.add(Uri.parse("content://de.blinkt.openvpn.FileProvider/" + ldump.getName() + ".log"));

		emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		startActivity(emailIntent);
	}

	static public File getLastestDump(Context c) {
		long newestDumpTime=0;
		File newestDumpFile=null;

		for(File f:c.getCacheDir().listFiles()) {
			if(!f.getName().endsWith(".dmp"))
				continue;

			if (newestDumpTime < f.lastModified()) {
				newestDumpTime = f.lastModified();
				newestDumpFile=f;
			}
		}
		return newestDumpFile;
	}
}
