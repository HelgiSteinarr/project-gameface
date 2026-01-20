/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * MODIFICATION NOTICE
 * as per the licence its required to give notice that this code has been modified by a third party.
 * 2026 - Helgi Steinarr Juliusson, changes can be found in version control.
 */

package com.google.projectgameface;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

/** The helper of camera feed. */
class FaceLandmarkerHelper extends HandlerThread {

    public static final String TAG = "FaceLandmarkerHelper";

    // number of allowed multiple detection works at the sametime.
    private static final int N_WORKS_LIMIT = 1;

    // Indicates if have new face landmarks detected.

    // Internal resolution for MediaPipe
    // this highly effect the performance.
    private static final float MP_WIDTH = 213.0f;
    private static final float MP_HEIGHT = 160.0f;

    private static final int TOTAL_BLENDSHAPES = 52;
    private static final int FOREHEAD_INDEX = 8;
    private static final int NOSE_TIP_INDEX = 1;
    private static final int NOSE_CENTER_INDEX = 6;

    // Landmarks for face normal calculation
    private static final int FOREHEAD_TOP_INDEX = 10;
    private static final int CHIN_INDEX = 152;
    private static final int LEFT_CHEEK_INDEX = 234;
    private static final int RIGHT_CHEEK_INDEX = 454;

    // Eye landmarks (iris centers) - used for validation
    private static final int LEFT_EYE_CENTER_INDEX = 468;
    private static final int RIGHT_EYE_CENTER_INDEX = 473;

    // Mouth landmark for validation
    private static final int MOUTH_CENTER_INDEX = 13;

    // Landmark validation thresholds (in normalized coordinates 0-1)
    // Max Y difference between eyes (they should be at similar height)
    private static final float MAX_EYE_Y_DIFF = 0.15f;
    // Min/max face aspect ratio (width / height)
    private static final float MIN_FACE_ASPECT_RATIO = 0.5f;
    private static final float MAX_FACE_ASPECT_RATIO = 1.5f;

    public volatile boolean isRunning = false;

    // Configs for FaceLandmarks model.
    private static final float MIN_FACE_DETECTION_CONFIDENCE = 0.7f;
    private static final float MIN_FACE_TRACKING_CONFIDENCE = 0.7f;
    private static final float MIN_FACE_PRESENCE_CONFIDENCE = 0.7f;
    private static final int MAX_NUM_FACES = 1;
    private static final RunningMode RUNNING_MODE = RunningMode.LIVE_STREAM;

    private Context context;

    private FaceLandmarker faceLandmarker = null;

    public int frameWidth = 0;
    public int frameHeight = 0;

    float currHeadX = 0.f;
    float currHeadY = 0.f;
    float currNoseTipX = 0.f;
    float currNoseTipY = 0.f;
    float currNoseBridgeX = 0.f;
    float currNoseBridgeY = 0.f;

    // Face normal vector (points outward from face)
    float faceNormalX = 0.f;
    float faceNormalY = 0.f;
    float faceNormalZ = 0.f;
    boolean isLookingAtCamera = false;

    // Tracks which validation check failed (0 = passed, 1-5 = specific check that failed)
    int failedValidationCheck = 0;

    // Configurable gaze thresholds (degrees)
    private float yawThresholdDegrees = 30.f;
    private float pitchThresholdDegrees = 60.f;
    public long mediapipeTimeMs = 0;
    public long preprocessTimeMs = 0;


    // tracking how many works in process.
    private int currentInWorks = 0;

    private Handler handler;
    public int mpInputWidth;
    public int mpInputHeight;
    private float[] currBlendshapes;

    /** How many milliseconds passed after previous image. */
    public long gapTimeMs = 1;

    public long prevCallbackTimeMs = 0;

    public long timeSinceLastMeasurement = 0;
    private long lastMeasurementTsMs;

    FaceLandmarker.FaceLandmarkerOptions options;
    public boolean isFaceVisible;
    public int frontCameraOrientation = 270;



    // Frame rotation state for MediaPipe graph.
    private int currentRotationState = Surface.ROTATION_0;

