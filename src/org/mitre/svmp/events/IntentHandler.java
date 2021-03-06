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

import com.google.protobuf.ByteString;
import java.io.FileOutputStream;
import java.io.File;
import android.webkit.MimeTypeMap;

import java.util.Date;
import java.text.SimpleDateFormat;

/** C->S: Receives intents from the client and starts activities accordingly
 * S->C: Receives intercepted Intent broadcasts, converts them to Protobuf
 * messages, and sends them to the client @author Joe Portner
 */
public class IntentHandler extends BaseHandler {
	private static final String TAG = IntentHandler.class.getName();

	public IntentHandler(BaseServer baseServer) {
		super(baseServer, Intent.ACTION_NEW_OUTGOING_CALL);
	}

	public void onReceive(Context context, Intent intent) {
		// validate the action of the broadcast (ACTION_NEW_OUTGOING_CALL is a
		// protected broadcast)
		if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction()) &&
				intent.hasExtra(Intent.EXTRA_PHONE_NUMBER)) {
			// pull relevant data from the intercepted intent
			String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			Log.v(TAG, "Intercepted outgoing call");

			// attempt to build the Protobuf message
			Response response =
				buildIntentResponse(IntentAction.ACTION_DIAL.getNumber(),
						"tel:" + number);

			// if we encountered an error, log it; otherwise, send the Protobuf
			// message
			if( response == null ) Log.e(TAG, "Error converting intercepted intent into a Protobuf message");
			else sendMessage(response);

            // null out the result data so the system doesn't try to place the call
            setResultData(null);
        }
    }

	protected void saveToFile(SVMPProtocol.File f) {
		try {
			ByteString bs = f.getData();
			byte[] arr = bs.toByteArray();
			FileOutputStream out = new FileOutputStream("/sdcard/" + f.getFilename());
			out.write(arr);
			out.close();
		} catch(Exception e) {}
	}

    // receive messages from the client and pass them to the appropriate Android component
    protected void handleMessage(Request request) {
        if( request.hasIntent() ) {

			String timeStamp = new SimpleDateFormat("HH.mm.ss.SS").format(new Date());
			Log.d(TAG, "Forwarding intent. Timestamp: " + timeStamp + " " + System.currentTimeMillis());

            SVMPProtocol.Intent intentRequest = request.getIntent();
			Intent intent = new Intent();
            if(intentRequest.getAction().equals(SVMPProtocol.IntentAction.ACTION_VIEW)) {
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(intentRequest.getData()));
            } else if(intentRequest.getAction().equals(SVMPProtocol.IntentAction.ACTION_SEND)) {
				intent.setAction(Intent.ACTION_SEND);
			}
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if( intentRequest.hasFile() ) {
				SVMPProtocol.File f = intentRequest.getFile();
				Log.e(TAG, "Receiving a file with filename: " + f.getFilename());
				saveToFile(f);
				File savedFile = new File("/sdcard/" + f.getFilename());

				int idx = savedFile.getName().lastIndexOf('.');
				String type = "*/*";
				if (idx > 0) {
					String extension = savedFile.getName().substring(idx+1);
					type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
				}
				Log.e(TAG, "MIME type is " + type);

				if(intentRequest.getAction().equals(SVMPProtocol.IntentAction.ACTION_VIEW)) {
					intent.setDataAndType(Uri.fromFile(savedFile), type);
	            } else if(intentRequest.getAction().equals(SVMPProtocol.IntentAction.ACTION_SEND)) {
					intent.setType(type);
					intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(savedFile));
				}
			}
			baseServer.getContext().startActivity(intent);
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
