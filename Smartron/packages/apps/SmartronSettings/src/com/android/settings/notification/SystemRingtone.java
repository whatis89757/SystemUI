/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.ContentUris;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;
import android.support.v4.widget.CursorAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.android.settings.notification.RingtonePicker;
import com.android.settings.RingtonePreference;
import com.android.settings.R;

/**
 * Created by G Murali Madhav on 21/07/17.
 */

public class SystemRingtone extends Fragment implements AdapterView.OnItemClickListener,
        View.OnClickListener, OnCheckedChangeListener{

    private ListView mListView;
    private static final String COLUMN_LABEL = MediaStore.Audio.Media.TITLE;
    private static final String COLUMN_ID = MediaStore.Audio.Media._ID;
    private static final String COLUMN_URI = "content://media/internal/audio/media";
    private RingtoneManager mRingtoneManager;
    private int mPhoneId;
    private Cursor mCursor;
    private TextView mSetRingtone, mCancel;
    private CheckBox mApplyBoth;
    private ImageView mView;
    private Uri mDefaultUri = null;
    private Uri mSelectedUri = null;
    private long mPlayingId;
    private CustomAdapter mCustomAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.system_ringtone, container, false);

        mSetRingtone = (TextView) view.findViewById(R.id.set_ringtone);
        mSetRingtone.setOnClickListener(this);
        mCancel = (TextView) view.findViewById(R.id.cancel);
        mCancel.setOnClickListener(this);
        mApplyBoth = (CheckBox) view.findViewById(R.id.apply_checkbox_other_sim);
        mApplyBoth.setOnCheckedChangeListener(this);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);

        Intent intent = getActivity().getIntent();
        mDefaultUri = mSelectedUri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI);
        if (savedInstanceState != null) {
            mSelectedUri = (Uri)savedInstanceState.getParcelable(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
        }
        mPhoneId = intent.getIntExtra(RingtonePreference.PHONE_ID, 0);
        String title = intent.getStringExtra("title");
        ((TextView) view.findViewById(R.id.current_tone)).setText(getString(R.string.current_ringtone, title));
        if (mDefaultUri == null ) {
            ((TextView) view.findViewById(R.id.system_tone)).setText(R.string.ringtone_none);
        } else if (!(mDefaultUri.toString().contains("internal"))) {
            ((TextView) view.findViewById(R.id.system_tone)).setText(R.string.custom_ringtone);
        }
        ((TextView) view.findViewById(R.id.apply_to_other_sim)).setText(mPhoneId == 0 ? R.string.apply_the_same_to_second_sim : R.string.apply_the_same_to_first_sim);

        mRingtoneManager = new RingtoneManager(getActivity());
        mCursor = mRingtoneManager.getCursor();
        MatrixCursor matrixCursor = new MatrixCursor(mCursor.getColumnNames());
        matrixCursor.addRow(new Object[] { -1, "None", null, null, 0 });
        MergeCursor mergeCursor = new MergeCursor(new Cursor[] { matrixCursor, mCursor });
        mCustomAdapter = new CustomAdapter(getActivity(), mergeCursor);
        mListView.setAdapter(mCustomAdapter);

        mApplyBoth.setChecked(((SelectRingtoneSettings) getActivity()).getPreference());
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // If we are becoming invisible, then...
        if (!isVisibleToUser) {
            if (mRingtoneManager != null) {
                mRingtoneManager.stopPreviousRingtone();
            }
            if (mView != null) mView.setVisibility(View.INVISIBLE);
            mPlayingId = -1;
        } else {
            if (mApplyBoth != null) {
                mApplyBoth.setChecked(((SelectRingtoneSettings) getActivity()).getPreference());
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mRingtoneManager.stopPreviousRingtone();
        mPlayingId = -1;
        mSelectedUri = (Uri)view.getTag();
        if (mSelectedUri != null) mPlayingId = ContentUris.parseId(mSelectedUri);
        mCustomAdapter.notifyDataSetChanged();

        if (mSelectedUri != null) {
            mRingtoneManager.getRingtone(mRingtoneManager.getRingtonePosition(mSelectedUri)).play();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        icicle.putParcelable(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mSelectedUri);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.set_ringtone:
                Intent intent = new Intent();
                intent.setData(mSelectedUri);
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().finish();
                break;
            case R.id.cancel:
                getActivity().finish();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        CheckBox cb = (CheckBox) buttonView;
        ((SelectRingtoneSettings) getActivity()).savePreference(isChecked);
    }

    public void onPause() {
        super.onPause();
        mRingtoneManager.stopPreviousRingtone();
    }

    public class CustomAdapter extends CursorAdapter {

        public CustomAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.ringtone_list, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView ringTitle = (TextView) view.findViewById(R.id.text_view);
            RadioButton radio = (RadioButton) view.findViewById(R.id.radio_button);
            radio.setChecked(false);
            TextView toneDuration = (TextView) view.findViewById(R.id.duration);
            ImageView imageView = (ImageView) view.findViewById(R.id.indicator);
            Uri uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
            try {
                // Extract properties from cursor
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LABEL));
                int secs = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) / 1000;
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                if (RingtoneManager.URI_COLUMN_INDEX < cursor.getColumnCount()) {
                    String uriPath = cursor.getString(RingtoneManager.URI_COLUMN_INDEX);
                    if (uriPath != null) {
                        uri = Uri.parse(uriPath);
                    }
                }
                uri = ContentUris.withAppendedId(uri, id);
                ringTitle.setText(title);
                view.setTag(id == -1 ? null : uri);
                toneDuration.setText(secs == 0 ? "" : RingtonePicker.makeTimeString(context, secs)+(secs < 60 ? " sec" : " min"));

                if (mSelectedUri != null && uri != null ) {
                    radio.setChecked(uri.equals(mSelectedUri));
                }
                if (id == -1) {
                    radio.setChecked(mSelectedUri == null);
                }
                if (id != -1 && id == mPlayingId) {
                    imageView.setVisibility(View.VISIBLE);
                    mView = imageView;
                } else {
                    imageView.setVisibility(View.INVISIBLE);
                }
            } catch (IllegalArgumentException iae) {
                // Some other error retrieving the column from the provider
            }
        }
    }

}
