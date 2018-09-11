package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.settings.R;
import android.widget.AdapterView;
import android.os.Vibrator;
import android.content.res.Resources;
import android.os.VibrationEffect;
import android.content.res.TypedArray;

public class VibrationDialogFragment extends DialogFragment {
    private static final String PATTERN_ENTRIES = "pattern_entries";
    private static final String SELCTED_PATTERN_VALUE = "selcted_pattern_value";
    private static final String PATTERN_TYPE = "pattern_type";
    private static final int[] PULSE_AMPLITUDE_DEFAULT= {0, 255, 0, 255, 0, // priming  + interval
            77, 77, 78, 79, 81, 84, 87, 93, 101, 114, 133, 162, 205, 255, // ease-in (min amplitude = 30%)
            255, // Peak
            0}; // pause before repetition
    private static final int CALL_VIBRATION_TYPE = 1;
    private static final int NOTIFICATION_VIBRATION_TYPE = 2;
    private String[] mEntries;
    private int mSelectedValue;
    private ListView mPatterns;
    private int mType;
    private OnPatternSelectedListener patternSelectedListener;
    private Vibrator mVibrator;
    private TypedArray mTypedArray;

    private VibrationDialogFragment() {

    }

    public static VibrationDialogFragment newInstance(String[] entries, int value, int type) {
        Bundle args = new Bundle();
        args.putStringArray(PATTERN_ENTRIES, entries);
        args.putInt(SELCTED_PATTERN_VALUE, value);
        args.putInt(PATTERN_TYPE, type);
        VibrationDialogFragment fragment = new VibrationDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mEntries = args.getStringArray(PATTERN_ENTRIES);
        mType = args.getInt(PATTERN_TYPE);
        if (savedInstanceState != null) {
            mSelectedValue = savedInstanceState.getInt(SELCTED_PATTERN_VALUE);
        } else
            mSelectedValue = args.getInt(SELCTED_PATTERN_VALUE);
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        setTypedArray();
    }

    public void setOnPatternSelectedListener(OnPatternSelectedListener patternSelectedListener) {
        this.patternSelectedListener = patternSelectedListener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPatternSelectedListener) {
            setOnPatternSelectedListener((OnPatternSelectedListener) context);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SELCTED_PATTERN_VALUE, mSelectedValue);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedINstanceState) {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View rootView = layoutInflater.inflate(R.layout.vibration_dialog_fragment, null);
        int tempValue = mSelectedValue;
        mPatterns = (ListView) rootView.findViewById(R.id.patterns);
        mPatterns.setDivider(null);
        mPatterns.setDividerHeight(0);
        CustomListAdapter adapter = new CustomListAdapter(getActivity());
        mPatterns.setAdapter(adapter);
        mPatterns.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedValue = position;
                adapter.notifyDataSetChanged();
                playCurrentVibration(position);
            }
        });
        return new AlertDialog.Builder(getActivity())
                .setView(rootView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (patternSelectedListener != null) {
                            patternSelectedListener.onPatternSelected(mSelectedValue);
                        }
                        if (mVibrator != null)
                            mVibrator.cancel();
                        dialog.cancel();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mVibrator != null)
                            mVibrator.cancel();
                        dialog.cancel();
                    }
                }).create();
    }

    private void setTypedArray() {
        try {
            if (CALL_VIBRATION_TYPE == mType)
                mTypedArray = getActivity().getResources().obtainTypedArray(com.android.internal.R.array.config_call_vibration_patterns);
            else if (NOTIFICATION_VIBRATION_TYPE == mType)
                mTypedArray = getActivity().getResources().obtainTypedArray(com.android.internal.R.array.config_defalt_notification_patterns);
            else
                mTypedArray = null;
        } catch (Resources.NotFoundException resourceNotFoundException) {
            mTypedArray = null;
        }

    }

    private void playCurrentVibration(int position) {
        if (mTypedArray != null) {
            int id = mTypedArray.getResourceId(position, 0);
            if (id > 0) {
                long[] pattern = getLongArray(getActivity().getResources(), id);

                if (mVibrator != null)
                    mVibrator.cancel();
                if (mType == CALL_VIBRATION_TYPE) {
                    if (position == 0)
                        mVibrator.vibrate(VibrationEffect.createWaveform(pattern, PULSE_AMPLITUDE_DEFAULT, -1));
                    else
                        mVibrator.vibrate(VibrationEffect.createWaveform(pattern, getAmplitude(pattern.length), -1));
                } else if (mType == NOTIFICATION_VIBRATION_TYPE)
                    mVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            }
        }
        return;
    }

    private int[] getAmplitude(int length) {
        int[] amplitude = new int[length];
        for (int i = 0; i < length; i++) {
            if (i % 2 == 0)
                amplitude[i] = 0;
            else
                amplitude[i] = -1;
        }
        return amplitude;
    }

    private long[] getLongArray(Resources res, int id) {
        int[] arr = res.getIntArray(id);
        long[] out = new long[arr.length];
        for (int i = 0; i < arr.length; i++)
            out[i] = (long) (arr[i]);
        return out;
    }

    public interface OnPatternSelectedListener {
        void onPatternSelected(int newPattern);
    }

    private class CustomListAdapter extends BaseAdapter {
        public CustomListAdapter(Context context) {

        }

        public int getCount() {
            return mEntries.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View row = layoutInflater.inflate(R.layout.custom_list_row, null, true);
            ;
            TextView title = (TextView) row.findViewById(R.id.title);
            RadioButton enabled = (RadioButton) row.findViewById(R.id.enabled);
            title.setText(mEntries[position]);
            if (position == mSelectedValue)
                enabled.setChecked(true);
            else
                enabled.setChecked(false);

            return row;
        }
    }
}