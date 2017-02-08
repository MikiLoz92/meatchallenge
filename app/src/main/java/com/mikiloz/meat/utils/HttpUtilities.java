package com.mikiloz.meat.utils;

import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


// Created by Miguel Vera Belmonte on 04/02/2017.
public abstract class HttpUtilities {

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String baseURL, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(baseURL, params, responseHandler);
    }

    public static class Twitter {

        private static String encodeKeys(String consumerKey, String consumerSecret) {
            try {
                String encConsumerKey = URLEncoder.encode(consumerKey, "UTF-8");
                System.out.println(encConsumerKey);
                String encConsumerSecret = URLEncoder.encode(consumerSecret, "UTF-8");
                System.out.println(encConsumerSecret);
                String fullKey = encConsumerKey + ":" + encConsumerSecret;
                byte[] encBytes = Base64.encode(fullKey.getBytes(), Base64.NO_WRAP);
                return new String(encBytes);
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }

        public static void requestBearerToken(RequestQueue queue, String consumerKey, String secretKey,
                                              Response.Listener<String> responseListener,
                                              Response.ErrorListener errorListener) {

            final String encodedCredentials = encodeKeys(consumerKey, secretKey);
            String url = "https://api.twitter.com/oauth2/token";
            StringRequest postRequest = new StringRequest(Request.Method.POST, url, responseListener, errorListener) {
                @Override public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String>  params = new HashMap<>();
                    params.put("Host", "api.twitter.com");
                    params.put("User-Agent", "MeatChallenge");
                    params.put("Authorization", "Basic " + encodedCredentials);
                    params.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                    return params;
                }
                @Override public byte[] getBody() throws AuthFailureError {
                    return "grant_type=client_credentials".getBytes();
                }
            };
            queue.add(postRequest);

        }

        public static void searchTweets(RequestQueue queue, String meatString, final String bearerToken,
                                        Response.Listener<String> responseListener,
                                        Response.ErrorListener errorListener) {

            String lang = Locale.getDefault().getLanguage();
            String url = "https://api.twitter.com/1.1/search/tweets.json?q=" + meatString + "&lang=" + lang;
            StringRequest getRequest = new StringRequest(Request.Method.GET, url, responseListener, errorListener) {
                @Override public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String>  headers = new HashMap<>();
                    headers.put("Host", "api.twitter.com");
                    headers.put("User-Agent", "MeatChallenge");
                    headers.put("Authorization", "Bearer " + bearerToken);
                    return headers;
                }
            };
            queue.add(getRequest);
        }

    }


}
