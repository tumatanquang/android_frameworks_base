/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.net;

import static android.net.NetworkPolicyManager.POLICY_ALLOW_METERED_BACKGROUND;
import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_ALL;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;
import static android.net.NetworkPolicyManager.POLICY_REJECT_WIFI;
import static android.net.NetworkPolicyManager.POLICY_REJECT_CELLULAR;
import static android.net.NetworkPolicyManager.POLICY_REJECT_VPN;

import android.content.Context;
import android.net.NetworkPolicyManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ShellCommand;

import java.io.PrintWriter;
import java.util.List;

class NetworkPolicyManagerShellCommand extends ShellCommand {

    private final NetworkPolicyManagerService mInterface;
    private final WifiManager mWifiManager;
    private final NetworkPolicyManager mPolicyManager;

    NetworkPolicyManagerShellCommand(Context context, NetworkPolicyManagerService service) {
        mPolicyManager = (NetworkPolicyManager) context
                .getSystemService(Context.NETWORK_POLICY_SERVICE);
        mInterface = service;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        final PrintWriter pw = getOutPrintWriter();
        try {
            switch(cmd) {
                case "get":
                    return runGet();
                case "set":
                    return runSet();
                case "list":
                    return runList();
                case "add":
                    return runAdd();
                case "remove":
                    return runRemove();
                case "start-watching":
                    return runStartWatching();
                case "stop-watching":
                    return runStopWatching();
                default:
                    return handleDefaultCommands(cmd);
            }
        } catch (RemoteException e) {
            pw.println("Remote exception: " + e);
        }
        return -1;
    }

    @Override
    public void onHelp() {
        final PrintWriter pw = getOutPrintWriter();
        pw.println("Network policy manager (netpolicy) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  add restrict-background-whitelist UID");
        pw.println("    Adds a UID to the whitelist for restrict background usage.");
        pw.println("  add restrict-background-blacklist UID");
        pw.println("    Adds a UID to the blacklist for restrict background usage.");
        pw.println("  add restrict-network-usage-blacklist UID");
        pw.println("    Adds a UID to the blacklist for restrict network usage.");
        pw.println("  add restrict-wifi-data-blacklist UID");
        pw.println("    Adds a UID to the blacklist for restrict Wi-Fi data usage.");
        pw.println("  add restrict-mobile-data-blacklist UID");
        pw.println("    Adds a UID to the blacklist for restrict Mobile data usage.");
        pw.println("  add restrict-vpn-data-blacklist UID");
        pw.println("    Adds a UID to the blacklist for restrict VPN data usage.");
        pw.println("  add app-idle-whitelist UID");
        pw.println("    Adds a UID to the temporary app idle whitelist.");
        pw.println("  get restrict-background");
        pw.println("    Gets the global restrict background usage status.");
        pw.println("  list wifi-networks [true|false]");
        pw.println("    Lists all saved wifi networks and whether they are metered or not.");
        pw.println("    If a boolean argument is passed, filters just the metered (or unmetered)");
        pw.println("    networks.");
        pw.println("  list restrict-background-whitelist");
        pw.println("    Lists UIDs that are whitelisted for restrict background usage.");
        pw.println("  list restrict-background-blacklist");
        pw.println("    Lists UIDs that are blacklisted for restrict background usage.");
        pw.println("  list restrict-network-usage-blacklist");
        pw.println("    Lists UIDs that are blacklisted for restrict network usage.");
        pw.println("  list restrict-wifi-data-blacklist");
        pw.println("    Lists UIDs that are blacklisted for restrict Wi-Fi data usage.");
        pw.println("  list restrict-mobile-data-blacklist");
        pw.println("    Lists UIDs that are blacklisted for restrict Mobile data usage.");
        pw.println("  list restrict-vpn-data-blacklist");
        pw.println("    Lists UIDs that are blacklisted for restrict VPN data usage.");
        pw.println("  remove restrict-background-whitelist UID");
        pw.println("    Removes a UID from the whitelist for restrict background usage.");
        pw.println("  remove restrict-background-blacklist UID");
        pw.println("    Removes a UID from the blacklist for restrict background usage.");
        pw.println("  remove restrict-network-usage-blacklist UID");
        pw.println("    Removes a UID from the blacklist for restrict network usage.");
        pw.println("  remove restrict-wifi-data-blacklist UID");
        pw.println("    Removes a UID from the blacklist for restrict Wi-Fi data usage.");
        pw.println("  remove restrict-mobile-data-blacklist UID");
        pw.println("    Removes a UID from the blacklist for restrict Mobile data usage.");
        pw.println("  remove restrict-vpn-data-blacklist UID");
        pw.println("    Removes a UID from the blacklist for restrict VPN data usage.");
        pw.println("  remove app-idle-whitelist UID");
        pw.println("    Removes a UID from the temporary app idle whitelist.");
        pw.println("  set metered-network ID [undefined|true|false]");
        pw.println("    Toggles whether the given wi-fi network is metered.");
        pw.println("  set restrict-background BOOLEAN");
        pw.println("    Sets the global restrict background usage status.");
        pw.println("  set sub-plan-owner subId [packageName]");
        pw.println("    Sets the data plan owner package for subId.");
    }

