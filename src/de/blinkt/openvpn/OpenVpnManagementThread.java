package de.blinkt.openvpn;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

import android.content.SharedPreferences;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;

public class OpenVpnManagementThread implements Runnable, OpenVPNMangement {

	private static final String TAG = "openvpn";
	private LocalSocket mSocket;
	private VpnProfile mProfile;
	private OpenVpnService mOpenVPNService;
	private LinkedList<FileDescriptor> mFDList=new LinkedList<FileDescriptor>();
	private int mBytecountinterval=2;
	private LocalServerSocket mServerSocket;
	private boolean mReleaseHold=true;
	private boolean mWaitingForRelease=false;
	private long mLastHoldRelease=0; 

	private static Vector<OpenVpnManagementThread> active=new Vector<OpenVpnManagementThread>();

	static private native void jniclose(int fdint);

	public OpenVpnManagementThread(VpnProfile profile, LocalServerSocket mgmtsocket, OpenVpnService openVpnService) {
		mProfile = profile;
		mServerSocket = mgmtsocket;
		mOpenVPNService = openVpnService;
		

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(openVpnService);
		boolean managemeNetworkState = prefs.getBoolean("netchangereconnect", true);
		if(managemeNetworkState)
			mReleaseHold=false;

	}

	static {
		System.loadLibrary("opvpnutil");
	}

	public void managmentCommand(String cmd) {
		if(mSocket!=null) {
			try {
				mSocket.getOutputStream().write(cmd.getBytes());
				mSocket.getOutputStream().flush();
			} catch (IOException e) {
				// Ignore socket stack traces
			}
		}
	}


