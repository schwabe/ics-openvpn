package de.blinkt.openvpn;

public interface OpenVPNMangement {

	void reconnect();

	void pause();

	void resume();

	boolean stopVPN();

}
