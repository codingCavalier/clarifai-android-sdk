<img src="https://clarifai.com/cms-assets/20180307033326/logo2.svg" width="512">

# Clarifai Android SDK

Hello and welcome! This is the public repository of the Clarifai SDK for Android devices.

Our vision at Clarifai is to answer every question. With this SDK we can help you to bring the power of A.I. to mobile applications. Check our developer's site at [https://developer.clarifai.com](https://developer.clarifai.com) or contact us at <android-dev@clarifai.com> to learn more.

# Getting started

In order to run the SDK, you would require a Clarifai account. Please <a href="/signup" target="_blank">create one here</a> before proceeding with this guide.

Clarifai Android SDK supports applications running on Android API 21 (Version 5.0 “Lollipop”), or later.

### Git LFS

Before doing anything else, please make sure you have Git-LFS installed on your system. The binary contained in the framework is managed by GitHub using `git-lfs`.

If you don't have it installed yet, you can find details at: [https://git-lfs.github.com](https://git-lfs.github.com). IF YOU DON'T HAVE GIT-LFS INSTALLED, THE SDK WILL **NOT** WORK. 

### Verifying the SDK

After setting up Git LFS, please ensure that the SDK cloned correctly, by checking the size of the *.aar. If the size is less than ~120MB, then you will need to re-pull master now that Git LFS is set up. 

If you downloaded a ZIP of the project via the "Clone or download" button, the SDK **will be cloned incorrectly**. Either clone the repo with git, or download the *.aar file specifically by clicking on the file through GitHub, and clicking the "Download" button.

### Install the SDK with an *.aar
Much of the Android SDK is built with Kotlin. As such, add the following to the **project-level** `build.gradle`:

```gradle
buildscript {
    ext.kotlin_version = "1.3.21"
    ext.kotlin_coroutine_version = '1.1.1'
    ...
    dependencies {
        ...
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```

Next, place the `*.aar` file into the modules's `libs` folder of your application, creating one if necessary. 

In the **app-level** `build.gradle` file, add the following:

```gradle
repositories {
    flatDir {
        dirs 'libs'
    }
}
...
dependencies {
    ...
    implementation (name:'SDK_FILE_NAME', ext:'aar')
    implementation 'android.arch.lifecycle:extensions:1.1.1'
    implementation 'com.android.volley:volley:1.1.0'
    implementation 'com.google.protobuf:protobuf-java:3.5.0'
    implementation 'com.google.protobuf:protobuf-java-util:3.4.0'
    implementation 'com.loopj.android:android-async-http:1.4.9'

    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlin_coroutine_version"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutine_version"
    ...
}
```
replacing `SDK_FILE_NAME`, with the name of the library(without `aar` extension) added to the `/app/libs` folder in the previous step.


### Start the SDK

The Clarifai SDK is initialized by calling `Clarifai.start(applicationContext, apiKey);` within the `com.clarifai.clarifai_android_sdk.core.Clarifai` package. We recommend starting it when your app has finished launching, but that is not absolutely required. Furthermore, work is offloaded to background threads, so there should be little to no impact on the launching of your app.

```java
import com.clarifai.clarifai_android_sdk.core.Clarifai;

public class MainActivity extends AppCompatActivity {
  private final String apiKey = "ENTER YOUR API KEY";
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Clarifai.start(getApplicationContext(), apiKey);
  }
}
```

```kotlin
import com.clarifai.clarifai_android_sdk.core.Clarifai;

class MainActivity : AppCompatActivity() {
  val apiKey = "ENTER YOUR API KEY"
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    Clarifai.start(this, apiKey)
  }
}
```

### Add Device Inputs with SDK

The SDK is built around a simple idea. You give inputs (images) to the library and it returns predictions (concepts). You need to add inputs to make predictions on it.

All inputs are created from a DataAsset object in the Android SDK. A Data Asset is a container for the asset in question, plus metadata related to it. You can create a DataAsset initialized with an Image on Device or from a URL as shown in the example below.

