package org.tensorflow.demo.vision_module;

import android.graphics.Bitmap;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class OcrRequest extends JsonObjectRequest {
    private static final String REQUEST_URL = IpPath.WEBIP + "/ocr";
    private byte[] ocrImage;

    public OcrRequest(Bitmap bitmap, Response.Listener<JSONObject> listener) {
        super(Method.POST, REQUEST_URL,null, listener, null);
        this.ocrImage = etcToolBox.bitmapToByteArray(bitmap);
    }



    @Override
    public byte[] getBody() {
        JSONObject j = new JSONObject();
        try {
            j.put("data",new JSONArray(ocrImage));
            //j.put("data",Base64.encodeToString(ocrImage,0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return j.toString().getBytes();
    }
}
