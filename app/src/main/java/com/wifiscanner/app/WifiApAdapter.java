package com.wifiscanner.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WifiApAdapter extends RecyclerView.Adapter<WifiApAdapter.VH> {

    public static final int SORT_SIGNAL = 0;
    public static final int SORT_SSID = 1;
    public static final int SORT_VENDOR = 2;
    public static final int SORT_CHANNEL = 3;
    public static final int SORT_SECURITY = 4;
    public static final int SORT_BSSID = 5;

    private final List<WifiAp> items = new ArrayList<>();   // everything found
    private final List<WifiAp> visible = new ArrayList<>(); // after the open-only filter
    private int sortField = SORT_SIGNAL;
    private boolean ascending = true;
    private boolean openOnly = false;

    /** Normalized (lowercase) BSSIDs of favorited access points. */
    private final Set<String> favorites = new HashSet<>();
    private boolean favoritesOnTop = true;

    public interface OnRowClick { void onClick(WifiAp d); }
    public interface OnFavoriteToggle { void onToggle(WifiAp d); }
    private final OnRowClick rowClick;
    private OnFavoriteToggle favoriteToggle;

    public WifiApAdapter(OnRowClick rowClick) { this.rowClick = rowClick; }

    public void setOnFavoriteToggle(OnFavoriteToggle t) { this.favoriteToggle = t; }

    /** Normalizes a BSSID to a stable favorite key, or null if there isn't one. */
    public static String favKey(WifiAp d) {
        if (d == null || d.bssid == null) return null;
        String m = d.bssid.trim().toLowerCase(Locale.US);
        return m.isEmpty() ? null : m;
    }

    public boolean isFavorite(WifiAp d) {
        String k = favKey(d);
        return k != null && favorites.contains(k);
    }

    public void setFavorites(Set<String> macs) {
        favorites.clear();
        if (macs != null) {
            for (String m : macs) {
                if (m != null && !m.trim().isEmpty()) favorites.add(m.trim().toLowerCase(Locale.US));
            }
        }
        applySort();
    }

    public Set<String> getFavorites() { return new HashSet<>(favorites); }

    public void setFavoritesOnTop(boolean on) {
        if (favoritesOnTop == on) return;
        favoritesOnTop = on;
        applySort();
    }

    public boolean isFavoritesOnTop() { return favoritesOnTop; }

    public void setOpenOnly(boolean on) {
        if (openOnly == on) return;
        openOnly = on;
        applySort();
    }

    public boolean isOpenOnly() { return openOnly; }

    public void setAll(List<WifiAp> aps) {
        items.clear();
        items.addAll(aps);
        applySort();
    }

    /** Merges a fresh scan into the accumulated list, keyed by BSSID. */
    public void mergeAll(List<WifiAp> aps) {
        for (WifiAp d : aps) {
            boolean found = false;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).key().equals(d.key())) {
                    items.set(i, d);
                    found = true;
                    break;
                }
            }
            if (!found) items.add(d);
        }
        applySort();
    }

    public void clear() {
        items.clear();
        visible.clear();
        notifyDataSetChanged();
    }

    public List<WifiAp> getItems() { return new ArrayList<>(items); }
    public int getTotalCount() { return items.size(); }
    public int getSortField() { return sortField; }
    public boolean isAscending() { return ascending; }

    public void setSort(int field, boolean asc) {
        this.sortField = field;
        this.ascending = asc;
        applySort();
    }

    private void applySort() {
        Comparator<WifiAp> c;
        switch (sortField) {
            case SORT_SSID:
                c = Comparator.comparing(d -> d.displayName().toLowerCase(Locale.US)); break;
            case SORT_VENDOR:
                c = Comparator.comparing(d -> d.vendor.toLowerCase(Locale.US)); break;
            case SORT_CHANNEL:
                c = Comparator.comparingInt(d -> d.channel <= 0 ? Integer.MAX_VALUE : d.channel); break;
            case SORT_SECURITY:
                c = Comparator.comparing(d -> d.security.toLowerCase(Locale.US)); break;
            case SORT_BSSID:
                c = Comparator.comparing(d -> d.bssid.toLowerCase(Locale.US)); break;
            case SORT_SIGNAL:
            default:
                // Strongest first by default; unknown RSSI sinks to the bottom.
                c = Comparator.comparingInt(d -> -(d.hasRssi() ? d.rssi : -10_000)); break;
        }
        if (!ascending) c = c.reversed();
        if (favoritesOnTop) {
            Comparator<WifiAp> favFirst = Comparator.comparingInt(d -> isFavorite(d) ? 0 : 1);
            c = favFirst.thenComparing(c);
        }
        items.sort(c);

        visible.clear();
        for (WifiAp d : items) {
            if (!openOnly || d.isOpen()) visible.add(d);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_ap, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        WifiAp d = visible.get(position);
        Context ctx = h.itemView.getContext();
        int primary = primaryColor(ctx);

        h.name.setText(d.displayName());

        // Meta line: security • vendor • channel/band (skip blanks/dupes).
        StringBuilder meta = new StringBuilder();
        appendMeta(meta, d.security);
        appendMeta(meta, d.vendor);
        appendMeta(meta, d.channelLabel());
        if (meta.length() == 0) {
            h.meta.setVisibility(View.GONE);
        } else {
            h.meta.setVisibility(View.VISIBLE);
            h.meta.setText(meta.toString());
        }

        h.mac.setText(d.bssid);

        // Signal line: "-67 dBm · Good · 3s ago", coloured by strength.
        StringBuilder sig = new StringBuilder();
        if (d.hasRssi()) sig.append(d.rssi).append(" dBm · ").append(d.signalLabel());
        else sig.append("Signal unknown");
        if (d.lastSeen > 0) sig.append(" · ").append(ago(d.lastSeen));
        h.signal.setText(sig.toString());
        int sigColor = signalColor(ctx, d);
        h.signal.setTextColor(sigColor);
        h.signal.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(sigColor));

        // Security badge: OPEN networks flagged in red, secured shown neutral.
        if (d.isOpen()) {
            h.badge.setText("OPEN");
            h.badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.badge_open)));
            h.badge.setVisibility(View.VISIBLE);
        } else {
            h.badge.setVisibility(View.GONE);
        }

        // Lock icon reflects security state.
        h.icon.setImageResource(d.isOpen() ? R.drawable.ic_lock_open : R.drawable.ic_wifi);
        h.icon.setColorFilter(primary);

        // Favorite star (keyed on BSSID).
        boolean canFavorite = favKey(d) != null;
        boolean fav = isFavorite(d);
        h.star.setImageResource(fav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        if (canFavorite) {
            h.star.setColorFilter(fav
                    ? ContextCompat.getColor(ctx, R.color.favorite)
                    : primary);
            h.star.setAlpha(fav ? 1f : 0.55f);
            h.star.setEnabled(true);
            h.star.setOnClickListener(v -> { if (favoriteToggle != null) favoriteToggle.onToggle(d); });
        } else {
            h.star.setColorFilter(ContextCompat.getColor(ctx, R.color.favorite_disabled));
            h.star.setAlpha(0.5f);
            h.star.setEnabled(false);
            h.star.setOnClickListener(null);
        }

        h.itemView.setOnClickListener(v -> { if (rowClick != null) rowClick.onClick(d); });
    }

    private int signalColor(Context ctx, WifiAp d) {
        int res;
        switch (d.signalBars()) {
            case 4: res = R.color.signal_strong; break;
            case 3: res = R.color.signal_good; break;
            case 2: res = R.color.signal_fair; break;
            case 1: res = R.color.signal_weak; break;
            default: return ContextCompat.getColor(ctx, R.color.text_secondary);
        }
        return ContextCompat.getColor(ctx, res);
    }

    private int primaryColor(Context c) {
        android.util.TypedValue tv = new android.util.TypedValue();
        c.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, tv, true);
        return tv.data;
    }

    private void appendMeta(StringBuilder sb, String value) {
        if (value == null || value.isEmpty()) return;
        if (sb.toString().toLowerCase(Locale.US).contains(value.toLowerCase(Locale.US))) return;
        if (sb.length() > 0) sb.append("  •  ");
        sb.append(value);
    }

    /** Compact "Xs/Xm ago" for a timestamp. */
    static String ago(long millis) {
        long diff = System.currentTimeMillis() - millis;
        if (diff < 1_000) return "now";
        if (diff < 60_000) return (diff / 1_000) + "s ago";
        if (diff < 3_600_000) return (diff / 60_000) + "m ago";
        if (diff < 86_400_000) return (diff / 3_600_000) + "h ago";
        return (diff / 86_400_000) + "d ago";
    }

    @Override
    public int getItemCount() { return visible.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, meta, mac, signal, badge;
        final ImageView icon, star;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.col_name);
            meta = v.findViewById(R.id.col_meta);
            mac = v.findViewById(R.id.col_mac);
            signal = v.findViewById(R.id.col_signal);
            badge = v.findViewById(R.id.col_badge);
            icon = v.findViewById(R.id.dev_icon);
            star = v.findViewById(R.id.col_star);
        }
    }
}
