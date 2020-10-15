# VCam
A simple, light-weight computer vision class designed for use in OnBot java in the First Tech Challenge.
## Setup
Simply upload the VCam.java file onto OnBot Java (or paste it into the teamcode directory if you are working in Android Studios).
## Usage
Create a VCam object:
```java
VCam vcam = new VCam("webcam name", hardwareMap);
```
or if you want to use debugging:
```java
VCam vcam = new VCam("webcam name", hardwareMap, telemetry);
```
To start streaming images, start the capture session:
```java
vcam.startCapture();
```
To stop streaming, stop the capture (do this before the opmode finishes):
```java
vcam.stopCapture();
```
## Processing Images
Once the camera has begun streaming, you can start processing images. To get the most recent image the camera has captured, use:
```java
VCam.VColor[][] image = vcam.getLatestImage();
```
Because OnBot java does not have access to the java.awt.Color class, VCam uses an embedded color class named VCam.VColor that acts in a similar way.

VCam also has an embedded class called VCam.CVUtils that can be use to perform basic computer vision functions.
To see how similar a pixel is to a specific color, use:
```java
double similarity = VCam.CVUtils.getSimilarity(image[y][x], VCam.VColor.ORANGE);
```
You can use a Gaussian Blur to blur a pixel using the preset kernels:
```java
VCam.VColor blurredPixel = VCam.VCUtils.getAverage(image, x, y, VCam.CVUtils.KERNEL_7x7);
```
