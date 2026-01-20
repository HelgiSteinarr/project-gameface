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
 */

package com.google.projectgameface;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager.LayoutParams;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.projectgameface.R;

import java.util.Objects;

/** The cursor speed activity of Gameface app. */
public class CursorSpeed extends AppCompatActivity {

    protected static final int SEEK_BAR_MAXIMUM_VALUE = 10;

    protected static final int SEEK_BAR_MINIMUM_VALUE = 0;

    private TextView textViewMu; // Show move up speed value.
    private TextView textViewMd; // Show move down speed value.
    private TextView textViewMr; // Show move right speed value.
    private TextView textViewMl; // Show move left speed value.
    private TextView textViewSmoothPointer; // Show the smoothness value of the cursor.
    private TextView textViewBlendshapes; // Show flickering of the a trigger.
    private TextView textViewDelay; // Show the how long the user should hold a gesture value.

    private SeekBar seekBarMu; // Move up speed.
    private SeekBar seekBarMd; // Move down speed.
    private SeekBar seekBarMr; // Move right speed.
    private SeekBar seekBarMl; // Move left speed.
    private SeekBar seekBarSmoothPointer; // The smoothness of the cursor.
    private SeekBar seekBarBlendshapes; // The flickering of the a trigger.
    private SeekBar seekBarDelay; // Controls how long the user should hold a gesture.

    // Gaze settings UI elements
    private Switch switchGazeEnabled;
    private SeekBar seekBarYawThreshold;
    private SeekBar seekBarPitchThreshold;
    private TextView textViewYawThreshold;
    private TextView textViewPitchThreshold;

    private final int[] viewIds = {
        R.id.fasterUp,
        R.id.slowerUp,
        R.id.fasterDown,
        R.id.slowerDown,
        R.id.fasterRight,
        R.id.slowerRight,
        R.id.fasterLeft,
        R.id.slowerLeft,
        R.id.fasterPointer,
        R.id.slowerPointer,
        R.id.fasterBlendshapes,
        R.id.slowerBlendshapes,
        R.id.fasterDelay,
        R.id.slowerDelay,
        R.id.widerYaw,
        R.id.narrowerYaw,
        R.id.widerPitch,
        R.id.narrowerPitch
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cursor_speed);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        // setting actionbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Adjust cursor speed");

        // SeekBar Binding and Textview Progress
        seekBarMu = (SeekBar) findViewById(R.id.seekBarMU);
        textViewMu = findViewById(R.id.progressMU);
        setUpSeekBarAndTextView(
            seekBarMu, textViewMu, String.valueOf(CursorMovementConfig.CursorMovementConfigType.UP_SPEED));

        seekBarMd = (SeekBar) findViewById(R.id.seekBarMD);
        textViewMd = findViewById(R.id.progressMD);
        setUpSeekBarAndTextView(
            seekBarMd, textViewMd, String.valueOf(CursorMovementConfig.CursorMovementConfigType.DOWN_SPEED));

        seekBarMr = (SeekBar) findViewById(R.id.seekBarMR);
        textViewMr = findViewById(R.id.progressMR);
        setUpSeekBarAndTextView(
            seekBarMr, textViewMr, String.valueOf(CursorMovementConfig.CursorMovementConfigType.RIGHT_SPEED));

        seekBarMl = (SeekBar) findViewById(R.id.seekBarML);
        textViewMl = findViewById(R.id.progressML);
        setUpSeekBarAndTextView(
            seekBarMl, textViewMl, String.valueOf(CursorMovementConfig.CursorMovementConfigType.LEFT_SPEED));

        seekBarSmoothPointer = (SeekBar) findViewById(R.id.seekBarSmoothPointer);
        textViewSmoothPointer = findViewById(R.id.progressSmoothPointer);
        setUpSeekBarAndTextView(
            seekBarSmoothPointer,
            textViewSmoothPointer,
            String.valueOf(CursorMovementConfig.CursorMovementConfigType.SMOOTH_POINTER));

