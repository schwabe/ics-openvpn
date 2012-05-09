package de.blinkt.openvpn;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Vector;

import android.net.LocalSocket;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class OpenVpnManagementThread implements Runnable {

	private static final String TAG = "openvpn";
	private LocalSocket mSocket;
	private VpnProfile mProfile;
	private OpenVpnService mOpenVPNService;
	private LinkedList<FileDescriptor> mFDList=new LinkedList<FileDescriptor>(); 

	private static Vector<OpenVpnManagementThread> active=new Vector<OpenVpnManagementThread>();

	public OpenVpnManagementThread(VpnProfile profile, LocalSocket mgmtsocket, OpenVpnService openVpnService) {
		mProfile = profile;
		mSocket = mgmtsocket;
		mOpenVPNService = openVpnService;
	}

	static {
		System.loadLibrary("opvpnutil");
	}

	public void managmentCommand(String cmd) {
		//Log.d("openvpn", "mgmt cmd" + mSocket + " "  +cmd + " " );
		try {
			mSocket.getOutputStream().write(cmd.getBytes());
			mSocket.getOutputStream().flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void run() {
		Log.i(TAG, "Managment Socket Thread started");
		byte [] buffer  =new byte[2048];
		//	mSocket.setSoTimeout(5); // Setting a timeout cannot be that bad
		InputStream instream = null;
		try {
			instream = mSocket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String pendingInput="";
		active.add(this);

		try {

			while(true) {
				int numbytesread = instream.read(buffer);
				if(numbytesread==-1)
					return;

				FileDescriptor[] fds = null;
				try {
					fds = mSocket.getAncillaryFileDescriptors();
				} catch (IOException e) {
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
		try {
			Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
			int fdint = (Integer) getInt.invoke(fd);

			// You can even get more evil by parsing toString() and extract the int from that :)
			
			Log.d("Openvpn", "Got FD from socket: " + fd + " " + fdint);

			mOpenVPNService.protect(fdint);
			
			//ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(fdint);
			//pfd.close();
			jniclose(fdint);
			return;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		Log.d("Openvpn", "Failed to retrieve fd from socket: " + fd);
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
				managmentCommand("hold release\n");
				//managmentCommand("log on\n");
			} else if (cmd.equals("NEED-OK")) {
				processNeedCommand(argument);
			} else if (cmd.equals("LOG")) {
				String[] args = argument.split(",",3);
				// 0 unix time stamp
				// 1 log level N,I,E etc.
				// 2 log message
				OpenVPN.logMessage(0, "",  args[2]);
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
		} else if (needed.equals("IFCONFIG")) {
			String[] ifconfigparts = extra.split(" ");
			int mtu = Integer.parseInt(ifconfigparts[2]);
			mOpenVPNService.setLocalIP(ifconfigparts[0], ifconfigparts[1],mtu);
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
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}


	private native void jniclose(int fdint);



	private void processPWCommand(String argument) {
		//argument has the form 	Need 'Private Key' password
		int p1 =argument.indexOf('\'');
		int p2 = argument.indexOf('\'',p1+1);
		//String needed = argument.replace("Need '", "").replace("' password", "");
		String needed = argument.substring(p1+1, p2);

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
		}

	}




	private void logStatusMessage(String command) {
		OpenVPN.logMessage(0,"MGMT:", command);
	}


	public static boolean stopOpenVPN() {
		boolean sendCMD=false;
		for (OpenVpnManagementThread mt: active){
			mt.managmentCommand("signal SIGINT\n");
			sendCMD=true;
			try {
				mt.mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sendCMD;		
	}

}
