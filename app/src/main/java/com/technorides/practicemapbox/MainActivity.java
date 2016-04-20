package com.technorides.practicemapbox;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.mapbox.geocoder.android.AndroidGeocoder;
import com.mapbox.geocoder.service.models.GeocoderFeature;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements android.location.LocationListener, MapboxMap.OnMapClickListener,
                                                                    View.OnClickListener{

    public static final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoibW9pc2VzMDciLCJhIjoiY2luMjNjZWMzMGI5MnY3a2tkZ25udHJoMCJ9.SSZYVKUPTtwPZmolPq0xNw";
    private double latitudeDefault = -34.603633;
    private double longitudeDefault = -58.380809;
    private FrameLayout mainContainer;
    private Toolbar toolbar;
    private MapView mapView;
    private LocationManager locationManager;
    private Location location;
    public MapboxMap mainMapBoxMap;

    private AutoCompleteTextView actvSearchDirection;
    private Button btnSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setup();
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                // Set map style
                mainMapBoxMap = mapboxMap;
                //mainMapBoxMap.setOnMapClickListener(MainActivity.this);
                mapboxMap.setStyleUrl(Style.MAPBOX_STREETS);

                // Set the camera's starting position
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(latitudeDefault, longitudeDefault)) // set the camera's center position
                        .zoom(12)  // set the camera's zoom level
                        .tilt(45)  // set the camera's tilt
                        .build();

                // Move the camera to that position
                mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                mapboxMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitudeDefault, longitudeDefault))
                        .title("Hello World!")
                        .snippet("Welcome Technorides"));
            }
        });

        setupFloating();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Snackbar.make(mainContainer, "REQUEST PERMISSIONS", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 0, this);
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    private void setup(){
        mainContainer = (FrameLayout) findViewById(R.id.containerMain);
        mapView = (MapView) findViewById(R.id.mapBoxTechno);
        actvSearchDirection = (AutoCompleteTextView)findViewById(R.id.actvSearchDirection);
        btnSearch = (Button)findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);
    }

    @Override
    public void onClick(View v){
        if(btnSearch.getId() == v.getId()){
            searchAddress(actvSearchDirection.getText().toString());
        }
    }

    private void setupFloating(){
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView)MenuItemCompat.getActionView(searchItem);
        if(searchView != null){
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    Toast.makeText(getApplicationContext(), "Searching " + query, Toast.LENGTH_LONG).show();
                    //searchAddress(query);
                    new SearchAddress().execute(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_current_location:
                showCurrentLocation();
                return true;
            case R.id.action_current_direction:
                //new SearchDirection().execute();
                searchCurrentDirection();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCurrentLocation(){
        try{
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && location != null){
                this.mainMapBoxMap.addMarker(new MarkerOptions()
                        .position(new LatLng(location.getLatitude(), location.getLongitude()))
                        .title("I'm here!")
                        .snippet("Welcome Moises")
                        .icon(getCustomIcon(R.mipmap.ic_details_pinpickup)));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude())) // set the camera's center position
                        .zoom(12)  // set the camera's zoom level
                        .tilt(45)  // set the camera's tilt
                        .build();

                // Move the camera to that position
                mainMapBoxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                Snackbar.make(mainContainer, "Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }else{
                Snackbar.make(mainContainer, "GPS DISABLED", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void searchAddress(String textAddress){
        List<Address> addressList = null;
        double latitude = latitudeDefault;
        double longitude = longitudeDefault;
        if(location == null){
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }

        final double lLat = latitude - 1;
        final double rLat = latitude + 1;
        final double lLong = longitude - 1;
        final double rLong = longitude + 1;
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try{
            addressList = geocoder.getFromLocationName(textAddress, 100, lLat, lLong, rLat, rLong);
            if(addressList != null){
                DialogListAddresses dialogListAddresses = new DialogListAddresses(MainActivity.this, addressList);
                dialogListAddresses.show(getSupportFragmentManager(), "tagList");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void searchCurrentDirection(){
        Address address = null;
        double latitude = latitudeDefault;
        double longitude = longitudeDefault;
        if(location == null){
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        //geocoder.setAccessToken(MAPBOX_ACCESS_TOKEN);
        try{
            address = geocoder.getFromLocation(latitude, longitude, 1).get(0);
            showCurrentDirection(address);
            //addressList = geocoder.getFromLocation(latitude, longitude, 100);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void showCurrentDirection(Address address){
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle(getString(R.string.you_are));
        String result = "";
        if(address.getLocality() != null)
            result += address.getLocality() + " - ";
        if(address.getAdminArea() != null)
            result += address.getAdminArea() + " - ";
        if(address.getCountryCode() != null)
            result += address.getCountryName() + " - ";
        if(address.getAddressLine(0) != null)
            result += address.getAddressLine(0);
        dialog.setMessage(result);
        dialog.create().show();
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        this.mainMapBoxMap.addMarker(new MarkerOptions()
                .position(point)//new LatLng(location.getLatitude(), location.getLongitude()))
                .title("New position!")
                .snippet("Latitude: " + point.getLatitude() + " Longitude: " + point.getLongitude())
                .icon(getCustomIcon(R.mipmap.tool)));
    }

    public class SearchAddress extends AsyncTask<String, Void, List<Address>>{
        ProgressDialog pd;
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage(getResources().getString(R.string.loading));
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setCancelable(false);
            pd.show();
        }
        @Override
        protected List<Address> doInBackground(String... params) {
            String txtAddress = params[0];
            List<Address> list = searchAddress(txtAddress);
            return list;
        }

        @Override
        protected void onPostExecute(List<Address> list){
            super.onPostExecute(list);
            pd.dismiss();
            if(list != null){
                DialogListAddresses dialogListAddresses = new DialogListAddresses(MainActivity.this, list);
                if(!dialogListAddresses.isVisible())
                    dialogListAddresses.show(getSupportFragmentManager(), "tagList");
            }else{
                Toast.makeText(getApplicationContext(), "Not found data", Toast.LENGTH_SHORT).show();
            }

        }

        private List<Address> searchAddress(String textAddress){
            List<Address> addressList = null;
            double latitude = latitudeDefault;
            double longitude = longitudeDefault;
            if(location == null){
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            final double lLat = latitude - 1;
            final double rLat = latitude + 1;
            final double lLong = longitude - 1;
            final double rLong = longitude + 1;
            AndroidGeocoder geocoder = new AndroidGeocoder(getApplicationContext(), Locale.getDefault());
            geocoder.setAccessToken(MAPBOX_ACCESS_TOKEN);
            try{
                //addressList = geocoder.getFromLocationName(textAddress, 100, lLat, lLong, rLat, rLong);
                //addressList = geocoder.getFromLocationName(textAddress, 100, latitude, longitude, latitude, longitude);
                //listAddresses = gc.getFromLocationName(textSpoken, 100, lLat, lLong, rLat, rLong);
                addressList = geocoder.getFromLocationName(textAddress, 100);
                //addressList = geocoder.getFromLocation(latitude, longitude, 100);
            }catch (Exception e){
                e.printStackTrace();
            }
            return addressList;
        }
    }

    public class SearchDirection extends AsyncTask<Void, Void, Address>{
        ProgressDialog pd;
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage(getResources().getString(R.string.searching));
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setCancelable(false);
            pd.show();
        }
        @Override
        protected Address doInBackground(Void... params) {
            Address address = searchDirection();
            return address;
        }

        @Override
        protected void onPostExecute(Address address){
            super.onPostExecute(address);
            pd.dismiss();
            if(address != null){
                showCurrentDirection(address);
            }else{
                Toast.makeText(getApplicationContext(), "Not found your direction", Toast.LENGTH_SHORT).show();
            }

        }

        private Address searchDirection(){
            Address address = null;
            double latitude = latitudeDefault;
            double longitude = longitudeDefault;
            if(location == null){
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
            AndroidGeocoder geocoder = new AndroidGeocoder(getApplicationContext(), Locale.getDefault());
            geocoder.setAccessToken(MAPBOX_ACCESS_TOKEN);
            try{
                address = geocoder.getFromLocation(latitude, longitude, 1).get(0);
                //addressList = geocoder.getFromLocation(latitude, longitude, 100);
            }catch (Exception e){
                e.printStackTrace();
            }
            return address;
        }
    }

    private Icon getCustomIcon(int icon_drawable){
        IconFactory iconFactory = IconFactory.getInstance(this);
        Drawable iconDrawable = ContextCompat.getDrawable(this, icon_drawable);
        Icon icon = iconFactory.fromDrawable(iconDrawable);
        return icon;
    }

    @Override
    public void onResume(){
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    //METHODS OF LOCATION LISTENER
    @Override
    public void onLocationChanged(Location location) {
        Snackbar.make(mainContainer, "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude(), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Snackbar.make(mainContainer, "GPS ON", Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Snackbar.make(mainContainer, "GPS OFF", Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

}
