package com.mikiloz.meat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.os.Build;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.mikiloz.meat.data.Place;
import com.mikiloz.meat.data.Tweet;
import com.mikiloz.meat.utils.HttpUtilities;
import com.mikiloz.meat.utils.LanguageContextWrapper;
import com.mikiloz.meat.utils.ScrollableMapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final int GPS_REQUEST_CODE = 1;
    private static final int MAP_SEARCH_RADIUS = 500;

    private final List<Place> places = new ArrayList<>();

    private String languageCode = "es";


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageContextWrapper.wrap(newBase, languageCode));
        System.out.println("Attaching to " + languageCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        languageCode = (savedInstanceState != null) ? savedInstanceState.getString("language") : "es";
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

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

        LinearLayout twitterLayout = (LinearLayout) findViewById(R.id.twitter_layout);
        twitterLayout.setVisibility(View.VISIBLE);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_language) {
            return true;
        } else if (id == R.id.action_language_english) {
            Intent intent = getIntent();
            intent.putExtra("language", "en");
            finish();
            startActivity(intent);
        } else if (id == R.id.action_language_spanish) {
            Intent intent = getIntent();
            intent.putExtra("language", "es");
            finish();
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void setLocale(String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        Intent refresh = new Intent(this, MainActivity.class);
        startActivity(refresh);
        finish();
    }

    private void changeLocale(String localeCode) {
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            getApplicationContext().getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        recreate();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        map.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/

        MapsInitializer.initialize(this);
        mapView.onResume();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_REQUEST_CODE);
            return;
        }

        onConnectedAndPermissionsGranted();

        /*LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        try {
        } catch (SecurityException ex) {
            final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getString(R.string.alert_location_permission_title));
            alertDialog.setMessage(getString(R.string.alert_location_permission));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
        }*/


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) onConnectedAndPermissionsGranted();
    }

    private void onConnectedAndPermissionsGranted() {
        try {

            // Map
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            double lat = 0, lon = 0;
            if (lastLocation != null) {
                lat = lastLocation.getLatitude();
                lon = lastLocation.getLongitude();
            }
            System.out.println("Last location: " + lat + ", " + lon);
            map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
            if (lastLocation != null) populateMap();

            // Tweets
            if (lastLocation != null) {
                final RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                HttpUtilities.Twitter.requestBearerToken(queue, getString(R.string.twitter_api_consumer_key), getString(R.string.twitter_api_secret_key), new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            String bearerToken = new JSONObject(response).getString("access_token");
                            System.out.println("BEARER: " + bearerToken);
                            HttpUtilities.Twitter.searchTweets(queue, getString(R.string.meat_hashtag), bearerToken, new Response.Listener<String>() {
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
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    System.out.println("ERROR");
                                    System.out.println(new String(error.networkResponse.data));
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
            }
        } catch (SecurityException ex) {
            showGPSPermissionAlertDialog();
        }
    }

    private void showGPSPermissionAlertDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.alert_location_permission_title));
        alertDialog.setMessage(getString(R.string.alert_location_permission));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void populateMap() {
        RequestParams rp = new RequestParams();
        rp.add("location", String.valueOf(lastLocation.getLatitude()) + "," + String.valueOf(lastLocation.getLongitude()));
        //rp.add("location", "41.3736825,2.1124893"); // false location :-B
        rp.add("radius", String.valueOf(MAP_SEARCH_RADIUS));
        rp.add("types", "convenience_store|grocery_or_supermarket|store|shopping_mall");
        rp.add("name", "carniceria");
        rp.add("key", getString(R.string.google_maps_api_key));
        HttpUtilities.get(PLACES_BASE_URL, rp, new PlacesJSONHandler());
    }

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



            /*Twitter twitter = TwitterFactory.getSingleton();
            Query query = new Query("source:twitter4j yusukey");
            QueryResult result = twitter.search(query);
            TwitterListener listener = new TwitterAdapter() {
                @Override
                public void searched(QueryResult queryResult) {
                    super.searched(queryResult);
                }

                @Override public void updatedStatus(Status status) {
                    System.out.println("Successfully updated the status to [" +
                            status.getText() + "].");
                }
            };
            // The factory instance is re-useable and thread safe.
            AsyncTwitterFactory factory = new AsyncTwitterFactory();
            AsyncTwitter asyncTwitter = factory.getInstance();
            asyncTwitter.addListener(listener);
            asyncTwitter.updateStatus(args[0]);*/
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
}

