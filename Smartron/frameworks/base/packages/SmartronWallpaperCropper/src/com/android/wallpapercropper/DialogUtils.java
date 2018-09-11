package com.android.wallpapercropper;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;


/**
 * Utility class used to show dialogs for things like picking which wallpaper to set.
 */
public class DialogUtils {

    public static void executeWallpaperTypeSelection(
            Context context,
            DialogInterface.OnCancelListener onCancelListener, final DialogInterfaceClickListener dialogInterfaceClicked) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.wallpaper_instructions)
                .setItems(R.array.which_wallpaper_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedItemIndex) {
                        int whichWallpaper;
                        if (selectedItemIndex == 0) {
                            whichWallpaper = WallpaperManager.FLAG_SYSTEM;
                        } else if (selectedItemIndex == 1) {
                            whichWallpaper = WallpaperManager.FLAG_LOCK;
                        } else {
                            whichWallpaper = WallpaperManager.FLAG_SYSTEM
                                    | WallpaperManager.FLAG_LOCK;
                        }
                        dialogInterfaceClicked.onWallpaperTypeSelected(whichWallpaper);
                    }
                })
                .setOnCancelListener(onCancelListener)
                .show();
    }

    public static interface DialogInterfaceClickListener {
        public void  onWallpaperTypeSelected(int wallpaperType);
    }


}