    public FaceLandmarkerHelper() {
        super(TAG);
    }


    public void setFrontCameraOrientation(int orientation) {
        frontCameraOrientation = orientation;
    }



    /**
     * Sets internal frame rotation state for the MediaPipe graph.
     *
     * @param rotationValue Current rotation of the device screen, the value should be {@link
     *     Surface#ROTATION_0}, {@link Surface#ROTATION_90}, {@link Surface#ROTATION_180} or {@link
     *     Surface#ROTATION_180}.
     */
    public void setRotation(int rotationValue) {
        currentRotationState = rotationValue;
        Log.i(TAG, "setRotation: " + rotationValue);
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        handler =
            new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    // Function for handle message from main thread.
                    detectLiveStream((ImageProxy) msg.obj);

                }
            };
    }

    public Handler getHandler() {
        return handler;
    }

    /**
     * Create and configure the {@link FaceLandmarker}.
     *
     * @param context context for assets file loading.
     */
    public void init(Context context) {

        Log.i(TAG, "init : " + Thread.currentThread());
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
        isRunning = true;

        currBlendshapes = new float[TOTAL_BLENDSHAPES];

        this.context = context;

        // Set general FaceLandmarker options.
        Log.i(TAG, "Init MediaPipe");
        BaseOptions.Builder baseOptionBuilder = BaseOptions.builder();
        baseOptionBuilder.setDelegate(Delegate.GPU);
        baseOptionBuilder.setModelAssetPath("face_landmarker.task");

        try {
            BaseOptions baseOptions = baseOptionBuilder.build();
            // Create an option builder with base options and specific
            // options only use for Face Landmarker.
            FaceLandmarker.FaceLandmarkerOptions.Builder optionsBuilder =
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinFaceDetectionConfidence(MIN_FACE_DETECTION_CONFIDENCE)
                    .setMinTrackingConfidence(MIN_FACE_TRACKING_CONFIDENCE)
                    .setMinFacePresenceConfidence(MIN_FACE_PRESENCE_CONFIDENCE)
                    .setNumFaces(MAX_NUM_FACES)
                    .setOutputFaceBlendshapes(true)
                    .setOutputFacialTransformationMatrixes(false)
                    .setRunningMode(RUNNING_MODE);

            optionsBuilder.setResultListener(this::postProcessLandmarks);

            options = optionsBuilder.build();
            faceLandmarker = FaceLandmarker.createFromOptions(this.context, options);

        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPipe failed to load the task with error: " + e.getMessage());
        } catch (RuntimeException e) {
            Log.e(TAG, "Face Landmarker failed to load model with error: " + e.getMessage());
        }
    }


    /**
     * Converts the ImageProxy to MP Image and feed it to Mediapipe Graph.
     * @param imageProxy An image proxy from camera feed
     */
    public void detectLiveStream(ImageProxy imageProxy) {

        // Reject new work if exceed limit.
        if (currentInWorks >= N_WORKS_LIMIT) {
            imageProxy.close();
            return;
        }

        // Reject new work if not ready.
        if (!isRunning || (faceLandmarker == null) || (imageProxy == null)) {
            return;
        }

        currentInWorks += 1;
        long startPreprocessTimeMs = SystemClock.uptimeMillis();

        frameWidth = imageProxy.getWidth();
        frameHeight = imageProxy.getHeight();


        Bitmap bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);

        bitmap.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

        // Handle rotations.
        Matrix rotationMatrix = getRotationMatrix(imageProxy);

        Bitmap rotatedBitmap =
            Bitmap.createBitmap(
                bitmap, 0, 0, imageProxy.getWidth(), imageProxy.getHeight(), rotationMatrix, true);

        // Convert the input Bitmap object to an MPImage object to run inference.
        MPImage mpImage = new BitmapImageBuilder(rotatedBitmap).build();

        try {
            faceLandmarker.detectAsync(mpImage, SystemClock.uptimeMillis());
        } catch (RuntimeException e) {
            Log.e(TAG, "Face Landmarker failed to detect async: " + e.getMessage());
        }

        imageProxy.close();

        // True input resolution for post.
        mpInputWidth = mpImage.getWidth();
        mpInputHeight = mpImage.getHeight();

        preprocessTimeMs = SystemClock.uptimeMillis() - startPreprocessTimeMs;

    }

    @NonNull
    private Matrix getRotationMatrix(ImageProxy imageProxy) {
        Matrix matrix = new Matrix();

        // Front camera rotation constant is 270 degrees.
        int matrixRotDegrees = frontCameraOrientation;
        int widthCorrected = imageProxy.getWidth();
        int heightCorrected = imageProxy.getHeight();
        float mpWidthCorrected = MP_WIDTH;
        float mpHeightCorrected = MP_HEIGHT;
        switch (currentRotationState) {
            case Surface.ROTATION_0:
                break;
            case Surface.ROTATION_90:
                matrixRotDegrees = frontCameraOrientation + 90;
                widthCorrected = imageProxy.getHeight();
                heightCorrected = imageProxy.getWidth();
                mpWidthCorrected = MP_HEIGHT;
                mpHeightCorrected = MP_WIDTH;
                break;
            case Surface.ROTATION_180:
                matrixRotDegrees = frontCameraOrientation + 180;
                widthCorrected = imageProxy.getWidth();
                heightCorrected = imageProxy.getHeight();
                mpWidthCorrected = MP_HEIGHT;
                mpHeightCorrected = MP_WIDTH;
                break;
            case Surface.ROTATION_270:
                matrixRotDegrees = frontCameraOrientation - 90;
                widthCorrected = imageProxy.getHeight();
                heightCorrected = imageProxy.getWidth();
                mpWidthCorrected = MP_HEIGHT;
                mpHeightCorrected = MP_WIDTH;
                break;
            default: // fall out
        }
        matrix.postRotate(matrixRotDegrees);
        matrix.postScale(-mpWidthCorrected / widthCorrected, mpHeightCorrected / heightCorrected);
        return matrix;
    }

    /**
     * Gets result landmarks and blendshapes then apply some scaling and save the value.
     *
     * @param result The result of face landmarker.
     * @param input The input image of face landmarker.
     */
    private void postProcessLandmarks(FaceLandmarkerResult result, MPImage input) {
        currentInWorks -= 1;
        mediapipeTimeMs = SystemClock.uptimeMillis() - result.timestampMs();
        input.close();

        if (!isRunning) {
            ensurePauseThread();
        }

        if (!result.faceLandmarks().isEmpty()) {
            currHeadX = result.faceLandmarks().get(0).get(FOREHEAD_INDEX).x() * mpInputWidth;
            currHeadY = result.faceLandmarks().get(0).get(FOREHEAD_INDEX).y() * mpInputHeight;
            currNoseTipX = result.faceLandmarks().get(0).get(NOSE_TIP_INDEX).x() * mpInputWidth;
            currNoseTipY = result.faceLandmarks().get(0).get(NOSE_TIP_INDEX).y() * mpInputHeight;
            currNoseBridgeX = result.faceLandmarks().get(0).get(NOSE_CENTER_INDEX).x() * mpInputWidth;
            currNoseBridgeY = result.faceLandmarks().get(0).get(NOSE_CENTER_INDEX).y() * mpInputHeight;

            // Get 3D coordinates for face normal calculation
            // Using forehead, chin, left cheek, right cheek to define face plane
            float foreheadX = result.faceLandmarks().get(0).get(FOREHEAD_TOP_INDEX).x();
            float foreheadY = result.faceLandmarks().get(0).get(FOREHEAD_TOP_INDEX).y();
            float foreheadZ = result.faceLandmarks().get(0).get(FOREHEAD_TOP_INDEX).z();

            float chinX = result.faceLandmarks().get(0).get(CHIN_INDEX).x();
            float chinY = result.faceLandmarks().get(0).get(CHIN_INDEX).y();
            float chinZ = result.faceLandmarks().get(0).get(CHIN_INDEX).z();

            float leftCheekX = result.faceLandmarks().get(0).get(LEFT_CHEEK_INDEX).x();
            float leftCheekY = result.faceLandmarks().get(0).get(LEFT_CHEEK_INDEX).y();
            float leftCheekZ = result.faceLandmarks().get(0).get(LEFT_CHEEK_INDEX).z();

            float rightCheekX = result.faceLandmarks().get(0).get(RIGHT_CHEEK_INDEX).x();
            float rightCheekY = result.faceLandmarks().get(0).get(RIGHT_CHEEK_INDEX).y();
            float rightCheekZ = result.faceLandmarks().get(0).get(RIGHT_CHEEK_INDEX).z();

            // Get eye and mouth positions for validation
            float leftEyeX = result.faceLandmarks().get(0).get(LEFT_EYE_CENTER_INDEX).x();
            float leftEyeY = result.faceLandmarks().get(0).get(LEFT_EYE_CENTER_INDEX).y();
            float rightEyeX = result.faceLandmarks().get(0).get(RIGHT_EYE_CENTER_INDEX).x();
            float rightEyeY = result.faceLandmarks().get(0).get(RIGHT_EYE_CENTER_INDEX).y();
            float noseX = result.faceLandmarks().get(0).get(NOSE_TIP_INDEX).x();
            float noseY = result.faceLandmarks().get(0).get(NOSE_TIP_INDEX).y();
            float mouthY = result.faceLandmarks().get(0).get(MOUTH_CENTER_INDEX).y();

            // Validate landmark positions to filter false detections
            // Returns 0 if passed, 1-5 indicates which check failed
            failedValidationCheck = validateLandmarks(
                leftEyeX, leftEyeY, rightEyeX, rightEyeY,
                noseX, noseY, mouthY, foreheadY, chinY,
                leftCheekX, rightCheekX);

            isFaceVisible = (failedValidationCheck == 0);

            // Vector A: from chin to forehead (vertical axis of face, pointing up)
            float ax = foreheadX - chinX;
            float ay = foreheadY - chinY;
            float az = foreheadZ - chinZ;

            // Vector B: from left cheek to right cheek (horizontal axis, pointing right)
            float bx = rightCheekX - leftCheekX;
            float by = rightCheekY - leftCheekY;
            float bz = rightCheekZ - leftCheekZ;

            // Cross product A × B = face normal (points outward from face)
            // Right-hand rule: up × right = forward (out of face)
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;

            // Normalize the face normal
            float magnitude = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (magnitude > 0) {
                faceNormalX = nx / magnitude;
                faceNormalY = ny / magnitude;
                faceNormalZ = nz / magnitude;
            }

            // Check if looking at camera: face normal should point toward camera (negative Z in MediaPipe)

            // Left-Right
            float yawLen = (float) Math.sqrt(faceNormalX * faceNormalX + faceNormalZ * faceNormalZ);
            float yawDot = (faceNormalZ) / yawLen; // dot with camera forward (0,0,-1)
            yawDot = Math.max(-1f, Math.min(1f, yawDot));
            float yawAngle = (float) Math.toDegrees(Math.acos(yawDot));

            // Up-Down
            float pitchLen = (float) Math.sqrt(faceNormalY * faceNormalY + faceNormalZ * faceNormalZ);
            float pitchDot = (faceNormalZ) / pitchLen;
            pitchDot = Math.max(-1f, Math.min(1f, pitchDot));
            float pitchAngle = (float) Math.toDegrees(Math.acos(pitchDot));

            boolean lookingYaw   = yawAngle   < yawThresholdDegrees;
            boolean lookingPitch = pitchAngle < pitchThresholdDegrees;
            isLookingAtCamera = lookingYaw && lookingPitch;

            if (result.faceBlendshapes().isPresent()) {
                // Convert from Category to simple float array.
                for (int i = 0; i < TOTAL_BLENDSHAPES; i++) {
                    currBlendshapes[i] = result.faceBlendshapes().get().get(0).get(i).score();
                }
            }

            timeSinceLastMeasurement = SystemClock.uptimeMillis() - lastMeasurementTsMs;
            lastMeasurementTsMs = SystemClock.uptimeMillis();
        } else {
            isFaceVisible = false;
            failedValidationCheck = 0; // No face detected, so validation wasn't the issue
        }

        long ts = SystemClock.uptimeMillis();
        gapTimeMs = ts - prevCallbackTimeMs;
        prevCallbackTimeMs = ts;
    }

    /** Get user's head X, Y coordinate in image space. */
    public float[] getHeadCoordXY() {
        return new float[] {currHeadX, currHeadY};
    }
    public float[] getNoseTipCoordXY() { return new float[] {currNoseTipX, currNoseTipY}; }

    public float[] getNoseBridgeCoordXY() { return new float[] {currNoseBridgeX, currNoseBridgeY}; }

    public float[] getFaceNormal() { return new float[] {faceNormalX, faceNormalY, faceNormalZ}; }

    public boolean isLookingAtCamera() { return isLookingAtCamera; }

    /** Returns which validation check failed (0 = passed, 1-5 = specific check). */
    public int getFailedValidationCheck() { return failedValidationCheck; }

    /** Set the yaw threshold in degrees for gaze detection. */
    public void setYawThreshold(float degrees) {
        this.yawThresholdDegrees = degrees;
    }

    /** Set the pitch threshold in degrees for gaze detection. */
    public void setPitchThreshold(float degrees) {
        this.pitchThresholdDegrees = degrees;
    }

    public float[] getBlendshapes() {

        return currBlendshapes;
    }

    /** Recreates {@link FaceLandmarker} and resume the process. */
    public void resumeThread() {
        faceLandmarker = FaceLandmarker.createFromOptions(this.context, options);
        isRunning = true;
    }


    /**
     * Completely pause the detection process.
     */
    public void pauseThread() {
        Log.i(TAG, "pauseThread");

        // There might be some image processing.
        isRunning = false;
        if (currentInWorks < 0) {
            ensurePauseThread();
        }
    }

    private void ensurePauseThread() {
        if (faceLandmarker != null) {
            faceLandmarker.close();
            faceLandmarker = null;
        }
    }

    /**
     * Validates that detected landmarks are in physically plausible positions for a real face.
     * This helps filter out false positives from profile views or random objects.
     *
     * @return 0 if landmarks pass validation, 1-5 indicates which check failed
     */
    private int validateLandmarks(
            float leftEyeX, float leftEyeY, float rightEyeX, float rightEyeY,
            float noseX, float noseY, float mouthY, float foreheadY, float chinY,
            float leftCheekX, float rightCheekX) {

        // Check 1: Eyes should be at similar Y level (not too tilted)
        float eyeYDiff = Math.abs(leftEyeY - rightEyeY);
        if (eyeYDiff > MAX_EYE_Y_DIFF) {
            return 1;
        }

        // Check 2: Nose should be below eyes (higher Y = lower on screen)
        float avgEyeY = (leftEyeY + rightEyeY) / 2f;
        if (noseY < avgEyeY) {
            return 2;
        }

        // Check 3: Nose should be horizontally between the eyes
        float minEyeX = Math.min(leftEyeX, rightEyeX);
        float maxEyeX = Math.max(leftEyeX, rightEyeX);
        if (noseX < minEyeX || noseX > maxEyeX) {
            return 3;
        }

        // Check 4: Mouth should be below nose
        if (mouthY < noseY) {
            return 4;
        }

        // Check 5: Face aspect ratio should be reasonable
        float faceWidth = Math.abs(rightCheekX - leftCheekX);
        float faceHeight = Math.abs(chinY - foreheadY);
        if (faceHeight > 0) {
            float aspectRatio = faceWidth / faceHeight;
            if (aspectRatio < MIN_FACE_ASPECT_RATIO || aspectRatio > MAX_FACE_ASPECT_RATIO) {
                return 5;
            }
        }

        return 0; // All checks passed
    }

    /** Destroys {@link FaceLandmarker} and stop. */
    public void destroy() {
        Log.i(TAG, "destroy");
        isRunning = false;
        ensurePauseThread();
    }
}