package com.wifiscanner.app;

import android.Manifest;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "wifi_scanner_prefs";
    private static final String K_THEME = "theme_mode";       // 0 system, 1 light, 2 dark
    private static final String K_ACCENT = "accent";          // 0 blue, 1 green
    private static final String K_SORT_FIELD = "sort_field";
    private static final String K_SORT_ASC = "sort_asc";
    private static final String K_FAV_TOP = "favorites_on_top";
    private static final String K_OPEN_ONLY = "open_only";
    private static final String K_FAVORITES = "favorites";    // set of normalized BSSIDs
    private static final String LAST_SCAN_FILE = "last_scan.json";
    private static final int REQ_PERMS = 101;

    private SharedPreferences prefs;
    private WifiApAdapter adapter;
    private WifiScanner scanner;
    private boolean scanning = false;

    private TextView txtAdapter, txtSubtitle, chipCount, chipStatus;
    private LinearProgressIndicator progress;
    private MaterialButton btnScan;
    private View emptyState;

    private long currentScanTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        applyTheme(prefs.getInt(K_THEME, 2));
        super.onCreate(savedInstanceState);
        if (prefs.getInt(K_ACCENT, 0) == 1) setTheme(R.style.Theme_WifiScanner_Green);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.app_name);

        // targetSdk 35 draws edge-to-edge on Android 15: pad the toolbar below the
        // status bar and lift the content above the navigation bar.
        final View root = findViewById(R.id.root);
        final int toolbarPadTop = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            toolbar.setPadding(toolbar.getPaddingLeft(), toolbarPadTop + sb.top,
                    toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            root.setPadding(sb.left, 0, sb.right, sb.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        txtAdapter = findViewById(R.id.txt_adapter);
        txtSubtitle = findViewById(R.id.txt_subtitle);
        chipCount = findViewById(R.id.chip_count);
        chipStatus = findViewById(R.id.chip_status);
        progress = findViewById(R.id.progress);
        btnScan = findViewById(R.id.btn_scan);
        emptyState = findViewById(R.id.empty_state);

        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WifiApAdapter(this::showApActions);
        adapter.setOnFavoriteToggle(this::toggleFavorite);
        adapter.setFavorites(new java.util.HashSet<>(
                prefs.getStringSet(K_FAVORITES, java.util.Collections.emptySet())));
        adapter.setFavoritesOnTop(prefs.getBoolean(K_FAV_TOP, true));
        adapter.setOpenOnly(prefs.getBoolean(K_OPEN_ONLY, false));
        adapter.setSort(prefs.getInt(K_SORT_FIELD, WifiApAdapter.SORT_SIGNAL),
                prefs.getBoolean(K_SORT_ASC, true));
        list.setAdapter(adapter);

        btnScan.setOnClickListener(v -> {
            if (scanning) stopScan(); else requestScan();
        });

        loadLastScan();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-scan: resume scanning automatically when everything's ready.
        if (!scanning && WifiScanner.isEnabled(this) && hasScanPermissions()) {
            startScan();
        }
    }

    // ---------------- Scanning ----------------

    private void requestScan() {
        if (!WifiScanner.hasWifi(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.no_wifi)
                    .setPositiveButton("Close", null)
                    .show();
            return;
        }
        if (!hasScanPermissions()) {
            requestPermissions(requiredPermissions(), REQ_PERMS);
            return;
        }
        if (!WifiScanner.isEnabled(this)) {
            promptEnableWifi();
            return;
        }
        startScan();
    }

    private void promptEnableWifi() {
        // Apps can no longer toggle Wi-Fi programmatically on API 29+; open the
        // settings panel (or full settings) so the user can switch it on.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivity(new Intent(Settings.Panel.ACTION_WIFI));
            } else {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        } catch (Exception e) {
            toast(getString(R.string.wifi_off));
        }
    }

    private String[] requiredPermissions() {
        // ACCESS_FINE_LOCATION is what actually unlocks getScanResults() on every
        // API level; on 33+ we also ask for NEARBY_WIFI_DEVICES so startScan()
        // works on devices that route it through the nearby-devices permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    private boolean hasScanPermissions() {
        for (String p : requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERMS) return;
        if (hasScanPermissions()) {
            requestScan();   // proceed to the enable / scan steps
        } else {
            toast("Location permission is needed to read Wi-Fi scan results.");
        }
    }

    private void startScan() {
        if (scanning) return;
        scanning = true;
        emptyState.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        chipStatus.setText(R.string.scanning);
        btnScan.setText(R.string.stop);
        btnScan.setIconResource(R.drawable.ic_stop);
        txtAdapter.setText(R.string.scanning_title);
        txtSubtitle.setText(R.string.scanning_subtitle);

        // Warn once per scan session if Location services are off — the platform
        // hides Wi-Fi scan results until the device Location toggle is enabled.
        if (!isLocationServicesOn()) {
            showLocationHint();
        }

        scanner = new WifiScanner(this);
        scanner.start(new WifiScanner.Callback() {
            @Override public void onStart() { }

            @Override
            public void onResults(List<WifiAp> aps) {
                adapter.mergeAll(aps);
                currentScanTime = System.currentTimeMillis();
                int n = adapter.getTotalCount();
                chipCount.setText(n + (n == 1 ? " network" : " networks"));
                chipStatus.setText(R.string.scanning);
                emptyState.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
                txtSubtitle.setText(getString(R.string.scanning_live, n));
                saveLastScan();
            }

            @Override
            public void onError(String error) {
                toast(error);
                chipStatus.setText("Error");
            }
        });
    }

    private void stopScan() {
        scanning = false;
        if (scanner != null) { scanner.stop(); scanner = null; }
        progress.setVisibility(View.GONE);
        btnScan.setText(R.string.rescan);
        btnScan.setIconResource(R.drawable.ic_wifi);
        int n = adapter.getTotalCount();
        chipCount.setText(n + (n == 1 ? " network" : " networks"));
        chipStatus.setText(n > 0 ? "Stopped" : "Idle");
        int open = 0;
        for (WifiAp d : adapter.getItems()) if (d.isOpen()) open++;
        txtAdapter.setText(R.string.app_name);
        txtSubtitle.setText(n > 0
                ? getString(R.string.stopped_summary, n, open)
                : getString(R.string.empty_subtitle));
        saveLastScan();
    }

    /** Shown on API < 33 when Location services are off — scans return nothing. */
    private void showLocationHint() {
        new AlertDialog.Builder(this)
                .setTitle("Turn on Location")
                .setMessage("Android hides Wi-Fi scan results until Location services are "
                        + "switched on. WiFi Scanner never uses or collects your location — the "
                        + "system just gates the scan behind that toggle.")
                .setPositiveButton("Location settings", (d, w) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    } catch (Exception e) {
                        toast("Couldn't open Location settings");
                    }
                })
                .setNegativeButton("Ignore", null)
                .show();
    }

    private boolean isLocationServicesOn() {
        android.location.LocationManager lm =
                (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        return lm != null && androidx.core.location.LocationManagerCompat.isLocationEnabled(lm);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Pause scanning in the background; onStart resumes it.
        if (scanning && scanner != null) {
            scanner.stop();
            progress.setVisibility(View.GONE);
            btnScan.setText(R.string.rescan);
            btnScan.setIconResource(R.drawable.ic_wifi);
            chipStatus.setText("Paused");
            scanning = false;
            saveLastScan();
        }
    }

    // ---------------- Menu ----------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        int field = adapter.getSortField();
        int[] ids = {R.id.sort_signal, R.id.sort_ssid, R.id.sort_vendor,
                R.id.sort_channel, R.id.sort_security, R.id.sort_bssid};
        int[] vals = {WifiApAdapter.SORT_SIGNAL, WifiApAdapter.SORT_SSID, WifiApAdapter.SORT_VENDOR,
                WifiApAdapter.SORT_CHANNEL, WifiApAdapter.SORT_SECURITY, WifiApAdapter.SORT_BSSID};
        for (int i = 0; i < ids.length; i++) {
            MenuItem mi = menu.findItem(ids[i]);
            if (mi != null && vals[i] == field) mi.setChecked(true);
        }
        MenuItem rev = menu.findItem(R.id.sort_reverse);
        if (rev != null) rev.setChecked(!adapter.isAscending());
        MenuItem favTop = menu.findItem(R.id.sort_favorites_top);
        if (favTop != null) favTop.setChecked(adapter.isFavoritesOnTop());
        MenuItem openOnly = menu.findItem(R.id.sort_open_only);
        if (openOnly != null) openOnly.setChecked(adapter.isOpenOnly());
        int theme = prefs.getInt(K_THEME, 2);
        int[] tIds = {R.id.theme_system, R.id.theme_light, R.id.theme_dark};
        if (theme >= 0 && theme < tIds.length) {
            MenuItem ti = menu.findItem(tIds[theme]);
            if (ti != null) ti.setChecked(true);
        }
        int accent = prefs.getInt(K_ACCENT, 0);
        MenuItem ai = menu.findItem(accent == 1 ? R.id.accent_green : R.id.accent_blue);
        if (ai != null) ai.setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sort_signal) { applyMenuSort(WifiApAdapter.SORT_SIGNAL); return true; }
        if (id == R.id.sort_ssid) { applyMenuSort(WifiApAdapter.SORT_SSID); return true; }
        if (id == R.id.sort_vendor) { applyMenuSort(WifiApAdapter.SORT_VENDOR); return true; }
        if (id == R.id.sort_channel) { applyMenuSort(WifiApAdapter.SORT_CHANNEL); return true; }
        if (id == R.id.sort_security) { applyMenuSort(WifiApAdapter.SORT_SECURITY); return true; }
        if (id == R.id.sort_bssid) { applyMenuSort(WifiApAdapter.SORT_BSSID); return true; }
        if (id == R.id.sort_reverse) {
            adapter.setSort(adapter.getSortField(), !adapter.isAscending());
            prefs.edit().putBoolean(K_SORT_ASC, adapter.isAscending()).apply();
            invalidateOptionsMenu();
            return true;
        }
        if (id == R.id.sort_favorites_top) {
            boolean on = !adapter.isFavoritesOnTop();
            adapter.setFavoritesOnTop(on);
            prefs.edit().putBoolean(K_FAV_TOP, on).apply();
            invalidateOptionsMenu();
            return true;
        }
        if (id == R.id.sort_open_only) {
            boolean on = !adapter.isOpenOnly();
            adapter.setOpenOnly(on);
            prefs.edit().putBoolean(K_OPEN_ONLY, on).apply();
            invalidateOptionsMenu();
            return true;
        }
        if (id == R.id.action_export_csv) { export(false); return true; }
        if (id == R.id.action_export_json) { export(true); return true; }
        if (id == R.id.theme_system) { selectTheme(0); return true; }
        if (id == R.id.theme_light) { selectTheme(1); return true; }
        if (id == R.id.theme_dark) { selectTheme(2); return true; }
        if (id == R.id.accent_blue) { selectAccent(0); return true; }
        if (id == R.id.accent_green) { selectAccent(1); return true; }
        if (id == R.id.action_about) { showAbout(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void applyMenuSort(int field) {
        adapter.setSort(field, true);
        prefs.edit().putInt(K_SORT_FIELD, field).putBoolean(K_SORT_ASC, true).apply();
        invalidateOptionsMenu();
    }

    private void selectTheme(int mode) {
        prefs.edit().putInt(K_THEME, mode).apply();
        applyTheme(mode);
    }

    private void selectAccent(int accent) {
        if (prefs.getInt(K_ACCENT, 0) == accent) return;
        prefs.edit().putInt(K_ACCENT, accent).apply();
        recreate();
    }

    private void applyTheme(int mode) {
        switch (mode) {
            case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    // ---------------- Export / Share ----------------

    private void export(boolean json) {
        List<WifiAp> items = adapter.getItems();
        if (items.isEmpty()) {
            toast("Nothing to export yet — run a scan first.");
            return;
        }
        try {
            File dir = new File(getCacheDir(), "exports");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            long when = currentScanTime > 0 ? currentScanTime : System.currentTimeMillis();
            String stamp = Exporter.timestampFor(when);
            String name = "wifi-scan-" + stamp + (json ? ".json" : ".csv");
            File out = new File(dir, name);
            String content = json ? Exporter.toJson(items, when) : Exporter.toCsv(items);
            FileWriter w = new FileWriter(out);
            w.write(content);
            w.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", out);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(json ? "application/json" : "text/csv");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "Wi-Fi Scan " + stamp);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share " + (json ? "JSON" : "CSV")));
        } catch (Exception e) {
            toast("Export failed: " + e.getMessage());
        }
    }

    // ---------------- Persistence ----------------

    private void saveLastScan() {
        try {
            String content = Exporter.toJson(adapter.getItems(),
                    currentScanTime > 0 ? currentScanTime : System.currentTimeMillis());
            FileOutputStream fos = openFileOutput(LAST_SCAN_FILE, MODE_PRIVATE);
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception ignored) {}
    }

    private void loadLastScan() {
        File f = new File(getFilesDir(), LAST_SCAN_FILE);
        if (!f.exists()) {
            emptyState.setVisibility(View.VISIBLE);
            return;
        }
        try {
            byte[] data = new byte[(int) f.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(f);
            //noinspection ResultOfMethodCallIgnored
            fis.read(data);
            fis.close();
            JSONObject root = new JSONObject(new String(data, StandardCharsets.UTF_8));
            currentScanTime = root.optLong("scannedAt", 0L);
            JSONArray arr = root.optJSONArray("accessPoints");
            List<WifiAp> aps = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    aps.add(WifiAp.fromJson(arr.getJSONObject(i)));
                }
            }
            adapter.setAll(aps);
            txtAdapter.setText(R.string.app_name);
            String when = currentScanTime > 0
                    ? "Last scan " + relativeTime(currentScanTime) : getString(R.string.empty_subtitle);
            txtSubtitle.setText(when);
            int n = aps.size();
            chipCount.setText(n + (n == 1 ? " network" : " networks"));
            chipStatus.setText(n > 0 ? "Loaded" : "Idle");
            emptyState.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
            if (n > 0) btnScan.setText(R.string.rescan);
        } catch (Exception e) {
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    // ---------------- Favorites / Details / About ----------------

    /** Flips an AP's favorite state, keyed on its BSSID. */
    private void toggleFavorite(WifiAp d) {
        String key = WifiApAdapter.favKey(d);
        if (key == null) { toast(getString(R.string.favorite_no_mac)); return; }
        java.util.Set<String> favs = adapter.getFavorites();
        boolean nowFav = favs.add(key);   // add() is false if already present
        if (!nowFav) favs.remove(key);
        adapter.setFavorites(favs);       // re-sorts (pins to top if enabled)
        prefs.edit().putStringSet(K_FAVORITES, favs).apply();
        toast(nowFav ? "Added to favorites" : "Removed from favorites");
    }

    private void showApActions(WifiAp d) {
        final java.util.List<String> labels = new ArrayList<>();
        final java.util.List<Runnable> actions = new ArrayList<>();

        labels.add(adapter.isFavorite(d) ? getString(R.string.unfavorite) : getString(R.string.favorite));
        actions.add(() -> toggleFavorite(d));

        labels.add("Copy BSSID");
        actions.add(() -> copyToClipboard("BSSID", d.bssid));

        if (!d.ssid.isEmpty()) {
            labels.add("Copy SSID");
            actions.add(() -> copyToClipboard("SSID", d.ssid));
        }

        labels.add("Share network info");
        actions.add(() -> shareAp(d));

        labels.add("Search the web");
        actions.add(() -> searchAp(d));

        String title = d.displayName();
        if (!d.bssid.isEmpty()) title = title + "  ·  " + d.bssid;

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels.toArray(new String[0]),
                        (dlg, which) -> actions.get(which).run())
                .setNegativeButton("Close", null)
                .show();
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
            toast(label + " copied");
        }
    }

    private void shareAp(WifiAp d) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Wi-Fi network " + d.displayName());
        i.putExtra(Intent.EXTRA_TEXT, buildApText(d));
        startActivity(Intent.createChooser(i, "Share network info"));
    }

    private void searchAp(WifiAp d) {
        StringBuilder q = new StringBuilder();
        if (!d.vendor.isEmpty()) q.append(d.vendor).append(' ');
        if (!d.ssid.isEmpty()) q.append(d.ssid);
        else q.append(d.bssid);
        String query = q.toString().trim();
        if (query.isEmpty()) { toast("Not enough info to search"); return; }
        try {
            Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
            i.putExtra(SearchManager.QUERY, query);
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))));
            } catch (Exception e2) {
                toast("No browser available");
            }
        }
    }

    private String buildApText(WifiAp d) {
        StringBuilder sb = new StringBuilder();
        sb.append("WiFi Scanner — network\n");
        sb.append("SSID: ").append(d.displayName()).append('\n');
        sb.append("BSSID: ").append(d.bssid).append('\n');
        if (d.hasRssi()) sb.append("Signal: ").append(d.rssi).append(" dBm (")
                .append(d.signalLabel()).append(")\n");
        if (d.channel > 0) sb.append("Channel: ").append(d.channel).append('\n');
        if (!d.band.isEmpty()) sb.append("Band: ").append(d.band).append('\n');
        if (d.frequency > 0) sb.append("Frequency: ").append(d.frequency).append(" MHz\n");
        sb.append("Security: ").append(d.security).append('\n');
        if (!d.vendor.isEmpty()) sb.append("Vendor: ").append(d.vendor).append('\n');
        return sb.toString();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage("A fast, private Wi-Fi scanner.\n\n"
                        + "• Continuously discovers nearby Wi-Fi access points and updates live\n"
                        + "• Shows SSID, BSSID (MAC), signal (RSSI), channel, band, security and "
                        + "vendor\n"
                        + "• Flags OPEN (unsecured) networks in red\n"
                        + "• Sort by signal, name, vendor, channel, security or BSSID · filter to "
                        + "open networks · export to CSV or JSON\n"
                        + "• Favorite an access point (★) — the star is anchored to its BSSID\n\n"
                        + "All scanning happens locally on your device. Some routers hide their SSID "
                        + "(shown as “Hidden network”) and mesh systems broadcast several BSSIDs.\n\n"
                        + "Version 1.0")
                .setPositiveButton("Close", null)
                .show();
    }

    // ---------------- helpers ----------------

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    private String relativeTime(long millis) {
        long diff = System.currentTimeMillis() - millis;
        if (diff < 60_000) return "just now";
        if (diff < 3_600_000) return (diff / 60_000) + " min ago";
        if (diff < 86_400_000) return (diff / 3_600_000) + " h ago";
        return (diff / 86_400_000) + " d ago";
    }
}
