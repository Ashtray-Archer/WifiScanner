package com.wifiscanner.app;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/** A single Wi-Fi access point (BSS) discovered nearby. */
public class WifiAp {
    public String ssid = "";        // network name; empty means hidden
    public String bssid = "";       // AP MAC, e.g. "AA:BB:CC:DD:EE:FF"
    public int rssi = 0;            // signal level in dBm; 0 means unknown
    public int frequency = 0;       // channel centre frequency in MHz
    public int channel = 0;         // derived Wi-Fi channel number
    public String band = "";        // "2.4 GHz", "5 GHz", "6 GHz"
    public String security = "";    // "Open", "WEP", "WPA2", "WPA3", "WPA2/WPA3 Enterprise"…
    public String capabilities = ""; // raw capabilities string from the scan
    public String vendor = "";      // OUI lookup of the BSSID
    public boolean hidden = false;  // AP does not broadcast its SSID
    public long lastSeen = 0L;      // last time this BSS was seen

    public boolean hasRssi() { return rssi != 0; }

    public boolean isOpen() {
        return security.isEmpty() || security.equalsIgnoreCase("Open");
    }

    public String displayName() {
        if (ssid != null && !ssid.isEmpty()) return ssid;
        return "<Hidden network>";
    }

    /** Qualitative signal label from RSSI in dBm. */
    public String signalLabel() {
        if (!hasRssi()) return "—";
        if (rssi >= -55) return "Excellent";
        if (rssi >= -67) return "Good";
        if (rssi >= -80) return "Fair";
        return "Weak";
    }

    /** 0 = unknown, 1 = weak … 4 = excellent. */
    public int signalBars() {
        if (!hasRssi()) return 0;
        if (rssi >= -55) return 4;
        if (rssi >= -67) return 3;
        if (rssi >= -80) return 2;
        return 1;
    }

    public String channelLabel() {
        if (channel <= 0) return band;
        StringBuilder sb = new StringBuilder("ch ").append(channel);
        if (!band.isEmpty()) sb.append(" · ").append(band);
        return sb.toString();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("ssid", ssid);
        o.put("bssid", bssid);
        o.put("rssi", rssi);
        o.put("frequency", frequency);
        o.put("channel", channel);
        o.put("band", band);
        o.put("security", security);
        o.put("capabilities", capabilities);
        o.put("vendor", vendor);
        o.put("hidden", hidden);
        o.put("lastSeen", lastSeen);
        return o;
    }

    public static WifiAp fromJson(JSONObject o) {
        WifiAp d = new WifiAp();
        d.ssid = o.optString("ssid", "");
        d.bssid = o.optString("bssid", "");
        d.rssi = o.optInt("rssi", 0);
        d.frequency = o.optInt("frequency", 0);
        d.channel = o.optInt("channel", 0);
        d.band = o.optString("band", "");
        d.security = o.optString("security", "");
        d.capabilities = o.optString("capabilities", "");
        d.vendor = o.optString("vendor", "");
        d.hidden = o.optBoolean("hidden", false);
        d.lastSeen = o.optLong("lastSeen", 0L);
        return d;
    }

    /** Stable lowercase key for de-duplication and favoriting (the BSSID). */
    public String key() {
        return bssid == null ? "" : bssid.trim().toLowerCase(Locale.US);
    }
}
