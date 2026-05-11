package com.eseo.imc_cret;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;

public class ProfileActivity extends BaseActivity {

    private static final String PREFS_NAME = "imc_cret_prefs";
    private static final String KEY_FIRSTNAME = "firstname";
    private static final String KEY_LAST_BMI = "last_bmi";

    private ActivityResultLauncher<Intent> newMeasureLauncher;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_profile;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        TextInputEditText etFirstname = findViewById(R.id.et_firstname);
        android.widget.TextView tvSummary = findViewById(R.id.tv_summary);
        MaterialButton btnCalculate = findViewById(R.id.btn_calculate);
        ImageView iv = findViewById(R.id.iv_profile_illustration);

        newMeasureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    if (result.getData().hasExtra(NewMeasureActivity.EXTRA_BMI)) {
                        float bmi = result.getData().getFloatExtra(NewMeasureActivity.EXTRA_BMI, -1f);
                        if (bmi > 0f) {
                            prefs.edit().putFloat(KEY_LAST_BMI, bmi).apply();
                            String currentFirstname = etFirstname.getText() == null ? "" : etFirstname.getText().toString().trim();
                            updateSummary(tvSummary, currentFirstname, bmi);
                        }
                    }
                }
        );

        // Restore persisted values
        String firstname = prefs.getString(KEY_FIRSTNAME, "");
        float lastBmi = prefs.getFloat(KEY_LAST_BMI, -1f);
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
                prefs.edit().putString(KEY_FIRSTNAME, trimmed).apply();
                btnCalculate.setEnabled(!trimmed.isEmpty());
                updateSummary(tvSummary, trimmed, prefs.getFloat(KEY_LAST_BMI, -1f));
            }
        });

        btnCalculate.setOnClickListener(v -> newMeasureLauncher.launch(new Intent(this, NewMeasureActivity.class)));

        // Simple animation on the image (scale pulse)
        iv.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(900)
                .setStartDelay(200)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> iv.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(900)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .start())
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
}

