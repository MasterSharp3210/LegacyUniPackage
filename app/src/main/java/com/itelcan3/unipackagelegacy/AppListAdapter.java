package com.itelcan3.unipackagelegacy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itelcan3.unipackagelegacy.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    public interface OnUninstallClickListener {
        void onUninstallClick(LeastUsedApp app);
    }

    private final List<LeastUsedApp> appList = new ArrayList<>();
    private final OnUninstallClickListener uninstallClickListener;

    public AppListAdapter(OnUninstallClickListener listener) {
        this.uninstallClickListener = listener;
    }

    public void setApps(List<LeastUsedApp> newApps) {
        this.appList.clear();
        if (newApps != null) {
            this.appList.addAll(newApps);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        LeastUsedApp app = appList.get(position);
        holder.tvAppName.setText(app.getAppName());
        holder.tvPackageName.setText(app.getPackageName());
        holder.tvAppSize.setText(holder.itemView.getContext().getString(R.string.size_prefix, formatFileSize(app.getAppSize())));

        holder.btnUninstall.setOnClickListener(v -> {
            if (uninstallClickListener != null) {
                uninstallClickListener.onUninstallClick(app);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppName;
        TextView tvPackageName;
        TextView tvAppSize;
        Button btnUninstall;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvPackageName = itemView.findViewById(R.id.tv_package_name);
            tvAppSize = itemView.findViewById(R.id.tv_app_size);
            btnUninstall = itemView.findViewById(R.id.btn_uninstall);
        }
    }

    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        if (digitGroups >= units.length) {
            digitGroups = units.length - 1;
        }
        return String.format(Locale.US, "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
