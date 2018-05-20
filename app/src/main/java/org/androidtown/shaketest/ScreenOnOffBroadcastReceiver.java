package org.androidtown.shaketest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

public class ScreenOnOffBroadcastReceiver extends BroadcastReceiver {

    private final String SCREEN_ON = "android.intent.action.USER_PRESENT";
    private final String SCREEN_OFF = "android.intent.action.SCREEN_OFF";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SCREEN_ON)) {
            Log.d("MyReceiver", "OnScreen ");
            ServiceApplication.service_flag = true;
        } else if (intent.getAction().equals(SCREEN_OFF)) {
            Log.e("MyReceiver", "OffScreen");
            ServiceApplication.service_flag = false;
        }
    }

    public IntentFilter getFilter () {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(SCREEN_OFF);
        return filter;
    }
}
