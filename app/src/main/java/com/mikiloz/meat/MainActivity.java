package com.mikiloz.meat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mikiloz.meat.data.Place;
import com.mikiloz.meat.data.Tweet;
import com.mikiloz.meat.utils.HttpUtilities;
import com.mikiloz.meat.utils.ScrollableMapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private MapView mapView;
    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private RecyclerView tweetsRecyclerView;
    private TweetsAdapter tweetsAdapter;

    private static final String PLACES_BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    private static final String MEAT_SHOP_SEARCH_TOKEN = "carniceria";
    private static final int GPS_REQUEST_CODE = 1;
    private static final int MAP_SEARCH_RADIUS = 500;

    private final List<Place> places = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // Create an instance of GoogleAPIClient
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mapView = (ScrollableMapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        tweetsRecyclerView = (RecyclerView) findViewById(R.id.twitter_recyclerview);
        tweetsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        requestTweets();
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Swap languages based on the user selection
        if (id == R.id.action_language) {
            return true;
        } else if (id == R.id.action_language_english) {
            setLocale("en");
        } else if (id == R.id.action_language_spanish) {
            setLocale("es");
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Request tweets from the Twitter API, using the {@link HttpUtilities} utility class.
     */
    private void requestTweets() {
        final RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        HttpUtilities.Twitter.requestBearerToken(queue,
                getString(R.string.twitter_api_consumer_key),
                getString(R.string.twitter_api_secret_key),
                new BearerTokenResponseListener(queue),
                new TwitterErrorResponseListener());
    }

    /**
     * Set the locale for the entire application and restart the Activity to see the changes.
     * @param lang The language locale code (2 letter code, like "es" or "en").
     */
    private void setLocale(String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        Intent refresh = new Intent(this, MainActivity.class);
        finish();
        startActivity(refresh);
    }

    /**
     * Called when the {@link GoogleMap} has finished loading.
     * @param googleMap The {@link GoogleMap} instance.
     */
    @Override public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        MapsInitializer.initialize(this);
        mapView.onResume();
    }

    /**
     * Called when the application is done connecting to the Google API.
     */
    @Override public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_REQUEST_CODE);
            return;
        }

        onGoogleApiConnectedAndPermissionsGranted();

    }

    /**
     * Called when the application has suspended the process of connecting to the Google API.
     */
    @Override public void onConnectionSuspended(int i) {

    }

    /**
     * Called when the application has failed connecting to the Google API.
     */
    @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * Called when the permissions request's result has been obtained.
     * @param requestCode The request code.
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) onGoogleApiConnectedAndPermissionsGranted();
    }

    /**
     * Called when there is connection, permissions granted, and it's now possible to make requests
     * to the Google API.
     */
    private void onGoogleApiConnectedAndPermissionsGranted() {
        try {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            double lat = 0, lon = 0;
            if (lastLocation != null) {
                lat = lastLocation.getLatitude();
                lon = lastLocation.getLongitude();
            }
            System.out.println("Last location: " + lat + ", " + lon);
            map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
            if (lastLocation != null) populateMap();
        } catch (SecurityException ex) {
            showGPSPermissionAlertDialog();
        }
    }

    /**
     * Show the GPS alert dialog
     */
    private void showGPSPermissionAlertDialog() {
        showAlertDialog(getString(R.string.alert_location_permission_title), getString(R.string.alert_location_permission));
    }

    /**
     * Show the Twitter error alert dialog
     */
    private void showTwitterErrorAlertDialog() {
        showAlertDialog(getString(R.string.alert_twitter_title), getString(R.string.alert_twitter));
    }

    /**
     * Show an alert dialog
     * @param title The title of the dialog
     * @param message The message of the dialog
     */
    private void showAlertDialog(String title, String message) {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    /**
     * Populate the {@link MapView} with some nearby meat shops.
     */
    private void populateMap() {
        HttpUtilities.Places.searchMeatShops(lastLocation.getLatitude(), lastLocation.getLongitude(),
                 MAP_SEARCH_RADIUS, MEAT_SHOP_SEARCH_TOKEN, getString(R.string.google_api_key),
                new PlacesJSONHandler());
    }

    /**
     * A JSON handler that will populate the map with different meat shops
     */
    private class PlacesJSONHandler extends JsonHttpResponseHandler {
        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            super.onSuccess(statusCode, headers, response);
            try {
                JSONArray results = response.getJSONArray("results");
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                LatLngBounds bounds;
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    double lat = result.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                    double lon = result.getJSONObject("geometry").getJSONObject("location").getDouble("lng");
                    String name = result.getString("name");
                    places.add(new Place(name, lat, lon));
                    LatLng latlng = new LatLng(lat, lon);
                    map.addMarker(new MarkerOptions().position(latlng).title(name));
                    builder.include(latlng);
                    System.out.println(name + "(" + lat + ", " + lon + ")");
                }
                bounds = builder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);
                map.moveCamera(cu);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            System.out.println("Response: " + response.toString());
        }
    }

    /**
     * RecyclerView adapter that will populate the twitter list with fresh, new tweets about meat.
     */
    private class TweetsAdapter extends RecyclerView.Adapter<TweetsAdapter.ViewHolder> {

        List<Tweet> tweets = new ArrayList<>();

        public TweetsAdapter(List<Tweet> tweets) {
            this.tweets = tweets;
        }

        @Override
        public TweetsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet, parent, false);

            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(TweetsAdapter.ViewHolder holder, int position) {
            Tweet tweet = tweets.get(position);
            String text = "@" + tweet.user + ": " + tweet.tweet;
            final SpannableStringBuilder str = new SpannableStringBuilder(text);
            str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, tweet.user.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.text.setText(str);
        }

        @Override
        public int getItemCount() {
            return tweets.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView text;

            public ViewHolder(View itemView) {
                super(itemView);
                text = (TextView) itemView.findViewById(R.id.text);
            }

        }
    }

    private class BearerTokenResponseListener implements Response.Listener<String> {

        private RequestQueue queue;
        public BearerTokenResponseListener(RequestQueue queue) {
            this.queue = queue;
        }

        @Override
        public void onResponse(String response) {
            try {
                String bearerToken = new JSONObject(response).getString("access_token");
                System.out.println("BEARER: " + bearerToken);
                HttpUtilities.Twitter.searchTweets(queue, getString(R.string.meat_hashtag),
                        bearerToken, new TweetsResponseListener(),
                        new TwitterErrorResponseListener());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class TwitterErrorResponseListener implements Response.ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {
            showTwitterErrorAlertDialog();
        }
    }

    private class TweetsResponseListener implements Response.Listener<String> {

        @Override
        public void onResponse(String response) {
            try {
                JSONArray statuses = new JSONObject(response).getJSONArray("statuses");
                List<Tweet> tweets = new ArrayList<>();
                for (int i = 0; i < statuses.length(); i++) {
                    JSONObject status = statuses.getJSONObject(i);
                    String text = status.getString("text");
                    String user = status.getJSONObject("user").getString("screen_name");
                    System.out.println("@" + user + ": " + text);
                    tweets.add(new Tweet(user, text));
                }
                tweetsAdapter = new TweetsAdapter(tweets);
                tweetsRecyclerView.setAdapter(tweetsAdapter);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}

