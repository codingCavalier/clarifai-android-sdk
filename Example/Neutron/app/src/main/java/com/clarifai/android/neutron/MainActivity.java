package com.clarifai.android.neutron;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.clarifai.clarifai_android_sdk.core.Clarifai;
import com.clarifai.clarifai_android_sdk.utils.App;

/**
 * MainActivity.java
 * Neutron
 *
 * Copyright © 2018 Clarifai. All rights reserved.
 */

public class MainActivity extends AppCompatActivity {
    private final String TAG = "Neutron";
    private static final int STORAGE_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String apiKey = getString(R.string.api_key);
        if (apiKey.equals("ENTER YOUR API KEY")) {
            Toast.makeText(this, "No valid API key was given!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "No valid API key was given!");
        } else {
            Clarifai.start(getApplicationContext(), apiKey);
        }
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        predictSelected();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_predict:
                    predictSelected();
                    return true;
                case R.id.navigation_train:
                    trainSelected();
                    return true;
            }
            return false;
        }
    };

    private void predictSelected() {
        setTitle("Predict");
        loadFragment(PredictFragment.newInstance());
    }

    private void trainSelected() {
        setTitle("Train");
        loadFragment(TrainFragment.newInstance());
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();
    }

    public boolean hasAllPermissions() {
        int writePermissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (writePermissionCheck != PackageManager.PERMISSION_GRANTED || readPermissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_REQUEST);
            return false;
        }
        return true;
    }

}