```java
import com.clarifai.clarifai_android_sdk.dataassets.DataAsset;
import com.clarifai.clarifai_android_sdk.dataassets.Image;
import com.clarifai.clarifai_android_sdk.datamodels.Input;
...

// Initialize Image object from an image URL
Image imageFromUrl = new Image(“”);

// Initialize Image object with an image on device with bitmap
Image imageFromBitmap = new Image(bitmap);

// A Data Asset is a container for the asset in question, plus
// metadata related to it
DataAsset dataAsset = new DataAsset(imageFromBitmap);

// An input object contains the data asset, temporal information, and
// is a fundamental component to be used by models to train on or
// predict
Input input = new Input(dataAsset);
```

```kotlin
import com.clarifai.clarifai_android_sdk.dataassets.DataAsset
import com.clarifai.clarifai_android_sdk.dataassets.Image
import com.clarifai.clarifai_android_sdk.datamodels.Input

// Initialize Image object from an image URL
val imageFromUrl = Image(“”)

// Initialize Image object with an image on device with bitmap
val imageFromBitmap = Image(bitmap)

// A Data Asset is a container for the asset in question, plus
// metadata related to it
val dataAsset = DataAsset(imageFromBitmap)

// An input object contains the data asset, temporal information, and
// is a fundamental component to be used by models to train on or
// predict
val input = Input(dataAsset)
```

### Load Models On Device

Clarifai has a variety of Pre-Built Models to predict against. However, only the `General Model` is readily available within the SDK. For access to other models please contact sales@clarifai.com. The sample code below shows how to load the `General Model` and make a prediction against it.  

```java
import com.clarifai.clarifai_android_sdk.datamodels.Model;
...

//Get General Model
Model model = Clarifai.getInstance().getGeneralModel();
```

```kotlin
import com.clarifai.clarifai_android_sdk.datamodels.Model
...

//Get General Model
val model = Clarifai.getInstance().generalModel
```

### Predict On Device
Just as with our API, you can use the predict functionality on device with any of our available Pre-Built Models. Predictions generate outputs. An output has a similar structure to an input. It contains a data asset and concepts. The concepts associated with an output contain the predictions and their respective score (degree of confidence).


Note that the prediction results from pre-built models on the SDK may differ from those on the API. Specifically, there may be a loss of accuracy up to 5% due to the conversion of the models that allow them to be compact enough to be used on lightweight devices. This loss is expected within the current industry standards. 



```java
import com.clarifai.clarifai_android_sdk.datamodels.Input;
import com.clarifai.clarifai_android_sdk.datamodels.Model;
import com.clarifai.clarifai_android_sdk.utils.Error;
...

// See how to create an input from the examples above
Input input = new Input(dataAsset);

// Use the model you want to predict on. The model in the sample code
// below is our General Model.
final Model model = Clarifai.getInstance().getGeneralModel();
model.addInput(input);
model.predict(new Model.ModelCallbacks() {
  @Override
  public void PredictionComplete(boolean successful, Error error) {
    if (successful) {
      List<Output> outputs = model.getOutputs();
      for (Output output: outputs) {
        List<Concept> concepts = output.getDataAsset().getConcepts();
        // concepts now contains a list of each concept found by the prediction
      }
    } else {
      Log.e(TAG, error.getErrorMessage());
    }
  }
});
```

```kotlin
import com.clarifai.clarifai_android_sdk.datamodels.Input
import com.clarifai.clarifai_android_sdk.datamodels.Model
import com.clarifai.clarifai_android_sdk.utils.Error
...

// See how to create an input from the examples above
val input = Input(dataAsset)

// Use the model you want to predict on. The model in the sample code
// below is our General Model.
val model = Clarifai.getInstance().generalModel
model.addInput(input)
model.predict(object: Model.ModelCallbacks() {
  override fun PredictionComplete(successful: Boolean, error: Error?) {
    if (successful) {
      val outputs = model.outputs
      outputs.forEach {
        val concepts = it.dataAsset.concepts
        // concepts now contains a list of each concept found by the prediction
      }
    } else {
      Log.e(TAG, error?.errorMessage);
    }
  }
});
```


## Learn and do more

Check out our [documentation site](https://developer.clarifai.com/docs/) to learn a lot more about how to bring A.I. to your app.


## Support

Questions? Have an issue? Send us a message at <android-dev@clarifai.com>.


## License

The Clarifai-Android-SDK is available under a commercial license. See the [LICENSE](https://github.com/Clarifai/clarifai-android-sdk/blob/master/LICENSE) file for more information.
