package com.dssathe.locationtracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    Button button;
    EditText userName;
    EditText host;
    TextView textView;
    LocationManager locationManager;
    Location currLocation;
    Boolean isTracking = true;
    Boolean start = false;
    String URL="http://10.13.49.108:9000/locationupdate";
    static final int ACCESS_FINE_LOCATION_PERMISSION=99;
    RequestQueue requestQueue;
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currLocation = location;
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addListenerOnButton();
        requestQueue = Volley.newRequestQueue(this);
        try{
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_PERMISSION);
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }catch (SecurityException e){

        }

    }

    private void addListenerOnButton() {
        button = (Button) findViewById(R.id.button);
        userName = (EditText) findViewById(R.id.editText);
        host= (EditText) findViewById(R.id.host);
        textView = (TextView) findViewById(R.id.textView);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (button.getText().equals("start tracking")) {
                    if(!host.getText().equals(""))
                        URL="http://"+host.getText()+"/locationupdate";
                    isTracking = true;
                    button.setText("stop tracking");
                    sendPostRequest(userName.getText().toString(), textView, button, true);
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            while (isTracking) {
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (isTracking)
                                    sendPostRequest(userName.getText().toString(), textView, button, false);
                            }
                        }
                    });
                } else {
                    isTracking = false;
                    button.setText("start tracking");

                }
            }

        });

    }

    private void sendPostRequest(final String Name, final TextView textView, final Button button, final boolean isStart) {

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("userName", Name);
            jsonBody.put("timeStamp", System.currentTimeMillis());
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);
            if (currLocation == null) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        ACCESS_FINE_LOCATION_PERMISSION);
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, locationListener);
                currLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if(currLocation!=null) {
                jsonBody.put("latitude", currLocation.getLatitude());
                jsonBody.put("longitude", currLocation.getLongitude());
            }else{
                jsonBody.put("latitude", 400);
                jsonBody.put("longitude", 400);
            }
            if(isStart){
                jsonBody.put("start",true);
            }
            final String mRequestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    JSONObject res=null;
                    try {
                        res=new JSONObject(response);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double distance=Double.parseDouble((String) res.get("body"));
                        distance=Double.valueOf(df.format(distance));
                        if(isStart)
                            textView.setText("Hi, "+Name+" Update Successful. Distance : "+ distance +"m \n");
                        else
                            textView.append("Hi, "+Name+" Update Successful. Distance : "+distance+"m \n");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), "Please enter the correct host name and restart tracking.", Toast.LENGTH_LONG).show();
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        return null;
                    }
                }
            };

            requestQueue.add(stringRequest);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
