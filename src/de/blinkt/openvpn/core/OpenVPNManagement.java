package de.blinkt.openvpn.core;

public interface OpenVPNManagement {
	int mBytecountinterval=2;

	void reconnect();

	void pause();

	void resume();

	boolean stopVPN();

}
