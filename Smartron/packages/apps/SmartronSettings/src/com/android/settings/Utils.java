/**
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings;

import static android.content.Intent.EXTRA_USER;
import static android.content.Intent.EXTRA_USER_ID;
import static android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.Fragment;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.fingerprint.FingerprintManager;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.preference.PreferenceFrameLayout;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;
import android.provider.Settings;
import android.provider.Telephony;
import android.service.persistentdata.PersistentDataBlockManager;
import android.support.annotation.StringRes;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.TtsSpan;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.TabWidget;

import com.android.internal.app.UnlaunchableAppActivity;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.UserIcons;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.enterprise.DevicePolicyManagerWrapper;
import com.android.settings.password.FingerprintManagerWrapper;
import com.android.settings.password.IFingerprintManager;
import com.android.settings.bluetooth.BluetoothSettings;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class Utils extends com.android.settingslib.Utils {

    private static final String TAG = "Settings";

    /**
     * Set the preference's title to the matching activity's label.
     */
    public static final int UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY = 1;

    /**
     * The opacity level of a disabled icon.
     */
    public static final float DISABLED_ALPHA = 0.4f;

    /**
     * Color spectrum to use to indicate badness.  0 is completely transparent (no data),
     * 1 is most bad (red), the last value is least bad (green).
     */
    public static final int[] BADNESS_COLORS = new int[] {
            0x00000000, 0xffc43828, 0xffe54918, 0xfff47b00,
            0xfffabf2c, 0xff679e37, 0xff0a7f42
    };

    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    public static final String OS_PKG = "os";

    private static SparseArray<Bitmap> sDarkDefaultUserBitmapCache = new SparseArray<Bitmap>();

    public static final String READ_ONLY = "read_only";
    public static final String PERSISTENT = "persistent";

    /**
     * Finds a matching activity for a preference's intent. If a matching
     * activity is not found, it will remove the preference.
     *
     * @param context The context.
     * @param parentPreferenceGroup The preference group that contains the
     *            preference whose intent is being resolved.
     * @param preferenceKey The key of the preference whose intent is being
     *            resolved.
     * @param flags 0 or one or more of
     *            {@link #UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY}
     *            .
     * @return Whether an activity was found. If false, the preference was
     *         removed.
     */
    public static boolean updatePreferenceToSpecificActivityOrRemove(Context context,
            PreferenceGroup parentPreferenceGroup, String preferenceKey, int flags) {

        Preference preference = parentPreferenceGroup.findPreference(preferenceKey);
        if (preference == null) {
            return false;
        }

        Intent intent = preference.getIntent();
        if (intent != null) {
            // Find the activity that is in the system image
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);
                if ((resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0) {

                    // Replace the intent with this specific activity
                    preference.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name));

                    if ((flags & UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY) != 0) {
                        // Set the preference title to the activity's label
                        preference.setTitle(resolveInfo.loadLabel(pm));
                    }

                    return true;
                }
            }
        }

        // Did not find a matching activity, so remove the preference
        parentPreferenceGroup.removePreference(preference);

        return false;
    }

    /**
     * Returns the UserManager for a given context
     *
     * @throws IllegalStateException if no UserManager could be retrieved.
     */
    public static UserManager getUserManager(Context context) {
        UserManager um = UserManager.get(context);
        if (um == null) {
            throw new IllegalStateException("Unable to load UserManager");
        }
        return um;
    }

    /**
     * Returns true if Monkey is running.
     */
    public static boolean isMonkeyRunning() {
        return ActivityManager.isUserAMonkey();
    }

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

    /**
     * Returns the WIFI IP Addresses, if any, taking into account IPv4 and IPv6 style addresses.
     * @param context the application context
     * @return the formatted and newline-separated IP addresses, or null if none.
     */
    public static String getWifiIpAddresses(Context context) {
        WifiManager wifiManager = context.getSystemService(WifiManager.class);
        Network currentNetwork = wifiManager.getCurrentNetwork();
        if (currentNetwork != null) {
            ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            LinkProperties prop = cm.getLinkProperties(currentNetwork);
            return formatIpAddresses(prop);
        }
        return null;
    }

    /**
     * Returns the default link's IP addresses, if any, taking into account IPv4 and IPv6 style
     * addresses.
     * @return the formatted and newline-separated IP addresses, or null if none.
     */
    public static String getDefaultIpAddresses(ConnectivityManager cm) {
        LinkProperties prop = cm.getActiveLinkProperties();
        return formatIpAddresses(prop);
    }

    private static String formatIpAddresses(LinkProperties prop) {
        if (prop == null) return null;
        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        // If there are no entries, return null
        if (!iter.hasNext()) return null;
        // Concatenate all available addresses, comma separated
        String addresses = "";
        while (iter.hasNext()) {
            addresses += iter.next().getHostAddress();
            if (iter.hasNext()) addresses += "\n";
        }
        return addresses;
    }

    public static Locale createLocaleFromString(String localeStr) {
        // TODO: is there a better way to actually construct a locale that will match?
        // The main problem is, on top of Java specs, locale.toString() and
        // new Locale(locale.toString()).toString() do not return equal() strings in
        // many cases, because the constructor takes the only string as the language
        // code. So : new Locale("en", "US").toString() => "en_US"
        // And : new Locale("en_US").toString() => "en_us"
        if (null == localeStr)
            return Locale.getDefault();
        String[] brokenDownLocale = localeStr.split("_", 3);
        // split may not return a 0-length array.
        if (1 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0]);
        } else if (2 == brokenDownLocale.length) {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1]);
        } else {
            return new Locale(brokenDownLocale[0], brokenDownLocale[1], brokenDownLocale[2]);
        }
    }

    public static boolean isBatteryPresent(Intent batteryChangedIntent) {
        return batteryChangedIntent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
    }

    public static String getBatteryPercentage(Intent batteryChangedIntent) {
        return formatPercentage(getBatteryLevel(batteryChangedIntent));
    }

    /**
     * Prepare a custom preferences layout, moving padding to {@link ListView}
     * when outside scrollbars are requested. Usually used to display
     * {@link ListView} and {@link TabWidget} with correct padding.
     */
    public static void prepareCustomPreferencesList(
            ViewGroup parent, View child, View list, boolean ignoreSidePadding) {
        final boolean movePadding = list.getScrollBarStyle() == View.SCROLLBARS_OUTSIDE_OVERLAY;
        if (movePadding) {
            final Resources res = list.getResources();
            final int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);
            final int paddingBottom = res.getDimensionPixelSize(
                    com.android.internal.R.dimen.preference_fragment_padding_bottom);

            if (parent instanceof PreferenceFrameLayout) {
                ((PreferenceFrameLayout.LayoutParams) child.getLayoutParams()).removeBorders = true;

                final int effectivePaddingSide = ignoreSidePadding ? 0 : paddingSide;
                list.setPaddingRelative(effectivePaddingSide, 0, effectivePaddingSide, paddingBottom);
            } else {
                list.setPaddingRelative(paddingSide, 0, paddingSide, paddingBottom);
            }
        }
    }

    public static void forceCustomPadding(View view, boolean additive) {
        final Resources res = view.getResources();
        final int paddingSide = res.getDimensionPixelSize(R.dimen.settings_side_margin);

        final int paddingStart = paddingSide + (additive ? view.getPaddingStart() : 0);
        final int paddingEnd = paddingSide + (additive ? view.getPaddingEnd() : 0);
        final int paddingBottom = res.getDimensionPixelSize(
                com.android.internal.R.dimen.preference_fragment_padding_bottom);

        view.setPaddingRelative(paddingStart, 0, paddingEnd, paddingBottom);
    }

    /* Used by UserSettings as well. Call this on a non-ui thread. */
    public static void copyMeProfilePhoto(Context context, UserInfo user) {
        Uri contactUri = Profile.CONTENT_URI;

        int userId = user != null ? user.id : UserHandle.myUserId();

        InputStream avatarDataStream = Contacts.openContactPhotoInputStream(
                    context.getContentResolver(),
                    contactUri, true);
        // If there's no profile photo, assign a default avatar
        if (avatarDataStream == null) {
            assignDefaultPhoto(context, userId);
            return;
        }

        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bitmap icon = BitmapFactory.decodeStream(avatarDataStream);
        um.setUserIcon(userId, icon);
        try {
            avatarDataStream.close();
        } catch (IOException ioe) { }
    }

    /**
     * Assign the default photo to user with {@paramref userId}
     * @param context used to get the {@link UserManager}
     * @param userId  used to get the icon bitmap
     * @return true if assign photo successfully, false if failed
     */
    public static boolean assignDefaultPhoto(Context context, int userId) {
        if (context == null) {
            return false;
        }
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bitmap bitmap = getDefaultUserIconAsBitmap(userId);
        um.setUserIcon(userId, bitmap);

        return true;
    }

    public static String getMeProfileName(Context context, boolean full) {
        if (full) {
            return getProfileDisplayName(context);
        } else {
            return getShorterNameIfPossible(context);
        }
    }

    private static String getShorterNameIfPossible(Context context) {
        final String given = getLocalProfileGivenName(context);
        return !TextUtils.isEmpty(given) ? given : getProfileDisplayName(context);
    }

    private static String getLocalProfileGivenName(Context context) {
        final ContentResolver cr = context.getContentResolver();

        // Find the raw contact ID for the local ME profile raw contact.
        final long localRowProfileId;
        final Cursor localRawProfile = cr.query(
                Profile.CONTENT_RAW_CONTACTS_URI,
                new String[] {RawContacts._ID},
                RawContacts.ACCOUNT_TYPE + " IS NULL AND " +
                        RawContacts.ACCOUNT_NAME + " IS NULL",
                null, null);
        if (localRawProfile == null) return null;

        try {
            if (!localRawProfile.moveToFirst()) {
                return null;
            }
            localRowProfileId = localRawProfile.getLong(0);
        } finally {
            localRawProfile.close();
        }

        // Find the structured name for the raw contact.
        final Cursor structuredName = cr.query(
                Profile.CONTENT_URI.buildUpon().appendPath(Contacts.Data.CONTENT_DIRECTORY).build(),
                new String[] {CommonDataKinds.StructuredName.GIVEN_NAME,
                    CommonDataKinds.StructuredName.FAMILY_NAME},
                Data.RAW_CONTACT_ID + "=" + localRowProfileId,
                null, null);
        if (structuredName == null) return null;

        try {
            if (!structuredName.moveToFirst()) {
                return null;
            }
            String partialName = structuredName.getString(0);
            if (TextUtils.isEmpty(partialName)) {
                partialName = structuredName.getString(1);
            }
            return partialName;
        } finally {
            structuredName.close();
        }
    }

    private static final String getProfileDisplayName(Context context) {
        final ContentResolver cr = context.getContentResolver();
        final Cursor profile = cr.query(Profile.CONTENT_URI,
                new String[] {Profile.DISPLAY_NAME}, null, null, null);
        if (profile == null) return null;

        try {
            if (!profile.moveToFirst()) {
                return null;
            }
            return profile.getString(0);
        } finally {
            profile.close();
        }
    }

    /** Not global warming, it's global change warning. */
    public static Dialog buildGlobalChangeWarningDialog(final Context context, int titleResId,
            final Runnable positiveAction) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleResId);
        builder.setMessage(R.string.global_change_warning);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                positiveAction.run();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    public static boolean hasMultipleUsers(Context context) {
        return ((UserManager) context.getSystemService(Context.USER_SERVICE))
                .getUsers().size() > 1;
    }

    /**
     * Start a new instance of the activity, showing only the given fragment.
     * When launched in this mode, the given preference fragment will be instantiated and fill the
     * entire activity.
     *
     * @param context The context.
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param resultTo Option fragment that should receive the result of the activity launch.
     * @param resultRequestCode If resultTo is non-null, this is the request code in which
     *                          to report the result.
     * @param titleResId resource id for the String to display for the title of this set
     *                   of preferences.
     * @param title String to display for the title of this set of preferences.
     * @param metricsCategory The current metricsCategory for logging source when fragment starts
     */
    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode, int titleResId,
            CharSequence title, int metricsCategory) {
        startWithFragment(context, fragmentName, args, resultTo, resultRequestCode,
                null /* titleResPackageName */, titleResId, title, false /* not a shortcut */,
                metricsCategory);
    }

    /**
     * Start a new instance of the activity, showing only the given fragment.
     * When launched in this mode, the given preference fragment will be instantiated and fill the
     * entire activity.
     *
     * @param context The context.
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param resultTo Option fragment that should receive the result of the activity launch.
     * @param resultRequestCode If resultTo is non-null, this is the request code in which
     *                          to report the result.
     * @param titleResPackageName Optional package name for the resource id of the title.
     * @param titleResId resource id for the String to display for the title of this set
     *                   of preferences.
     * @param title String to display for the title of this set of preferences.
     * @param metricsCategory The current metricsCategory for logging source when fragment starts
     */
    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode, String titleResPackageName, int titleResId,
            CharSequence title, int metricsCategory) {
        startWithFragment(context, fragmentName, args, resultTo, resultRequestCode,
                titleResPackageName, titleResId, title, false /* not a shortcut */,
                metricsCategory);
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode, int titleResId,
            CharSequence title, boolean isShortcut, int metricsCategory) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args,
                null /* titleResPackageName */, titleResId, title, isShortcut, metricsCategory);
        if (resultTo == null) {
            context.startActivity(intent);
        } else {
            resultTo.getActivity().startActivityForResult(intent, resultRequestCode);
        }
    }

    public static void startWithFragment(Context context, String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode, String titleResPackageName, int titleResId,
            CharSequence title, boolean isShortcut, int metricsCategory) {
        Intent intent = onBuildStartFragmentIntent(context, fragmentName, args, titleResPackageName,
                titleResId, title, isShortcut, metricsCategory);
        if (resultTo == null) {
            context.startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    public static void startWithFragmentAsUser(Context context, String fragmentName, Bundle args,
            int titleResId, CharSequence title, boolean isShortcut, int metricsCategory,
            UserHandle userHandle) {
        // workaround to avoid crash in b/17523189
        if (userHandle.getIdentifier() == UserHandle.myUserId()) {
            startWithFragment(context, fragmentName, args, null, 0, titleResId, title, isShortcut,
                    metricsCategory);
        } else {
            Intent intent = onBuildStartFragmentIntent(context, fragmentName, args,
                    null /* titleResPackageName */, titleResId, title, isShortcut, metricsCategory);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivityAsUser(intent, userHandle);
        }
    }

    /**
     * Build an Intent to launch a new activity showing the selected fragment.
     * The implementation constructs an Intent that re-launches the current activity with the
     * appropriate arguments to display the fragment.
     *
     *
     * @param context The Context.
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param titleResPackageName Optional package name for the resource id of the title.
     * @param titleResId Optional title resource id to show for this item.
     * @param title Optional title to show for this item.
     * @param isShortcut  tell if this is a Launcher Shortcut or not
     * @param sourceMetricsCategory The context (source) from which an action is performed
     * @return Returns an Intent that can be launched to display the given
     * fragment.
     */
    public static Intent onBuildStartFragmentIntent(Context context, String fragmentName,
            Bundle args, String titleResPackageName, int titleResId, CharSequence title,
            boolean isShortcut, int sourceMetricsCategory) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        if (BluetoothSettings.class.getName().equals(fragmentName)) {
            intent.setClass(context, SubSettings.BluetoothSubSettings.class);
            intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
         } else {
             intent.setClass(context, SubSettings.class);
         }
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME,
                titleResPackageName);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, titleResId);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE, title);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SHORTCUT, isShortcut);
        intent.putExtra(SettingsActivity.EXTRA_SOURCE_METRICS_CATEGORY, sourceMetricsCategory);
        return intent;
    }

    /**
     * Returns the managed profile of the current user or {@code null} if none is found or a profile
     * exists but it is disabled.
     */
    public static UserHandle getManagedProfile(UserManager userManager) {
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        final int count = userProfiles.size();
        for (int i = 0; i < count; i++) {
            final UserHandle profile = userProfiles.get(i);
            if (profile.getIdentifier() == userManager.getUserHandle()) {
                continue;
            }
            final UserInfo userInfo = userManager.getUserInfo(profile.getIdentifier());
            if (userInfo.isManagedProfile()) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Returns the managed profile of the current user or {@code null} if none is found. Unlike
     * {@link #getManagedProfile} this method returns enabled and disabled managed profiles.
     */
    public static UserHandle getManagedProfileWithDisabled(UserManager userManager) {
        // TODO: Call getManagedProfileId from here once Robolectric supports
        // API level 24 and UserManager.getProfileIdsWithDisabled can be Mocked (to avoid having
        // yet another implementation that loops over user profiles in this method). In the meantime
        // we need to use UserManager.getProfiles that is available on API 23 (the one currently
        // used for Settings Robolectric tests).
        final int myUserId = UserHandle.myUserId();
        List<UserInfo> profiles = userManager.getProfiles(myUserId);
        final int count = profiles.size();
        for (int i = 0; i < count; i++) {
            final UserInfo profile = profiles.get(i);
            if (profile.isManagedProfile()
                    && profile.getUserHandle().getIdentifier() != myUserId) {
                return profile.getUserHandle();
            }
        }
        return null;
    }

    /**
     * Retrieves the id for the given user's managed profile.
     *
     * @return the managed profile id or UserHandle.USER_NULL if there is none.
     */
    public static int getManagedProfileId(UserManager um, int parentUserId) {
        int[] profileIds = um.getProfileIdsWithDisabled(parentUserId);
        for (int profileId : profileIds) {
            if (profileId != parentUserId) {
                return profileId;
            }
        }
        return UserHandle.USER_NULL;
    }

    /**
     * Returns the target user for a Settings activity.
     * <p>
     * User would be retrieved in this order:
     * <ul>
     * <li> If this activity is launched from other user, return that user id.
     * <li> If this is launched from the Settings app in same user, return the user contained as an
     *      extra in the arguments or intent extras.
     * <li> Otherwise, return UserHandle.myUserId().
     * </ul>
     * <p>
     * Note: This is secure in the sense that it only returns a target user different to the current
     * one if the app launching this activity is the Settings app itself, running in the same user
     * or in one that is in the same profile group, or if the user id is provided by the system.
     */
    public static UserHandle getSecureTargetUser(IBinder activityToken,
            UserManager um, @Nullable Bundle arguments, @Nullable Bundle intentExtras) {
        UserHandle currentUser = new UserHandle(UserHandle.myUserId());
        IActivityManager am = ActivityManager.getService();
        try {
            String launchedFromPackage = am.getLaunchedFromPackage(activityToken);
            boolean launchedFromSettingsApp = SETTINGS_PACKAGE_NAME.equals(launchedFromPackage);

            UserHandle launchedFromUser = new UserHandle(UserHandle.getUserId(
                    am.getLaunchedFromUid(activityToken)));
            if (launchedFromUser != null && !launchedFromUser.equals(currentUser)) {
                // Check it's secure
                if (isProfileOf(um, launchedFromUser)) {
                    return launchedFromUser;
                }
            }
            UserHandle extrasUser = getUserHandleFromBundle(intentExtras);
            if (extrasUser != null && !extrasUser.equals(currentUser)) {
                // Check it's secure
                if (launchedFromSettingsApp && isProfileOf(um, extrasUser)) {
                    return extrasUser;
                }
            }
            UserHandle argumentsUser = getUserHandleFromBundle(arguments);
            if (argumentsUser != null && !argumentsUser.equals(currentUser)) {
                // Check it's secure
                if (launchedFromSettingsApp && isProfileOf(um, argumentsUser)) {
                    return argumentsUser;
                }
            }
        } catch (RemoteException e) {
            // Should not happen
            Log.v(TAG, "Could not talk to activity manager.", e);
        }
        return currentUser;
    }

    /**
     * Lookup both {@link Intent#EXTRA_USER} and {@link Intent#EXTRA_USER_ID} in the bundle
     * and return the {@link UserHandle} object. Return {@code null} if nothing is found.
     */
    private static @Nullable UserHandle getUserHandleFromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        final UserHandle user = bundle.getParcelable(EXTRA_USER);
        if (user != null) {
            return user;
        }
        final int userId = bundle.getInt(EXTRA_USER_ID, -1);
        if (userId != -1) {
            return UserHandle.of(userId);
        }
        return null;
    }

    /**
     * Returns the target user for a Settings activity.
     *
     * The target user can be either the current user, the user that launched this activity or
     * the user contained as an extra in the arguments or intent extras.
     *
     * You should use {@link #getSecureTargetUser(IBinder, UserManager, Bundle, Bundle)} if
     * possible.
     *
     * @see #getInsecureTargetUser(IBinder, Bundle, Bundle)
     */
   public static UserHandle getInsecureTargetUser(IBinder activityToken, @Nullable Bundle arguments,
           @Nullable Bundle intentExtras) {
       UserHandle currentUser = new UserHandle(UserHandle.myUserId());
       IActivityManager am = ActivityManager.getService();
       try {
           UserHandle launchedFromUser = new UserHandle(UserHandle.getUserId(
                   am.getLaunchedFromUid(activityToken)));
           if (launchedFromUser != null && !launchedFromUser.equals(currentUser)) {
               return launchedFromUser;
           }
           UserHandle extrasUser = intentExtras != null
                   ? (UserHandle) intentExtras.getParcelable(EXTRA_USER) : null;
           if (extrasUser != null && !extrasUser.equals(currentUser)) {
               return extrasUser;
           }
           UserHandle argumentsUser = arguments != null
                   ? (UserHandle) arguments.getParcelable(EXTRA_USER) : null;
           if (argumentsUser != null && !argumentsUser.equals(currentUser)) {
               return argumentsUser;
           }
       } catch (RemoteException e) {
           // Should not happen
           Log.v(TAG, "Could not talk to activity manager.", e);
           return null;
       }
       return currentUser;
   }

   /**
    * Returns true if the user provided is in the same profiles group as the current user.
    */
   private static boolean isProfileOf(UserManager um, UserHandle otherUser) {
       if (um == null || otherUser == null) return false;
       return (UserHandle.myUserId() == otherUser.getIdentifier())
               || um.getUserProfiles().contains(otherUser);
   }

    /**
     * Return whether or not the user should have a SIM Cards option in Settings.
     * TODO: Change back to returning true if count is greater than one after testing.
     * TODO: See bug 16533525.
     */
    public static boolean showSimCardTile(Context context) {
        final TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        return tm.getSimCount() > 1;
    }

    /**
     * Returns elapsed time for the given millis, in the following format:
     * 2d 5h 40m 29s
     * @param context the application context
     * @param millis the elapsed time in milli seconds
     * @param withSeconds include seconds?
     * @return the formatted elapsed time
     */
    public static CharSequence formatElapsedTime(Context context, double millis,
            boolean withSeconds) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int seconds = (int) Math.floor(millis / 1000);
        if (!withSeconds) {
            // Round up.
            seconds += 30;
        }

        int days = 0, hours = 0, minutes = 0;
        if (seconds >= SECONDS_PER_DAY) {
            days = seconds / SECONDS_PER_DAY;
            seconds -= days * SECONDS_PER_DAY;
        }
        if (seconds >= SECONDS_PER_HOUR) {
            hours = seconds / SECONDS_PER_HOUR;
            seconds -= hours * SECONDS_PER_HOUR;
        }
        if (seconds >= SECONDS_PER_MINUTE) {
            minutes = seconds / SECONDS_PER_MINUTE;
            seconds -= minutes * SECONDS_PER_MINUTE;
        }

        final ArrayList<Measure> measureList = new ArrayList(4);
        if (days > 0) {
            measureList.add(new Measure(days, MeasureUnit.DAY));
        }
        if (hours > 0) {
            measureList.add(new Measure(hours, MeasureUnit.HOUR));
        }
        if (minutes > 0) {
            measureList.add(new Measure(minutes, MeasureUnit.MINUTE));
        }
        if (withSeconds && seconds > 0) {
            measureList.add(new Measure(seconds, MeasureUnit.SECOND));
        }
        if (measureList.size() == 0) {
            // Everything addable was zero, so nothing was added. We add a zero.
            measureList.add(new Measure(0, withSeconds ? MeasureUnit.SECOND : MeasureUnit.MINUTE));
        }
        final Measure[] measureArray = measureList.toArray(new Measure[measureList.size()]);

        final Locale locale = context.getResources().getConfiguration().locale;
        final MeasureFormat measureFormat = MeasureFormat.getInstance(
                locale, MeasureFormat.FormatWidth.NARROW);
        sb.append(measureFormat.formatMeasures(measureArray));

        if (measureArray.length == 1 && MeasureUnit.MINUTE.equals(measureArray[0].getUnit())) {
            // Add ttsSpan if it only have minute value, because it will be read as "meters"
            final TtsSpan ttsSpan = new TtsSpan.MeasureBuilder().setNumber(minutes)
                    .setUnit("minute").build();
            sb.setSpan(ttsSpan, 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return sb;
    }

    /**
     * Queries for the UserInfo of a user. Returns null if the user doesn't exist (was removed).
     * @param userManager Instance of UserManager
     * @param checkUser The user to check the existence of.
     * @return UserInfo of the user or null for non-existent user.
     */
    public static UserInfo getExistingUser(UserManager userManager, UserHandle checkUser) {
        final List<UserInfo> users = userManager.getUsers(true /* excludeDying */);
        final int checkUserId = checkUser.getIdentifier();
        for (UserInfo user : users) {
            if (user.id == checkUserId) {
                return user;
            }
        }
        return null;
    }

    public static View inflateCategoryHeader(LayoutInflater inflater, ViewGroup parent) {
        final TypedArray a = inflater.getContext().obtainStyledAttributes(null,
                com.android.internal.R.styleable.Preference,
                com.android.internal.R.attr.preferenceCategoryStyle, 0);
        final int resId = a.getResourceId(com.android.internal.R.styleable.Preference_layout,
                0);
        a.recycle();
        return inflater.inflate(resId, parent, false);
    }

    /**
     * Return if we are running low on storage space or not.
     *
     * @param context The context
     * @return true if we are running low on storage space
     */
    public static boolean isLowStorage(Context context) {
        final StorageManager sm = StorageManager.from(context);
        return (sm.getStorageBytesUntilLow(context.getFilesDir()) < 0);
    }

    /**
     * Returns a default user icon (as a {@link Bitmap}) for the given user.
     *
     * Note that for guest users, you should pass in {@code UserHandle.USER_NULL}.
     * @param userId the user id or {@code UserHandle.USER_NULL} for a non-user specific icon
     */
    public static Bitmap getDefaultUserIconAsBitmap(int userId) {
        Bitmap bitmap = null;
        // Try finding the corresponding bitmap in the dark bitmap cache
        bitmap = sDarkDefaultUserBitmapCache.get(userId);
        if (bitmap == null) {
            bitmap = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(userId, false));
            // Save it to cache
            sDarkDefaultUserBitmapCache.put(userId, bitmap);
        }
        return bitmap;
    }

    public static boolean hasPreferredActivities(PackageManager pm, String packageName) {
        // Get list of preferred activities
        List<ComponentName> prefActList = new ArrayList<>();
        // Intent list cannot be null. so pass empty list
        List<IntentFilter> intentList = new ArrayList<>();
        pm.getPreferredActivities(intentList, prefActList, packageName);
        Log.d(TAG, "Have " + prefActList.size() + " number of activities in preferred list");
        return prefActList.size() > 0;
    }

    public static ArraySet<String> getHandledDomains(PackageManager pm, String packageName) {
        List<IntentFilterVerificationInfo> iviList = pm.getIntentFilterVerifications(packageName);
        List<IntentFilter> filters = pm.getAllIntentFilters(packageName);

        ArraySet<String> result = new ArraySet<>();
        if (iviList.size() > 0) {
            for (IntentFilterVerificationInfo ivi : iviList) {
                for (String host : ivi.getDomains()) {
                    result.add(host);
                }
            }
        }
        if (filters != null && filters.size() > 0) {
            for (IntentFilter filter : filters) {
                if (filter.hasCategory(Intent.CATEGORY_BROWSABLE)
                        && (filter.hasDataScheme(IntentFilter.SCHEME_HTTP) ||
                                filter.hasDataScheme(IntentFilter.SCHEME_HTTPS))) {
                    result.addAll(filter.getHostsList());
                }
            }
        }
        return result;
    }

    /**
     * Returns the application info of the currently installed MDM package.
     */
    public static ApplicationInfo getAdminApplicationInfo(Context context, int profileId) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName mdmPackage = dpm.getProfileOwnerAsUser(profileId);
        if (mdmPackage == null) {
            return null;
        }
        String mdmPackageName = mdmPackage.getPackageName();
        try {
            IPackageManager ipm = AppGlobals.getPackageManager();
            ApplicationInfo mdmApplicationInfo =
                    ipm.getApplicationInfo(mdmPackageName, 0, profileId);
            return mdmApplicationInfo;
        } catch (RemoteException e) {
            Log.e(TAG, "Error while retrieving application info for package " + mdmPackageName
                    + ", userId " + profileId, e);
            return null;
        }
    }

    public static boolean isBandwidthControlEnabled() {
        final INetworkManagementService netManager = INetworkManagementService.Stub
                .asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        try {
            return netManager.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            return false;
        }
    }

    /**
     * Returns an accessible SpannableString.
     * @param displayText the text to display
     * @param accessibileText the text text-to-speech engines should read
     */
    public static SpannableString createAccessibleSequence(CharSequence displayText,
            String accessibileText) {
        SpannableString str = new SpannableString(displayText);
        str.setSpan(new TtsSpan.TextBuilder(accessibileText).build(), 0,
                displayText.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return str;
    }

    /**
     * Returns the user id present in the bundle with {@link Intent#EXTRA_USER_ID} if it
     * belongs to the current user.
     *
     * @throws SecurityException if the given userId does not belong to the current user group.
     */
    public static int getUserIdFromBundle(Context context, Bundle bundle) {
        if (bundle == null) {
            return getCredentialOwnerUserId(context);
        }
        int userId = bundle.getInt(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        if (userId == LockPatternUtils.USER_FRP) {
            return enforceSystemUser(context, userId);
        } else {
            return enforceSameOwner(context, userId);
        }
    }

    /**
     * Returns the given user id if the current user is the system user.
     *
     * @throws SecurityException if the current user is not the system user.
     */
    public static int enforceSystemUser(Context context, int userId) {
        if (UserHandle.myUserId() == UserHandle.USER_SYSTEM) {
            return userId;
        }
        throw new SecurityException("Given user id " + userId + " must only be used from "
                + "USER_SYSTEM, but current user is " + UserHandle.myUserId());
    }

    /**
     * Returns the given user id if it belongs to the current user.
     *
     * @throws SecurityException if the given userId does not belong to the current user group.
     */
    public static int enforceSameOwner(Context context, int userId) {
        final UserManager um = getUserManager(context);
        final int[] profileIds = um.getProfileIdsWithDisabled(UserHandle.myUserId());
        if (ArrayUtils.contains(profileIds, userId)) {
            return userId;
        }
        throw new SecurityException("Given user id " + userId + " does not belong to user "
                + UserHandle.myUserId());
    }

    /**
     * Returns the effective credential owner of the calling user.
     */
    public static int getCredentialOwnerUserId(Context context) {
        return getCredentialOwnerUserId(context, UserHandle.myUserId());
    }

    /**
     * Returns the user id of the credential owner of the given user id.
     */
    public static int getCredentialOwnerUserId(Context context, int userId) {
        UserManager um = getUserManager(context);
        return um.getCredentialOwnerProfile(userId);
    }

    public static int resolveResource(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.resourceId;
    }

    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final java.util.Formatter sFormatter = new java.util.Formatter(
            sBuilder, Locale.getDefault());

    public static String formatDateRange(Context context, long start, long end) {
        final int flags = FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH;

        synchronized (sBuilder) {
            sBuilder.setLength(0);
            return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null)
                    .toString();
        }
    }

    public static List<String> getNonIndexable(int xml, Context context) {
        if (Looper.myLooper() == null) {
            // Hack to make sure Preferences can initialize.  Prefs expect a looper, but
            // don't actually use it for the basic stuff here.
            Looper.prepare();
        }
        final List<String> ret = new ArrayList<>();
        PreferenceManager manager = new PreferenceManager(context);
        PreferenceScreen screen = manager.inflateFromResource(context, xml, null);
        checkPrefs(screen, ret);

        return ret;
    }

    private static void checkPrefs(PreferenceGroup group, List<String> ret) {
        if (group == null) return;
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof SelfAvailablePreference
                    && !((SelfAvailablePreference) pref).isAvailable(group.getContext())) {
                ret.add(pref.getKey());
                if (pref instanceof PreferenceGroup) {
                    addAll((PreferenceGroup) pref, ret);
                }
            } else if (pref instanceof PreferenceGroup) {
                checkPrefs((PreferenceGroup) pref, ret);
            }
        }
    }

    private static void addAll(PreferenceGroup group, List<String> ret) {
        if (group == null) return;
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            ret.add(pref.getKey());
            if (pref instanceof PreferenceGroup) {
                addAll((PreferenceGroup) pref, ret);
            }
        }
    }

    public static boolean isDeviceProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) != 0;
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public static boolean startQuietModeDialogIfNecessary(Context context, UserManager um,
            int userId) {
        if (um.isQuietModeEnabled(UserHandle.of(userId))) {
            final Intent intent = UnlaunchableAppActivity.createInQuietModeDialogIntent(userId);
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    public static boolean unlockWorkProfileIfNecessary(Context context, int userId) {
        try {
            if (!ActivityManager.getService().isUserRunning(userId,
                    ActivityManager.FLAG_AND_LOCKED)) {
                return false;
            }
        } catch (RemoteException e) {
            return false;
        }
        if (!(new LockPatternUtils(context)).isSecure(userId)) {
            return false;
        }
        return confirmWorkProfileCredentials(context, userId);
    }

    public static boolean confirmWorkProfileCredentialsIfNecessary(Context context, int userId) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (!km.isDeviceLocked(userId)) {
            return false;
        }
        return confirmWorkProfileCredentials(context, userId);
    }

    private static boolean confirmWorkProfileCredentials(Context context, int userId) {
        final KeyguardManager km = (KeyguardManager) context.getSystemService(
                Context.KEYGUARD_SERVICE);
        final Intent unlockIntent = km.createConfirmDeviceCredentialIntent(null, null, userId);
        if (unlockIntent != null) {
            context.startActivity(unlockIntent);
            return true;
        } else {
            return false;
        }
    }

    public static CharSequence getApplicationLabel(Context context, String packageName) {
        try {
            final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    packageName,
                    PackageManager.MATCH_DISABLED_COMPONENTS
                    | PackageManager.MATCH_ANY_USER);
            return appInfo.loadLabel(context.getPackageManager());
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to find info for package: " + packageName);
        }
        return null;
    }

    public static boolean isPackageDirectBootAware(Context context, String packageName) {
        try {
            final ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    packageName, 0);
            return ai.isDirectBootAware() || ai.isPartiallyDirectBootAware();
        } catch (NameNotFoundException ignored) {
        }
        return false;
    }

    /**
     * Returns a context created from the given context for the given user, or null if it fails
     */
    public static Context createPackageContextAsUser(Context context, int userId) {
        try {
            return context.createPackageContextAsUser(
                    context.getPackageName(), 0 /* flags */, UserHandle.of(userId));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to create user context", e);
        }
        return null;
    }

    public static FingerprintManager getFingerprintManagerOrNull(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return context.getSystemService(FingerprintManager.class);
        } else {
            return null;
        }
    }

    public static IFingerprintManager getFingerprintManagerWrapperOrNull(Context context) {
        FingerprintManager fingerprintManager = getFingerprintManagerOrNull(context);
        if (fingerprintManager != null) {
            return new FingerprintManagerWrapper(fingerprintManager);
        } else {
            return null;
        }
    }

    public static boolean hasFingerprintHardware(Context context) {
        FingerprintManager fingerprintManager = getFingerprintManagerOrNull(context);
        return fingerprintManager != null && fingerprintManager.isHardwareDetected();
    }

    /**
     * Launches an intent which may optionally have a user id defined.
     * @param fragment Fragment to use to launch the activity.
     * @param intent Intent to launch.
     */
    public static void launchIntent(Fragment fragment, Intent intent) {
        try {
            final int userId = intent.getIntExtra(Intent.EXTRA_USER_ID, -1);

            if (userId == -1) {
                fragment.startActivity(intent);
            } else {
                fragment.getActivity().startActivityAsUser(intent, new UserHandle(userId));
            }
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No activity found for " + intent);
        }
    }

    public static boolean isDemoUser(Context context) {
        return UserManager.isDeviceInDemoMode(context) && getUserManager(context).isDemoUser();
    }

    public static ComponentName getDeviceOwnerComponent(Context context) {
        final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        return dpm.getDeviceOwnerComponentOnAnyUser();
    }

    /**
     * Returns if a given user is a profile of another user.
     * @param user The user whose profiles wibe checked.
     * @param profile The (potential) profile.
     * @return if the profile is actually a profile
     */
    public static boolean isProfileOf(UserInfo user, UserInfo profile) {
        return user.id == profile.id ||
                (user.profileGroupId != UserInfo.NO_PROFILE_GROUP_ID
                        && user.profileGroupId == profile.profileGroupId);
    }

    /**
     * Tries to initalize a volume with the given bundle. If it is a valid, private, and readable
     * {@link VolumeInfo}, it is returned. If it is not valid, null is returned.
     */
    @Nullable
    public static VolumeInfo maybeInitializeVolume(StorageManager sm, Bundle bundle) {
        final String volumeId = bundle.getString(VolumeInfo.EXTRA_VOLUME_ID,
                VolumeInfo.ID_PRIVATE_INTERNAL);
        VolumeInfo volume = sm.findVolumeById(volumeId);
        return isVolumeValid(volume) ? volume : null;
    }

    /**
     * Return {@code true} if the supplied package is device owner or profile owner of at
     * least one user.
     * @param userManager used to get profile owner app for each user
     * @param devicePolicyManager used to check whether it is device owner app
     * @param packageName package to check about
     */
    public static boolean isProfileOrDeviceOwner(UserManager userManager,
            DevicePolicyManagerWrapper devicePolicyManager, String packageName) {
        List<UserInfo> userInfos = userManager.getUsers();
        if (devicePolicyManager.isDeviceOwnerAppOnAnyUser(packageName)) {
            return true;
        }
        for (int i = 0, size = userInfos.size(); i < size; i++) {
            ComponentName cn = devicePolicyManager.getProfileOwnerAsUser(userInfos.get(i).id);
            if (cn != null && cn.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the resource id to represent the install status for an app
     */
    @StringRes
    public static int getInstallationStatus(ApplicationInfo info) {
        if ((info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
            return R.string.not_installed;
        }
        return info.enabled ? R.string.installed : R.string.disabled;
    }

    private static boolean isVolumeValid(VolumeInfo volume) {
        return (volume != null) && (volume.getType() == VolumeInfo.TYPE_PRIVATE)
                && volume.isMountedReadable();
    }

    /**
     * check whether NetworkSetting apk exist in system, if yes, return true, else
     * return false.
     */
    public static boolean isNetworkSettingsApkAvailable(Context context) {
        // check whether the target handler exist in system
        Intent intent = new Intent(
                    "org.codeaurora.settings.NETWORK_OPERATOR_SETTINGS_ASYNC");
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : list){
            // check is it installed in system.img, exclude the application
            // installed by user
            if ((resolveInfo.activityInfo.applicationInfo.flags &
                    ApplicationInfo.FLAG_SYSTEM) != 0) {
                return true;
            }
        }
        return false;
    }

    public static String getLocalizedName(Context context, Cursor cursor, int index) {
        if(context == null || cursor == null || index < 0){
           return null;
        }
        // If can find a localized name, replace the APN name with it
        String resName = cursor.getString(index);
        String localizedName = null;
        if (resName != null && !resName.isEmpty()) {
            int resId = context.getResources().getIdentifier(resName, "string",
                    context.getPackageName());
            if(resId > 0){
                try {
                    localizedName = context.getResources().getString(resId);
                    Log.d(TAG, "Replaced apn name with localized name");
                } catch (NotFoundException e) {
                    Log.e(TAG, "Got execption while getting the localized apn name.", e);
                }
            }
        }
        return localizedName;
    }

    public static boolean carrierTableFieldValidate(String field){
        if(field == null)
            return false;
        if(READ_ONLY.equalsIgnoreCase(field) || PERSISTENT.equalsIgnoreCase(field)
                || Telephony.Carriers.AUTH_TYPE.equalsIgnoreCase(field)
                || Telephony.Carriers.SUBSCRIPTION_ID.equalsIgnoreCase(field))
            return true;
        field = field.toUpperCase();
        Class clazz = Telephony.Carriers.class;
        try{
            clazz.getDeclaredField(field);
        }catch(NoSuchFieldException e){
            Log.w(TAG, field + "is not a valid field in class Telephony.Carriers");
            return false;
        }
        return true;
    }

    public static String getServiceStateString(int state, Resources res) {
        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                return res.getString(R.string.radioInfo_service_in);
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                return res.getString(R.string.radioInfo_service_out);
            case ServiceState.STATE_POWER_OFF:
                return res.getString(R.string.radioInfo_service_off);
            default:
                return res.getString(R.string.radioInfo_unknown);
        }
    }

}
