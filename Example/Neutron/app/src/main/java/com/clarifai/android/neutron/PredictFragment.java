package com.clarifai.android.neutron;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.clarifai.clarifai_android_sdk.dataassets.DataAsset;
import com.clarifai.clarifai_android_sdk.dataassets.Image;
import com.clarifai.clarifai_android_sdk.datamodels.Concept;
import com.clarifai.clarifai_android_sdk.datamodels.Input;
import com.clarifai.clarifai_android_sdk.datamodels.Model;
import com.clarifai.clarifai_android_sdk.datamodels.Output;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PredictFragment.java
 * Neutron
 *
 * Copyright © 2018 Clarifai. All rights reserved.
 */

@SuppressLint("SetTextI18n")
public class PredictFragment extends Fragment {
    private static final String TAG = PredictFragment.class.getSimpleName();

    View parent;
    AppCompatRadioButton generalModelButton;
    AppCompatRadioButton customModelButton;
    Button predictButton;
    ImageView imageView;
    LinearLayout linearLayoutConcepts;
    LinearLayout linearLayoutScores;
    ProgressBar progressBar;
    TextView statusText;

    List<Concept> concepts;
    Model model;
    ConditionVariable modelLoaded = new ConditionVariable();


    private static class LoadModel extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<PredictFragment> fragmentReference;

        LoadModel(PredictFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            PredictFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) {
                return;
            }
            fragment.statusText.setText("Loading model...");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            PredictFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) {
                return false;
            }
            if (fragment.model != null) {
                fragment.modelLoaded.close();
                fragment.model.delete();
                fragment.model = null;
            }
            fragment.model = new Model("aaa03c23b3724a16a56b629203edc62c", "General");
            return true;
        }

        @Override
        protected void onPostExecute(Boolean modelLoaded) {
            PredictFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) {
                return;
            }
            if (modelLoaded) {
                fragment.modelLoaded.open();
                fragment.statusText.setText("Model loaded!");
            } else {
                fragment.statusText.setText("Model failed to load!");
            }
        }
    }

    private static class PredictOnModel extends AsyncTask<Void, Void, Void> {
        private WeakReference<PredictFragment> fragmentReference;

        PredictOnModel(PredictFragment context) {
            fragmentReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            final PredictFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) {
                return;
            }
            fragment.linearLayoutConcepts.setVisibility(View.GONE);
            fragment.linearLayoutScores.setVisibility(View.GONE);
            fragment.progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final PredictFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) {
                return null;
            }
            Activity fragmentActivity = fragment.getActivity();
            if (fragmentActivity == null || fragmentActivity.isFinishing()) {
                return null;
            }
            fragment.modelLoaded.block(); // wait for model to finish loading
            Image image = new Image(((BitmapDrawable)fragment.imageView.getDrawable()).getBitmap());

            DataAsset dataAsset = new DataAsset(image);

            Input input = new Input(dataAsset);
            List<Input> inputs = new ArrayList<>();
            inputs.add(input);

            fragment.model.setInputs(inputs);

            fragmentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fragment.statusText.setText("Prediction starting...");
                }
            });
            fragment.model.predict();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            final PredictFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) {
                return;
            }
            fragment.statusText.setText("Prediction finished!");
            fragment.progressBar.setVisibility(View.GONE);
            fragment.linearLayoutConcepts.setVisibility(View.VISIBLE);
            fragment.linearLayoutScores.setVisibility(View.VISIBLE);

            List<Output> outputs = fragment.model.getOutputs();
            fragment.concepts.clear();
            Output output = outputs.get(0);
            DataAsset dataAsset = output.getDataAsset();
            List<Concept> concepts = dataAsset.getConcepts();
            if (concepts == null) {
                Log.e(TAG, "No concepts were gotten during prediction!");
                concepts = new ArrayList<>();
            }
            fragment.concepts.addAll(concepts);

            fragment.addConceptsToTableView();
        }
    }

    public PredictFragment() {
        // required empty default constructor
    }

    public static PredictFragment newInstance() {
        return new PredictFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        concepts = new ArrayList<>();
        parent = view;
        statusText = view.findViewById(R.id.status_text);
        new LoadModel(this).execute();

        generalModelButton = view.findViewById(R.id.general_button);
        customModelButton = view.findViewById(R.id.custom_button);
        predictButton = view.findViewById(R.id.predict_button);
        imageView = view.findViewById(R.id.image_view);
        linearLayoutConcepts = view.findViewById(R.id.linear_layout_concepts);
        linearLayoutScores = view.findViewById(R.id.linear_layout_scores);
        progressBar = view.findViewById(R.id.progress_bar_spinner);

        // custom model not available
        customModelButton.setAlpha(0.5f);
        customModelButton.setClickable(false);

        predictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPrediction();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (model != null) {
            modelLoaded.close();
            model.delete();
        }
    }

    private void doPrediction() {
        new PredictOnModel(this).execute();
    }


    private void addConceptsToTableView() {
        for (int i = 0; i < concepts.size(); i++) {
            Concept concept = concepts.get(i);

            TextView conceptName = new TextView(getContext());
            conceptName.setText(concept.getName());
            conceptName.setGravity(Gravity.START);

            TextView conceptScore = new TextView(getContext());
            conceptScore.setText(String.format(Locale.ENGLISH,"%.5f", concept.getScore()));
            conceptScore.setGravity(Gravity.END);

            linearLayoutScores.addView(conceptScore);
            linearLayoutConcepts.addView(conceptName);
        }

        parent.invalidate();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_predict, container, false);
    }
}