    private int runGet() throws RemoteException {
        final PrintWriter pw = getOutPrintWriter();
        final String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to get");
            return -1;
        }
        switch(type) {
            case "restrict-background":
                return getRestrictBackground();
            case "restricted-mode":
                return getRestrictedModeState();
        }
        pw.println("Error: unknown get type '" + type + "'");
        return -1;
    }

    private int runSet() throws RemoteException  {
        final PrintWriter pw = getOutPrintWriter();
        final String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to set");
            return -1;
        }
        switch(type) {
            case "metered-network":
                return setMeteredWifiNetwork();
            case "restrict-background":
                return setRestrictBackground();
            case "sub-plan-owner":
                return setSubPlanOwner();
        }
        pw.println("Error: unknown set type '" + type + "'");
        return -1;
    }

    private int runList() throws RemoteException  {
        final PrintWriter pw = getOutPrintWriter();
        final String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to list");
            return -1;
        }
        switch(type) {
            case "app-idle-whitelist":
                return listAppIdleAllowlist();
            case "wifi-networks":
                return listWifiNetworks();
            case "restrict-background-whitelist":
                return listRestrictBackgroundAllowlist();
            case "restrict-background-blacklist":
                return listRestrictBackgroundDenylist();
            case "restrict-network-usage-blacklist":
                return listRestrictNetworkUsageDenylist();
            case "restrict-wifi-data-blacklist":
                return listRestrictWiFiDataDenylist();
            case "restrict-mobile-data-blacklist":
                return listRestrictCellularDataDenylist();
            case "restrict-vpn-data-blacklist":
                return listRestrictVpnDataDenylist();
        }
        pw.println("Error: unknown list type '" + type + "'");
        return -1;
    }

    private int runAdd() throws RemoteException  {
        final PrintWriter pw = getOutPrintWriter();
        final String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to add");
            return -1;
        }
        switch(type) {
            case "restrict-background-whitelist":
                return addRestrictBackgroundAllowlist();
            case "restrict-background-blacklist":
                return addRestrictBackgroundDenylist();
            case "restrict-network-usage-blacklist":
                return addRestrictNetworkUsageDenylist();
            case "restrict-wifi-data-blacklist":
                return addRestrictWiFiDataDenylist();
            case "restrict-mobile-data-blacklist":
                return addRestrictCellularDataDenylist();
            case "restrict-vpn-data-blacklist":
                return addRestrictVpnDataDenylist();
            case "app-idle-whitelist":
                return addAppIdleAllowlist();
        }
        pw.println("Error: unknown add type '" + type + "'");
        return -1;
    }

    private int runRemove() throws RemoteException {
        final PrintWriter pw = getOutPrintWriter();
        final String type = getNextArg();
        if (type == null) {
            pw.println("Error: didn't specify type of data to remove");
            return -1;
        }
        switch(type) {
            case "restrict-background-whitelist":
                return removeRestrictBackgroundWhitelist();
            case "restrict-background-blacklist":
                return removeRestrictBackgroundDenylist();
            case "restrict-network-usage-blacklist":
                return removeRestrictNetworkUsageDenylist();
            case "restrict-wifi-data-blacklist":
                return removeRestrictWiFiDataDenylist();
            case "restrict-mobile-data-blacklist":
                return removeRestrictCellularDataDenylist();
            case "restrict-vpn-data-blacklist":
                return removeRestrictVpnDataDenylist();
            case "app-idle-whitelist":
                return removeAppIdleAllowlist();
        }
        pw.println("Error: unknown remove type '" + type + "'");
        return -1;
    }

    private int runStartWatching() {
        final int uid = Integer.parseInt(getNextArgRequired());
        if (uid < 0) {
            final PrintWriter pw = getOutPrintWriter();
            pw.print("Invalid UID: "); pw.println(uid);
            return -1;
        }
        mInterface.setDebugUid(uid);
        return 0;
    }

    private int runStopWatching() {
        mInterface.setDebugUid(Process.INVALID_UID);
        return 0;
    }

    private int listUidPolicies(String msg, int policy) throws RemoteException {
        final int[] uids = mInterface.getUidsWithPolicy(policy);
        return listUidList(msg, uids);
    }

    private int listUidList(String msg, int[] uids) {
        final PrintWriter pw = getOutPrintWriter();
        pw.print(msg); pw.print(": ");
        if (uids.length == 0) {
            pw.println("none");
        } else {
            for (int uid : uids) {
                pw.print(uid);
                pw.print(' ');
            }
        }
        pw.println();
        return 0;
    }

    private int listRestrictBackgroundAllowlist() throws RemoteException {
        return listUidPolicies("Restrict background whitelisted UIDs",
                POLICY_ALLOW_METERED_BACKGROUND);
    }

    private int listRestrictBackgroundDenylist() throws RemoteException {
        return listUidPolicies("Restrict background blacklisted UIDs",
                POLICY_REJECT_METERED_BACKGROUND);
    }

    private int listRestrictNetworkUsageDenylist() throws RemoteException {
        return listUidPolicies("Restrict network usage blacklisted UIDs",
                POLICY_REJECT_ALL);
    }

    private int listRestrictWiFiDataDenylist() throws RemoteException {
        return listUidPolicies("Restrict Wi-Fi data blacklisted UIDs",
                POLICY_REJECT_WIFI);
    }

    private int listRestrictCellularDataDenylist() throws RemoteException {
        return listUidPolicies("Restrict Mobile data blacklisted UIDs",
                POLICY_REJECT_CELLULAR);
    }

    private int listRestrictVpnDataDenylist() throws RemoteException {
        return listUidPolicies("Restrict VPN data blacklisted UIDs",
                POLICY_REJECT_VPN);
    }

    private int listAppIdleAllowlist() throws RemoteException {
        final PrintWriter pw = getOutPrintWriter();
        final int[] uids = mInterface.getAppIdleWhitelist();
        return listUidList("App Idle whitelisted UIDs", uids);
    }

    private int getRestrictedModeState() {
        final PrintWriter pw = getOutPrintWriter();
        pw.print("Restricted mode status: ");
        pw.println(mInterface.isRestrictedModeEnabled() ? "enabled" : "disabled");
        return 0;
    }

    private int getRestrictBackground() throws RemoteException {
        final PrintWriter pw = getOutPrintWriter();
        pw.print("Restrict background status: ");
        pw.println(mInterface.getRestrictBackground() ? "enabled" : "disabled");
        return 0;
    }

    private int setRestrictBackground() throws RemoteException {
        final int enabled = getNextBooleanArg();
        if (enabled < 0) {
            return enabled;
        }
        mInterface.setRestrictBackground(enabled > 0);
        return 0;
    }

    private int setSubPlanOwner() throws RemoteException {
        final int subId = Integer.parseInt(getNextArgRequired());
        final String packageName = getNextArg();
        mInterface.setSubscriptionPlansOwner(subId, packageName);
        return 0;
    }

    private int setUidPolicy(int policy) throws RemoteException {
        final int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        if (policy == POLICY_REJECT_ALL) {
            mPolicyManager.addUidPolicy(uid, POLICY_REJECT_WIFI);
            mPolicyManager.addUidPolicy(uid, POLICY_REJECT_METERED_BACKGROUND);
            mPolicyManager.addUidPolicy(uid, POLICY_REJECT_CELLULAR);
            mPolicyManager.addUidPolicy(uid, POLICY_REJECT_VPN);
        }
        mPolicyManager.addUidPolicy(uid, policy);
        mInterface.addUidPolicy(uid, policy);
        mInterface.setUidPolicy(uid, policy);
        return 0;
    }

    private int resetUidPolicy(String errorMessage, int expectedPolicy) throws RemoteException {
        final int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        int actualPolicy = mInterface.getUidPolicy(uid);
        if (actualPolicy != expectedPolicy) {
            final PrintWriter pw = getOutPrintWriter();
            pw.print("Error: UID "); pw.print(uid); pw.print(' '); pw.println(errorMessage);
            return -1;
        }
        if (expectedPolicy == POLICY_REJECT_ALL) {
            mPolicyManager.removeUidPolicy(uid, POLICY_REJECT_WIFI);
            mPolicyManager.removeUidPolicy(uid, POLICY_REJECT_METERED_BACKGROUND);
            mPolicyManager.removeUidPolicy(uid, POLICY_REJECT_CELLULAR);
            mPolicyManager.removeUidPolicy(uid, POLICY_REJECT_VPN);
        }
        mPolicyManager.removeUidPolicy(uid, expectedPolicy);
        mInterface.removeUidPolicy(uid, expectedPolicy);
        mInterface.setUidPolicy(uid, POLICY_NONE);
        return 0;
    }

    private int addRestrictBackgroundAllowlist() throws RemoteException {
        return setUidPolicy(POLICY_ALLOW_METERED_BACKGROUND);
    }

    private int removeRestrictBackgroundWhitelist() throws RemoteException {
        return resetUidPolicy("not whitelisted", POLICY_ALLOW_METERED_BACKGROUND);
    }

    private int addRestrictBackgroundDenylist() throws RemoteException {
        return setUidPolicy(POLICY_REJECT_METERED_BACKGROUND);
    }

    private int addRestrictNetworkUsageDenylist() throws RemoteException {
        return setUidPolicy(POLICY_REJECT_ALL);
    }

    private int addRestrictWiFiDataDenylist() throws RemoteException {
        return setUidPolicy(POLICY_REJECT_WIFI);
    }

    private int addRestrictCellularDataDenylist() throws RemoteException {
        return setUidPolicy(POLICY_REJECT_CELLULAR);
    }

    private int addRestrictVpnDataDenylist() throws RemoteException {
        return setUidPolicy(POLICY_REJECT_VPN);
    }

    private int removeRestrictBackgroundDenylist() throws RemoteException {
        return resetUidPolicy("not blacklisted", POLICY_REJECT_METERED_BACKGROUND);
    }

    private int removeRestrictNetworkUsageDenylist() throws RemoteException {
        return resetUidPolicy("not blacklisted", POLICY_REJECT_ALL);
    }

    private int removeRestrictWiFiDataDenylist() throws RemoteException {
        return resetUidPolicy("not blacklisted", POLICY_REJECT_WIFI);
    }

    private int removeRestrictCellularDataDenylist() throws RemoteException {
        return resetUidPolicy("not blacklisted", POLICY_REJECT_CELLULAR);
    }

    private int removeRestrictVpnDataDenylist() throws RemoteException {
        return resetUidPolicy("not blacklisted", POLICY_REJECT_VPN);
    }

    private int setAppIdleWhitelist(boolean isWhitelisted) {
        final int uid = getUidFromNextArg();
        if (uid < 0) {
            return uid;
        }
        mInterface.setAppIdleWhitelist(uid, isWhitelisted);
        return 0;
    }

    private int addAppIdleAllowlist() throws RemoteException {
        return setAppIdleWhitelist(true);
    }

    private int removeAppIdleAllowlist() throws RemoteException {
        return setAppIdleWhitelist(false);
    }

    private int listWifiNetworks() {
        final PrintWriter pw = getOutPrintWriter();
        final String arg = getNextArg();
        final int match;
        if (arg == null) {
            match = WifiConfiguration.METERED_OVERRIDE_NONE;
        } else if (Boolean.parseBoolean(arg)) {
            match = WifiConfiguration.METERED_OVERRIDE_METERED;
        } else {
            match = WifiConfiguration.METERED_OVERRIDE_NOT_METERED;
        }

        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (arg == null || config.meteredOverride == match) {
                pw.print(NetworkPolicyManager.resolveNetworkId(config));
                pw.print(';');
                pw.println(overrideToString(config.meteredOverride));
            }
        }
        return 0;
    }

    private int setMeteredWifiNetwork() throws RemoteException {
        final PrintWriter pw = getOutPrintWriter();
        final String networkId = getNextArg();
        if (networkId == null) {
            pw.println("Error: didn't specify networkId");
            return -1;
        }
        final String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify meteredOverride");
            return -1;
        }
        mInterface.setWifiMeteredOverride(NetworkPolicyManager.resolveNetworkId(networkId),
                stringToOverride(arg));
        return -1;
    }

    private static String overrideToString(int override) {
        switch (override) {
            case WifiConfiguration.METERED_OVERRIDE_METERED: return "true";
            case WifiConfiguration.METERED_OVERRIDE_NOT_METERED: return "false";
            default: return "none";
        }
    }

    private static int stringToOverride(String override) {
        switch (override) {
            case "true": return WifiConfiguration.METERED_OVERRIDE_METERED;
            case "false": return WifiConfiguration.METERED_OVERRIDE_NOT_METERED;
            default: return WifiConfiguration.METERED_OVERRIDE_NONE;
        }
    }

    private int getNextBooleanArg() {
        final PrintWriter pw = getOutPrintWriter();
        final String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify BOOLEAN");
            return -1;
        }
        return Boolean.valueOf(arg) ? 1 : 0;
    }

    private int getUidFromNextArg() {
        final PrintWriter pw = getOutPrintWriter();
        final String arg = getNextArg();
        if (arg == null) {
            pw.println("Error: didn't specify UID");
            return -1;
        }
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            pw.println("Error: UID (" + arg + ") should be a number");
            return -2;
        }
    }
}
