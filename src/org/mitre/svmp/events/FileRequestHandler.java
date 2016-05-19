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

import android.net.Uri;
import com.google.protobuf.ByteString;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class FileRequestHandler {
    private static final String TAG = FileRequestHandler.class.getName();

    private BaseServer baseServer;
    private Context mContext;

    public FileRequestHandler(BaseServer baseServer) {
        this.baseServer = baseServer;
        mContext = baseServer.getContext();
    }

    public void handleMessage(Request request) {
        if( request.hasFileRequest() ) {
            SVMPProtocol.FileRequest fileRequest = request.getFileRequest();
            SVMPProtocol.File.Builder f = SVMPProtocol.File.newBuilder();
            String filePath = "/sdcard/" + fileRequest.getFilename();
            f.setFilename(fileRequest.getFilename());
            f.setData(getByteString(Uri.fromFile(new File(filePath))));
            Response response = buildFileResponse(f);
            if(response != null) baseServer.sendMessage(response);
        }
    }
    private Response buildFileResponse(SVMPProtocol.File.Builder file) {
        try {
            Response.Builder responseBuilder = Response.newBuilder();
            responseBuilder.setType(ResponseType.FILE);
            responseBuilder.setFile(file);
            return responseBuilder.build();
        } catch( Exception e ) {
            e.printStackTrace();
        }

        return null;
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

}
