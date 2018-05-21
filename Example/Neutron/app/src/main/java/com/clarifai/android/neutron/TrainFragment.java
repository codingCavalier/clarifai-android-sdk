package com.clarifai.android.neutron;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * TrainFragment.java
 * Neutron
 *
 * Copyright © 2018 Clarifai. All rights reserved.
 */

public class TrainFragment extends Fragment {
    public TrainFragment() {
    }

    public static TrainFragment newInstance() {
        return new TrainFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_train, container, false);
    }

}
