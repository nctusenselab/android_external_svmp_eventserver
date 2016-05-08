package org.mitre.svmp.events;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * @author FatMinMin
 */
public class IntentCaptureActivity extends Activity implements Constants {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		    notifyHandler();
		    finish();
    }
    @Override
    public void onResume() {
        super.onResume();
        //notifyHandler();
    }

    @Override
    public void onBackPressed() {
        notifyHandler();
    }

    private void notifyHandler() {
        Log.d(IntentCaptureActivity.class.getName(), "Sending ACTION_VIEW_ACTION broadcast");
        Intent intent = new Intent(INTENT_VIEW_ACTION);
		    intent.putExtra("data", getIntent().getData().toString());
		    Log.d(IntentCaptureActivity.class.getName(), intent.getAction());
        sendBroadcast(intent);
    }
}
