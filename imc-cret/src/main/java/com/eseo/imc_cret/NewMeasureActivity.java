package com.eseo.imc_cret;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NewMeasureActivity extends BaseActivity {

    public static final String EXTRA_BMI = "extra_bmi";

    private static final String DOB_PATTERN = "dd/MM/yyyy";

    private TextView tvResult;
    private Spinner spGender;
    private TextInputEditText etBirthdate;
    private TextInputEditText etWeight;
    private TextInputEditText etHeight;
    private RadioButton rbCentimeter;
    private CheckBox cbDisplay;

    private float lastComputedBmi = -1f;
    private Date lastDob;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_new_measure;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
        toolbar.setNavigationOnClickListener(v -> navigateUpWithResult());
        toolbar.inflateMenu(R.menu.new_measure_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_email) {
                sendBmiByEmail();
                return true;
            }
            return false;
        });

        tvResult = findViewById(R.id.tv_result);
        spGender = findViewById(R.id.sp_gender);
        etBirthdate = findViewById(R.id.et_birthdate);
        etWeight = findViewById(R.id.et_weight);
        etHeight = findViewById(R.id.et_height);
        RadioGroup rgUnit = findViewById(R.id.rg_unit);
        findViewById(R.id.rb_meter);
        rbCentimeter = findViewById(R.id.rb_centimeter);
        cbDisplay = findViewById(R.id.cb_display);
        android.widget.ImageButton btnCalendar = findViewById(R.id.btn_birthdate_calendar);
        MaterialButton btnCalc = findViewById(R.id.btn_calculate);
        MaterialButton btnRaz = findViewById(R.id.btn_raz);

        // Spinner setup (simple)
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.gender_male), getString(R.string.gender_female)}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);

        // Any change invalidates the current result
        TextWatcher invalidateWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setDefaultResult();
            }
        };
        etBirthdate.addTextChangedListener(invalidateWatcher);
        attachBirthdateAutoFormat(etBirthdate);
        etWeight.addTextChangedListener(invalidateWatcher);
        etHeight.addTextChangedListener(invalidateWatcher);

        rgUnit.setOnCheckedChangeListener((group, checkedId) -> {
            convertHeightOnUnitChange(checkedId);
            setDefaultResult();
        });
        spGender.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                setDefaultResult();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        cbDisplay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // If we already computed, just re-render; otherwise keep default
            if (lastComputedBmi > 0f) {
                renderResult(lastComputedBmi);
            }
        });

        btnCalendar.setOnClickListener(v -> showDatePickerToday());

        btnCalc.setOnClickListener(v -> onCalculateClicked());

        btnRaz.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.raz_dialog_title)
                .setMessage(R.string.raz_dialog_message)
                .setPositiveButton(R.string.raz_dialog_yes, (dialog, which) -> resetForm())
                .setNegativeButton(R.string.raz_dialog_no, null)
                .show());

        setDefaultResult();
    }

    /**
     * Auto-formats a French birthdate as DD/MM/YYYY.
     * - Inserts '/' after 2 and 4 digits (so positions 2 and 5 in the full string).
     * - When deleting, also removes the preceding '/' if needed.
     */
    private void attachBirthdateAutoFormat(@NonNull TextInputEditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isEditing;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing || s == null) return;
                isEditing = true;
                try {
                    String current = s.toString();
                    // Keep only digits
                    String digits = current.replaceAll("[^0-9]", "");
                    if (digits.length() > 8) digits = digits.substring(0, 8);

                    // Rebuild with slashes
                    StringBuilder out = new StringBuilder();
                    for (int i = 0; i < digits.length(); i++) {
                        out.append(digits.charAt(i));
                        if (i == 1 || i == 3) {
                            if (i != digits.length() - 1) {
                                out.append('/');
                            }
                        }
                    }

                    // If user is deleting and lands on a slash, allow it to disappear naturally.
                    // Our rebuild already handles it, but we keep cursor correction below.
                    String formatted = out.toString();

                    if (!formatted.equals(current)) {
                        editText.setText(formatted);

                        // Cursor positioning: approximate by keeping it at end of entered content.
                        int cursor = formatted.length();
                        if (editText.getText() != null) {
                            // If deleting, don't jump forward (keep cursor at end).
                            editText.setSelection(cursor);
                        }
                    }
                } finally {
                    isEditing = false;
                }
            }
        });
    }

    private void setDefaultResult() {
        lastComputedBmi = -1f;
        lastDob = null;
        if (tvResult != null) {
            tvResult.setText(R.string.result_default);
        }
    }

    private void onCalculateClicked() {
        Date dob = parseDobOrToast();
        if (dob == null) return;
        lastDob = dob;

        Float weight = parsePositiveFloat(etWeight, R.string.error_invalid_weight);
        if (weight == null) return;

        Float heightInput = parsePositiveFloat(etHeight, R.string.error_invalid_height);
        if (heightInput == null) return;

        float heightMeters = rbCentimeter.isChecked() ? (heightInput / 100f) : heightInput;
        if (heightMeters <= 0f) {
            Toast.makeText(this, R.string.error_invalid_height, Toast.LENGTH_SHORT).show();
            return;
        }

        float bmi = weight / (heightMeters * heightMeters);
        if (Float.isNaN(bmi) || Float.isInfinite(bmi) || bmi <= 0f) {
            Toast.makeText(this, R.string.error_invalid_height, Toast.LENGTH_SHORT).show();
            return;
        }

        lastComputedBmi = bmi;
        renderResult(bmi);

        // Persist BMI for the profile relaunch message
        SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putFloat(AppPrefs.KEY_LAST_BMI, bmi).apply();
    }

    private void renderResult(float bmi) {
        DecimalFormat df = new DecimalFormat("0.0");
        String bmiStr = df.format(bmi);

        if (!cbDisplay.isChecked()) {
            tvResult.setText(getString(R.string.result_simple, bmiStr));
            return;
        }

        boolean male = spGender.getSelectedItemPosition() == 0;
        String intro = getString(male ? R.string.result_detailed_intro_m : R.string.result_detailed_intro_f, bmiStr);

        Integer age = lastDob == null ? null : computeAgeYears(lastDob);
        if (age == null) {
            tvResult.setText(intro);
            return;
        }
        int ageMin = Math.max(0, (age / 10) * 10);
        int ageMax = ageMin + 9;

        String category = bmiCategory(bmi);
        String catLine = getString(R.string.result_detailed_category, ageMin, ageMax, category);
        tvResult.setText(intro + "\n" + catLine);
    }

    @Nullable
    private Date parseDobOrToast() {
        String text = etBirthdate.getText() == null ? "" : etBirthdate.getText().toString().trim();
        SimpleDateFormat sdf = new SimpleDateFormat(DOB_PATTERN, Locale.FRANCE);
        sdf.setLenient(false);
        try {
            Date dob = sdf.parse(text);
            if (dob == null) throw new ParseException("null", 0);
            if (dob.after(new Date())) {
                Toast.makeText(this, R.string.error_invalid_birthdate, Toast.LENGTH_SHORT).show();
                return null;
            }
            return dob;
        } catch (ParseException e) {
            Toast.makeText(this, R.string.error_invalid_birthdate, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private Integer computeAgeYears(@NonNull Date dob) {
        Calendar birth = Calendar.getInstance();
        birth.setTime(dob);
        Calendar now = Calendar.getInstance();

        int age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        int nowDayOfYear = now.get(Calendar.DAY_OF_YEAR);
        int birthDayOfYear = birth.get(Calendar.DAY_OF_YEAR);
        if (nowDayOfYear < birthDayOfYear) {
            age--;
        }
        return Math.max(age, 0);
    }

    @Nullable
    private Float parsePositiveFloat(@NonNull TextInputEditText et, int errorStringRes) {
        String raw = et.getText() == null ? "" : et.getText().toString().trim().replace(',', '.');
        try {
            float v = Float.parseFloat(raw);
            if (v <= 0f) {
                Toast.makeText(this, errorStringRes, Toast.LENGTH_SHORT).show();
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            Toast.makeText(this, errorStringRes, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String bmiCategory(float bmi) {
        // Simple WHO categories
        if (bmi < 18.5f) return "Insuffisance pondérale";
        if (bmi < 25f) return "Normal";
        if (bmi < 30f) return "Surpoids";
        return "Obésité";
    }

    private void resetForm() {
        // Also clear persisted BMI so Profile page doesn't show an old value
        SharedPreferences prefs = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(AppPrefs.KEY_LAST_BMI).apply();

        spGender.setSelection(0);
        etBirthdate.setText("");
        etWeight.setText("");
        etHeight.setText("");
        rbCentimeter.setChecked(true);
        cbDisplay.setChecked(false);
        setDefaultResult();
    }

    private void convertHeightOnUnitChange(int checkedId) {
        if (etHeight == null) return;

        String raw = etHeight.getText() == null ? "" : etHeight.getText().toString().trim();
        if (raw.isEmpty()) return;

        // Accept both comma and dot
        String normalized = raw.replace(',', '.');
        float value;
        try {
            value = Float.parseFloat(normalized);
        } catch (NumberFormatException e) {
            return; // don't touch user input if it's not parseable
        }

        // Guard: ignore non-positive values
        if (value <= 0f) return;

        boolean toMeters = checkedId == R.id.rb_meter;
        boolean toCentimeters = checkedId == R.id.rb_centimeter;
        if (!toMeters && !toCentimeters) return;

        float converted = toMeters ? (value / 100f) : (value * 100f);

        // Format: meters with up to 2 decimals, centimeters without decimals when possible
        DecimalFormat df = new DecimalFormat(toMeters ? "0.##" : "0.#");
        String out = df.format(converted);
        etHeight.setText(out);
        etHeight.setSelection(out.length());
    }

    private void showDatePickerToday() {
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) -> {
            String dd = String.format(java.util.Locale.FRANCE, "%02d", dayOfMonth);
            String mm = String.format(java.util.Locale.FRANCE, "%02d", month + 1);
            etBirthdate.setText(String.format(java.util.Locale.FRANCE, "%s/%s/%d", dd, mm, year));
        }, y, m, d);
        dialog.show();
    }

    private void navigateUpWithResult() {
        Intent data = new Intent();
        if (lastComputedBmi > 0f) {
            data.putExtra(EXTRA_BMI, lastComputedBmi);
        }
        setResult(RESULT_OK, data);
        finish();
    }

    private void sendBmiByEmail() {
        String subject = getString(R.string.email_subject_bmi);
        String body;
        if (lastComputedBmi > 0f) {
            DecimalFormat df = new DecimalFormat("0.0");
            String bmiStr = df.format(lastComputedBmi);
            body = getString(R.string.email_body_bmi, bmiStr);
        } else {
            body = getString(R.string.email_body_no_result);
        }

        String encodedSubject = android.net.Uri.encode(subject);
        String encodedBody = android.net.Uri.encode(body);
        android.net.Uri mailUri = android.net.Uri.parse("mailto:?subject=" + encodedSubject + "&body=" + encodedBody);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(mailUri);

        // Robustness: avoid crash if no email app
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, R.string.error_no_email_app, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_no_email_app, Toast.LENGTH_SHORT).show();
        }
    }
}
