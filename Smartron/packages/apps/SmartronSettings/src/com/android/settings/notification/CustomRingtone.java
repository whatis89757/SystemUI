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
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.support.v4.widget.CursorAdapter;
import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.database.MergeCursor;
import android.content.ContentResolver;
import android.provider.MediaStore;
import android.media.RingtoneManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.content.ContentUris;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RadioButton;
import java.io.IOException;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.RingtonePreference;
import com.android.settings.R;

/**
 * Created by G Murali Madhav on 21/07/17.
 */

public class CustomRingtone extends Fragment implements AdapterView.OnItemClickListener, 
        View.OnClickListener, OnCheckedChangeListener, MediaPlayer.OnCompletionListener {

    private static final String ADDED_URI = "added_uri";
    private TextView mMoreRingtones;
    private ListView mListView;
    private TextView mSetRingtone, mCancel;
    private CheckBox mApplyBoth;
    private Cursor mCursor;
    private Cursor mDefault = null;
    private CustomAdapter mCustomAdapter;
    private String mRingName;
    private int mPhoneId;
    private Uri mSelectedUri = null;
    private Uri mDefaultUri = null;
    private Uri mAddedUri = null;
    private MediaPlayer mMediaPlayer = null;
    private ContentResolver cr;
    private boolean isCustom;
    private ImageView mView;
    private long mPlayingId;

    private Uri mUri = MediaStore.Files.getContentUri("external");
    private static final String[] mProjection = new String[] {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.custom_ringtone, container, false);

        mMoreRingtones = (TextView) view.findViewById(R.id.more_ringtones);
        mMoreRingtones.setOnClickListener(this);
        mSetRingtone = (TextView) view.findViewById(R.id.set_ringtone);
        mSetRingtone.setOnClickListener(this);
        mCancel = (TextView) view.findViewById(R.id.cancel);
        mCancel.setOnClickListener(this);
        mApplyBoth = (CheckBox) view.findViewById(R.id.apply_checkbox_other_sim);
        mApplyBoth.setOnCheckedChangeListener(this);

        Intent intent = getActivity().getIntent();
        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);

        mDefaultUri = mSelectedUri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI);
        if (savedInstanceState != null) {
            mSelectedUri = (Uri)savedInstanceState.getParcelable(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
            mAddedUri = (Uri)savedInstanceState.getParcelable(ADDED_URI);
        }

        mPhoneId = intent.getIntExtra(RingtonePreference.PHONE_ID, 0);
        mRingName = intent.getStringExtra("title");
        ((TextView) view.findViewById(R.id.current_tone)).setText(getString(R.string.current_ringtone, mRingName));
        ((TextView) view.findViewById(R.id.apply_to_other_sim)).setText(mPhoneId == 0 ? R.string.apply_the_same_to_second_sim : R.string.apply_the_same_to_first_sim);
        cr = getActivity().getContentResolver();
        String where = "media_type = ?";
        String[] args = new String[] {"2"};

        if (mDefaultUri == null ) {
            ((TextView) view.findViewById(R.id.system_tone)).setText((R.string.ringtone_none));
        } else if (mDefaultUri.toString().contains("external")) {
            isCustom = true;
            ((TextView) view.findViewById(R.id.system_tone)).setText(R.string.custom_ringtone);
            mDefault = cr.query(mDefaultUri, null, null, null, null);
        } else if (!(mDefaultUri.toString().contains("internal"))) {
            isCustom = true;
            ((TextView) view.findViewById(R.id.system_tone)).setText(R.string.custom_ringtone);
            mDefault = cr.query(mUri, mProjection, "media_type = ? AND _id = ?",new String[] {"2", getIdFromUri(mDefaultUri)}, null);
        }

        if (mDefault != null && mRingName != null) {
            where = "media_type = ? AND title <> ?";
            args = new String[] {"2", mRingName};
        }

        if (mAddedUri != null) {
            mCursor = getCursor(String.valueOf(ContentUris.parseId(mAddedUri)));
        } else {
            mCursor = cr.query(mUri, mProjection, where, args, "date_added DESC LIMIT 3");
        }

        MergeCursor mergeCursor = null;
        if (mDefault != null) {
            mergeCursor = new MergeCursor(new Cursor[] {mDefault, mCursor});
            mCursor = (Cursor) mergeCursor;
            mergeCursor = null;
        }

        if (mAddedUri != null) {
            Cursor addedCursor = cr.query(mAddedUri, null, null, null, null);
            if (addedCursor != null) {
                mergeCursor = new MergeCursor(new Cursor[] {addedCursor, mCursor});
            }
        }

        mCustomAdapter = new CustomAdapter(getActivity(), mergeCursor != null ? mergeCursor : mCursor);
        mListView.setAdapter(mCustomAdapter);
        mListView.setEmptyView(view.findViewById(R.id.empty));

        mApplyBoth.setChecked(((SelectRingtoneSettings) getActivity()).getPreference());
        setRetainInstance(true);
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // If we are becoming invisible, then...
        if (!isVisibleToUser) {
            stopMediaPlayer();
        } else {
            if (mApplyBoth != null) {
                mApplyBoth.setChecked(((SelectRingtoneSettings) getActivity()).getPreference());
            }
        }
    }

    private String getIdFromUri(Uri uri) {
        if (uri == null) return "-1";
        String last = uri.getLastPathSegment();
        if (last != null && TextUtils.isDigitsOnly(last)) {
            return last;
        }
        String id[] = last.split(":");
        if (id.length > 1 && id[1] != null && TextUtils.isDigitsOnly(id[1])) {
            return id[1];
        }
        return "-1";
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.more_ringtones:
                Intent intent  = new Intent(getActivity(), RingtonePicker.class);
                startActivityForResult(intent, 200);
                break;
             case R.id.set_ringtone:
                Intent resultIntent = new Intent();
                resultIntent.setData(mSelectedUri != null ? mSelectedUri : mDefaultUri);
                getActivity().setResult(Activity.RESULT_OK, resultIntent);
                getActivity().finish();
                break;
            case R.id.cancel:
                getActivity().finish();
                break;
        }
    }

    private void startPlaying(Uri uri) {
        if (mView != null) mView.setVisibility(View.VISIBLE);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(getActivity(), mSelectedUri);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException e) {
            Log.w("CustomRingtone", "Unable to play track", e);
        } catch (NullPointerException e) {
            Log.w("CustomRingtone", "The mSelectedUri is invalid", e);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mMediaPlayer == mp) {
            mp.stop();
            mp.release();
            mMediaPlayer = null;
            mPlayingId = -1;
        }
        if (mView != null) mView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopMediaPlayer();
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        CheckBox cb = (CheckBox) buttonView;
        ((SelectRingtoneSettings) getActivity()).savePreference(isChecked);
    }

    private void stopMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mPlayingId = -1;
        }
        if (mView != null) mView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        stopMediaPlayer();
        mSelectedUri = (Uri)view.getTag();
        mPlayingId = ContentUris.parseId(mSelectedUri);
        mCustomAdapter.notifyDataSetChanged();
        startPlaying(mSelectedUri);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //in fragment class callback
        if (data != null && data.getData() != null) {
            mSelectedUri =  data.getData();
            if (!mSelectedUri.equals(mDefaultUri) && !getIdFromUri(mSelectedUri).equals(getIdFromUri(mDefaultUri))) {
                mAddedUri = mSelectedUri;
                mPlayingId = ContentUris.parseId(mSelectedUri);
                Cursor mNew = cr.query(mSelectedUri, null, null, null, null);
                String st = String.valueOf(mPlayingId);
                if (isCustom) {
                    mNew = new MergeCursor(new Cursor[] {mNew, mDefault});
                }
                if (mNew!= null) {
                    MergeCursor mergeCursor = new MergeCursor(new Cursor[] {mNew, getCursor(st)});
                    mCustomAdapter = new CustomAdapter(getActivity(), mergeCursor);
                    mListView.setAdapter(mCustomAdapter);
                }
                startPlaying(mSelectedUri);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        icicle.putParcelable(ADDED_URI, mAddedUri);
        icicle.putParcelable(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mSelectedUri);
    }


    private Cursor getCursor(String title) {
        String where = "media_type = ? AND _id <> ? ";
        String[] args = new String[] {"2", title};
        if (isCustom && mRingName != null) {
            where = "media_type = ? AND title <> ? AND _id <> ?";
            args = new String[] {"2", mRingName, title};
        }
        return cr.query(mUri, mProjection, where, args, "date_added DESC LIMIT 3");
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
            TextView toneDuration = (TextView) view.findViewById(R.id.duration);
            ImageView imageView = (ImageView) view.findViewById(R.id.indicator);
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            try {
                // Extract properties from cursor
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                int secs = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) / 1000;
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                uri = ContentUris.withAppendedId(uri, id);

                ringTitle.setText(title);
                view.setTag(uri);
                toneDuration.setText(secs == 0 ? "" : RingtonePicker.makeTimeString(context, secs)+(secs < 60 ? " sec" : " min"));

                if (mSelectedUri != null && uri != null) {
                    radio.setChecked(uri.equals(mSelectedUri) || id == Long.parseLong(getIdFromUri(mSelectedUri)));
                }

                if (id == mPlayingId) {
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
