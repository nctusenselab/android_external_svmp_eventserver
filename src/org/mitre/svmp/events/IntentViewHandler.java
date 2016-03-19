/*
 Copyright 2013 The MITRE Corporation, All Rights Reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this work except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 License for the specific language governing permissions and limitations under
 the License.
 */
package org.mitre.svmp.events;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.IntentAction;
import org.mitre.svmp.protocol.SVMPProtocol.Request;
import org.mitre.svmp.protocol.SVMPProtocol.Response;
import org.mitre.svmp.protocol.SVMPProtocol.Response.ResponseType;

/** C->S: Receives intents from the client and starts activities accordingly
 * S->C: Receives intercepted Intent broadcasts, converts them to Protobuf
 * messages, and sends them to the client @author Joe Portner
 */
public class IntentViewHandler extends BaseHandler {
	private static final String TAG = IntentViewHandler.class.getName();

	public IntentViewHandler(BaseServer baseServer) {
		super(baseServer, INTENT_VIEW_ACTION);
		Log.d(TAG, "register!");
	}

	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "get" + intent.getAction());
		if(INTENT_VIEW_ACTION.equals(intent.getAction())) {
			Log.d(TAG, "get intent!!");
			Uri data = Uri.parse(intent.getStringExtra("data"));
			Log.d(TAG, data.toString());
			if(data.getHost().contains("youtube") || data.getHost().contains("youtu")) {
				Response response = buildIntentResponse(IntentAction.ACTION_VIEW.getNumber(), data.toString());
				if(response == null) Log.e(TAG, "Error converting intercepted youtube intent into a Protobuf message");
				else {
					Log.e(TAG, "Youtube intent passthrough to client");
					sendMessage(response);
			   	}
			}
		}
    }

    // attempt to convert intercepted intent values into a Protobuf message, return null if an error occurs
    private Response buildIntentResponse(int intentActionValue, String data) {
        // validate that we pulled the data we need from the intercepted intent
        if( intentActionValue > -1 && data != null ) {
            try {
                SVMPProtocol.Intent.Builder intentBuilder = SVMPProtocol.Intent.newBuilder();
                intentBuilder.setAction(IntentAction.valueOf(intentActionValue));
                intentBuilder.setData(data);

                Response.Builder responseBuilder = Response.newBuilder();
                responseBuilder.setType(ResponseType.INTENT);
                responseBuilder.setIntent(intentBuilder);
                return responseBuilder.build();
            } catch( Exception e ) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
