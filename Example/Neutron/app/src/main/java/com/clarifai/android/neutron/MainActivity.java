package com.clarifai.android.neutron;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
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

}
