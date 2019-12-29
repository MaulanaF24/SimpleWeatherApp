package com.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.CursorResult;
import com.raizlabs.android.dbflow.sql.queriable.StringQuery;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;
import com.raizlabs.android.dbflow.structure.database.transaction.ProcessModelTransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.QueryTransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.Transaction;
import com.weatherapp.ApiService.APIClient;
import com.weatherapp.ApiService.APIInterfacesRest;
import com.weatherapp.WeatherModel.Example;
import com.weatherapp.WeatherModel.Main;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_LOCATION_CODE = 99;
    double latitude,longitude;
    TextView temperature,cityname,weathername,day;
    EditText etCity;
    ImageView weatherimg;
    ImageButton btnSearch;
    Example example;
    SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperature = findViewById(R.id.temperature);
        cityname = findViewById(R.id.cityname);
        weathername = findViewById(R.id.weathername);
        weatherimg = findViewById(R.id.weatherimg);
        etCity = findViewById(R.id.etCity);
        btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchData(etCity.getText().toString());
            }
        });
        day = findViewById(R.id.day);

        swipeRefresh = findViewById(R.id.swipeRefresh);

        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                etCity.setText("");
                getCurrentLocation();
                swipeRefresh.setRefreshing(false);
            }
        });

        etCity.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent keyevent) {
                //If the keyevent is a key-down event on the "enter" button
                if ((keyevent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    //...
                    // Perform your action on key press here
                    // ...
                    searchData(etCity.getText().toString());
                    return true;
                }
                return false;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        ForceTurnOnLocation();
        getCurrentLocation();


    }

    public boolean checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED )
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
            }
            else
            {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
            }
            return false;
        }
        else
            return true;
    }

    private void ForceTurnOnLocation(){
        LocationManager locationManager = (LocationManager) MainActivity.this.getSystemService(Context.LOCATION_SERVICE);
        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
            new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle("Layanan GPS Mati")  // GPS not found
                    .setMessage("Mohon nyalakan layanan GPS Anda") // Want to enable?
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .show();
        }
    }

    public void getCurrentLocation(){
        FusedLocationProviderClient mFusedLocation = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocation.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null){

                    Log.d("My Current location", "Lat : " + location.getLatitude() + " Long : "
                            + location.getLongitude());

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    callData(latitude,longitude);
                }
            }
        });
    }

    APIInterfacesRest apiInterface;
    ProgressDialog progressDialog;
    private void callData(double lat,double lon){
        apiInterface = APIClient.getClientWithApi().create(APIInterfacesRest.class);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Call<Example> mulaiReq = apiInterface.getWeatherData(lat,lon);
        mulaiReq.enqueue(new Callback<Example>() {
            @Override
            public void onResponse(Call<Example> call, Response<Example> response) {
                progressDialog.dismiss();
                Example example = response.body();

                if(example != null){
                    setData(example);
                } else {
                    Toast.makeText(MainActivity.this,"City not found", Toast.LENGTH_SHORT).show();
                }
                //savedb();
            }

            @Override
            public void onFailure(Call<Example> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Maaf koneksi bermasalah", Toast.LENGTH_LONG).show();
                call.cancel();
            }
        });

    }

    public void searchData (String city){
        apiInterface = APIClient.getClientWithApi().create(APIInterfacesRest.class);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Call<Example> mulaiReq = apiInterface.getCityData(city);
        mulaiReq.enqueue(new Callback<Example>() {
            @Override
            public void onResponse(Call<Example> call, Response<Example> response) {
                progressDialog.dismiss();
                Example example = response.body();

                if(example != null){
                    setData(example);

                } else {
                    Toast.makeText(MainActivity.this,"City not found", Toast.LENGTH_SHORT).show();
                }
                //savedb();
            }

            @Override
            public void onFailure(Call<Example> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Maaf koneksi bermasalah", Toast.LENGTH_LONG).show();
                call.cancel();
            }
        });
    }

    public void setData (Example example){
        String temp = String.valueOf(Math.round(example.getMain().getTemp()-273));
        temperature.setText(temp + "℃");
        cityname.setText(example.getName());
        weathername.setText(example.getWeather().get(0).getMain());

        switch (example.getWeather().get(0).getIcon().substring(2)){
            case"d":
                day.setText("Day");
                break;

            case"n":
                day.setText("Night");
                break;
        }


        switch (example.getWeather().get(0).getIcon()){
            case "01d":
            case "01n":
                weatherimg.setImageDrawable(getResources().getDrawable(R.drawable.cerah));
                break;

            case "02d":
            case "02n":
                weatherimg.setImageDrawable(getResources().getDrawable(R.drawable.berawan));
                break;

            case "03d":
            case "03n":
            case "04d":
            case "04n":
                weatherimg.setImageDrawable(getResources().getDrawable(R.drawable.mendung));
                break;

            case "09d":
            case "09n":
            case "10d":
            case "10n":
                weatherimg.setImageDrawable(getResources().getDrawable(R.drawable.hujan));
                break;

            case "11d":
            case "11n":
                weatherimg.setImageDrawable(getResources().getDrawable(R.drawable.badai));
                break;

            case "13d":
            case "13n":
                weatherimg.setImageDrawable(getResources().getDrawable(R.drawable.bersalju));
                break;

            case "50d":
            case "50n":
                weatherimg.setImageDrawable(getResources().getDrawable(R.drawable.berangin));
                break;
        }
    }

    public void savedb() {
        FlowManager.getDatabase(AppController.class)
                .beginTransactionAsync(new ProcessModelTransaction.Builder<>(
                        new ProcessModelTransaction.ProcessModel<Example>() {
                            @Override
                            public void processModel(Example example, DatabaseWrapper wrapper) {
                                example.save();
                            }

                        }).addAll(example).build())  // add elements (can also handle multiple)
                .error(new Transaction.Error() {
                    @Override
                    public void onError(Transaction transaction, Throwable error) {
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .success(new Transaction.Success() {
                    @Override
                    public void onSuccess(Transaction transaction) {
                        Toast.makeText(getApplicationContext(), "Data Tersimpan", Toast.LENGTH_LONG).show();
                        sqlQueryList();
                    }
                }).build().execute();


    }

    public void sqlQueryList() {

        String rawQuery = "SELECT * FROM `Transaksi` ";
        StringQuery<Example> stringQuery = new StringQuery<>(Example.class, rawQuery);

        stringQuery.async().queryResultCallback(new QueryTransaction.QueryResultCallback<Example>() {
            @Override
            public void onQueryResult(@NonNull QueryTransaction<Example> transaction, @NonNull CursorResult<Example> tResult) {
                //setData(transaction);
            }
        }).execute();
    }

    long back_pressed;

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()){
            super.onBackPressed();
        }
        else{
            Toast.makeText(getBaseContext(),
                    "Press once again to exit!", Toast.LENGTH_SHORT)
                    .show();
        }
        back_pressed = System.currentTimeMillis();
    }
}
