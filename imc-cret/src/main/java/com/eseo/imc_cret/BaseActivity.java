package com.eseo.imc_cret;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String CORE_ACTIVITY = "com.mjm.elixir_reign.android.AndroidLauncher";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        EdgeToEdge.enable(this);
        setContentView(getLayoutId());

        setupNavigation();
        setupWindowInsets();
    }

    protected abstract int getLayoutId();

    private void setupNavigation() {
        View navLeft = findViewById(R.id.nav_left);
        View navCenter = findViewById(R.id.nav_center);
        View navRight = findViewById(R.id.nav_right);

        navLeft.setOnClickListener(v -> navigateTo(NewMeasureActivity.class));
        navCenter.setOnClickListener(v -> navigateTo(ProfileActivity.class));
        navRight.setOnClickListener(v -> openCore());
    }

    private void navigateTo(Class<?> activityClass) {
        if (getClass() == activityClass) {
            return;
        }
        startActivity(new Intent(this, activityClass));
    }

    private void setupWindowInsets() {
        View main = findViewById(R.id.main);
        if (main == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void openCore() {
        Intent intent = new Intent();
        intent.setClassName(this, CORE_ACTIVITY);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.core_not_installed, Toast.LENGTH_LONG).show();
        }
    }

    private void applySavedTheme() {
        android.content.SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        if (!prefs.contains(AppPrefs.KEY_DARK_MODE)) {
            return;
        }
        boolean night = prefs.getBoolean(AppPrefs.KEY_DARK_MODE, false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                night ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
