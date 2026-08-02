package com.wifiscanner.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/** Builds CSV / JSON text for a list of Wi-Fi access points. */
public final class Exporter {
    private Exporter() {}

    public static String toCsv(List<WifiAp> aps) {
        StringBuilder sb = new StringBuilder();
        sb.append("SSID,BSSID,RSSI (dBm),Signal,Channel,Band,Frequency (MHz),Security,Vendor,Hidden,Capabilities\n");
        for (WifiAp d : aps) {
            sb.append(csv(d.ssid)).append(',')
              .append(csv(d.bssid)).append(',')
              .append(d.hasRssi() ? String.valueOf(d.rssi) : "").append(',')
              .append(csv(d.signalLabel())).append(',')
              .append(d.channel > 0 ? String.valueOf(d.channel) : "").append(',')
              .append(csv(d.band)).append(',')
              .append(d.frequency > 0 ? String.valueOf(d.frequency) : "").append(',')
              .append(csv(d.security)).append(',')
              .append(csv(d.vendor)).append(',')
              .append(d.hidden ? "yes" : "no").append(',')
              .append(csv(d.capabilities))
              .append('\n');
        }
        return sb.toString();
    }

    public static String toJson(List<WifiAp> aps, long timestamp) {
        try {
            JSONObject root = new JSONObject();
            root.put("scannedAt", timestamp);
            root.put("apCount", aps.size());
            JSONArray arr = new JSONArray();
            for (WifiAp d : aps) arr.put(d.toJson());
            root.put("accessPoints", arr);
            return root.toString(2);
        } catch (JSONException e) {
            return "{}";
        }
    }

    private static String csv(String v) {
        if (v == null) v = "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    public static String timestampFor(long millis) {
        java.text.SimpleDateFormat f =
                new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        return f.format(new java.util.Date(millis));
    }
}
