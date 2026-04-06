package com.eseo.imc_cret;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lance directement vers la première page
        startActivity(new Intent(this, ProfileActivity.class));
        finish();
    }
}
