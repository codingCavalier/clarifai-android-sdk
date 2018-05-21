<img src="https://clarifai.com/static/images/logo.png" width="256">

# Clarifai Android SDK Examples

Hello and welcome! Here you will find Android examples for the Clarifai-Android-SDK.

# Getting Started

If you don't have a developer account yet, sign up for one at our [Developer Site](https://developer.clarifai.com/) (It's free!)

The Clarifai-Android-SDK examples require that you have an active API key. In your developer dashboard you will find a section called API Keys. Here you can generate a new api key. Make sure it has **all scopes**.

## API Key

Enter your API Key in the MainActivity:

```java
private final String apiKey = "ENTER YOUR API KEY";
```

# Neutron

Neutron can help you understand how to use Clarifai's AI in your app by showing how to use prediction and custom training features.

## Prediction

Once the main prediction view has been loaded, you will see a sample image used for predictions. This image is a pre-selected on-device image.

> In PredictFragment.java, there is an example of how prediction can occur from on-device images.

When you click predict, you will be presented with a list of the concepts that the AI predicts to be in your image, along with corresponding confidence scores. This list is sorted in descending order, with the highest confidence (ie. most likely) concepts at the top.

### Important Methods

The prediction happens in the **doPrediction** method of [PredictFragment.java](https://github.com/Clarifai/clarifai-android-sdk/blob/Example/Neutron/app/src/main/java/com/clarifai/android/neutron/PredictFragment.java). This method demonstrates how to create a Clarifai Image, Data Asset, and Input, and then perform a prediction on a Model.

*TODO: add stub showing the predict method once implemented*

## Custom Model Prediction

Coming soon!
