/*
 * Copyright (C) 2018 The Android Open Source Project
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

/* Created by G Murali Madhav, 08-03-2018 */

package com.android.settings.colorpicker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.view.Gravity;
import android.content.ContextWrapper;
import android.support.annotation.Nullable;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.TypedValue;
import com.android.settings.R;

public class ColorDialogFragment extends DialogFragment {
    private static final String NUM_COLUMNS_KEY = "num_columns";
    private static final String COLOR_CHOICES_KEY = "color_choices";
    private static final String SELECTED_COLOR_KEY = "selected_color";
    private static final String DEFAULT_COLOR_KEY = "default_color";
    private GridLayout colorGrid;
    private OnColorSelectedListener colorSelectedListener;
    private int numColumns;
    private int mSelectedColor;
    private ImageView mImageView;
    private int[] colorChoices;
    //the color to be checked
    private int selectedColorValue;
    private int mDefaultColor;

    public ColorDialogFragment() {
    }

    public static ColorDialogFragment newInstance(int numColumns, int[] colorChoices, int selectedColorValue, int defaultColor) {
        Bundle args = new Bundle();
        args.putInt(NUM_COLUMNS_KEY, numColumns);
        args.putIntArray(COLOR_CHOICES_KEY, colorChoices);
        args.putInt(SELECTED_COLOR_KEY, selectedColorValue);
        args.putInt(DEFAULT_COLOR_KEY, defaultColor);

        ColorDialogFragment dialog = new ColorDialogFragment();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        numColumns = args.getInt(NUM_COLUMNS_KEY);
        colorChoices = args.getIntArray(COLOR_CHOICES_KEY);
        selectedColorValue = args.getInt(SELECTED_COLOR_KEY);
        mDefaultColor = args.getInt(DEFAULT_COLOR_KEY);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener colorSelectedListener) {
        this.colorSelectedListener = colorSelectedListener;
        repopulateItems();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnColorSelectedListener) {
            setOnColorSelectedListener((OnColorSelectedListener) context);
        } else {
            repopulateItems();
        }

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View rootView = layoutInflater.inflate(R.layout.color_dialog_fragment, null);

        ImageView defaultView = (ImageView) rootView.findViewById(R.id.default_color_view);
        if (selectedColorValue == mDefaultColor) {
            mImageView = defaultView;
            mSelectedColor = selectedColorValue;
        }

        ColorUtils.setColorViewValue(defaultView, mDefaultColor, selectedColorValue == mDefaultColor);
        defaultView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mImageView != null)
                    ColorUtils.setColorViewValue(mImageView, mSelectedColor, false);
                mSelectedColor = mDefaultColor;
                mImageView = defaultView;
                ColorUtils.setColorViewValue(mImageView, mSelectedColor, true);
            }
        });

        colorGrid = (GridLayout) rootView.findViewById(R.id.color_grid);
        colorGrid.setColumnCount(numColumns);
        repopulateItems();

        return new AlertDialog.Builder(getActivity())
                .setView(rootView)
                .setTitle(R.string.color_picker_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (colorSelectedListener != null) {
                            colorSelectedListener.onColorSelected(mSelectedColor);
                        }
                        dialog.cancel();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();
    }

    private void repopulateItems() {
        if (colorSelectedListener == null || colorGrid == null) {
            return;
        }

        Context context = colorGrid.getContext();
        colorGrid.removeAllViews();
        for (final int color : colorChoices) {
            if (color == mDefaultColor) {
                continue;
            }
            View itemView = LayoutInflater.from(context)
                    .inflate(R.layout.color_dialog_grid_item, colorGrid, false);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.color_view);
            itemView.setFocusable(true);
            itemView.setClickable(true);
            if (selectedColorValue == color) {
                mImageView = imageView;
                mSelectedColor = selectedColorValue;
            }
            ColorUtils.setColorViewValue((ImageView) itemView.findViewById(R.id.color_view), color, selectedColorValue == color);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mImageView != null)
                        ColorUtils.setColorViewValue(mImageView, mSelectedColor, false);
                    mSelectedColor = color;
                    mImageView = imageView;
                    ColorUtils.setColorViewValue(mImageView, mSelectedColor, true);
                }
            });

            colorGrid.addView(itemView);
        }

    }

    public interface OnColorSelectedListener {
        void onColorSelected(int newColor);
    }

    public static class ColorUtils {
        public static void setColorViewValue(ImageView imageView, int color, boolean selected) {
            imageView.setImageDrawable(null);
            Resources res = imageView.getContext().getResources();

            Drawable currentDrawable = imageView.getDrawable();
            GradientDrawable colorChoiceDrawable;
            if (currentDrawable instanceof GradientDrawable) {
                // Reuse drawable
                colorChoiceDrawable = (GradientDrawable) currentDrawable;
            } else {
                colorChoiceDrawable = new GradientDrawable();
                colorChoiceDrawable.setShape(GradientDrawable.OVAL);
            }

            colorChoiceDrawable.setColor(color);
            if (color == Color.WHITE) {
                // Set stroke to dark version of color
                int darkenedColor = Color.rgb(
                        Color.red(color) * 192 / 256,
                        Color.green(color) * 192 / 256,
                        Color.blue(color) * 192 / 256);
                colorChoiceDrawable.setStroke((int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 2, res.getDisplayMetrics()), darkenedColor);
            }
            Drawable drawable = colorChoiceDrawable;
            if (selected) {
                BitmapDrawable checkmark = (BitmapDrawable) res.getDrawable(R.drawable.ic_color_check);
                checkmark.setGravity(Gravity.CENTER);
                drawable = new LayerDrawable(new Drawable[]{
                        colorChoiceDrawable,
                        checkmark});
            }

            imageView.setImageDrawable(drawable);
        }

        public static int[] extractColorArray(@ArrayRes int arrayId, Context context) {
            String[] choicesString = context.getResources().getStringArray(arrayId);
            int[] choicesInt = context.getResources().getIntArray(arrayId);

            // If user uses color reference(i.e. @color/color_choice) in the array,
            // the choicesString contains null values. We use the choicesInt in such case.
            boolean isStringArray = choicesString[0] != null;
            int length = isStringArray ? choicesString.length : choicesInt.length;

            int[] colorChoices = new int[length];
            for (int i = 0; i < length; i++) {
                colorChoices[i] = isStringArray ? Color.parseColor(choicesString[i]) : choicesInt[i];
            }

            return colorChoices;
        }
    }
}
