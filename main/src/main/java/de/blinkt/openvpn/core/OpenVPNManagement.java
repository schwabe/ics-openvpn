package de.blinkt.openvpn.core;

public interface OpenVPNManagement {
    enum pauseReason {
        noNetwork,
        userPause,
        screenOff
    }

	int mBytecountInterval =2;

	void reconnect();

	void pause(pauseReason reason);

	void resume();

	boolean stopVPN();

}
