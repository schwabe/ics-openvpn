/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

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

    /*
     * Rebind the interface
     */
    void networkChange();
}