        seekBarBlendshapes = (SeekBar) findViewById(R.id.seekBarBlendshapes);
        textViewBlendshapes = findViewById(R.id.progressBlendshapes);
        setUpSeekBarAndTextView(
            seekBarBlendshapes,
            textViewBlendshapes,
            String.valueOf(CursorMovementConfig.CursorMovementConfigType.SMOOTH_BLENDSHAPES));

        seekBarDelay = (SeekBar) findViewById(R.id.seekBarDelay);
        textViewDelay = findViewById(R.id.progressDelay);
        setUpSeekBarAndTextView(
            seekBarDelay, textViewDelay, String.valueOf(CursorMovementConfig.CursorMovementConfigType.HOLD_TIME_MS));

        // Gaze settings setup
        setUpGazeSettings();

        // Binding buttons.
        for (int id : viewIds) {
            findViewById(id).setOnClickListener(buttonClickListener);
        }
    }

    private void setUpGazeSettings() {
        SharedPreferences preferences = getSharedPreferences("GameFaceLocalConfig", Context.MODE_PRIVATE);

        // Setup switch for gaze enabled
        switchGazeEnabled = findViewById(R.id.switchGazeEnabled);
        boolean gazeEnabled = preferences.getInt(
            String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_PAUSE_ENABLED),
            CursorMovementConfig.InitialRawValue.GAZE_PAUSE_ENABLED) > 0;
        switchGazeEnabled.setChecked(gazeEnabled);
        switchGazeEnabled.setOnCheckedChangeListener(gazeEnabledListener);

        // Setup yaw threshold seekbar
        seekBarYawThreshold = findViewById(R.id.seekBarYawThreshold);
        textViewYawThreshold = findViewById(R.id.progressYawThreshold);
        int yawValue = preferences.getInt(
            String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_YAW_THRESHOLD),
            CursorMovementConfig.InitialRawValue.GAZE_YAW_THRESHOLD);
        seekBarYawThreshold.setMax(10);
        seekBarYawThreshold.setMin(1);
        seekBarYawThreshold.setProgress(yawValue);
        seekBarYawThreshold.setOnSeekBarChangeListener(gazeSeekBarChange);
        updateGazeThresholdText(textViewYawThreshold, yawValue);

        // Setup pitch threshold seekbar
        seekBarPitchThreshold = findViewById(R.id.seekBarPitchThreshold);
        textViewPitchThreshold = findViewById(R.id.progressPitchThreshold);
        int pitchValue = preferences.getInt(
            String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_PITCH_THRESHOLD),
            CursorMovementConfig.InitialRawValue.GAZE_PITCH_THRESHOLD);
        seekBarPitchThreshold.setMax(10);
        seekBarPitchThreshold.setMin(1);
        seekBarPitchThreshold.setProgress(pitchValue);
        seekBarPitchThreshold.setOnSeekBarChangeListener(gazeSeekBarChange);
        updateGazeThresholdText(textViewPitchThreshold, pitchValue);

        // Update UI enabled state based on switch
        updateGazeUIEnabled(gazeEnabled);
    }

    private void updateGazeThresholdText(TextView textView, int rawValue) {
        int degrees = (int) (rawValue * CursorMovementConfig.RawConfigMultiplier.GAZE_YAW_THRESHOLD);
        textView.setText(degrees + "°");
    }

    private void updateGazeUIEnabled(boolean enabled) {
        seekBarYawThreshold.setEnabled(enabled);
        seekBarPitchThreshold.setEnabled(enabled);
        seekBarYawThreshold.setAlpha(enabled ? 1.0f : 0.5f);
        seekBarPitchThreshold.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private CompoundButton.OnCheckedChangeListener gazeEnabledListener =
        new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int value = isChecked ? 1 : 0;
                sendValueToService(
                    String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_PAUSE_ENABLED), value);
                updateGazeUIEnabled(isChecked);
            }
        };

    private SeekBar.OnSeekBarChangeListener gazeSeekBarChange =
        new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getId() == R.id.seekBarYawThreshold) {
                    updateGazeThresholdText(textViewYawThreshold, progress);
                } else if (seekBar.getId() == R.id.seekBarPitchThreshold) {
                    updateGazeThresholdText(textViewPitchThreshold, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getId() == R.id.seekBarYawThreshold) {
                    sendValueToService(
                        String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_YAW_THRESHOLD),
                        seekBar.getProgress());
                } else if (seekBar.getId() == R.id.seekBarPitchThreshold) {
                    sendValueToService(
                        String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_PITCH_THRESHOLD),
                        seekBar.getProgress());
                }
            }
        };

    private void setUpSeekBarAndTextView(SeekBar seekBar, TextView textView, String preferencesId) {
        seekBar.setMax(SEEK_BAR_MAXIMUM_VALUE);
        seekBar.setMin(SEEK_BAR_MINIMUM_VALUE);
        SharedPreferences preferences = getSharedPreferences("GameFaceLocalConfig", Context.MODE_PRIVATE);
        int savedProgress;
        if (Objects.equals(preferencesId, CursorMovementConfig.CursorMovementConfigType.SMOOTH_POINTER.toString())) {
            savedProgress = preferences.getInt(preferencesId, CursorMovementConfig.InitialRawValue.SMOOTH_POINTER);
        } else if (Objects.equals(preferencesId, CursorMovementConfig.CursorMovementConfigType.HOLD_TIME_MS.toString())) {
            savedProgress = preferences.getInt(preferencesId, CursorMovementConfig.InitialRawValue.HOLD_TIME_MS);
        } else {
            savedProgress = preferences.getInt(preferencesId, CursorMovementConfig.InitialRawValue.DEFAULT_SPEED);
        }
        seekBar.setProgress(savedProgress);
        seekBar.setOnSeekBarChangeListener(seekBarChange);
        if (Objects.equals(preferencesId, CursorMovementConfig.CursorMovementConfigType.HOLD_TIME_MS.toString())) {
            int timeMsForShow = (int) (savedProgress * CursorMovementConfig.RawConfigMultiplier.HOLD_TIME_MS);
            textView.setText(String.valueOf(timeMsForShow));
        } else {
            textView.setText(String.valueOf(savedProgress));
        }
    }

    private void sendValueToService(String configName, int value) {
        saveCursorSpeed(configName, value);
        Intent intent = new Intent("LOAD_SHARED_CONFIG_BASIC");
        intent.putExtra("configName", configName);
        sendBroadcast(intent);
    }

    private View.OnClickListener buttonClickListener =
        new OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentValue;
                int newValue = 0;
                boolean isFaster = true; // False means slower
                String valueName = "";
                if (v.getId() == R.id.fasterUp) {
                    currentValue = seekBarMu.getProgress();
                    newValue = currentValue + 1;
                    isFaster = true;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.UP_SPEED);
                } else if (v.getId() == R.id.slowerUp) {
                    currentValue = seekBarMu.getProgress();
                    newValue = currentValue - 1;
                    isFaster = false;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.UP_SPEED);
                } else if (v.getId() == R.id.fasterDown) {
                    currentValue = seekBarMd.getProgress();
                    newValue = currentValue + 1;
                    isFaster = true;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.DOWN_SPEED);
                } else if (v.getId() == R.id.slowerDown) {
                    currentValue = seekBarMd.getProgress();
                    newValue = currentValue - 1;
                    isFaster = false;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.DOWN_SPEED);
                } else if (v.getId() == R.id.fasterRight) {
                    currentValue = seekBarMr.getProgress();
                    newValue = currentValue + 1;
                    isFaster = true;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.RIGHT_SPEED);
                } else if (v.getId() == R.id.slowerRight) {
                    currentValue = seekBarMr.getProgress();
                    newValue = currentValue - 1;
                    isFaster = false;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.RIGHT_SPEED);
                } else if (v.getId() == R.id.fasterLeft) {
                    currentValue = seekBarMl.getProgress();
                    newValue = currentValue + 1;
                    isFaster = true;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.LEFT_SPEED);
                } else if (v.getId() == R.id.slowerLeft) {
                    currentValue = seekBarMl.getProgress();
                    newValue = currentValue - 1;
                    isFaster = false;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.LEFT_SPEED);
                } else if (v.getId() == R.id.fasterPointer) {
                    currentValue = seekBarSmoothPointer.getProgress();
                    newValue = currentValue + 1;
                    isFaster = true;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.SMOOTH_POINTER);
                } else if (v.getId() == R.id.slowerPointer) {
                    currentValue = seekBarSmoothPointer.getProgress();
                    newValue = currentValue - 1;
                    isFaster = false;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.SMOOTH_POINTER);
                } else if (v.getId() == R.id.fasterBlendshapes) {
                    currentValue = seekBarBlendshapes.getProgress();
                    newValue = currentValue + 1;
                    isFaster = true;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.SMOOTH_BLENDSHAPES);
                } else if (v.getId() == R.id.slowerBlendshapes) {
                    currentValue = seekBarBlendshapes.getProgress();
                    newValue = currentValue - 1;
                    isFaster = false;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.SMOOTH_BLENDSHAPES);
                } else if (v.getId() == R.id.fasterDelay) {
                    currentValue = seekBarDelay.getProgress();
                    newValue = currentValue + 1;
                    isFaster = true;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.HOLD_TIME_MS);
                } else if (v.getId() == R.id.slowerDelay) {
                    currentValue = seekBarDelay.getProgress();
                    newValue = currentValue - 1;
                    isFaster = false;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.HOLD_TIME_MS);
                } else if (v.getId() == R.id.widerYaw) {
                    currentValue = seekBarYawThreshold.getProgress();
                    newValue = currentValue + 1;
                    isFaster = true;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_YAW_THRESHOLD);
                } else if (v.getId() == R.id.narrowerYaw) {
                    currentValue = seekBarYawThreshold.getProgress();
                    newValue = currentValue - 1;
                    isFaster = false;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_YAW_THRESHOLD);
                } else if (v.getId() == R.id.widerPitch) {
                    currentValue = seekBarPitchThreshold.getProgress();
                    newValue = currentValue + 1;
                    isFaster = true;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_PITCH_THRESHOLD);
                } else if (v.getId() == R.id.narrowerPitch) {
                    currentValue = seekBarPitchThreshold.getProgress();
                    newValue = currentValue - 1;
                    isFaster = false;
                    valueName = String.valueOf(CursorMovementConfig.CursorMovementConfigType.GAZE_PITCH_THRESHOLD);
                }
                // For gaze thresholds, min is 1 instead of 0
                int minValue = (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.GAZE_YAW_THRESHOLD.toString()) ||
                               Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.GAZE_PITCH_THRESHOLD.toString())) ? 1 : 0;
                if ((isFaster && newValue < 11) || (!isFaster && newValue > minValue - 1)) {
                    if (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.UP_SPEED.toString())) {
                        seekBarMu.setProgress(newValue);
                        sendValueToService(valueName, newValue);
                    } else if (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.DOWN_SPEED.toString())) {
                        seekBarMd.setProgress(newValue);
                        sendValueToService(valueName, newValue);
                    } else if (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.RIGHT_SPEED.toString())) {
                        seekBarMr.setProgress(newValue);
                        sendValueToService(valueName, newValue);
                    } else if (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.LEFT_SPEED.toString())) {
                        seekBarMl.setProgress(newValue);
                        sendValueToService(valueName, newValue);
                    } else if (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.SMOOTH_POINTER.toString())) {
                        seekBarSmoothPointer.setProgress(newValue);
                        sendValueToService(valueName, newValue);
                    } else if (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.SMOOTH_BLENDSHAPES.toString())) {
                        seekBarBlendshapes.setProgress(newValue);
                        sendValueToService(valueName, newValue);
                    } else if (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.HOLD_TIME_MS.toString())) {
                        seekBarDelay.setProgress(newValue);
                        sendValueToService(valueName, newValue);
                    } else if (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.GAZE_YAW_THRESHOLD.toString())) {
                        seekBarYawThreshold.setProgress(newValue);
                        sendValueToService(valueName, newValue);
                    } else if (Objects.equals(valueName, CursorMovementConfig.CursorMovementConfigType.GAZE_PITCH_THRESHOLD.toString())) {
                        seekBarPitchThreshold.setProgress(newValue);
                        sendValueToService(valueName, newValue);
                    }
                }
            }
        };

    private SeekBar.OnSeekBarChangeListener seekBarChange =
        new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getId() == R.id.seekBarMU) {
                    textViewMu.setText(String.valueOf(progress));
                } else if (seekBar.getId() == R.id.seekBarMD) {
                    textViewMd.setText(String.valueOf(progress));
                } else if (seekBar.getId() == R.id.seekBarMR) {
                    textViewMr.setText(String.valueOf(progress));
                } else if (seekBar.getId() == R.id.seekBarML) {
                    textViewMl.setText(String.valueOf(progress));
                } else if (seekBar.getId() == R.id.seekBarSmoothPointer) {
                    textViewSmoothPointer.setText(String.valueOf(progress));
                } else if (seekBar.getId() == R.id.seekBarBlendshapes) {
                    textViewBlendshapes.setText(String.valueOf(progress));
                } else if (seekBar.getId() == R.id.seekBarDelay) {
                    int timeMsForShow = (int) (progress * CursorMovementConfig.RawConfigMultiplier.HOLD_TIME_MS);
                    textViewDelay.setText(String.valueOf(timeMsForShow));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getId() == R.id.seekBarMU) {
                    sendValueToService(
                        String.valueOf(CursorMovementConfig.CursorMovementConfigType.UP_SPEED), seekBar.getProgress());
                } else if (seekBar.getId() == R.id.seekBarMD) {
                    sendValueToService(
                        String.valueOf(CursorMovementConfig.CursorMovementConfigType.DOWN_SPEED), seekBar.getProgress());
                } else if (seekBar.getId() == R.id.seekBarMR) {
                    sendValueToService(
                        String.valueOf(CursorMovementConfig.CursorMovementConfigType.RIGHT_SPEED), seekBar.getProgress());
                } else if (seekBar.getId() == R.id.seekBarML) {
                    sendValueToService(
                        String.valueOf(CursorMovementConfig.CursorMovementConfigType.LEFT_SPEED), seekBar.getProgress());
                } else if (seekBar.getId() == R.id.seekBarSmoothPointer) {
                    sendValueToService(
                        String.valueOf(CursorMovementConfig.CursorMovementConfigType.SMOOTH_POINTER), seekBar.getProgress());
                } else if (seekBar.getId() == R.id.seekBarBlendshapes) {
                    sendValueToService(
                        String.valueOf(CursorMovementConfig.CursorMovementConfigType.SMOOTH_BLENDSHAPES), seekBar.getProgress());
                } else if (seekBar.getId() == R.id.seekBarDelay) {
                    sendValueToService(
                        String.valueOf(CursorMovementConfig.CursorMovementConfigType.HOLD_TIME_MS), seekBar.getProgress());
                }
            }
        };

    private void saveCursorSpeed(String key, int value) {
        SharedPreferences preferences = getSharedPreferences("GameFaceLocalConfig", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }
}