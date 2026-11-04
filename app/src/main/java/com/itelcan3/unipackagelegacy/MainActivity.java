package com.itelcan3.unipackagelegacy;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itelcan3.unipackagelegacy.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements AppListAdapter.OnUninstallClickListener {

    private final Integer[] dayOptions = new Integer[]{7, 15, 30, 90, 180, 360};
    private int selectedDays = 30;

    private View cardPermission;
    private View btnGrantPermission;
    private View layoutSectionHeader;
    private Spinner spinnerDays;
    private View layoutLoading;
    private TextView tvEmptyState;
    private RecyclerView rvUnusedApps;

    private AppListAdapter appListAdapter;
    private LeastUsedAppsReader appsReader;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        appsReader = new LeastUsedAppsReader(this);

        initViews();
        setupSpinner();
        setupRecyclerView();
    }

    private void initViews() {
        cardPermission = findViewById(R.id.card_permission);
        btnGrantPermission = findViewById(R.id.btn_grant_permission);
        layoutSectionHeader = findViewById(R.id.layout_section_header);
        spinnerDays = findViewById(R.id.spinner_days);
        layoutLoading = findViewById(R.id.layout_loading);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        rvUnusedApps = findViewById(R.id.rv_unused_apps);

        btnGrantPermission.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        });
    }

    private void setupSpinner() {
        List<String> spinnerLabels = new ArrayList<>();
        int defaultPosition = 2; // 30 days is at index 2
        for (int i = 0; i < dayOptions.length; i++) {
            spinnerLabels.add(getString(R.string.days_option_format, dayOptions[i]));
            if (dayOptions[i] == selectedDays) {
                defaultPosition = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                spinnerLabels
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerDays.setAdapter(adapter);
        spinnerDays.setSelection(defaultPosition, false);

        spinnerDays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDays = dayOptions[position];
                if (checkUsageStatsPermission()) {
                    scanUnusedApps();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupRecyclerView() {
        rvUnusedApps.setLayoutManager(new LinearLayoutManager(this));
        appListAdapter = new AppListAdapter(this);
        rvUnusedApps.setAdapter(appListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void updatePermissionState() {
        boolean hasPermission = checkUsageStatsPermission();
        if (!hasPermission) {
            cardPermission.setVisibility(View.VISIBLE);
            layoutSectionHeader.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.GONE);
            rvUnusedApps.setVisibility(View.GONE);
        } else {
            cardPermission.setVisibility(View.GONE);
            layoutSectionHeader.setVisibility(View.VISIBLE);
            scanUnusedApps();
        }
    }

    private boolean checkUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void scanUnusedApps() {
        layoutLoading.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        rvUnusedApps.setVisibility(View.GONE);

        executorService.execute(() -> {
            List<LeastUsedApp> unusedApps = appsReader.getLeastUsedApps(selectedDays);
            mainHandler.post(() -> {
                layoutLoading.setVisibility(View.GONE);
                if (unusedApps == null || unusedApps.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvUnusedApps.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvUnusedApps.setVisibility(View.VISIBLE);
                    appListAdapter.setApps(unusedApps);
                }
            });
        });
    }

    @Override
    public void onUninstallClick(LeastUsedApp app) {
        if (app == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.uninstall_dialog_title)
                .setMessage(getString(R.string.uninstall_dialog_msg, app.getAppName()))
                .setPositiveButton(R.string.uninstall, (dialog, which) -> {
                    uninstallApp(app.getPackageName());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void uninstallApp(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }
}
