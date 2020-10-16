package org.firstinspires.ftc.teamcode;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ThreadPool;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.android.util.Size;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSequenceId;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraFrame;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.util.concurrent.TimeUnit;

/**
 * VCam.java is a simple light-weight all-in-one computer vision class designed to work on OnBot java.
 * VCam is completely open source and created by Team 14470. To learn how to use VCam, please see the
 * github page: https://github.com/Volt40/VCam
 *
 * @author Michael Baljet, FTC Team 14470
 * @version 1.0.0
 * @see VColor
 * @see CVUtils
 *
 */
public class VCam {

    // Seconds until the camera times out.
    private static final int TIMEOUT_SECONDS = 9999; // ;)

    // Used for debugging.
    private Telemetry telemetry;
    private boolean debuggingEnabled;

    // The camera.
    private Camera camera;
    private CameraCharacteristics cameraInfo;

    // The camera capture session.
    private CameraCaptureSession session;

    // The last image the camera took.
    private VColor[][] latestImage;

    /**
     * Creates a VCam object.
     * @param name Name of the Webcam.
     * @param hwMap Hardware map.
     * @param telemetry Telemetry, used for debugging.
     */
    public VCam(String name, HardwareMap hwMap, Telemetry telemetry) {
        this.telemetry = telemetry;
        debuggingEnabled = true;
        init(name, hwMap);
    }

    /**
     * Creates a VCam object.
     * @param name Name of the Webcam.
     * @param hwMap Hardware map.
     */
    public VCam(String name, HardwareMap hwMap) {
        debuggingEnabled = false;
        init(name, hwMap);
    }

