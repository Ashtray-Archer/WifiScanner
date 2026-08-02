package com.wifiscanner.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Continuously scans for nearby Wi-Fi access points.
 *
 * The Android Wi-Fi framework delivers results asynchronously: we ask for a
 * scan with {@link WifiManager#startScan()} and read {@link
 * WifiManager#getScanResults()} whenever a SCAN_RESULTS_AVAILABLE broadcast
 * fires. Broadcasts arrive both from our own requests and from system scans,
 * so once we are registered the list keeps refreshing on its own.
 *
 * Android 9+ throttles foreground apps to ~4 startScan() calls per 2 minutes,
 * so we re-request on a modest interval rather than hammering it; between our
 * requests the cached results are still refreshed by the platform.
 *
 * Callbacks are delivered on the main thread.
 */
public class WifiScanner {

    /** How often we ask the platform for a fresh scan (throttle-friendly). */
    private static final long RESCAN_INTERVAL_MS = 20_000L;

    public interface Callback {
        void onStart();
        void onResults(List<WifiAp> aps);
        void onError(String error);
    }

    private final Context appCtx;
    private final WifiManager wifi;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private Callback cb;
    private boolean running = false;
    private boolean receiverRegistered = false;
    private boolean ouiReady = false;

    public WifiScanner(Context ctx) {
        this.appCtx = ctx.getApplicationContext();
        this.wifi = (WifiManager) appCtx.getSystemService(Context.WIFI_SERVICE);
    }

    // ---------------- static helpers ----------------

    public static boolean hasWifi(Context ctx) {
        return ctx.getSystemService(Context.WIFI_SERVICE) != null;
    }

    public static boolean isEnabled(Context ctx) {
        WifiManager w = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        return w != null && w.isWifiEnabled();
    }

    // ---------------- lifecycle ----------------

    public void start(Callback callback) {
        this.cb = callback;
        if (running) return;
        if (wifi == null) { if (cb != null) cb.onError("This device has no Wi-Fi adapter."); return; }
        running = true;
        cb.onStart();

        // Load the vendor database off the UI thread, then start scanning.
        worker.execute(() -> {
            OuiLookup.ensureLoaded(appCtx);
            ouiReady = true;
            main.post(this::beginScan);
        });
    }

    public void stop() {
        if (!running) return;
        running = false;
        main.removeCallbacks(rescanRunnable);
        if (receiverRegistered) {
            try { appCtx.unregisterReceiver(receiver); } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
    }

    public boolean isRunning() { return running; }

    // ---------------- scanning ----------------

    private void beginScan() {
        if (!running) return;

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        // SCAN_RESULTS_AVAILABLE is a protected system broadcast; NOT_EXPORTED is
        // the correct flag on API 34+ and still receives it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appCtx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            appCtx.registerReceiver(receiver, filter);
        }
        receiverRegistered = true;

        // Show whatever the platform already has cached, then kick a fresh scan.
        publishResults();
        requestScan();
        main.postDelayed(rescanRunnable, RESCAN_INTERVAL_MS);
    }

    private final Runnable rescanRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            requestScan();
            main.postDelayed(this, RESCAN_INTERVAL_MS);
        }
    };

    @SuppressWarnings("deprecation")
    private void requestScan() {
        try {
            if (wifi != null) wifi.startScan();
        } catch (Exception ignored) {
            // startScan may be rate-limited or restricted; cached results still flow.
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                publishResults();
            }
        }
    };

    private void publishResults() {
        if (!running || wifi == null || cb == null) return;
        List<ScanResult> results;
        try {
            results = wifi.getScanResults();
        } catch (SecurityException e) {
            cb.onError("Location/Wi-Fi permission is required to read scan results.");
            return;
        }
        if (results == null) return;

        long now = System.currentTimeMillis();
        Map<String, WifiAp> byBssid = new HashMap<>();
        for (ScanResult r : results) {
            if (r == null || r.BSSID == null) continue;
            WifiAp ap = new WifiAp();
            ap.bssid = r.BSSID;
            ap.ssid = ssidOf(r);
            ap.hidden = ap.ssid.isEmpty();
            ap.rssi = r.level;
            ap.frequency = r.frequency;
            ap.channel = channelFor(r.frequency);
            ap.band = bandFor(r.frequency);
            ap.capabilities = r.capabilities == null ? "" : r.capabilities;
            ap.security = securityFor(ap.capabilities);
            ap.vendor = ouiReady ? OuiLookup.lookup(r.BSSID) : "";
            ap.lastSeen = now;

            // A BSSID is unique per radio; keep the strongest sighting if dupes.
            WifiAp prev = byBssid.get(ap.key());
            if (prev == null || ap.rssi > prev.rssi) byBssid.put(ap.key(), ap);
        }
        cb.onResults(new java.util.ArrayList<>(byBssid.values()));
    }

    // ---------------- parsing helpers ----------------

    @SuppressWarnings("deprecation")
    private static String ssidOf(ScanResult r) {
        String s = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                android.net.wifi.WifiSsid w = r.getWifiSsid();
                if (w != null) {
                    s = w.toString();
                    // WifiSsid.toString() wraps the value in quotes for UTF-8 SSIDs.
                    if (s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                        s = s.substring(1, s.length() - 1);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (s == null) s = r.SSID;    // deprecated on 33+ but the reliable fallback
        return s == null ? "" : s;
    }

    /** IEEE 802.11 channel number from a centre frequency in MHz. */
    static int channelFor(int freq) {
        if (freq <= 0) return 0;
        if (freq == 2484) return 14;
        if (freq >= 2412 && freq <= 2472) return (freq - 2407) / 5;      // 2.4 GHz: ch 1-13
        if (freq >= 5160 && freq <= 5885) return (freq - 5000) / 5;      // 5 GHz
        if (freq >= 5955 && freq <= 7115) return (freq - 5950) / 5;      // 6 GHz (Wi-Fi 6E)
        if (freq == 5935) return 2;                                       // 6 GHz ch 2 special case
        return 0;
    }

    static String bandFor(int freq) {
        if (freq >= 2400 && freq < 2500) return "2.4 GHz";
        if (freq >= 4900 && freq < 5900) return "5 GHz";
        if (freq >= 5925 && freq <= 7125) return "6 GHz";
        return "";
    }

    /** Human-readable security summary parsed from the capabilities string. */
    static String securityFor(String caps) {
        if (caps == null) return "Open";
        String c = caps.toUpperCase(Locale.US);
        boolean enterprise = c.contains("EAP");
        String scheme;
        if (c.contains("WPA3") || c.contains("SAE")) {
            scheme = (c.contains("WPA2") || c.contains("RSN")) ? "WPA2/WPA3" : "WPA3";
        } else if (c.contains("WPA2") || c.contains("RSN")) {
            scheme = "WPA2";
        } else if (c.contains("WPA")) {
            scheme = "WPA";
        } else if (c.contains("WEP")) {
            scheme = "WEP";
        } else if (c.contains("OWE")) {
            scheme = "Enhanced Open";
        } else {
            scheme = "Open";
        }
        if (enterprise && !scheme.equals("Open")) scheme += " Enterprise";
        return scheme;
    }
}
