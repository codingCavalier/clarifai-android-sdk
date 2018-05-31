package com.clarifai.android.neutron;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private static final int TAKE_PICTURE = 0;
    private static final int PICK_IMAGE = 1;
    private String lastImagePath;


    View parent;
    AppCompatRadioButton generalModelButton;
    AppCompatRadioButton customModelButton;
    ImageView imageView;
    ProgressBar progressBar;

    ScrollView mainScrollView;
    ConstraintLayout container;
    TextView outputTextView;
    Button takePictureButton;
    Button selectPictureButton;
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        concepts = new ArrayList<>();
        parent = view;
        outputTextView = view.findViewById(R.id.output_area_tv);
        progressBar = view.findViewById(R.id.progress_bar_spinner);
        minimizeResultsBtn = view.findViewById(R.id.minimize_results_btn);

        new LoadModel(this).execute();

        mainScrollView = view.findViewById(R.id.main_scrollview);
        generalModelButton = view.findViewById(R.id.general_button);
        customModelButton = view.findViewById(R.id.custom_button);
        predictButton = view.findViewById(R.id.predict_btn);
        imageView = view.findViewById(R.id.image_view);
        container = view.findViewById(R.id.container);
        selectPictureButton = view.findViewById(R.id.select_picture_button);
        takePictureButton = view.findViewById(R.id.take_picture_button);

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
                if (currentText == "MAXIMIZE") {
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

        selectPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity)getActivity();
                if (activity != null) {
                    if (activity.hasAllPermissions()) {
                        dispatchSelectPictureIntent();
                    } else {
                        Log.w(TAG, "Getting permissions");
                    }
                } else {
                    Log.w(TAG, "Cannot get activity to doTests");
                }
            }
        });
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity)getActivity();
                if (activity != null) {
                    if (activity.hasAllPermissions()) {
                        dispatchTakePictureIntent();
                    } else {
                        Log.w(TAG, "Getting permissions");
                    }
                } else {
                    Log.w(TAG, "Cannot get activity to doTests");
                }
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getActivity() != null && takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = createImageFile();
            Uri photoURI = FileProvider.getUriForFile(getActivity(), "com.clarifai.android.neutron", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, TAKE_PICTURE);
        }
    }

    private void dispatchSelectPictureIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (model != null) {
            model.delete();
        }
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
                fragment.model.delete();
                fragment.model = null;
            }
            fragment.model = new Model(Constants.GeneralModelVersion, "General");
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
        this.progressBar.setVisibility(View.VISIBLE);
        this.predictButton.setEnabled(false);

        Image image = new Image(((BitmapDrawable)
        this.imageView.getDrawable()).getBitmap());
        DataAsset dataAsset = new DataAsset(image);
        Input input = new Input(dataAsset);
        List<Input> inputs = new ArrayList<>();
        inputs.add(input);

        this.model.setInputs(inputs);

        this.setOutputText("Prediction starting...");
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

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        File storageDir = Objects.requireNonNull(getActivity()).getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String imageFilePath = storageDir + "/" + imageFileName;
        File image = new File(imageFilePath);
        lastImagePath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (requestCode == TAKE_PICTURE) {
            galleryAddPic();
            File imgFile = new File(lastImagePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                bitmap = checkForRotation(lastImagePath, bitmap);
                if (bitmap == null) {
                    Log.w(TAG, "selected image is null");
                    return;
                }
                imageView.setImageBitmap(bitmap);
                model.clear();
                setOutputText("Model loaded!");
            }
        } else if (requestCode == PICK_IMAGE) {
            if (imageReturnedIntent == null) {
                Log.w(TAG, "returned image intent is null");
                return;
            }
            Uri imageUri = imageReturnedIntent.getData();
            if (imageUri == null) {
                Log.w(TAG, "imageUri is null");
                return;
            }
            lastImagePath = getRealPathFromURIPath(imageUri);

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(Objects.requireNonNull(getActivity()).getContentResolver(), imageUri);
                bitmap = checkForRotation(lastImagePath, bitmap);
                if (bitmap == null) {
                    Log.w(TAG, "selected image is null");
                    return;
                }
                imageView.setImageBitmap(bitmap);
                model.clear();
                setOutputText("Model loaded!");
            } catch (IOException e) {
                Log.w(TAG, "Error occured while selecting the image");
            }
        }
    }

    private String getRealPathFromURIPath(Uri contentURI) {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(getActivity(), contentURI)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(contentURI)) {
                final String docId = DocumentsContract.getDocumentId(contentURI);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(contentURI)) {
                final String id = DocumentsContract.getDocumentId(contentURI);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(Objects.requireNonNull(getActivity()), contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(contentURI)) {
                final String docId = DocumentsContract.getDocumentId(contentURI);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {split[1]};

                return getDataColumn(Objects.requireNonNull(getActivity()), contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(contentURI.getScheme())) {
            return getDataColumn(Objects.requireNonNull(getActivity()), contentURI, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(contentURI.getScheme())) {
            return contentURI.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) { return "com.android.externalstorage.documents".equals(uri.getAuthority()); }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) { return "com.android.providers.downloads.documents".equals(uri.getAuthority()); }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) { return "com.android.providers.media.documents".equals(uri.getAuthority()); }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(lastImagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        Objects.requireNonNull(getActivity()).sendBroadcast(mediaScanIntent);
    }
    public Bitmap checkForRotation(String imagePath, Bitmap bitmap) {
        Bitmap rotatedBitmap;
        try {
            ExifInterface ei = new ExifInterface(imagePath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;
            }
        } catch (IOException e) {
            rotatedBitmap = bitmap;
        }
        return rotatedBitmap;
    }
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
