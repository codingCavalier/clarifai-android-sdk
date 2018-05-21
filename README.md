<img src="https://clarifai.com/cms-assets/20180307033326/logo2.svg" width="512">

# Clarifai Android SDK

Hello and welcome! This is the public repository of the Clarifai SDK for Android devices.

Our vision at Clarifai is to answer every question. With this SDK we can help you to bring the power of A.I. to mobile applications. Check our developer's site at [https://developer.clarifai.com](https://developer.clarifai.com) or contact us at <mobile-feedback@clarifai.com> to learn more.

# Getting started

Sign up for a free developer account at: https://developer.clarifai.com/signup/

The Clarifai-Android-SDK is available via *TODO: fill in how it's available and can be installed*.

>### Git LFS
>
>Before we proceed with the installation, please make sure you have Git-LFS installed on your system. The binary contained in the framework is managed by GitHub using `git-lfs`.
>
>If you don't have it installed yet, you can find details at: [https://git-lfs.github.com](https://git-lfs.github.com)

*TODO: Installation instructions*

## Start the SDK

The Clarifai SDK is initialized by calling  `Clarifai.start(applicationContext, apiKey);` within the `com.clarifai.clarifai_android_sdk.core.Clarifai` package. We recommend starting it when your app has finished launching, but that is not absolutely required. Furthermore, work is offloaded to background threads, so there should be little to no impact on the launching of your app.

```
import com.clarifai.clarifai_android_sdk.core.Clarifai;

public class MainActivity extends AppCompatActivity {
  private final String apiKey = "ENTER YOUR API KEY"
  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      Clarifai.start(getApplicationContext(), apiKey);
  }
}
```

## General model availability notifications

Clarifai's *general model* is currently bundled with the SDK; in the future this will change and it will be made available on demand instead. When this change occurs, this document will be updated accordingly.


## Learn and do more

Check out our [documentation site](https://developer.clarifai.com/docs/) to learn a lot more about how to bring A.I. to your app.


## Support

Questions? Have an issue? Send us a message at <mobile-feedback@clarifai.com>.


## License

The Clarifai-Android-SDK is available under a commercial license. See the [LICENSE](https://github.com/Clarifai/clarifai-android-sdk/blob/master/LICENSE) file for more information.
