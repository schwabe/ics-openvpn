/*
 * Copyright (c) 2012-2025 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

/* This class is a data holder for the global preferences that are set when reading the app restrictions */
public class GlobalPreferences {
    boolean minimalUi = false;
    boolean forceConnected = false;
    boolean initialImport = false;

    /* will be set by AppRestrictions */
    static GlobalPreferences instance = null;

    GlobalPreferences(boolean minimalUi, boolean forceConnected, boolean initialImport)
    {
        this.minimalUi = minimalUi;
        this.forceConnected = forceConnected;
        this.initialImport = initialImport;
    }

    public static void setInstance(boolean minimalUi, boolean forceConnected, boolean initialImport)
    {
        instance = new GlobalPreferences(minimalUi, forceConnected, initialImport);
    }

    static public boolean getMinimalUi()
    {
        return getInstance().minimalUi;
    }

    static public boolean getForceConnected()
    {
        return getInstance().forceConnected;
    }

    static public boolean getAllowInitialImport() { return getInstance().initialImport; }

    static GlobalPreferences getInstance()
    {
        if (instance == null)
            throw new RuntimeException("Global preferences instance is not set");

        return instance;
    }
}
