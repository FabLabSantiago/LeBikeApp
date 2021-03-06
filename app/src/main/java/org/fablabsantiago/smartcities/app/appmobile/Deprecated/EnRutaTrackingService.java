package org.fablabsantiago.smartcities.app.appmobile.Deprecated;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.fablabsantiago.smartcities.app.appmobile.Utils.DatabaseHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class EnRutaTrackingService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    int mStartMode;       // indicates how to behave if the service is killed
    IBinder mBinder;      // interface for clients that bind
    boolean mAllowRebind; // indicates whether onRebind should be used

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;

    private SharedPreferences serviceLocationsSP;
    private Set<String> locationSet = new HashSet<String>();
    private String routeName;

    DatabaseHandler baseDatos;
    int idDestino;
    int idRuta;
    int seqNum;

    @Override
    public void onCreate() {
        Log.i("EnRutaTrackingService","onCreate - in");

        /*---------- Base Datos ----------*/
        baseDatos = new DatabaseHandler(this);

        DateFormat date = new SimpleDateFormat("HHmmss:ddMMyyyy");
        routeName = "origen_destino" + "_" + date.format(new Date()) + "_" + "n";
        Log.i("EnRutaTrackingService","onCreate: routeName=" + routeName + "(not used)");

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        seqNum = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        Log.i("EnRutaTrackingService","onStartCommand - in");

        idDestino = intent.getIntExtra("destino_id", -1);
        idRuta = baseDatos.getLastRutasId() + 1;
        Log.i("EnRutaTrackingService","idDestino: " + String.valueOf(idDestino) + ", idRuta: " + String.valueOf(idRuta));

        DateFormat date = new SimpleDateFormat("dd/MM/yyyy");
        DateFormat hour = new SimpleDateFormat("HH:mm:ss");

        //TODO: guardar ruta con nombre correspondiente.
        //                                    nombre, hora, fecha
        baseDatos.startRoute(idRuta, idDestino, "generic_track", hour.format(new Date()), date.format(new Date()));

        mGoogleApiClient.connect();
        return mStartMode;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        Log.i("EnRutaTrackingService","onBind - in");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        Log.i("EnRutaTrackingService","onUnbind - in");
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
        Log.i("EnRutaTrackingService","onRebind - in");
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.i("EnRutaTrackingService","onDestroy - in");
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        mGoogleApiClient.disconnect();

        //                      numPos, numNeg, duracion, distancia
        baseDatos.endRoute(idRuta, 0, 0, 0, 0);
        Log.i("EnRutaTrackingService", "ending route " + idRuta);
    }

    /* |                                                 */
    /* |  Non service methods, location services related */
    /* \/                                                */
    @Override
    public void onConnected(Bundle bundle) {
        Log.i("EnRutaTrackingService","onConnected - in");
        createLocationRequest();
        startLocationUpdates();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        seqNum = baseDatos.getLastSeqNum() + 1;
        //                            tiempo
        baseDatos.addTrackPoint(seqNum, 0, location.getLatitude(), location.getLongitude());

        Log.i("EnRutaTrackingService","onLocationChanged, " + location.toString());
        Toast.makeText(this, location.toString(),Toast.LENGTH_SHORT).show();

        //TODO: implement broadcast for the EnRutaActivity to receive and plot track in real time
    }
}
