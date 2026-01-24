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
import android.content.SharedPreferences;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/** Store cursor movement config such as speed or smoothing value. */
class CursorMovementConfig {

  public enum CursorMovementConfigType {
    UP_SPEED,
    DOWN_SPEED,
    RIGHT_SPEED,
    LEFT_SPEED,
    SMOOTH_POINTER,
    /** Smooth down the blendshape value so it doesn't trigger by accident. */
    SMOOTH_BLENDSHAPES,

    /** How long the hold duration will be dispatch. */
    HOLD_TIME_MS,
    HOLD_RADIUS,

    /** Gaze tracking settings for auto-pause feature. */
    GAZE_PAUSE_ENABLED,
    GAZE_YAW_THRESHOLD,
    GAZE_PITCH_THRESHOLD,

    /** Drag mode: 0 = toggle (default), 1 = hold expression to drag. */
    DRAG_MODE
  }

  private static final String TAG = "CursorMovementConfig";
  private static final int PREFERENCE_INT_NOT_FOUND = -1;

  /** Persistent storage on device (Data/data/{app}) */
  SharedPreferences sharedPreferences;

  /** Raw int value, same as the UI's slider. */
  private final Map<CursorMovementConfigType, Integer> rawValueMap;

  public static final class InitialRawValue {
    public static final int DEFAULT_SPEED = 3;
    public static final int SMOOTH_POINTER = 1;
    public static final int HOLD_TIME_MS = 5;
    public static final int HOLD_RADIUS = 2;
    public static final int GAZE_PAUSE_ENABLED = 1; // 1 = enabled, 0 = disabled
    public static final int GAZE_YAW_THRESHOLD = 3; // Raw value 0-10, multiplied to get degrees
    public static final int GAZE_PITCH_THRESHOLD = 5; // Raw value 0-10, multiplied to get degrees
    public static final int DRAG_MODE = 0; // 0 = toggle, 1 = hold

    private InitialRawValue() {}
  }

  /** Multiply raw int value from UI to real usable float value. */
  public static final class RawConfigMultiplier {
    public static final float UP_SPEED = 175.f;
    public static final float DOWN_SPEED = 175.f;
    public static final float RIGHT_SPEED = 175.f;
    public static final float LEFT_SPEED = 175.f;
    public static final float SMOOTH_POINTER = 5.f;
    public static final float SMOOTH_BLENDSHAPES = 30.f;
    public static final float HOLD_TIME_MS = 200.f;
    public static final float HOLD_RADIUS = 50;
    public static final float GAZE_PAUSE_ENABLED = 1.f; // Boolean, no multiplier needed
    public static final float GAZE_YAW_THRESHOLD = 10.f; // Degrees per raw unit (0-10 -> 0-100 degrees)
    public static final float GAZE_PITCH_THRESHOLD = 10.f; // Degrees per raw unit (0-10 -> 0-100 degrees)
    public static final float DRAG_MODE = 1.f; // Integer mode, no multiplier needed

    private RawConfigMultiplier() {}
  }

  /**
   * Stores cursor configs such as LEFT_SPEED or SMOOTH_POINTER.
   *
   * @param context Context for open SharedPreference in device's local storage.
   */
  public CursorMovementConfig(Context context) {

    Log.i(TAG, "Create CursorMovementConfig.");

    // Create or retrieve SharedPreference.
    sharedPreferences = context.getSharedPreferences("GameFaceLocalConfig", Context.MODE_PRIVATE);

    // Initialize default slider values.
    rawValueMap = new HashMap<>();
    rawValueMap.put(CursorMovementConfigType.UP_SPEED, InitialRawValue.DEFAULT_SPEED);
    rawValueMap.put(CursorMovementConfigType.DOWN_SPEED, InitialRawValue.DEFAULT_SPEED);
    rawValueMap.put(CursorMovementConfigType.RIGHT_SPEED, InitialRawValue.DEFAULT_SPEED);
    rawValueMap.put(CursorMovementConfigType.LEFT_SPEED, InitialRawValue.DEFAULT_SPEED);

    rawValueMap.put(CursorMovementConfigType.SMOOTH_POINTER, InitialRawValue.SMOOTH_POINTER);
    rawValueMap.put(CursorMovementConfigType.SMOOTH_BLENDSHAPES, InitialRawValue.DEFAULT_SPEED);
    rawValueMap.put(CursorMovementConfigType.HOLD_TIME_MS, InitialRawValue.HOLD_TIME_MS);
    rawValueMap.put(CursorMovementConfigType.HOLD_RADIUS, InitialRawValue.HOLD_RADIUS);

    rawValueMap.put(CursorMovementConfigType.GAZE_PAUSE_ENABLED, InitialRawValue.GAZE_PAUSE_ENABLED);
    rawValueMap.put(CursorMovementConfigType.GAZE_YAW_THRESHOLD, InitialRawValue.GAZE_YAW_THRESHOLD);
    rawValueMap.put(CursorMovementConfigType.GAZE_PITCH_THRESHOLD, InitialRawValue.GAZE_PITCH_THRESHOLD);
    rawValueMap.put(CursorMovementConfigType.DRAG_MODE, InitialRawValue.DRAG_MODE);
  }

