package de.blinkt.openvpn;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import android.net.LocalSocket;
import android.util.Log;

public class OpenVpnManagementThread implements Runnable {

	private static final String TAG = "openvpn";
	private LocalSocket mSocket;
	private VpnProfile mProfile;
	private OpenVpnService mOpenVPNService;
	
private static Vector<OpenVpnManagementThread> active=new Vector<OpenVpnManagementThread>();
	
	public OpenVpnManagementThread(VpnProfile profile, LocalSocket mgmtsocket, OpenVpnService openVpnService) {
		mProfile = profile;
		mSocket = mgmtsocket;
		mOpenVPNService = openVpnService;
	}


	private String managmentEscape(String unescape) {
		String escapedString = unescape.replace("\\", "\\\\");
		escapedString = escapedString.replace("\"","\\\"");
		escapedString = escapedString.replace("\n","\\n");
		return '"' + escapedString + '"';
	}


	public void managmentCommand(String cmd) {
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
			
			String input = new String(buffer,0,numbytesread,"UTF-8");
			
			pendingInput += input;
			
			pendingInput=processInput(pendingInput);
			
			
			
		}
		} catch (IOException e) {
			e.printStackTrace();
		}
		active.remove(this);
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


			if(cmd.equals("INFO"))
				logStatusMessage(command);
			else if (cmd.equals("PASSWORD")) {
				processPWCommand(argument);
			} else if (cmd.equals("HOLD")) {
				managmentCommand("hold release\n");
			} else if (cmd.equals("PROTECT-FD")) {
				protectFD(argument);
			}
		}
        Log.i(TAG, "Got unrecognized command" + command);

	}


	private void protectFD(String argument) {
		try {
			FileDescriptor[] fds = mSocket.getAncillaryFileDescriptors();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


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
					needed, managmentEscape(mProfile.mUsername));
			managmentCommand(usercmd);
			pw = mProfile.getPasswordAuth();
		}
		if(pw!=null) {
			String cmd = String.format("password '%s' %s\n", needed, managmentEscape(pw));
			managmentCommand(cmd);
		}
		
	}




	private void logStatusMessage(String command) {
		OpenVPN.logMessage(0,"MGMT:", command);
	}


	public static void stopOpenVPN() {
		for (OpenVpnManagementThread mt: active){
			mt.managmentCommand("signal SIGINT\n");
		}		
	}

}
