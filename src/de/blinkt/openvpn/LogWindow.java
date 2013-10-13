package de.blinkt.openvpn;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.*;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ImageSpan;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import android.widget.AdapterView.OnItemLongClickListener;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus.LogItem;
import de.blinkt.openvpn.core.VpnStatus.LogListener;
import de.blinkt.openvpn.core.VpnStatus.StateListener;
import de.blinkt.openvpn.core.OpenVpnService;
import de.blinkt.openvpn.core.OpenVpnService.LocalBinder;
import de.blinkt.openvpn.core.ProfileManager;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import static de.blinkt.openvpn.core.OpenVpnService.humanReadableByteCount;

public class LogWindow extends ListActivity implements StateListener, SeekBar.OnSeekBarChangeListener, RadioGroup.OnCheckedChangeListener, VpnStatus.ByteCountListener {
	private static final String LOGTIMEFORMAT = "logtimeformat";
	private static final int START_VPN_CONFIG = 0;
    private static final String VERBOSITYLEVEL = "verbositylevel";
    protected OpenVpnService mService;
	private ServiceConnection mConnection = new ServiceConnection() {


		@Override
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mService =null;
		}

	};
    private SeekBar mLogLevelSlider;
    private LinearLayout mOptionsLayout;
    private RadioGroup mTimeRadioGroup;
    private TextView mUpStatus;
    private TextView mDownStatus;
    private TextView mConnectStatus;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        ladapter.setLogLevel(progress+1);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.radioISO:
                ladapter.setTimeFormat(LogWindowListAdapter.TIME_FORMAT_ISO);
                break;
            case R.id.radioNone:
                ladapter.setTimeFormat(LogWindowListAdapter.TIME_FORMAT_NONE);
                break;
            case R.id.radioShort:
                ladapter.setTimeFormat(LogWindowListAdapter.TIME_FORMAT_SHORT);
                break;

        }
    }

    @Override
    public void updateByteCount(long in, long out, long diffin, long diffout) {
        //%2$s/s %1$s - ↑%4$s/s %3$s
        final String down = String.format("%s/s %s", humanReadableByteCount(in, false), humanReadableByteCount(diffin / OpenVPNManagement.mBytecountInterval, true));
        final String up = String.format("%s/s %s", humanReadableByteCount(out, false), humanReadableByteCount(diffout / OpenVPNManagement.mBytecountInterval, true));

        if(mUpStatus!=null && mDownStatus!=null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mUpStatus.setText(up);
                    mDownStatus.setText(down);
                }
            });
        }

    }


    class LogWindowListAdapter implements ListAdapter, LogListener, Callback {

        private static final int MESSAGE_NEWLOG = 0;

		private static final int MESSAGE_CLEARLOG = 1;

        private static final int MESSAGE_NEWTS = 2;
        private static final int MESSAGE_NEWLOGLEVEL = 3;

        public static final int TIME_FORMAT_NONE = 0;
        public static final int TIME_FORMAT_SHORT = 1;
        public static final int TIME_FORMAT_ISO = 2;

        private Vector<LogItem> allEntries=new Vector<LogItem>();

        private Vector<LogItem> currentLevelEntries=new Vector<LogItem>();

		private Handler mHandler;

		private Vector<DataSetObserver> observers=new Vector<DataSetObserver>();

		private int mTimeFormat=0;
        private int mLogLevel=3;


        public LogWindowListAdapter() {
			initLogBuffer();
			if (mHandler == null) {
				mHandler = new Handler(this);
			}

			VpnStatus.addLogListener(this);
		}



		private void initLogBuffer() {
			allEntries.clear();
            Collections.addAll(allEntries, VpnStatus.getlogbuffer());
            initCurrentMessages();
		}

		String getLogStr() {
			String str = "";
			for(LogItem entry:allEntries) {
				str+=entry.getString(LogWindow.this) + '\n';
			}
			return str;
		}


		private void shareLog() {
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.putExtra(Intent.EXTRA_TEXT, getLogStr());
			shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.ics_openvpn_log_file));
			shareIntent.setType("text/plain");
			startActivity(Intent.createChooser(shareIntent, "Send Logfile"));
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
			return currentLevelEntries.size();
		}

		@Override
		public Object getItem(int position) {
			return currentLevelEntries.get(position);
		}

		@Override
		public long getItemId(int position) {
			return ((Object)currentLevelEntries.get(position)).hashCode();
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
			
			LogItem le = currentLevelEntries.get(position);
			String msg = le.getString(LogWindow.this);
            String time ="";
			if (mTimeFormat != TIME_FORMAT_NONE) {
				Date d = new Date(le.getLogtime());
				java.text.DateFormat timeformat;
				if (mTimeFormat== TIME_FORMAT_ISO)
					timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault());
				else
					timeformat = DateFormat.getTimeFormat(LogWindow.this);
				 time = timeformat.format(d);

			}
            msg =  time + " " + le.getVerbosityLevel() + " " +  msg;

            int spanStart = time.length();

            SpannableString t = new SpannableString(msg);

            //t.setSpan(getSpanImage(le,(int)v.getTextSize()),spanStart,spanStart+1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
			v.setText(t);
			return v;
		}

        private ImageSpan getSpanImage(LogItem li, int imageSize) {
            int imageRes = android.R.drawable.ic_menu_call;

            switch (li.getLogLevel()) {
                case ERROR:
                    imageRes = android.R.drawable.ic_notification_clear_all;
                    break;
                case INFO:
                    imageRes = android.R.drawable.ic_menu_compass;
                    break;
                case VERBOSE:
                    imageRes = android.R.drawable.ic_menu_info_details;
                    break;
                case WARNING:
                    imageRes = android.R.drawable.ic_menu_camera;
                    break;
            }

            Drawable d = getResources().getDrawable(imageRes);


            //d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            d.setBounds(0, 0, imageSize, imageSize);
            ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);

            return span;
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
			return currentLevelEntries.isEmpty();

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
		public void newLog(LogItem logmessage) {
			Message msg = Message.obtain();
            assert (msg!=null);
			msg.what=MESSAGE_NEWLOG;
			Bundle bundle=new Bundle();
			bundle.putParcelable("logmessage", logmessage);
			msg.setData(bundle);
			mHandler.sendMessage(msg);
		}

		@Override
		public boolean handleMessage(Message msg) {
			// We have been called
			if(msg.what==MESSAGE_NEWLOG) {

				LogItem logmessage = msg.getData().getParcelable("logmessage");
                if(addLogMessage(logmessage))
                    for (DataSetObserver observer : observers) {
                        observer.onChanged();
                    }
            } else if (msg.what == MESSAGE_CLEARLOG) {
                for (DataSetObserver observer : observers) {
                    observer.onInvalidated();
                }
                initLogBuffer();
			}  else if (msg.what == MESSAGE_NEWTS) {
				for (DataSetObserver observer : observers) {
					observer.onInvalidated();
				}
			} else if (msg.what == MESSAGE_NEWLOGLEVEL) {
                initCurrentMessages();

                for (DataSetObserver observer: observers) {
                    observer.onChanged();
                }

            }

			return true;
		}

        private void initCurrentMessages() {
            currentLevelEntries.clear();
            for(LogItem li: allEntries) {
                if (li.getVerbosityLevel() <= mLogLevel)
                    currentLevelEntries.add(li);
            }
        }

        /**
         *
         * @param logmessage
         * @return True if the current entries have changed
         */
        private boolean addLogMessage(LogItem logmessage) {
            allEntries.add(logmessage);
            if (logmessage.getVerbosityLevel() <= mLogLevel) {
                 currentLevelEntries.add(logmessage);
                return true;
            } else {
                return false;
            }
        }

        void clearLog() {
			// Actually is probably called from GUI Thread as result of the user 
			// pressing a button. But better safe than sorry
			VpnStatus.clearLog();
			VpnStatus.logInfo(R.string.logCleared);
			mHandler.sendEmptyMessage(MESSAGE_CLEARLOG);
		}



		public void setTimeFormat(int newTimeFormat) {
			mTimeFormat= newTimeFormat;
			mHandler.sendEmptyMessage(MESSAGE_NEWTS);
		}

        public void setLogLevel(int logLevel) {
            mLogLevel = logLevel;
            mHandler.sendEmptyMessage(MESSAGE_NEWLOGLEVEL);
        }
		
	}



	private LogWindowListAdapter ladapter;
	private TextView mSpeedView;

    private void showDisconnectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_cancel);
        builder.setMessage(R.string.cancel_connection_query);
        builder.setNegativeButton(android.R.string.no, null);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProfileManager.setConntectedVpnProfileDisconnected(LogWindow.this);
                if (mService != null && mService.getManagement() != null)
                    mService.getManagement().stopVPN();
            }
        });

        builder.show();
    }


    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId()==R.id.clearlog) {
			ladapter.clearLog();
			return true;
		} else if(item.getItemId()==R.id.cancel){
            showDisconnectDialog();
            return true;
        } else if(item.getItemId()==R.id.send) {
			ladapter.shareLog();
		} else if(item.getItemId()==R.id.edit_vpn) {
			VpnProfile lastConnectedprofile = ProfileManager.getLastConnectedVpn();

			if(lastConnectedprofile!=null) {
				Intent vprefintent = new Intent(this,VPNPreferences.class)
				.putExtra(VpnProfile.EXTRA_PROFILEUUID,lastConnectedprofile.getUUIDString());
				startActivityForResult(vprefintent,START_VPN_CONFIG);
			} else {
				Toast.makeText(this, R.string.log_no_last_vpn, Toast.LENGTH_LONG).show();
			}
		} else if(item.getItemId() == R.id.toggle_time) {
			showHideOptionsPanel();
		} else if(item.getItemId() == android.R.id.home) {
			// This is called when the Home (Up) button is pressed
			// in the Action Bar.
			Intent parentActivityIntent = new Intent(this, MainActivity.class);
			parentActivityIntent.addFlags(
					Intent.FLAG_ACTIVITY_CLEAR_TOP |
					Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(parentActivityIntent);
			finish();
			return true;

		}
		return super.onOptionsItemSelected(item);

	}

    private void showHideOptionsPanel() {
        boolean optionsVisible = (mOptionsLayout.getVisibility() != View.GONE);

        ObjectAnimator anim;
        if (optionsVisible) {
            anim = ObjectAnimator.ofFloat(mOptionsLayout,"alpha",1.0f, 0f);
            anim.addListener(collapseListener);

        } else {
            mOptionsLayout.setVisibility(View.VISIBLE);
            anim = ObjectAnimator.ofFloat(mOptionsLayout,"alpha", 0f, 1.0f);
            //anim = new TranslateAnimation(0.0f, 0.0f, mOptionsLayout.getHeight(), 0.0f);

        }

        //anim.setInterpolator(new AccelerateInterpolator(1.0f));
        //anim.setDuration(300);
        //mOptionsLayout.startAnimation(anim);
        anim.start();

    }

    AnimatorListenerAdapter collapseListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            mOptionsLayout.setVisibility(View.GONE);
        }

    };


    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.logmenu, menu);
        if (getResources().getBoolean(R.bool.logSildersAlwaysVisible))
            menu.removeItem(R.id.toggle_time);

		return true;
	}


	@Override
	protected void onResume() {
		super.onResume();
		VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);
        Intent intent = new Intent(this, OpenVpnService.class);
        intent.setAction(OpenVpnService.START_SERVICE);

        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);


        if (getIntent() !=null && OpenVpnService.DISCONNECT_VPN.equals(getIntent().getAction()))
            showDisconnectDialog();

        setIntent(null);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == START_VPN_CONFIG && resultCode==RESULT_OK) {
			String configuredVPN = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);

			final VpnProfile profile = ProfileManager.get(this,configuredVPN);
			ProfileManager.getInstance(this).saveProfile(this, profile);
			// Name could be modified, reset List adapter

			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle(R.string.configuration_changed);
			dialog.setMessage(R.string.restart_vpn_after_change);


			dialog.setPositiveButton(R.string.restart,
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(getBaseContext(), LaunchVPN.class);
					intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
					intent.setAction(Intent.ACTION_MAIN);
					startActivity(intent);
				}


			});
			dialog.setNegativeButton(R.string.ignore, null);
			dialog.create().show();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onStop() {
		super.onStop();
		VpnStatus.removeStateListener(this);
        VpnStatus.removeByteCountListener(this);
        unbindService(mConnection);
        getPreferences(0).edit().putInt(LOGTIMEFORMAT, ladapter.mTimeFormat)
                                .putInt(VERBOSITYLEVEL, ladapter.mLogLevel).apply();

    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.logwindow);
		ListView lv = getListView();




        lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				ClipboardManager clipboard = (ClipboardManager)
						getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("Log Entry",((TextView) view).getText());
				clipboard.setPrimaryClip(clip);
				Toast.makeText(getBaseContext(), R.string.copied_entry, Toast.LENGTH_SHORT).show();
				return true;
			}
		});

		ladapter = new LogWindowListAdapter();
		ladapter.mTimeFormat = getPreferences(0).getInt(LOGTIMEFORMAT, 0);
        int loglevel = getPreferences(0).getInt(VERBOSITYLEVEL, 0);
        ladapter.setLogLevel(loglevel);

        lv.setAdapter(ladapter);

        mTimeRadioGroup = (RadioGroup) findViewById(R.id.timeFormatRadioGroup);
        mTimeRadioGroup.setOnCheckedChangeListener(this);

        if(ladapter.mTimeFormat== LogWindowListAdapter.TIME_FORMAT_ISO) {
            mTimeRadioGroup.check(R.id.radioISO);
        } else if (ladapter.mTimeFormat == LogWindowListAdapter.TIME_FORMAT_NONE) {
            mTimeRadioGroup.check(R.id.radioNone);
        } else if (ladapter.mTimeFormat == LogWindowListAdapter.TIME_FORMAT_SHORT) {
            mTimeRadioGroup.check(R.id.radioShort);
        }

		mSpeedView = (TextView) findViewById(R.id.speed);
		getActionBar().setDisplayHomeAsUpEnabled(true);

        mOptionsLayout = (LinearLayout) findViewById(R.id.logOptionsLayout);
        mLogLevelSlider = (SeekBar) findViewById(R.id.LogLevelSlider);
        mLogLevelSlider.setMax(VpnProfile.MAXLOGLEVEL-1);
        mLogLevelSlider.setProgress(loglevel-1);

        mLogLevelSlider.setOnSeekBarChangeListener(this);

        if(getResources().getBoolean(R.bool.logSildersAlwaysVisible))
            mOptionsLayout.setVisibility(View.VISIBLE);

        mUpStatus = (TextView) findViewById(R.id.speedUp);
        mDownStatus = (TextView) findViewById(R.id.speedDown);
        mConnectStatus = (TextView) findViewById(R.id.speedStatus);
    }


    @Override
	public void updateState(final String status,final String logMessage, final int resId, final ConnectionStatus level) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				String prefix=getString(resId) + ":";
				if (status.equals("BYTECOUNT") || status.equals("NOPROCESS") )
					prefix="";
				if (resId==R.string.unknown_state)
					prefix+=status;
                if(mSpeedView!=null)
				    mSpeedView.setText(prefix + logMessage);

                if(mConnectStatus!=null)
                    mConnectStatus.setText(getString(resId));
			}
		});

	}


	@Override
	protected void onDestroy() {
		VpnStatus.removeLogListener(ladapter);
		super.onDestroy();
	}

}
