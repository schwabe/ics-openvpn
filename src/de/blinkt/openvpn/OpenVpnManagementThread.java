package de.blinkt.openvpn;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.LinkedList;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import android.net.LocalSocket;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Log;

public class OpenVpnManagementThread implements Runnable {

	private static final String TAG = "openvpn";
	private LocalSocket mSocket;
	private VpnProfile mProfile;
	private OpenVpnService mOpenVPNService;
	private LinkedList<FileDescriptor> mFDList=new LinkedList<FileDescriptor>();
	private int mBytecountinterval=2;
	private long mLastIn=0; 
	private long mLastOut=0;
	private String mCurrentstate; 

	private static Vector<OpenVpnManagementThread> active=new Vector<OpenVpnManagementThread>();
	
	static private native void jniclose(int fdint);
	static private native byte[] rsasign(byte[] input,int pkey) throws InvalidKeyException;

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
			// Ignore socket stack traces
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
				managmentCommand("hold release\n");
				managmentCommand("bytecount " + mBytecountinterval + "\n");
				managmentCommand("state on\n");
			} else if (cmd.equals("NEED-OK")) {
				processNeedCommand(argument);
			} else if (cmd.equals("BYTECOUNT")){
				processByteCount(argument);
			} else if (cmd.equals("STATE")){
				processState(argument);
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

	private void processState(String argument) {
		String[] args = argument.split(",",3);
		mCurrentstate = args[1];
		OpenVPN.updateStateString(mCurrentstate,args[2]);
	}


	private void processByteCount(String argument) {
		//   >BYTECOUNT:{BYTES_IN},{BYTES_OUT}
		int comma = argument.indexOf(',');
		long in = Long.parseLong(argument.substring(0, comma));
		long out = Long.parseLong(argument.substring(comma+1));

		long diffin = in - mLastIn; 
		long diffout = out - mLastOut;

		mLastIn=in;
		mLastOut=out;

		String netstat = String.format("In: %8s, %8s/s  Out %8s, %8s/s",
				humanReadableByteCount(in, false),
				humanReadableByteCount(diffin, false),
				humanReadableByteCount(out, false),
				humanReadableByteCount(diffout, false));
		OpenVPN.updateStateString("BYTECOUNT",netstat);


	}

	// From: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
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

		String needed;
		try{

			int p1 = argument.indexOf('\'');
			int p2 = argument.indexOf('\'',p1+1);
			needed = argument.substring(p1+1, p2);
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
				// Ignore close error on maybe already closed socket
			}
		}
		return sendCMD;		
	}


	public void reconnect() {
		managmentCommand("signal SIGUSR1\n");

	}

	private void processSignCommand(String b64data) {
		
		PrivateKey privkey = mProfile.getKeystoreKey();
		Exception err =null;
		// The Jelly Bean *evil* Hack
		
		byte[] data = Base64.decode(b64data, Base64.DEFAULT);

		if(Build.VERSION.SDK_INT==16){
			processSignJellyBeans(privkey,data);
			return;
		}
		
		
		try{
		

			Cipher rsasinger = Cipher.getInstance("RSA/ECB/PKCS1PADDING");

			rsasinger.init(Cipher.ENCRYPT_MODE, privkey);

			byte[] signed_bytes = rsasinger.doFinal(data);
			String signed_string = Base64.encodeToString(signed_bytes, Base64.NO_WRAP);
			managmentCommand("rsa-sig\n");
			managmentCommand(signed_string);
			managmentCommand("\nEND\n");
		} catch (NoSuchAlgorithmException e){
			err =e;
		} catch (InvalidKeyException e) {
			err =e;
		} catch (NoSuchPaddingException e) {
			err =e;
		} catch (IllegalBlockSizeException e) {
			err =e;
		} catch (BadPaddingException e) {
			err =e;
		}
		if(err !=null) {
			OpenVPN.logError(R.string.error_rsa_sign,err.getClass().toString(),err.getLocalizedMessage());
		}

	}


	private void processSignJellyBeans(PrivateKey privkey, byte[] data) {
		Exception err =null;
		try {
			Method[] allm = privkey.getClass().getSuperclass().getDeclaredMethods();
			System.out.println(allm);
			Method getKey = privkey.getClass().getSuperclass().getDeclaredMethod("getOpenSSLKey");
			getKey.setAccessible(true);
			
			// Real object type is OpenSSLKey
			Object opensslkey = getKey.invoke(privkey);
			
			getKey.setAccessible(false);
			
			Method getPkeyContext = opensslkey.getClass().getDeclaredMethod("getPkeyContext");
			
			// integer pointer to EVP_pkey
			getPkeyContext.setAccessible(true);
			int pkey = (Integer) getPkeyContext.invoke(opensslkey);
			getPkeyContext.setAccessible(false);
			
			byte[] signed_bytes = rsasign(data, pkey); 
			String signed_string = Base64.encodeToString(signed_bytes, Base64.NO_WRAP);
			managmentCommand("rsa-sig\n");
			managmentCommand(signed_string);
			managmentCommand("\nEND\n");

		} catch (NoSuchMethodException e) {
			err=e;
		} catch (IllegalArgumentException e) {
			err=e;
		} catch (IllegalAccessException e) {
			err=e;
		} catch (InvocationTargetException e) {
			err=e;
		} catch (InvalidKeyException e) {
			err=e;
		}
		if(err !=null) {
			OpenVPN.logError(R.string.error_rsa_sign,err.getClass().toString(),err.getLocalizedMessage());
		}

	}
}
