package de.blinkt.openvpn;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;



public class LogWindow extends ListActivity implements OnItemClickListener {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		  //setListAdapter(new ArrayAdapter<String>(this, R.layout.log_entry, COUNTRIES));
		  setListAdapter(new ArrayAdapter<String>(this, R.layout.log_entry, OpenVPN.getlogbuffer()));
		  //setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, OpenVPN.logbuffer));

		  ListView lv = getListView();
		  lv.setTextFilterEnabled(true);

		  lv.setOnItemClickListener(this);
		}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view,
	        int position, long id) {
	      // When clicked, show a toast with the TextView text
	      //Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
	          //Toast.LENGTH_SHORT).show();

	      setListAdapter(new ArrayAdapter<String>(this, R.layout.log_entry, OpenVPN.getlogbuffer()));
	    }
}
