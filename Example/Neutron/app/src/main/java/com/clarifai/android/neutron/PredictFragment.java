package com.clarifai.android.neutron;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.clarifai.clarifai_android_sdk.core.Constants;
import com.clarifai.clarifai_android_sdk.dataassets.DataAsset;
import com.clarifai.clarifai_android_sdk.dataassets.Image;
import com.clarifai.clarifai_android_sdk.datamodels.Concept;
import com.clarifai.clarifai_android_sdk.datamodels.Input;
import com.clarifai.clarifai_android_sdk.datamodels.Model;
import com.clarifai.clarifai_android_sdk.datamodels.Output;
import com.clarifai.clarifai_android_sdk.utils.Error;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    ImageView imageView;
    ProgressBar progressBar;

    ScrollView mainScrollView;
    ConstraintLayout container;
    TextView outputTextView;
    Button predictButton;
    Button minimizeResultsBtn;

    List<Concept> concepts;
    Model model;

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
        setupPreModelLoadUI(view); // Loading some UI components before loading model

        new LoadModel(this).execute();

        setupRestOfTheUIs(view); // Can setup these ui components in parallel with model loading
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (model != null) {
            model.delete();
        }
    }

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
            fragment.progressBar.setVisibility(View.VISIBLE);
            fragment.setOutputText("Loading model...");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            PredictFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) {
                return false;
            }
            if (fragment.model != null) {
                fragment.model.delete(); // Do not forget to delete model to clear up memory that the model is using
                fragment.model = null;
            }
            fragment.model = new Model(Constants.GeneralModelVersion, "General"); // Create new general model
            return true;
        }

        @Override
        protected void onPostExecute(Boolean modelLoaded) {
            PredictFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.isRemoving()) {
                return;
            }
            fragment.progressBar.setVisibility(View.GONE);
            if (modelLoaded) {
                fragment.predictButton.setEnabled(true);
                fragment.setOutputText("Model loaded!");
            } else {
                fragment.setOutputText("Model failed to load!");
            }
        }
    }


    private void doPrediction() {
        // UI Stuffs
        this.progressBar.setVisibility(View.VISIBLE);
        this.predictButton.setEnabled(false);

        // Get image from ImageView
        Image image = new Image(((BitmapDrawable) this.imageView.getDrawable()).getBitmap());
        // Create data asset
        DataAsset dataAsset = new DataAsset(image);
        // Create input for the model
        Input input = new Input(dataAsset);
        List<Input> inputs = new ArrayList<>();
        inputs.add(input);

        // Give inputs to model
        this.model.setInputs(inputs);

        this.setOutputText("Prediction starting...");

        // Run prediction
        this.model.predict(new Model.ModelCallbacks() {
            @Override
            public void PredictionComplete(final boolean successful, final Error error) {
                Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finishPrediction(successful, error);
                    }
                });
            }
        });
    }

    private void finishPrediction(boolean successful, Error error) {
        this.progressBar.setVisibility(View.GONE);
        this.predictButton.setEnabled(true);
        if (successful) {
            this.setOutputText("Prediction finished!");

            List<Output> outputs = this.model.getOutputs();
            this.concepts.clear();
            Output output = outputs.get(0);
            DataAsset dataAsset = output.getDataAsset();
            List<Concept> concepts = dataAsset.getConcepts();

            if (concepts == null) {
                Log.e(TAG, "No concepts were gotten during prediction!");
                concepts = new ArrayList<>();
            }
            this.concepts.addAll(concepts);

            this.addConceptsToTableView();
        } else {
            this.setOutputText("Prediction failed: " + error.getErrorMessage());
        }
    }


    private void addConceptsToTableView() {
        StringBuilder sbNice = new StringBuilder();
        for (Concept concept : concepts) {
            String scorePercentage = String.format(Locale.getDefault(), "%.4f", concept.getScore());
            sbNice.append(concept.getName()).append(": ").append(scorePercentage).append("\n");
        }
        setOutputText(sbNice.toString());
        minimizeResultsBtn.setVisibility(View.VISIBLE);
        parent.invalidate();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_predict, container, false);
    }

    private void setupPreModelLoadUI(View view) {
        parent = view;
        outputTextView = view.findViewById(R.id.output_area_tv);
        progressBar = view.findViewById(R.id.progress_bar_spinner);
        minimizeResultsBtn = view.findViewById(R.id.minimize_results_btn);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupRestOfTheUIs(View view) {
        mainScrollView = view.findViewById(R.id.main_scrollview);
        generalModelButton = view.findViewById(R.id.general_button);
        customModelButton = view.findViewById(R.id.custom_button);
        predictButton = view.findViewById(R.id.predict_btn);
        imageView = view.findViewById(R.id.image_view);
        container = view.findViewById(R.id.container);

        outputTextView.setAlpha(0.85f);
        outputTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mainScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                outputTextView.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }
        });
        outputTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                outputTextView.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        // custom model not available
        customModelButton.setAlpha(0.5f);
        customModelButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast.makeText(getActivity(), "Custom model is currently unavailable", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        customModelButton.setClickable(false);

        minimizeResultsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button thisButton = (Button) v;
                String currentText = thisButton.getText().toString();
                if (currentText.equals("MAXIMIZE")) {
                    maximizeOutputTV();
                } else {
                    minimizeOutputTV();
                }
            }
        });
        predictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPrediction();
            }
        });

        predictButton.setEnabled(false);
    }

    private void setOutputText(String outputText) {
        outputTextView.setText(outputText);
        minimizeResultsBtn.setVisibility(View.GONE);
    }

    private void minimizeOutputTV() {
        ObjectAnimator animation = ObjectAnimator.ofInt(outputTextView, "maxLines", 2);
        animation.setDuration(150);
        animation.start();
        minimizeResultsBtn.setText("MAXIMIZE");
    }

    private void maximizeOutputTV() {
        ObjectAnimator animation = ObjectAnimator.ofInt(outputTextView, "maxLines", 15);
        animation.setDuration(150);
        animation.start();
        minimizeResultsBtn.setText("MINIMIZE");
    }
}
