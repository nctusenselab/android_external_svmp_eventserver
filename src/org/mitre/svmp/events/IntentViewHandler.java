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

// file forwarding
import android.net.Uri;
import com.google.protobuf.ByteString;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/** C->S: Receives intents from the client and starts activities accordingly
 * S->C: Receives intercepted Intent broadcasts, converts them to Protobuf
 * messages, and sends them to the client @author Joe Portner
 */
public class IntentViewHandler extends BaseHandler {
	private static final String TAG = IntentViewHandler.class.getName();
	private Context mContext;

	public IntentViewHandler(BaseServer baseServer) {
			super(baseServer, INTENT_VIEW_ACTION);
			Log.d(TAG, "register!");
	}

	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "get" + intent.getAction());
		mContext = context;
		if(INTENT_VIEW_ACTION.equals(intent.getAction())) {
			Log.d(TAG, "get intent!!");
			Uri data = Uri.parse(intent.getStringExtra("data"));
			Log.d(TAG, data.toString());
			SVMPProtocol.File.Builder f = null;
			if(data.getScheme().equals("file")) {
				f = SVMPProtocol.File.newBuilder();
				f.setFilename(data.getLastPathSegment());
				f.setData(getByteString(data));
			}
			Response response = buildIntentResponse(IntentAction.ACTION_VIEW.getNumber(), data.toString(), f);
			if(response == null) Log.e(TAG, "Error converting intercepted youtube intent into a Protobuf message");
			else {
				sendMessage(response);
			}
		}
  }

	private ByteString getByteString(Uri uri) {
		try {
			InputStream iStream = mContext.getContentResolver().openInputStream(uri);
			byte[] inputData = getBytes(iStream);
			return ByteString.copyFrom(inputData);
		} catch(Exception e) {}
		return null;
	}
	private byte[] getBytes(InputStream inputStream) throws Exception {
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
		  byteBuffer.write(buffer, 0, len);
		}
		return byteBuffer.toByteArray();
	}

  // attempt to convert intercepted intent values into a Protobuf message, return null if an error occurs
  private Response buildIntentResponse(int intentActionValue, String data, SVMPProtocol.File.Builder file) {
      // validate that we pulled the data we need from the intercepted intent
      if( intentActionValue > -1 && data != null ) {
          try {
              SVMPProtocol.Intent.Builder intentBuilder = SVMPProtocol.Intent.newBuilder();
              intentBuilder.setAction(IntentAction.valueOf(intentActionValue));
              intentBuilder.setData(data);
			  if(file != null) {
				  intentBuilder.setFile(file);
			  }

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
