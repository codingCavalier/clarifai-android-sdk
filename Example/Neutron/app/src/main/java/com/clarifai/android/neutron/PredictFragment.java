package com.clarifai.android.neutron;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.clarifai.clarifai_android_sdk.core.Clarifai;
import com.clarifai.clarifai_android_sdk.core.Constants;
import com.clarifai.clarifai_android_sdk.dataassets.BoundingBox;
import com.clarifai.clarifai_android_sdk.dataassets.DataAsset;
import com.clarifai.clarifai_android_sdk.dataassets.Face;
import com.clarifai.clarifai_android_sdk.dataassets.Image;
import com.clarifai.clarifai_android_sdk.dataassets.Region;
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
public class PredictFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = PredictFragment.class.getSimpleName();

    View parent;
    ImageView imageView;
    ProgressBar progressBar;

    ScrollView mainScrollView;
    ConstraintLayout container;
    TextView outputTextView;
    Button predictButton;
    Button minimizeResultsBtn;
    TextView output;
    private static final String CustomModelVersion = "appleCustomModel";

    Model model;
    Spinner modelSpinner;
    private String selectedModel = "General";

    public PredictFragment() {
        // required empty default constructor
    }

    public static PredictFragment newInstance() {
        return new PredictFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupPreModelLoadUI(view); // Loading some UI components before loading model

        output = view.findViewById(com.clarifai.clarifai_android_sdk.R.id.output_area_tv);
        new LoadModel(this).execute();
        modelSpinner = view.findViewById(R.id.model_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter =
                ArrayAdapter.createFromResource(Objects.requireNonNull(getActivity()), R.array.model_array,
                        android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(spinnerAdapter);
        modelSpinner.setOnItemSelectedListener(this);

        setupRestOfTheUIs(view); // Can setup these ui components in parallel with model loading
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Clarifai.getInstance().deleteModel(model);
        model = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedModel = (String)parent.getItemAtPosition(position);
        loadCurrentModel();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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
            Clarifai.getInstance().deleteModel(fragment.model);
            fragment.model = null;
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
        final Bitmap bitmap = ((BitmapDrawable) this.imageView.getDrawable()).getBitmap();
        Image image = new Image(bitmap);
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
                        finishPrediction(successful, error, bitmap);
                    }
                });
            }
        });
    }

    private void finishPrediction(boolean successful, Error error, Bitmap bitmap) {
        this.progressBar.setVisibility(View.GONE);
        this.predictButton.setEnabled(true);
        if (successful) {
            this.setOutputText("Prediction finished!");
            if (model.getOutputAt(0).getDataAsset().getConcepts() != null) {
                populateOutputListWithConcepts(model.getOutputAt(0).getDataAsset().getConcepts(), bitmap);
            } else if (model.getOutputAt(0).getDataAsset().getRegions() != null) {
                populateOutputListWithRegions(model.getOutputAt(0).getDataAsset().getRegions(), bitmap);
            }
        } else {
            this.setOutputText("Prediction failed: " + error.getErrorMessage());
        }
    }


    private void populateOutputListWithConcepts(List<Concept> allConcepts, Bitmap bitmap) {
        setPredictionOutput(stringifyConcepts(allConcepts, ""), bitmap);
    }

    private void populateOutputListWithRegions(List<Region> allRegions, Bitmap bitmap) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Regions:\n(top,bottom,left,right)\n");
        for (Region region : allRegions) {
            BoundingBox box = region.getRegionInfo().getBoundingBox();
            String regionBounds = String.format(Locale.getDefault(), "(%.2f, %.2f, %.2f, %.2f)", box.getTop(),
                    box.getBottom(), box.getLeft(), box.getRight());

            stringBuilder.append(region.getRegionId()).append(": ").append(regionBounds).append("\n");
            if (region.getDataAsset().getFace() != null) {
                Face face = region.getDataAsset().getFace();
                stringBuilder.append("\tAge:\n");
                stringBuilder.append(stringifyConcepts(face.getAge().getConcepts().subList(0, 5), "\t\t"));
                stringBuilder.append("\tGender:\n");
                stringBuilder.append(stringifyConcepts(face.getGender().getConcepts(), "\t\t"));
                stringBuilder.append("\tMcAffinity:\n");
                stringBuilder.append(stringifyConcepts(face.getMcAffinity().getConcepts(), "\t\t"));
                stringBuilder.append("\tIdentity:\n");
                stringBuilder.append(stringifyConcepts(face.getIdentity().getConcepts(), "\t\t"));
            }
        }
        setPredictionOutput(stringBuilder.toString(), bitmap);
    }

    private String stringifyConcepts(List<Concept> concepts, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Concept concept : concepts) {
            String scorePercentage = String.format(Locale.getDefault(), "%.3f", concept.getScore());
            stringBuilder.append(prefix).append(concept.getName()).append(": ").append(scorePercentage).append("\n");
        }
        return stringBuilder.toString();
    }

    private void addConceptsToTableView(List<Concept> concepts) {
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

    private void disableButton(Button button) {
        button.setEnabled(false);
        button.setClickable(false);
        button.setAlpha(.5f);
    }

    private void enableButton(Button button) {
        button.setEnabled(true);
        button.setClickable(true);
        button.setAlpha(1f);
    }

    private void setPredictionOutput(final String outputStr, final Bitmap bitmap) {
        Activity context = getActivity();
        if (context == null) {
            Log.d(TAG, "Activity is null");
            return;
        }
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                if (bitmap != null) {
                    PredictFragment.this.imageView.setImageBitmap(bitmap);
                }
                PredictFragment.this.output.setText(outputStr);
            }
        });
    }

    private void loadCurrentModel() {
        disableButton(this.predictButton);
        addConceptsToTableView(new ArrayList<Concept>());
        this.progressBar.setVisibility(View.VISIBLE);
        Clarifai.getInstance().deleteModel(this.model);
        switch (this.selectedModel) {
            default:
            case "General":
                this.model = Clarifai.getInstance().getGeneralModel();
                this.imageView.setImageDrawable(this.getResources().getDrawable(R.drawable.image_general));
                break;
            case "NSFW":
                this.model = new Model(Constants.NsfwModelVersion, "NSFW");
                this.model.setThreshold(0.0f);
                this.imageView.setImageDrawable(this.getResources().getDrawable(R.drawable.image_nsfw));
                break;
            case "FaceDetect":
                this.model = new Model(Constants.FaceDetectModelVersion, "FaceDetect");
                this.model.setThreshold(0.05f);
                this.imageView.setImageDrawable(this.getResources().getDrawable(R.drawable.image_facedetect));
                break;
            case "FaceDetect - No Face":
                this.model = new Model(Constants.FaceDetectModelVersion, "FaceDetect");
                this.model.setThreshold(0.05f);
                this.imageView.setImageDrawable(this.getResources().getDrawable(R.drawable.image_general));
                break;
            case "Demographics 1":
                this.model = new Model(Constants.DemographicsModelVersion, "Demographics");
                this.imageView.setImageDrawable(this.getResources().getDrawable(R.drawable.image_demographics_1));
                break;
            case "Demographics 2":
                this.model = new Model(Constants.DemographicsModelVersion, "Demographics");
                this.imageView.setImageDrawable(this.getResources().getDrawable(R.drawable.image_demographics_2));
                break;
            case "Custom":
                this.model = new Model(CustomModelVersion, "Custom");
                this.model.setThreshold(0.0f);
                this.imageView.setImageDrawable(this.getResources().getDrawable(R.drawable.image_custom));
        }
        setPredictionOutput("", this.imageView.getDrawingCache());
        enableButton(this.predictButton);
        this.progressBar.setVisibility(View.GONE);
    }
}
