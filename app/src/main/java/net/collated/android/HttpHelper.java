package net.collated.android;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class HttpHelper {

    private static final String TAG = HttpHelper.class.getSimpleName();

    public static String BASE_URL = "https://app.collated.net/api/v1/items/android";
    private static final int MY_SOCKET_TIMEOUT_MS = 30000;
    RequestQueue queue;
    Context ctx;

    public HttpHelper(Context ctx) {
        this.ctx = ctx;
        queue = VolleySingleton.getInstance(ctx.getApplicationContext()).getRequestQueue();
    }


    public interface VolleyCallback {
        void onVolleyResult(int calltype, String result, VolleyError error);
    }



    public void postShareInfo(final JSONObject shareInfo, final VolleyCallback callback) {

        String loginUrl = BASE_URL;

        StringRequest jsonObjRequest = new StringRequest(com.android.volley.Request.Method.POST, loginUrl, new com.android.volley.Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.i(TAG, "Response is: " + response.toString());
                callback.onVolleyResult(123, response, null);

            }
        }, new com.android.volley.Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Error is: " + error.getMessage());

                callback.onVolleyResult(123, null, error);

            }
        })
        {

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return shareInfo.toString() == null ? null : shareInfo.toString().getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", shareInfo.toString(), "utf-8");
                    return null;
                }
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {

                    responseString = String.valueOf(response.statusCode);

                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }


            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                return getStandardAuthHeaders();
            }

        };
        jsonObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleySingleton.getInstance(ctx).addToRequestQueue(jsonObjRequest);

    }



    //================================================================//
    /** standard authorization headers to be attached to all requests */
    //================================================================//
    private static Map<String,String> getStandardAuthHeaders() {

        HashMap<String, String> params = new HashMap<>();
        params.put("Content-Type", "application/json; charset=utf-8");
        params.put("Access-Control-Allow-Headers", "*");
        return params;

    }
}
