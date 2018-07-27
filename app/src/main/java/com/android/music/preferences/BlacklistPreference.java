package com.android.music.preferences;

import android.content.Context;
import android.util.AttributeSet;

import com.kabouzeid.appthemehelper.common.prefs.supportv7.ATEDialogPreference;

public class BlacklistPreference extends ATEDialogPreference {
    public BlacklistPreference(Context context) {
        super(context);
    }

    public BlacklistPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlacklistPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BlacklistPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}