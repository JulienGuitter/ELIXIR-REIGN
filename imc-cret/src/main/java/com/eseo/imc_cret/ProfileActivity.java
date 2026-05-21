package com.eseo.imc_cret;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;

public class ProfileActivity extends BaseActivity {

    private ActivityResultLauncher<Intent> newMeasureLauncher;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_profile;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);

        TextInputEditText etFirstname = findViewById(R.id.et_firstname);
        android.widget.TextView tvSummary = findViewById(R.id.tv_summary);
        MaterialButton btnCalculate = findViewById(R.id.btn_calculate);
        MaterialButton btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        ImageView iv = findViewById(R.id.iv_profile_illustration);

        updateThemeToggleLabel(btnThemeToggle);
        btnThemeToggle.setOnClickListener(v -> toggleTheme(prefs));

        newMeasureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    if (result.getData().hasExtra(NewMeasureActivity.EXTRA_BMI)) {
                        float bmi = result.getData().getFloatExtra(NewMeasureActivity.EXTRA_BMI, -1f);
                        if (bmi > 0f) {
                            prefs.edit().putFloat(AppPrefs.KEY_LAST_BMI, bmi).apply();
                            String currentFirstname = etFirstname.getText() == null ? "" : etFirstname.getText().toString().trim();
                            updateSummary(tvSummary, currentFirstname, bmi);
                        }
                    }
                }
        );

        // Restore persisted values
        String firstname = prefs.getString(AppPrefs.KEY_FIRSTNAME, "");
        float lastBmi = prefs.getFloat(AppPrefs.KEY_LAST_BMI, -1f);
        if (firstname != null) {
            etFirstname.setText(firstname);
        }
        updateSummary(tvSummary, firstname, lastBmi);

        btnCalculate.setEnabled(firstname != null && !firstname.trim().isEmpty());

        etFirstname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s == null ? "" : s.toString();
                String trimmed = value.trim();
                prefs.edit().putString(AppPrefs.KEY_FIRSTNAME, trimmed).apply();
                btnCalculate.setEnabled(!trimmed.isEmpty());
                updateSummary(tvSummary, trimmed, prefs.getFloat(AppPrefs.KEY_LAST_BMI, -1f));
            }
        });

        btnCalculate.setOnClickListener(v -> newMeasureLauncher.launch(new Intent(this, NewMeasureActivity.class)));

        iv.animate()
                .rotationBy(360f)
                .setDuration(1200)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .start();
    }

    private void updateSummary(android.widget.TextView tvSummary, @Nullable String firstname, float lastBmi) {
        String name = firstname == null ? "" : firstname.trim();
        if (name.isEmpty()) {
            tvSummary.setText(R.string.profile_default_summary);
            return;
        }
        if (lastBmi <= 0f) {
            tvSummary.setText(getString(R.string.profile_summary_no_bmi, name));
            return;
        }
        DecimalFormat df = new DecimalFormat("0.0");
        tvSummary.setText(getString(R.string.profile_summary_with_bmi, name, df.format(lastBmi)));
    }

    private void toggleTheme(@NonNull SharedPreferences prefs) {
        int current = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isNight = current == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        boolean nextNight = !isNight;

        prefs.edit().putBoolean(AppPrefs.KEY_DARK_MODE, nextNight).apply();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                nextNight ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void updateThemeToggleLabel(@NonNull MaterialButton button) {
        int current = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isNight = current == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        button.setText(isNight ? R.string.theme_toggle_light : R.string.theme_toggle_dark);
    }
}
