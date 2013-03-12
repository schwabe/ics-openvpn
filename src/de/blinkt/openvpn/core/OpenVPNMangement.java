package de.blinkt.openvpn.core;

public interface OpenVPNMangement {
	int mBytecountinterval=2;

	void reconnect();

	void pause();

	void resume();

	boolean stopVPN();

}
