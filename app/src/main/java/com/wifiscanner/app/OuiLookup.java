package com.wifiscanner.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Compact offline OUI (MAC prefix) -> vendor lookup. Covers the most common
 * consumer, mobile and IoT manufacturers found on home / office LANs. Not
 * exhaustive; unknown prefixes simply return "".
 */
public final class OuiLookup {
    private OuiLookup() {}

    private static final Map<String, String> MAP = new HashMap<>();

    private static void p(String prefix, String vendor) { MAP.put(prefix, vendor); }

    static {
        // Apple
        p("00:03:93", "Apple"); p("00:1B:63", "Apple"); p("00:1E:C2", "Apple");
        p("00:25:00", "Apple"); p("3C:07:54", "Apple"); p("A4:5E:60", "Apple");
        p("AC:BC:32", "Apple"); p("F0:18:98", "Apple"); p("F4:0F:24", "Apple");
        p("DC:A9:04", "Apple"); p("80:E6:50", "Apple"); p("90:B2:1F", "Apple");
        p("BC:52:B7", "Apple"); p("D0:81:7A", "Apple"); p("F8:1E:DF", "Apple");
        p("A8:66:7F", "Apple"); p("88:66:5A", "Apple"); p("9C:35:EB", "Apple");
        // Samsung
        p("00:12:FB", "Samsung"); p("00:16:32", "Samsung"); p("00:1A:8A", "Samsung");
        p("08:08:C2", "Samsung"); p("5C:0A:5B", "Samsung"); p("78:1F:DB", "Samsung");
        p("8C:77:12", "Samsung"); p("A0:21:95", "Samsung"); p("BC:44:86", "Samsung");
        p("E8:50:8B", "Samsung"); p("FC:A1:3E", "Samsung"); p("34:23:BA", "Samsung");
        // Google / Nest
        p("00:1A:11", "Google"); p("3C:5A:B4", "Google"); p("54:60:09", "Google");
        p("94:EB:2C", "Google"); p("F4:F5:D8", "Google"); p("F4:F5:E8", "Google");
        p("DA:A1:19", "Google"); p("A4:77:33", "Google"); p("1C:F2:9A", "Google");
        // Amazon
        p("00:FC:8B", "Amazon"); p("44:65:0D", "Amazon"); p("68:37:E9", "Amazon");
        p("74:75:48", "Amazon"); p("F0:27:2D", "Amazon"); p("FC:65:DE", "Amazon");
        p("AC:63:BE", "Amazon"); p("38:F7:3D", "Amazon"); p("50:DC:E7", "Amazon");
        // Raspberry Pi
        p("B8:27:EB", "Raspberry Pi"); p("DC:A6:32", "Raspberry Pi");
        p("E4:5F:01", "Raspberry Pi"); p("28:CD:C1", "Raspberry Pi");
        p("D8:3A:DD", "Raspberry Pi"); p("2C:CF:67", "Raspberry Pi");
        // Espressif (ESP8266/ESP32 IoT)
        p("24:0A:C4", "Espressif"); p("30:AE:A4", "Espressif"); p("3C:71:BF", "Espressif");
        p("84:CC:A8", "Espressif"); p("A4:CF:12", "Espressif"); p("BC:DD:C2", "Espressif");
        p("CC:50:E3", "Espressif"); p("DC:4F:22", "Espressif"); p("EC:FA:BC", "Espressif");
        p("7C:9E:BD", "Espressif"); p("C4:4F:33", "Espressif"); p("48:3F:DA", "Espressif");
        // Intel
        p("00:1B:21", "Intel"); p("00:21:6A", "Intel"); p("3C:A9:F4", "Intel");
        p("7C:7A:91", "Intel"); p("8C:A9:82", "Intel"); p("94:65:9C", "Intel");
        p("A0:88:69", "Intel"); p("DC:53:60", "Intel"); p("E4:A4:71", "Intel");
        p("34:13:E8", "Intel"); p("48:51:B7", "Intel"); p("AC:7B:A1", "Intel");
        // TP-Link
        p("00:1D:0F", "TP-Link"); p("14:CC:20", "TP-Link"); p("50:C7:BF", "TP-Link");
        p("60:32:B1", "TP-Link"); p("A4:2B:B0", "TP-Link"); p("C0:06:C3", "TP-Link");
        p("EC:08:6B", "TP-Link"); p("98:DA:C4", "TP-Link"); p("B0:4E:26", "TP-Link");
        // Netgear
        p("00:0F:B5", "Netgear"); p("20:4E:7F", "Netgear"); p("28:C6:8E", "Netgear");
        p("44:94:FC", "Netgear"); p("9C:3D:CF", "Netgear"); p("A0:40:A0", "Netgear");
        p("C0:3F:0E", "Netgear"); p("DC:EF:09", "Netgear");
        // Ubiquiti
        p("04:18:D6", "Ubiquiti"); p("24:A4:3C", "Ubiquiti"); p("44:D9:E7", "Ubiquiti");
        p("68:72:51", "Ubiquiti"); p("78:8A:20", "Ubiquiti"); p("B4:FB:E4", "Ubiquiti");
        p("FC:EC:DA", "Ubiquiti"); p("E0:63:DA", "Ubiquiti"); p("74:83:C2", "Ubiquiti");
        // Cisco / Linksys
        p("00:1A:2F", "Cisco"); p("00:25:9C", "Cisco/Linksys"); p("68:7F:74", "Cisco/Linksys");
        p("C0:56:27", "Belkin/Linksys"); p("48:F8:B3", "Cisco/Linksys"); p("00:14:BF", "Cisco/Linksys");
        // Xiaomi
        p("28:6C:07", "Xiaomi"); p("34:CE:00", "Xiaomi"); p("50:8F:4C", "Xiaomi");
        p("64:09:80", "Xiaomi"); p("78:11:DC", "Xiaomi"); p("98:FA:E3", "Xiaomi");
        p("F0:B4:29", "Xiaomi"); p("FC:64:BA", "Xiaomi"); p("A4:50:46", "Xiaomi");
        // Huawei
        p("00:E0:FC", "Huawei"); p("48:46:FB", "Huawei"); p("54:25:EA", "Huawei");
        p("70:72:3C", "Huawei"); p("88:E3:AB", "Huawei"); p("AC:E2:15", "Huawei");
        p("E0:24:7F", "Huawei"); p("D0:7A:B5", "Huawei");
        // Microsoft (Surface / Xbox)
        p("00:12:5A", "Microsoft"); p("28:18:78", "Microsoft"); p("50:1A:C5", "Microsoft");
        p("7C:1E:52", "Microsoft"); p("C8:3F:26", "Microsoft"); p("98:5F:D3", "Microsoft");
        // Sony
        p("00:13:A9", "Sony"); p("00:1D:BA", "Sony"); p("3C:07:71", "Sony");
        p("AC:9B:0A", "Sony"); p("FC:0F:E6", "Sony"); p("78:84:3C", "Sony");
        // LG
        p("00:1C:62", "LG"); p("00:1E:75", "LG"); p("3C:BD:D8", "LG");
        p("64:BC:0C", "LG"); p("A8:16:B2", "LG"); p("C4:9A:02", "LG");
        // Dell
        p("00:14:22", "Dell"); p("00:21:9B", "Dell"); p("18:03:73", "Dell");
        p("B8:CA:3A", "Dell"); p("D4:BE:D9", "Dell"); p("F8:BC:12", "Dell");
        // HP
        p("00:1B:78", "HP"); p("00:25:B3", "HP"); p("3C:D9:2B", "HP");
        p("70:5A:0F", "HP"); p("9C:8E:99", "HP"); p("A0:48:1C", "HP");
        p("00:17:A4", "HP/Aruba"); p("38:63:BB", "HP");
        // ASUS
        p("00:1B:FC", "ASUS"); p("04:D4:C4", "ASUS"); p("2C:56:DC", "ASUS");
        p("50:46:5D", "ASUS"); p("AC:22:0B", "ASUS"); p("D8:50:E6", "ASUS");
        p("1C:B7:2C", "ASUS"); p("88:D7:F6", "ASUS");
        // Realtek (common in cheap NICs / IoT)
        p("00:E0:4C", "Realtek"); p("52:54:00", "QEMU/KVM virtual");
        p("00:0C:29", "VMware"); p("00:50:56", "VMware"); p("00:1C:14", "VMware");
        p("08:00:27", "VirtualBox"); p("00:15:5D", "Microsoft Hyper-V");
        // Roku / Sonos / Philips Hue / Ring / Wyze
        p("00:0D:4B", "Roku"); p("AC:3A:7A", "Roku"); p("DC:3A:5E", "Roku");
        p("00:0E:58", "Sonos"); p("34:7E:5C", "Sonos"); p("48:A6:B8", "Sonos");
        p("00:17:88", "Philips Hue"); p("EC:B5:FA", "Philips Hue");
        p("00:62:6E", "Ring"); p("F0:81:73", "Wyze");
        // Nintendo
        p("00:09:BF", "Nintendo"); p("58:BD:A3", "Nintendo"); p("98:B6:E9", "Nintendo");
        p("7C:BB:8A", "Nintendo");
        // Synology / QNAP (NAS)
        p("00:11:32", "Synology"); p("00:08:9B", "QNAP"); p("24:5E:BE", "QNAP");
        // Brother / Canon / Epson printers
        p("00:80:77", "Brother"); p("00:1B:A9", "Brother"); p("00:00:85", "Canon");
        p("00:26:AB", "Epson"); p("64:EB:8C", "Seiko Epson");
    }

