package de.blinkt.openvpn;

import java.util.Vector;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.blinkt.openvpn.OpenVPN.LogListener;

public class LogWindow extends ListActivity  {
	private String[] mBconfig=null;


	class LogWindowListAdapter implements ListAdapter,LogListener, Callback {

		private static final int MESSAGE_NEWLOG = 0;

		private static final int MESSAGE_CLEARLOG = 1;

		private Vector<String> myEntries=new Vector<String>();

		private Handler mHandler;

		private Vector<DataSetObserver> observers=new Vector<DataSetObserver>();


		public LogWindowListAdapter() {
			initLogBuffer();

			if (mHandler == null) {
				mHandler = new Handler(this);
			}

			OpenVPN.addLogListener(this);
		}

		private void initLogBuffer() {
			myEntries.clear();
			for (String litem : OpenVPN.getlogbuffer()) {
				myEntries.add(litem);				
			}
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
			msg.what=MESSAGE_NEWLOG;
			Bundle mbundle=new Bundle();
			mbundle.putString("logmessage", logmessage);
			msg.setData(mbundle);
			mHandler.sendMessage(msg);
		}

		@Override
		public boolean handleMessage(Message msg) {
			// We have been called
			if(msg.what==MESSAGE_NEWLOG) {

				String logmessage = msg.getData().getString("logmessage");
				myEntries.add(logmessage);

				for (DataSetObserver observer : observers) {
					observer.onChanged();
				}
			} else if (msg.what == MESSAGE_CLEARLOG) {
				initLogBuffer();
				for (DataSetObserver observer : observers) {
					observer.onInvalidated();
				}
			} 

			return true;
		}

		void clearLog() {
			// Actually is probably called from GUI Thread as result of the user 
			// pressing a button. But better safe than sorry
			OpenVPN.clearLog();
			OpenVPN.logMessage(0,"","Log cleared.");
			mHandler.sendEmptyMessage(MESSAGE_CLEARLOG);
		}
	}



	private LogWindowListAdapter ladapter;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId()==R.id.clearlog) {
			ladapter.clearLog();
			return true;
		} else if(item.getItemId()==R.id.cancel){
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.title_cancel);
			builder.setMessage(R.string.cancel_connection_query);
			builder.setPositiveButton(android.R.string.yes, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					OpenVpnManagementThread.stopOpenVPN();		
				}
			});
			builder.setNegativeButton(android.R.string.no, null);
			builder.show();
			return true;
		} else if(item.getItemId()==R.id.info) {
			if(mBconfig==null)
				OpenVPN.triggerLogBuilderConfig();
		}

		return super.onOptionsItemSelected(item);

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
		ladapter = new LogWindowListAdapter();
		lv.setAdapter(ladapter);

	}



}
