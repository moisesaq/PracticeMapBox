package com.technorides.practicemapbox;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.BounceInterpolator;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.mapbox.directions.DirectionsCriteria;
import com.mapbox.directions.MapboxDirections;
import com.mapbox.directions.service.models.DirectionsResponse;
import com.mapbox.directions.service.models.DirectionsRoute;
import com.mapbox.directions.service.models.Waypoint;
import com.mapbox.geocoder.android.AndroidGeocoder;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit.Response;

public class MainActivity extends AppCompatActivity implements android.location.LocationListener, MapboxMap.OnMapClickListener,
                                                                    View.OnClickListener, MapboxMap.OnMapLongClickListener, MapboxMap.OnMarkerClickListener{

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

    public static long ID_ORIGIN_MARKER = 1000;
    public static long ID_DESTINATION_MARKER = 2000;
    private Marker originMarker, destinationMarker;
    private MarkerOptions originMarkerOptions, destinationMarkerOptions;
    private MenuItem searchItem, removeMarkersItem, drawRouteItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setup();
        try{
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(MapboxMap mapboxMap) {
                    // Set map style
                    mainMapBoxMap = mapboxMap;
                    //mainMapBoxMap.setOnMapClickListener(MainActivity.this);
                    mainMapBoxMap.setOnMapLongClickListener(MainActivity.this);
                    mainMapBoxMap.setOnMarkerClickListener(MainActivity.this);
                    mainMapBoxMap.setStyleUrl(Style.DARK);
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
            return;
        }catch (Exception e){
            e.printStackTrace();
        }


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
            getAddressWithText(actvSearchDirection.getText().toString());
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
        searchItem = menu.findItem(R.id.action_search);
        removeMarkersItem = menu.findItem(R.id.action_remove_markers);
        drawRouteItem = menu.findItem(R.id.action_draw_route);

        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView)MenuItemCompat.getActionView(searchItem);
        if(searchView != null){
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    Toast.makeText(getApplicationContext(), "Searching " + query, Toast.LENGTH_LONG).show();
                    getAddressWithText(query);
                    //new SearchAddress().execute(query);
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
            case R.id.action_draw_route:
                if(originMarker != null && destinationMarker != null)
                    searchRoute(new Waypoint(originMarker.getPosition().getLatitude(), originMarker.getPosition().getLongitude()),
                            new Waypoint(destinationMarker.getPosition().getLatitude(), destinationMarker.getPosition().getLongitude()));
                return true;
            case R.id.action_remove_markers:
                removeMarkers();
                return true;
            case R.id.action_current_location:
                showCurrentLocation();
                return true;
            case R.id.action_current_direction:
                //new SearchDirection().execute();
                searchCurrentDirection();
                return true;
            case R.id.action_change_style:
                mainMapBoxMap.setStyle(Style.MAPBOX_STREETS);
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

    private void getAddressWithText(String textAddress){
        List<Address> addressList;
        double latitude = latitudeDefault;
        double longitude = longitudeDefault;
        if(location != null){
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

    private Address getAddressWithLatLng(double latitude, double longitude){
        Address address = null;
        try{
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            address = geocoder.getFromLocation(latitude, longitude, 1).get(0);
        }catch (Exception e){
            e.printStackTrace();
        }
        return address;
    }

    private void searchCurrentDirection(){
        double latitude = latitudeDefault;
        double longitude = longitudeDefault;
        if(location != null){
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        showCurrentDirection(getAddressWithLatLng(latitude, longitude));
    }

    private void showCurrentDirection(Address address){
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle(getString(R.string.you_are));
        /*String result = "";
        if(address.getLocality() != null)
            result += address.getLocality() + " - ";
        if(address.getAdminArea() != null)
            result += address.getAdminArea() + " - ";
        if(address.getCountryCode() != null)
            result += address.getCountryName() + " - ";
        if(address.getAddressLine(0) != null)
            result += address.getAddressLine(0);*/
        dialog.setMessage(getTextAddress(address));
        dialog.create().show();
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
    }

    private void removeMarkers(){
        if(originMarker != null){
            originMarker.hideInfoWindow();
            originMarker.remove();
        }

        originMarker = null;
        originMarkerOptions = null;

        if(destinationMarker != null){
            destinationMarker.hideInfoWindow();
            destinationMarker.remove();
        }

        destinationMarker = null;
        destinationMarkerOptions = null;

        searchItem.setVisible(true);
        drawRouteItem.setVisible(false);
        removeMarkersItem.setVisible(false);
    }

    @Override
    public void onMapLongClick(@NonNull LatLng point) {
        try{
            if(originMarkerOptions == null){
                createOriginMarker(point);
                this.mainMapBoxMap.addMarker(originMarkerOptions);
                removeMarkersItem.setVisible(true);
            }else if(destinationMarkerOptions == null){
                createDestinationMarker(point);
                this.mainMapBoxMap.addMarker(destinationMarkerOptions);
                searchItem.setVisible(false);
                drawRouteItem.setVisible(true);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private StringBuilder getTextAddress(Address address){
        StringBuilder stringBuilder = new StringBuilder();
        if(address.getAddressLine(0) != null && !address.getAddressLine(0).isEmpty())
            stringBuilder.append(address.getAddressLine(0)).append(", ");
        if(address.getLocality() != null && !address.getLocality().isEmpty())
            stringBuilder.append(address.getLocality());
        return stringBuilder;
    }

    private void createOriginMarker(LatLng point){
        originMarkerOptions = new MarkerOptions();
        originMarkerOptions.position(point);
        originMarkerOptions.title("Origin");
        //originMarkerOptions.snippet("Latitude: " + point.getLatitude() + " Longitude: " + point.getLongitude());
        Address originAddress = getAddressWithLatLng(point.getLatitude(), point.getLongitude());
        originMarkerOptions.snippet(getTextAddress(originAddress).toString());
        originMarkerOptions.icon(getCustomIcon(R.mipmap.marker_origin));

        originMarker = originMarkerOptions.getMarker();
        originMarker.setId(ID_ORIGIN_MARKER);
    }

    private void createDestinationMarker(LatLng point) {
        destinationMarkerOptions = new MarkerOptions();
        destinationMarkerOptions.position(point);
        destinationMarkerOptions.title("Destination");
        //destinationMarkerOptions.snippet("Latitude: " + point.getLatitude() + " Longitude: " + point.getLongitude());
        Address destinationAddress = getAddressWithLatLng(point.getLatitude(), point.getLongitude());
        destinationMarkerOptions.snippet(getTextAddress(destinationAddress).toString());
        destinationMarkerOptions.icon(getCustomIcon(R.mipmap.marker_destination));

        destinationMarker = destinationMarkerOptions.getMarker();
        destinationMarker.setId(ID_DESTINATION_MARKER);
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        if(marker.getId() == ID_ORIGIN_MARKER){
            Toast.makeText(MainActivity.this, "Origin", Toast.LENGTH_SHORT).show();
        }else if(marker.getId() == ID_DESTINATION_MARKER){
            Toast.makeText(MainActivity.this, "Destination", Toast.LENGTH_SHORT).show();
        }
        return false;
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


    private void searchRoute(Waypoint origin, Waypoint destination){
        try{
            final MapboxDirections client = new MapboxDirections.Builder()
                    .setAccessToken(MAPBOX_ACCESS_TOKEN)
                    .setOrigin(origin)
                    .setDestination(destination)
                    .setProfile(DirectionsCriteria.PROFILE_DRIVING)
                    .build();
            //Response<DirectionsResponse> response = client.execute();
            new AsyncTask<Void, Void, Response<DirectionsResponse>>(){
                @Override
                protected void onPreExecute(){
                    super.onPreExecute();
                    Toast.makeText(getApplicationContext(), "Searching", Toast.LENGTH_SHORT).show();
                }
                @Override
                protected Response<DirectionsResponse> doInBackground(Void... params) {
                    Response<DirectionsResponse> response = null;
                    try{
                        response = client.execute();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    return response;
                }

                @Override
                protected void onPostExecute(Response<DirectionsResponse> response){
                    if(response != null){
                        DirectionsRoute route = response.body().getRoutes().get(0);
                        Log.d("DATE ROUTE ", " DISTANCE>>>> " + route.getDistance() / 1000 + "Km. TIME>>>> " + route.getDuration());
                        drawRoute(route);
                    }
                }
            }.execute();
            /*DirectionsRoute route = response.body().getRoutes().get(0);
            Log.d("DATE ROUTE ", " DISTANCE>>>> " +route.getDistance()/1000 + "Km. TIME>>>> " + route.getDuration());
            drawRoute(route);*/
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void drawRoute(DirectionsRoute route){
        try{
            List<Waypoint> listWayPoints = route.getGeometry().getWaypoints();
            LatLng[] points = new LatLng[listWayPoints.size()];
            for (int i = 0; i < points.length; i++){
                points[i] = new LatLng(listWayPoints.get(i).getLatitude(), listWayPoints.get(i).getLongitude());
            }
            mainMapBoxMap.addPolyline(new PolylineOptions()
                    .add(points)
                    .color(Color.parseColor("#3887be"))//R.color.colorAccent)
                    .width(5));
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private Icon getCustomIcon(int icon_drawable){
        IconFactory iconFactory = IconFactory.getInstance(this);
        Drawable iconDrawable = ContextCompat.getDrawable(this, icon_drawable);
        return iconFactory.fromDrawable(iconDrawable);
    }

    //--------------------------SEARCHER OF MAPBOX GEOCODER------------------------

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
            return searchAddress(txtAddress);
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
            if(location != null){
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
                addressList = geocoder.getFromLocationName(textAddress, 100, lLat, lLong, rLat, rLong);
                //listAddresses = gc.getFromLocationName(textSpoken, 100, lLat, lLong, rLat, rLong);
                //addressList = geocoder.getFromLocationName(textAddress, 100);
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
            return searchDirection();
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
            if(location != null){
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

}
