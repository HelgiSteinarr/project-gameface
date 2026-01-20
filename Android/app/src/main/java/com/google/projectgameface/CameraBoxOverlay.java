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


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;

/** The camera overlay of camera feed. */
public class CameraBoxOverlay extends View {

    private static final int DEBUG_TEXT_LOC_X = 10;
    private static final int DEBUG_TEXT_LOC_Y = 250;

    /** White dot on user head. */
    private float whiteDotX = -100.f;
    private float whiteDotY = -100.f;

    /** Red dot on nose tip. */
    private float noseDotX = -100.f;
    private float noseDotY = -100.f;

    /** Blue dot on nose bridge. */
    private float noseBridgeX = -100.f;
    private float noseBridgeY = -100.f;

    /** Gaze line data. */
    private float gazeEndX = -100.f;
    private float gazeEndY = -100.f;
    private boolean isLooking = false;
    private boolean gazeEnabled = true;
    private static final float GAZE_LINE_LENGTH = 40.f;

    /** Debug: face normal values for display. */
    private float debugNormalX = 0.f;
    private float debugNormalY = 0.f;
    private float debugNormalZ = 0.f;

    private String preprocessTimeText = "";
    private String mediapipeTimeText = "";
    private String pauseIndicatorText = "";

    private Paint paint;
    private Paint nosePaint;
    private Paint noseBridgePaint;
    private Paint gazePaintGreen;
    private Paint gazePaintRed;

    public CameraBoxOverlay(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), android.R.color.white));
        paint.setTextSize(32);

        nosePaint = new Paint();
        nosePaint.setStyle(Paint.Style.FILL);
        nosePaint.setColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_light));

        noseBridgePaint = new Paint();
        noseBridgePaint.setStyle(Paint.Style.FILL);
        noseBridgePaint.setColor(ContextCompat.getColor(getContext(), android.R.color.holo_blue_light));

        gazePaintGreen = new Paint();
        gazePaintGreen.setStyle(Paint.Style.FILL);
        gazePaintGreen.setColor(ContextCompat.getColor(getContext(), android.R.color.holo_green_light));
        gazePaintGreen.setStrokeWidth(3);
        gazePaintGreen.setTextSize(24);

        gazePaintRed = new Paint();
        gazePaintRed.setStyle(Paint.Style.FILL);
        gazePaintRed.setColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_light));
        gazePaintRed.setStrokeWidth(3);
        gazePaintRed.setTextSize(24);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Debug dots: white=forehead, red=nose tip, blue=nose bridge
        canvas.drawCircle(whiteDotX, whiteDotY, 5, paint);
        canvas.drawCircle(noseDotX, noseDotY, 5, nosePaint);
        canvas.drawCircle(noseBridgeX, noseBridgeY, 5, noseBridgePaint);

        // Debug: face normal line (direction face is pointing)
        // Use neutral color (white) when gaze auto-pause is disabled
        Paint gazePaint = gazeEnabled ? (isLooking ? gazePaintGreen : gazePaintRed) : paint;
        canvas.drawLine(noseBridgeX, noseBridgeY, gazeEndX, gazeEndY, gazePaint);

        // Debug: looking status (only show when gaze auto-pause is enabled)
        if (gazeEnabled) {
            String lookingText = isLooking ? "Looking" : "Not looking";
            canvas.drawText(lookingText, DEBUG_TEXT_LOC_X, 30, gazePaint);
        }

        // Debug: face normal vector values
        String normalText = String.format("N:(%.2f,%.2f,%.2f)", debugNormalX, debugNormalY, debugNormalZ);
        canvas.drawText(normalText, DEBUG_TEXT_LOC_X, 55, paint);

        canvas.drawText(preprocessTimeText, DEBUG_TEXT_LOC_X, DEBUG_TEXT_LOC_Y, paint);
        canvas.drawText(mediapipeTimeText, DEBUG_TEXT_LOC_X, DEBUG_TEXT_LOC_Y + 50, paint);
        canvas.drawText(pauseIndicatorText, DEBUG_TEXT_LOC_X, DEBUG_TEXT_LOC_Y + 100, paint);
    }

    public void setWhiteDot(float x, float y) {
        whiteDotX = x;
        whiteDotY = y;
        invalidate();
    }

    public void setNoseDot(float x, float y) {
        noseDotX = x;
        noseDotY = y;
        invalidate();
    }

    public void setNoseBridge(float x, float y) {
        noseBridgeX = x;
        noseBridgeY = y;
        invalidate();
    }

    public void setGaze(float bridgeX, float bridgeY, float normalX, float normalY, float normalZ, boolean looking, boolean enabled) {
        noseBridgeX = bridgeX;
        noseBridgeY = bridgeY;
        gazeEndX = bridgeX + normalX * GAZE_LINE_LENGTH;
        gazeEndY = bridgeY + normalY * GAZE_LINE_LENGTH;
        isLooking = looking;
        gazeEnabled = enabled;
        // Store for debug display
        debugNormalX = normalX;
        debugNormalY = normalY;
        debugNormalZ = normalZ;
        invalidate();
    }

    public void setOverlayInfo(long preprocessValue, long mediapipeValue) {
        preprocessTimeText = "pre: " + preprocessValue + " ms";
        mediapipeTimeText = "med: " + mediapipeValue + " ms";
        invalidate();
    }

    public void setPauseIndicator(boolean isPause) {
        if (isPause) {
            preprocessTimeText = "";
            mediapipeTimeText = "";
            pauseIndicatorText = "pause";
        } else {
            pauseIndicatorText = "";
        }
    }
}