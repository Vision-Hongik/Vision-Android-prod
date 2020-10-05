package org.tensorflow.demo.vision_module;

import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MapRequest extends JsonArrayRequest {
    //web 주소
    private static final String REQUEST_URL = IpPath.WEBIP + "/mapdata/";
    private static JSONObject jsonBody = new JSONObject();

    // string,string 해쉬맵
    private String text;


    //생성자
    public MapRequest(String mapURL, Response.Listener<JSONArray> listener) {
        //post형식으로 전송
        super(Method.GET,REQUEST_URL + mapURL,null,listener,null);
        this.text = text;
    }


    @Override
    public String getBodyContentType() {
        return super.getBodyContentType();
    }

    @Override
    public byte[] getBody() {
        JSONObject j = new JSONObject();
        try {
            j.put("android",this.text);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return j.toString().getBytes();
    }

}
