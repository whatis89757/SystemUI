/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.policy;

import android.content.Context;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy.PointerEventListener;

public class FingerSwipeGestureListener implements PointerEventListener {
    private static final String TAG = "FingerSwipeGestureListener";
    private static final boolean DEBUG = false;
    private static final long SWIPE_EVENT_TIMEOUT_MS = 500;
    private static final int MAX_TRACKED_POINTERS = 32;
    private static final int UNTRACKED_POINTER = -1;
    private static final int THREE_FINGER_SWIPE_DISTANCE = 350;
    private final int GESTURE_FINGER_SWIPE_MASK = 15;
    private final int GESTURE_FINGER_SWIPE_MASK_TWO = 7;
    private final int POINTER_NONE_MASK = 1;
    private final Callbacks mCallbacks;
    private static final int MAX_NUM_POINTERS = 3;
    private final int[] mDownPointerId = new int[MAX_TRACKED_POINTERS];
    private final float[] mDownX = new float[MAX_TRACKED_POINTERS];
    private final float[] mDownY = new float[MAX_TRACKED_POINTERS];
    private final long[] mDownTime = new long[MAX_TRACKED_POINTERS];
    private int mDownPointers;
    private boolean mdetectThreeFingerSwipe = false;
    private int mSwipeMask = 1;

    public FingerSwipeGestureListener(Context context, Callbacks callbacks) {
        mCallbacks = checkNull("callbacks", callbacks);
    }

    private static <T> T checkNull(String name, T arg) {
        if (arg == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return arg;
    }

    @Override
    public void onPointerEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mdetectThreeFingerSwipe = true;
                mDownPointers = 0;
                captureDownEvents(event, 0);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                captureDownEvents(event, event.getActionIndex());
                break;
            case MotionEvent.ACTION_MOVE:
                if (DEBUG) Slog.d(TAG, "count3" + event.getPointerCount());
                if (mdetectThreeFingerSwipe) {
                    Slog.d(TAG,"Got three touches, trying to detect three finger swipe");
                    detectThreeFingerSwipe(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mdetectThreeFingerSwipe && (mSwipeMask == GESTURE_FINGER_SWIPE_MASK || mSwipeMask == GESTURE_FINGER_SWIPE_MASK_TWO)) {
                    mSwipeMask = 1;
                    Slog.d(TAG,"Detected three finger swipe");
                    mCallbacks.onThreeFingerSwipeDetected();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mdetectThreeFingerSwipe = false;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            default:
                if (DEBUG) Slog.d(TAG, "Ignoring " + event);
        }
    }

    private void captureDownEvents(MotionEvent event, int pointerIndex) {
        final int pointerId = event.getPointerId(pointerIndex);
        final int i = getIndexOfPointer(pointerId);
        final int pointerCount  = event.getPointerCount();
        if (DEBUG) Slog.d(TAG, "pointer " + pointerId +
                " down pointerIndex=" + pointerIndex + " trackingIndex=" + i);
        if (i != UNTRACKED_POINTER) {
            mDownX[i] = event.getX(pointerIndex);
            mDownY[i] = event.getY(pointerIndex);
            mDownTime[i] = event.getEventTime();
            if (DEBUG) Slog.d(TAG, "pointer " + pointerId +
                    " down x=" + mDownX[i] + " y=" + mDownY[i]);
        }
        if (pointerCount == MAX_NUM_POINTERS) {
            mdetectThreeFingerSwipe = true;
            return;
        }
        mdetectThreeFingerSwipe = false;
    }

    private int getIndexOfPointer(int pointerId) {
        for (int i = 0; i < mDownPointers; i++) {
            if (mDownPointerId[i] == pointerId) {
                return i;
            }
        }
        if (mDownPointers == MAX_TRACKED_POINTERS || pointerId == MotionEvent.INVALID_POINTER_ID) {
            return UNTRACKED_POINTER;
        }
        mDownPointerId[mDownPointers++] = pointerId;
        return mDownPointers - 1;
    }

    private void detectThreeFingerSwipe(MotionEvent move) {
        final int pointerCount = move.getPointerCount();
        for (int p = 0; p < pointerCount; p++) {
            final int pointerId = move.getPointerId(p);
            final int i = getIndexOfPointer(pointerId);
            if (i != UNTRACKED_POINTER) {
                detectThreeFingerSwipe(i, move.getEventTime(), move.getX(p), move.getY(p),p);
            }
        }
    }

    private void detectThreeFingerSwipe(int i, long time, float x, float y,int p) {
        final float fromX = mDownX[i];
        final float fromY = mDownY[i];
        final long elapsed = time - mDownTime[i];
        Slog.d(TAG, "pointer " + mDownPointerId[i]
                + " moved (" + fromX + "->" + x + "," + fromY + "->" + y + ") in " + elapsed+" "+mSwipeMask);
        if (mSwipeMask < GESTURE_FINGER_SWIPE_MASK
                && y > fromY + THREE_FINGER_SWIPE_DISTANCE
                && elapsed < SWIPE_EVENT_TIMEOUT_MS) {
            mSwipeMask |= 1 << p + 1;
            if (DEBUG) Slog.d(TAG, "swipe mask = " + mSwipeMask);
        }
    }

    interface Callbacks {
        void onThreeFingerSwipeDetected();
    }
}

