package com.uag.micros.idrunkapp.request;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class VolleyRequester {
    private static VolleyRequester sInstance;

    private RequestQueue mRequestQueue;

    private VolleyRequester(Context aContext) {
        mRequestQueue = Volley.newRequestQueue(aContext);
    }

    public static VolleyRequester getInstance(Context aContext) {
        if (sInstance == null) {
            sInstance = new VolleyRequester(aContext);
        }
        return sInstance;
    }

    public void enqueueJSONRequest(JsonObjectRequest aJSONObjectRequest) {
        mRequestQueue.add(aJSONObjectRequest);
    }

    public void cancelRequest(String[] anArrayOfRequestsTags) {
        for (String tag : anArrayOfRequestsTags) {
            // cancel every request for tag
            mRequestQueue.cancelAll(tag);
        }
    }
}