  /**
   * Set config with the raw value from UI or SharedPreference.
   *
   * @param configName Name of the target config such as "UP_SPEED" or "SMOOTH_POINTER"
   * @param rawValueFromUi Slider value.
   */
  public void setRawValueFromUi(String configName, int rawValueFromUi) {
    try {
      CursorMovementConfigType targetConfig = CursorMovementConfigType.valueOf(configName);
      rawValueMap.put(targetConfig, rawValueFromUi);
    } catch (IllegalArgumentException e) {
      Log.w(TAG, configName + " is not exist in CursorMovementConfigType enum.");
    }
  }

  /**
   * Get the config and also apply UI-multiplier value.
   *
   * @param targetConfig Config to get.
   * @return Action value of cursor.
   */
  public float get(CursorMovementConfigType targetConfig) {
    int rawValue = (rawValueMap.get(targetConfig) != null) ? rawValueMap.get(targetConfig) : 0;
    float multiplier;
    switch (targetConfig) {
      case UP_SPEED:
        multiplier = RawConfigMultiplier.UP_SPEED;
        break;
      case DOWN_SPEED:
        multiplier = RawConfigMultiplier.DOWN_SPEED;
        break;
      case RIGHT_SPEED:
        multiplier = RawConfigMultiplier.RIGHT_SPEED;
        break;
      case LEFT_SPEED:
        multiplier = RawConfigMultiplier.LEFT_SPEED;
        break;
      case SMOOTH_POINTER:
        multiplier = RawConfigMultiplier.SMOOTH_POINTER;
        break;
      case SMOOTH_BLENDSHAPES:
        multiplier = RawConfigMultiplier.SMOOTH_BLENDSHAPES;
        break;
      case HOLD_TIME_MS:
        multiplier = RawConfigMultiplier.HOLD_TIME_MS;
        break;
      case HOLD_RADIUS:
        multiplier = RawConfigMultiplier.HOLD_RADIUS;
        break;
      case GAZE_PAUSE_ENABLED:
        multiplier = RawConfigMultiplier.GAZE_PAUSE_ENABLED;
        break;
      case GAZE_YAW_THRESHOLD:
        multiplier = RawConfigMultiplier.GAZE_YAW_THRESHOLD;
        break;
      case GAZE_PITCH_THRESHOLD:
        multiplier = RawConfigMultiplier.GAZE_PITCH_THRESHOLD;
        break;
      case DRAG_MODE:
        multiplier = RawConfigMultiplier.DRAG_MODE;
        break;
      default:
        multiplier = 0.f;
    }
    return (float) rawValue * multiplier;
  }

  /** Update and overwrite value from SharedPreference. */
  public void updateAllConfigFromSharedPreference() {
    Log.i(TAG, "Update all config from local SharedPreference...");
    for (CursorMovementConfigType configType : CursorMovementConfigType.values()) {
      updateOneConfigFromSharedPreference(configType.name());
    }
  }

  /**
   * Update cursor movement config from SharedPreference (persistent storage on device).
   *
   * @param configName String of {@link CursorMovementConfig}.
   */
  public void updateOneConfigFromSharedPreference(String configName) {
    Log.i(TAG, "updateOneConfigFromSharedPreference: " + configName);

    if (sharedPreferences == null) {
      Log.w(TAG, "sharedPreferences instance does not exist.");
      return;
    }

    int configValueInUi = sharedPreferences.getInt(configName, PREFERENCE_INT_NOT_FOUND);
    if (configValueInUi == PREFERENCE_INT_NOT_FOUND) {
      Log.i(TAG, "Key " + configName + " not found in SharedPreference, keep using default value.");
      return;
    }
    setRawValueFromUi(configName, configValueInUi);
    Log.i(TAG, "Set raw value to: " + configValueInUi);
  }
}