    /**
     * Inits the camera.
     * @param name Name of the webcam.
     * @param hwMap Hardware map.
     */
    private void init(String name, HardwareMap hwMap) {
        latestImage = null;
        try {
            WebcamName webcamName = hwMap.get(WebcamName.class, name);
            Deadline deadline = new Deadline(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            camera = ClassFactory.getInstance().getCameraManager().requestPermissionAndOpenCamera(deadline, webcamName, Continuation.create(ThreadPool.getDefault(), new CameraStateCallbackStub()));
            cameraInfo = webcamName.getCameraCharacteristics();
            session = camera.createCaptureSession(Continuation.create(ThreadPool.getDefault(), new StateCallbackStub()));
        } catch (Exception e) {
            if (debuggingEnabled)
                telemetry.addLine("Error with webcam.");
        }
    }

    /**
     * Returns true if an image is available.
     * @return true if an image is available.
     */
    public boolean imageAvailable() {
        return latestImage != null;
    }

    /**
     * Returns the camera's last image.
     * @return the camera's last image, or null if the camera has yet to take an image.
     */
    public VColor[][] getLatestImage() {
        return latestImage;
    }

    /**
     * Begins streaming from the camera.
     */
    public void startCapture() {
        try {
            Size size = cameraInfo.getDefaultSize(ImageFormat.YUY2);
            int framesPerSecond = cameraInfo.getMaxFramesPerSecond(ImageFormat.YUY2, size);
            CameraCaptureRequest request = camera.createCaptureRequest(ImageFormat.YUY2, size, framesPerSecond);
            session.startCapture(request, new Session(), Continuation.create(ThreadPool.getDefault(), new StatusCallbackStub()));
            if (debuggingEnabled)
                telemetry.addLine("Capture Session begun.");
        } catch (Exception e) {
            if (debuggingEnabled)
                telemetry.addLine("Unable to create capture request.");
        }
    }

    /**
     * Stops streaming from the camera.
     */
    public void stopCapture() {
        session.stopCapture();
        if (debuggingEnabled)
            telemetry.addLine("Capture session stopped.");
    }

    /**
     * Static methods used for simple computer vision.
     */
    public static final class CVUtils {

        // Default color value if point is out of range.
        private static final double DEFAULT_VALUE = 128;

        // 1x1 kernel.
        public static final double[][] KERNEL_1x1 = {
                {1}
        };

        // 3x3 kernel.
        public static final double[][] KERNEL_3x3 = {
                { 1./16, 2./16, 1./16},
                { 2./16, 4./16, 2./16},
                { 1./16, 2./16, 1./16}
        };

        // 5x5 kernel.
        public static final double[][] KERNEL_5x5 = {
                { 1./273, 4./273, 7./273, 4./273, 1./273},
                { 4./273,16./273,26./273,16./273, 4./273},
                { 7./273,26./273,41./273,26./273, 7./273},
                { 4./273,16./273,26./273,16./273, 4./273},
                { 1./273, 4./273, 7./273, 4./273, 1./273}
        };

        // 7x7 kernel.
        public static final double[][] KERNEL_7x7 = {
                {  1./4096,  6./4096, 15./4096, 20./4096, 15./4096,  6./4096,  1./4096},
                {  6./4096, 36./4096, 90./4096,120./4096, 90./4096, 36./4096,  6./4096},
                { 15./4096, 90./4096,225./4096,300./4096,225./4096, 90./4096, 15./4096},
                { 20./4096,120./4096,300./4096,400./4096,300./4096,120./4096, 20./4096},
                { 15./4096, 90./4096,225./4096,300./4096,225./4096, 90./4096, 15./4096},
                {  6./4096, 36./4096, 90./4096,120./4096, 90./4096, 36./4096,  6./4096},
                {  1./4096,  6./4096, 15./4096, 20./4096, 15./4096,  6./4096,  1./4096},
        };

        /**
         * Returns euclidean distance between two colors.
         * @param c1 Color 1.
         * @param c2 Color 2.
         * @return Euclidean distance between c1 and c2.
         * @see VColor
         */
        public static double getSimilarity(VColor c1, VColor c2) {
            double r1 = c1.getRed();
            double g1 = c1.getGreen();
            double b1 = c1.getBlue();
            double r2 = c2.getRed();
            double g2 = c2.getGreen();
            double b2 = c2.getBlue();
            double temp0 = r2 - r1;
            double temp1 = g2 - g1;
            double temp2 = b2 - b1;
            temp0 = Math.pow(temp0, 2);
            temp1 = Math.pow(temp1, 2);
            temp2 = Math.pow(temp2, 2);
            double temp3 = temp0 + temp1 + temp2;
            double similarity = Math.sqrt(temp3);
            return similarity;
        }

        /**
         * Returns the average color of selected area (x, y). The average is calculated
         * with Gaussian kernels. The kernels types are 1x1, 3x3, 5x5 and 7x7.
         * @param image A Color[][] with every pixel (x, y) represented as a Color object
         * located at image[x][y].
         * @param x X value for the kernel to be centered.
         * @param y Y value for the kernel to be centered.
         * @param kernel Kernel used for the calculation, Use this class's static fields, or make your own.
         * @return The average of the selected area.
         * @see VColor
         */
        public static VColor getAverage(VColor[][] image, int x, int y, double[][] kernel) {
            // Set counters for red, green, and blue.
            double red = 0;
            double green = 0;
            double blue = 0;
            // Define a variable for half the kernel length.
            int halfKernel = kernel.length / 2;
            // Loop through image.
            for (int i = x - halfKernel, ki = 0; i < x + halfKernel && ki < kernel.length; i++, ki++) {
                for (int j = y - halfKernel, kj = 0; j < y + halfKernel && kj < kernel.length; j++, kj++) {
                    // For each pixel:
                    try {
                        /* If inside image, add the color values scaled by their corresponding
                         * kernel scalars to the counters.
                         */
                        red += kernel[ki][kj] * image[i][j].getRed();
                        green += kernel[ki][kj] * image[i][j].getGreen();
                        blue += kernel[ki][kj] * image[i][j].getBlue();
                    } catch(Exception e) {
                        /* If outside of image, add the default color value scaled by the corresponding
                         * kernel scalars to the counters.
                         */
                        red += DEFAULT_VALUE * kernel[ki][kj];
                        green += DEFAULT_VALUE * kernel[ki][kj];
                        blue += DEFAULT_VALUE * kernel[ki][kj];
                    }
                }
            }
            // End loops.
            // Add 0.5 to each color value for proper rounding.
            red += 0.5;
            green += 0.5;
            blue += 0.5;
            // Convert each to an int.
            int redAverage = (int) red;
            int greenAverage = (int) green;
            int blueAverage = (int) blue;
            // Get RGB value.
            int rgb = (redAverage << 16 | greenAverage << 8 | blueAverage);
            // Create the new color.
            VColor average = new VColor(rgb);
            // Return the average color.
            return average;
        }

    }

    /**
     * Represents a color. Needed because OnBot java cannot use java.awt.Color
     * @see CVUtils
     */
    public static class VColor {

        // Static colors.
        public static final VColor RED = new VColor(255, 0, 0);
        public static final VColor GREEN = new VColor(0, 255, 0);
        public static final VColor BLUE = new VColor(0, 0, 255);
        public static final VColor YELLOW = new VColor(255, 255, 0);
        public static final VColor ORANGE = new VColor(255, 128, 0);
        public static final VColor PURPLE = new VColor(128, 0, 255);
        public static final VColor PINK = new VColor(255, 0, 255);

        // RGB value of this color.
        private int rgb;

        /**
         * Creates a VColor with the given RGB value.
         * @param rgb RGB value of this color.
         */
        public VColor(int rgb) {
            this.rgb = rgb;
        }

        /**
         * Creates a VColor with the given red, green and blue value.
         * @param red Red value.
         * @param blue Blue value.
         * @param green Green value.
         */
        public VColor(int red, int green, int blue) {
            rgb = (red << 16 | green << 8 | blue);
        }

        /**
         * Returns the RGB value of this color.
         * @return the RGB value of this color.
         */
        public int getRGB() {
            return rgb;
        }

        /**
         * Returns the red value of this color.
         * @return the red value of this color.
         */
        public int getRed() {
            return (rgb >> 16) & 0x000000FF;
        }

        /**
         * Returns the green value of this color.
         * @return the green value of this color.
         */
        public int getGreen() {
            return (rgb >> 8) & 0x000000FF;
        }

        /**
         * Returns the blue value of this color.
         * @return the blue value of this color.
         */
        public int getBlue() {
            return (rgb) & 0x000000FF;
        }

    }

    /**
     * Handles new frames.
     */
    private class Session implements CameraCaptureSession.CaptureCallback {

        @Override
        public void onNewFrame(CameraCaptureSession session, CameraCaptureRequest request, CameraFrame cameraFrame) {
            Bitmap bitmap = Bitmap.createBitmap(cameraFrame.getSize().getWidth(), cameraFrame.getSize().getHeight(), Bitmap.Config.ARGB_8888);
            cameraFrame.copyToBitmap(bitmap);
            VColor[][] colorMap = new VColor[bitmap.getHeight()][bitmap.getWidth()];
            for (int i = 0; i < colorMap.length; i++)
                for (int j = 0; j < colorMap[i].length; j++)
                    colorMap[i][j] = new VColor(-bitmap.getPixel(j, i));
            latestImage = colorMap;
        }

    }

    /**
     * Stub class needed for setup.
     */
    private class CameraStateCallbackStub implements Camera.StateCallback {

        @Override
        public void onOpened(Camera camera) {
            if (debuggingEnabled)
                telemetry.addLine("Camera opened.");
        }

        @Override
        public void onOpenFailed(CameraName cameraName, Camera.OpenFailure reason) {
            if (debuggingEnabled)
                telemetry.addLine("Camera failed to open.");
        }

        @Override
        public void onClosed(Camera camera) {
            if (debuggingEnabled)
                telemetry.addLine("Camera closed.");
        }

        @Override
        public void onError(Camera camera, Camera.Error error) {
            if (debuggingEnabled)
                telemetry.addLine("Camera error: " + error);
        }
    }

    /**
     * Stub class needed for setup.
     */
    private class StateCallbackStub implements CameraCaptureSession.StateCallback {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            // Stub, unused.
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            // Stub, unused.
        }
    }

    /**
     * Stub class needed for setup.
     */
    private class StatusCallbackStub implements CameraCaptureSession.StatusCallback {

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, CameraCaptureSequenceId cameraCaptureSequenceId, long lastFrameNumber) {
            // Stub, unused.
        }

    }

}
