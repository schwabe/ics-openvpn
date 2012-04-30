package de.blinkt.openvpn;

import java.util.Vector;

import android.app.ListActivity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.blinkt.openvpn.OpenVPN.LogListener;

public class LogWindow extends ListActivity  {
	
	class LogWindowListAdapter implements ListAdapter,LogListener, Callback {

		private Vector<String> myEntries=new Vector<String>();
		
		private Handler mHandler;

		private Vector<DataSetObserver> observers=new Vector<DataSetObserver>();

		public LogWindowListAdapter() {
			for (String litem : OpenVPN.getlogbuffer()) {
				myEntries.add(litem);				
			}
			  
				if (mHandler == null) {
					mHandler = new Handler(this);
				}
			  
			OpenVPN.addLogListener(this);
		}
		
		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
			
		}
		
		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}

		@Override
		public int getCount() {
			return myEntries.size();
		}

		@Override
		public Object getItem(int position) {
			return myEntries.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView v;
			if(convertView==null)
				v = new TextView(getBaseContext());
			else
				v = (TextView) convertView;
			v.setText(myEntries.get(position));
			return v;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return myEntries.isEmpty();
			
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public void newLog(String logmessage) {
			Message msg = Message.obtain();
			Bundle mbundle=new Bundle();
			mbundle.putString("logmessage", logmessage);
			msg.setData(mbundle);
			mHandler.sendMessage(msg);
		}

		@Override
		public boolean handleMessage(Message msg) {
			// We have been called
			String logmessage = msg.getData().getString("logmessage");
			myEntries.add(logmessage);

			for (DataSetObserver observer : observers) {
				observer.onChanged();
			}
			
			
			return true;
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.logmenu, menu);
	    return true;
	}

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		
		  ListView lv = getListView();
		  //lv.setTextFilterEnabled(true);
		  LogWindowListAdapter adapter = new LogWindowListAdapter();
		  lv.setAdapter(adapter);

		}

	
	
}