    // Full IEEE OUI database, loaded once from assets/oui.txt (key = "AABBCC").
    private static volatile Map<String, String> assetMap = null;

    /** Loads the bundled OUI database (≈40k vendors). Safe to call repeatedly. */
    public static synchronized void ensureLoaded(Context ctx) {
        if (assetMap != null) return;
        Map<String, String> m = new HashMap<>(48000);
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(ctx.getAssets().open("oui.txt")));
            String line;
            while ((line = r.readLine()) != null) {
                int t = line.indexOf('\t');
                if (t == 6) m.put(line.substring(0, 6), line.substring(t + 1));
            }
        } catch (Exception ignored) {
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) {}
        }
        assetMap = m;
    }

    /** Returns vendor for a MAC like "AA:BB:CC:DD:EE:FF" or "" if unknown. */
    public static String lookup(String mac) {
        if (mac == null || mac.length() < 8) return "";
        String hex = mac.replace(":", "").replace("-", "").toUpperCase(Locale.US);
        if (hex.length() >= 6) {
            Map<String, String> am = assetMap;
            if (am != null) {
                String v = am.get(hex.substring(0, 6));
                if (v != null) return v;
            }
        }
        String v = MAP.get(mac.substring(0, 8).toUpperCase(Locale.US));
        return v == null ? "" : v;
    }
}