	@Override
	public void run() {
		Log.i(TAG, "Managment Socket Thread started");
		byte [] buffer  =new byte[2048];
		//	mSocket.setSoTimeout(5); // Setting a timeout cannot be that bad

		String pendingInput="";
		active.add(this);

		try {
			// Wait for a client to connect
			mSocket= mServerSocket.accept();
			InputStream instream = mSocket.getInputStream();

			while(true) {
				int numbytesread = instream.read(buffer);
				if(numbytesread==-1)
					return;

				FileDescriptor[] fds = null;
				try {
					fds = mSocket.getAncillaryFileDescriptors();
				} catch (IOException e) {
					OpenVPN.logMessage(0, "", "Error reading fds from socket" + e.getLocalizedMessage());
					e.printStackTrace();
				}
				if(fds!=null){

					for (FileDescriptor fd : fds) {

						mFDList.add(fd);
					}
				}

				String input = new String(buffer,0,numbytesread,"UTF-8");

				pendingInput += input;

				pendingInput=processInput(pendingInput);



			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		active.remove(this);
	}

	//! Hack O Rama 2000!
	private void protectFileDescriptor(FileDescriptor fd) {
		Exception exp=null;
		try {
			Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
			int fdint = (Integer) getInt.invoke(fd);

			// You can even get more evil by parsing toString() and extract the int from that :)

			mOpenVPNService.protect(fdint);

			//ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(fdint);
			//pfd.close();
			jniclose(fdint);
			return;
		} catch (NoSuchMethodException e) {
			exp =e;
		} catch (IllegalArgumentException e) {
			exp =e;
		} catch (IllegalAccessException e) {
			exp =e;
		} catch (InvocationTargetException e) {
			exp =e;
		} catch (NullPointerException e) {
			exp =e;
		}
		if(exp!=null) {
			exp.printStackTrace();
			Log.d("Openvpn", "Failed to retrieve fd from socket: " + fd);
			OpenVPN.logMessage(0, "",  "Failed to retrieve fd from socket: " + exp.getLocalizedMessage());
		}
	}

	private String processInput(String pendingInput) {


		while(pendingInput.contains("\n")) {
			String[] tokens = pendingInput.split("\\r?\\n", 2);
			processCommand(tokens[0]);
			if(tokens.length == 1)
				// No second part, newline was at the end
				pendingInput="";
			else
				pendingInput=tokens[1];
		}
		return pendingInput;
	}


	private void processCommand(String command) {
		if (command.startsWith(">") && command.contains(":")) {
			String[] parts = command.split(":",2);
			String cmd = parts[0].substring(1);
			String argument = parts[1];


			if(cmd.equals("INFO")) {
				// Ignore greeting from mgmt
				//logStatusMessage(command);
			}else if (cmd.equals("PASSWORD")) {
				processPWCommand(argument);
			} else if (cmd.equals("HOLD")) {
				handleHold();
			} else if (cmd.equals("NEED-OK")) {
				processNeedCommand(argument);
			} else if (cmd.equals("BYTECOUNT")){
				processByteCount(argument);
			} else if (cmd.equals("STATE")) {
				processState(argument);
			} else if (cmd.equals("PROXY")) {
				processProxyCMD(argument);
			} else if (cmd.equals("LOG")) {
				String[] args = argument.split(",",3);
				// 0 unix time stamp
				// 1 log level N,I,E etc.
				// 2 log message
				OpenVPN.logMessage(0, "",  args[2]);
			} else if (cmd.equals("RSA_SIGN")) {
				processSignCommand(argument);
			} else {
				OpenVPN.logMessage(0, "MGMT:", "Got unrecognized command" + command);
				Log.i(TAG, "Got unrecognized command" + command);
			}
		} else if (command.startsWith("SUCCESS:")) {
			// ignore
		} else {
			Log.i(TAG, "Got unrecognized line from managment" + command);
			OpenVPN.logMessage(0, "MGMT:", "Got unrecognized line from management:" + command);
		}
	}
	private void handleHold() {
		if(mReleaseHold) {
			releaseHoldCmd();
		} else { 
			mWaitingForRelease=true;
			OpenVPN.updateStateString("NONETWORK", "",R.string.state_nonetwork,OpenVPN.LEVEL_NONETWORK);
		}
	}
	private void releaseHoldCmd() {
		if ((System.currentTimeMillis()- mLastHoldRelease) < 5000) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {}
			
		}
		mWaitingForRelease=false;
		mLastHoldRelease  = System.currentTimeMillis();
		managmentCommand("hold release\n");
		managmentCommand("bytecount " + mBytecountinterval + "\n");
		managmentCommand("state on\n");
	}
	
	public void releaseHold() {
		mReleaseHold=true;
		if(mWaitingForRelease)
			releaseHoldCmd();
			
	}

	private void processProxyCMD(String argument) {
		String[] args = argument.split(",",3);
		SocketAddress proxyaddr = ProxyDetection.detectProxy(mProfile);

		
		if(args.length >= 2) {
			String proto = args[1];
			if(proto.equals("UDP")) {
				proxyaddr=null;
			}
		}

		if(proxyaddr instanceof InetSocketAddress ){
			InetSocketAddress isa = (InetSocketAddress) proxyaddr;
			
			OpenVPN.logInfo(R.string.using_proxy, isa.getHostName(),isa.getPort());
			
			String proxycmd = String.format(Locale.ENGLISH,"proxy HTTP %s %d\n", isa.getHostName(),isa.getPort());
			managmentCommand(proxycmd);
		} else {
			managmentCommand("proxy NONE\n");
		}

	}
	private void processState(String argument) {
		String[] args = argument.split(",",3);
		String currentstate = args[1];
		if(args[2].equals(",,"))
			OpenVPN.updateStateString(currentstate,"");
		else
			OpenVPN.updateStateString(currentstate,args[2]);
	}


	private void processByteCount(String argument) {
		//   >BYTECOUNT:{BYTES_IN},{BYTES_OUT}
		int comma = argument.indexOf(',');
		long in = Long.parseLong(argument.substring(0, comma));
		long out = Long.parseLong(argument.substring(comma+1));

		OpenVPN.updateByteCount(in,out);


	}



	private void processNeedCommand(String argument) {
		int p1 =argument.indexOf('\'');
		int p2 = argument.indexOf('\'',p1+1);

		String needed = argument.substring(p1+1, p2);
		String extra = argument.split(":",2)[1];

		String status = "ok";


		if (needed.equals("PROTECTFD")) {
			FileDescriptor fdtoprotect = mFDList.pollFirst();
			protectFileDescriptor(fdtoprotect);
		} else if (needed.equals("DNSSERVER")) {
			mOpenVPNService.addDNS(extra);
		}else if (needed.equals("DNSDOMAIN")){
			mOpenVPNService.setDomain(extra);
		} else if (needed.equals("ROUTE")) {
			String[] routeparts = extra.split(" ");
			mOpenVPNService.addRoute(routeparts[0], routeparts[1]);
		} else if (needed.equals("ROUTE6")) {
			mOpenVPNService.addRoutev6(extra);
		} else if (needed.equals("IFCONFIG")) {
			String[] ifconfigparts = extra.split(" ");
			int mtu = Integer.parseInt(ifconfigparts[2]);
			mOpenVPNService.setLocalIP(ifconfigparts[0], ifconfigparts[1],mtu,ifconfigparts[3]);
		} else if (needed.equals("IFCONFIG6")) {
			mOpenVPNService.setLocalIPv6(extra);

		} else if (needed.equals("OPENTUN")) {
			if(sendTunFD(needed,extra))
				return;
			else
				status="cancel";
			// This not nice or anything but setFileDescriptors accepts only FilDescriptor class :(

		} else {
			Log.e(TAG,"Unkown needok command " + argument);
			return;
		}

		String cmd = String.format("needok '%s' %s\n", needed, status);
		managmentCommand(cmd);
	}

	private boolean sendTunFD (String needed, String extra) {
		Exception exp = null;
		if(!extra.equals("tun")) {
			// We only support tun
			String errmsg = String.format("Devicetype %s requested, but only tun is possible with the Android API, sorry!",extra);
			OpenVPN.logMessage(0, "", errmsg );

			return false;
		}
		ParcelFileDescriptor pfd = mOpenVPNService.openTun(); 
		if(pfd==null)
			return false;

		Method setInt;
		int fdint = pfd.getFd();
		try {
			setInt = FileDescriptor.class.getDeclaredMethod("setInt$",int.class);
			FileDescriptor fdtosend = new FileDescriptor();

			setInt.invoke(fdtosend,fdint);

			FileDescriptor[] fds = {fdtosend};
			mSocket.setFileDescriptorsForSend(fds);

			Log.d("Openvpn", "Sending FD tosocket: " + fdtosend + " " + fdint + "  " + pfd);
			// Trigger a send so we can close the fd on our side of the channel
			// The API documentation fails to mention that it will not reset the file descriptor to
			// be send and will happily send the file descriptor on every write ...
			String cmd = String.format("needok '%s' %s\n", needed, "ok");
			managmentCommand(cmd);

			// Set the FileDescriptor to null to stop this mad behavior 
			mSocket.setFileDescriptorsForSend(null);

			pfd.close();			

			return true;
		} catch (NoSuchMethodException e) {
			exp =e;
		} catch (IllegalArgumentException e) {
			exp =e;
		} catch (IllegalAccessException e) {
			exp =e;
		} catch (InvocationTargetException e) {
			exp =e;
		} catch (IOException e) {
			exp =e;
		}
		if(exp!=null) {
			OpenVPN.logMessage(0,"", "Could not send fd over socket:" + exp.getLocalizedMessage());
			exp.printStackTrace();
		}
		return false;
	}

	private void processPWCommand(String argument) {
		//argument has the form 	Need 'Private Key' password
		// or  ">PASSWORD:Verification Failed: '%s' ['%s']"
		String needed;
		
		
		
		try{

			int p1 = argument.indexOf('\'');
			int p2 = argument.indexOf('\'',p1+1);
			needed = argument.substring(p1+1, p2);
			if (argument.startsWith("Verification Failed")) {
				proccessPWFailed(needed, argument.substring(p2+1));
				return;
			}
		} catch (StringIndexOutOfBoundsException sioob) {
			OpenVPN.logMessage(0, "", "Could not parse management Password command: "  + argument);
			return;
		}

		String pw=null;

		if(needed.equals("Private Key")) {
			pw = mProfile.getPasswordPrivateKey();
		} else if (needed.equals("Auth")) {
			String usercmd = String.format("username '%s' %s\n", 
					needed, VpnProfile.openVpnEscape(mProfile.mUsername));
			managmentCommand(usercmd);
			pw = mProfile.getPasswordAuth();
		} 
		if(pw!=null) {
			String cmd = String.format("password '%s' %s\n", needed, VpnProfile.openVpnEscape(pw));
			managmentCommand(cmd);
		} else {
			OpenVPN.logMessage(0, OpenVPN.MANAGMENT_PREFIX, String.format("Openvpn requires Authentication type '%s' but no password/key information available", needed));
		}

	}




	private void proccessPWFailed(String needed, String args) {
		OpenVPN.updateStateString("AUTH_FAILED", needed + args,R.string.state_auth_failed,OpenVPN.LEVEL_AUTH_FAILED);
	}
	private void logStatusMessage(String command) {
		OpenVPN.logMessage(0,"MGMT:", command);
	}


	private static boolean stopOpenVPN() {
		boolean sendCMD=false;
		for (OpenVpnManagementThread mt: active){
			mt.managmentCommand("signal SIGINT\n");
			sendCMD=true;
			try {
				if(mt.mSocket !=null)
					mt.mSocket.close();
			} catch (IOException e) {
				// Ignore close error on already closed socket
			}
		}
		return sendCMD;		
	}

	public void signalusr1() {
		mReleaseHold=false;
		if(!mWaitingForRelease)
			managmentCommand("signal SIGUSR1\n");
	}

	public void reconnect() {
		signalusr1();
		releaseHold();
	}

	private void processSignCommand(String b64data) {

		String signed_string = mProfile.getSignedData(b64data);
		managmentCommand("rsa-sig\n");
		managmentCommand(signed_string);
		managmentCommand("\nEND\n");
	}

	@Override
	public void pause() {
		signalusr1();
	}

	@Override
	public void resume() {
		releaseHold();
	}

	@Override
	public boolean stopVPN() {
		return stopOpenVPN();
	}
}
