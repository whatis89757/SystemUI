package com.android.systemui.tuner;


import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;

import com.android.systemui.statusbar.phone.StatusBarIconController;

import java.util.Set;

/**
 * Created by smartron on 26/2/18.
 */

public class ClickableIcon extends LinearLayout implements Checkable {
    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private static final float DISABLED_STATE_OPACITY = .3f;
    private boolean broadcasting;
    private boolean isChecked;
    private ImageView iconView;
    private TextView labelView;
    private Set<String> mBlacklist;
    private OnCheckedChangeListener onCheckedChangeListener;
    private Drawable mIconOff,mIconActive;

    public ClickableIcon(Context context) {
        this(context, null);
    }

    public ClickableIcon(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mBlacklist = StatusBarIconController.getIconBlacklist(Settings.Secure.getString(getContext().getContentResolver(), StatusBarIconController.ICON_BLACKLIST));
        init(context, attributeSet);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        CharSequence labelText;
        boolean enabled;
        TypedArray typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.ClickableIcon);
        try {
            mIconOff = typedArray.getDrawable(R.styleable.ClickableIcon_status_icon);
            mIconActive = typedArray.getDrawable(R.styleable.ClickableIcon_status_icon_active);
            labelText = typedArray.getString(R.styleable.ClickableIcon_status_labelText);
            enabled = typedArray.getBoolean(R.styleable.ClickableIcon_android_enabled, true);
        } finally {
            typedArray.recycle();
        }

        iconView = new ImageView(context, null, android.R.style.Widget_Material_Button_Colored);
        LayoutParams iconParams = generateDefaultLayoutParams();
        iconParams.width = (int) getResources().getDimension(R.dimen.status_icon_size);
        iconParams.height = (int) getResources().getDimension(R.dimen.status_icon_size);
        iconView.setLayoutParams(iconParams);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addView(iconView);

        labelView = new TextView(context);
        LayoutParams labelParams = generateDefaultLayoutParams();
        labelParams.width = LayoutParams.WRAP_CONTENT;
        labelParams.height = LayoutParams.WRAP_CONTENT;
        labelParams.topMargin =
                context.getResources().getDimensionPixelOffset(R.dimen.status_icon_label_size);
        labelView.setLayoutParams(labelParams);
        labelView.setText(labelText);
        labelView.setGravity(Gravity.CENTER);
        labelView.setSingleLine();
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        labelView.setDuplicateParentStateEnabled(true);
        addView(labelView);
        setFocusable(true);
        setClickable(true);
        setEnabled(enabled);
        setOutlineProvider(null);
        setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ClickableIcon clickableIcon, boolean isChecked) {
                setChecked(isChecked);
                String key = getPref(clickableIcon.getId());
                mBlacklist = StatusBarIconController.getIconBlacklist(Settings.Secure.getString(getContext().getContentResolver(), StatusBarIconController.ICON_BLACKLIST));
                if (!isChecked)
                    mBlacklist.add(key);
                else
                    mBlacklist.remove(key);

                setList(mBlacklist);
            }
        });
        if (mBlacklist.contains(getPref(getId())))
            setChecked(false);
        else
            setChecked(true);

    }

    @Override
    public void refreshDrawableState() {
        super.refreshDrawableState();
        iconView.setAlpha(isEnabled() ? 1f : DISABLED_STATE_OPACITY);
        if (isChecked) {
            iconView.setImageDrawable(mIconActive);
            iconView.setBackground(getResources().getDrawable(R.drawable.status_icon_background_checked, null));
            labelView.setTextColor(getResources().getColor(R.color.status_icon_background_checked));
        } else {
            iconView.setImageDrawable(mIconOff);
            iconView.setBackground(getResources().getDrawable(R.drawable.status_icon_background_unchecked, null));
            labelView.setTextColor(getResources().getColor(R.color.status_icon_text_unchecked));
        }
    }


    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        performSetChecked(checked);
    }

    @Override
    public void toggle() {
        userRequestedSetChecked(!isChecked());
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.onCheckedChangeListener = listener;
    }

    @Override
    public boolean performClick() {
        if (!isCheckable()) {
            return super.performClick();
        }

        toggle();
        final boolean handled = super.performClick();
        return handled;
    }

    private boolean isCheckable() {
        return onCheckedChangeListener != null;
    }

    private void performSetChecked(boolean checked) {
        if (isChecked() == checked) {
            return;
        }
        isChecked = checked;
        refreshDrawableState();
    }

    private void userRequestedSetChecked(boolean checked) {
        if (isChecked() == checked) {
            return;
        }
        if (broadcasting) {
            return;
        }
        broadcasting = true;
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener.onCheckedChanged(this, checked);
        }
        broadcasting = false;
    }

    private void setList(Set<String> blacklist) {
        ContentResolver contentResolver = getContext().getContentResolver();
        Settings.Secure.putStringForUser(contentResolver, StatusBarIconController.ICON_BLACKLIST,
                TextUtils.join(",", blacklist), ActivityManager.getCurrentUser());
    }

    private String getPref(int id) {
        switch (id) {

            case R.id.alarm:
                return "alarm_clock";

            case R.id.airplane:
                return "airplane";

            case R.id.rotate:
                return "rotate";

            case R.id.battery:
                return "battery";

            case R.id.bluetooth:
                return "bluetooth";

            case R.id.cast:
                return "cast";

            case R.id.dnd:
                return "zen";

            case R.id.ethernet:
                return "ethernet";

            case R.id.headset:
                return "headset";

            case R.id.hotspot:
                return "hotspot";

            case R.id.mobile:
                return "mobile";

            case R.id.time:
                return "clock";

            case R.id.volume:
                return "volume";
            case R.id.wifi:
                return "wifi";
            case R.id.work_profile:
                return "managed_profile";
            case R.id.volte:
                return "ims";
            default:
                return null;

        }
    }

    public interface OnCheckedChangeListener {

        void onCheckedChanged(ClickableIcon clickableIcon, boolean isChecked);
    }
}
