package utb.desarrollomovil.ubicacionutb;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import permissions.dispatcher.*;
import utb.desarrollomovil.ubicacionutb.net.ClienteJSON;

@RuntimePermissions
public class MapaActivity extends UbicacionUTBActivity implements OnMapReadyCallback, ClienteJSON.HandlerClienteJSON {
    public static final int ACCION_NINGUNA = 0;
    public static final int ACCION_MOSTRAR_UBICACION = 1;

    public static final String PROPIEDAD_ACCION = "ACCION";
    public static final String PROPIEDAD_REGRESO_SETTINGS = "REGRESO_SETTINGS";
    public static final String PROPIEDAD_LATITUD_MAPA = "LATITUD_MAPA";
    public static final String PROPIEDAD_LONGITUD_MAPA = "LONGITUD_MAPA";
    public static final String PROPIEDAD_LATITUD_INTENT = "LATITUD_INTENT";
    public static final String PROPIEDAD_LONGITUD_INTENT = "LONGITUD_INTENT";
    public static final String PROPIEDAD_ZOOM = "ZOOM_MAPA";
    public static final String PROPIEDAD_ROTACION = "ROTACION_MAPA";

    // Usado con getSharedPreferences. Propiedades globales para toda la aplicación. Ejemplo:
    //public static final String PREFERENCIA_GLOBAL_NOMBRE = "NOMBRE";

    // Usado con getPreferences. Propiedades locales sólo para esta activity.
    public static final String PREFERENCIA_DESACTIVAR_MY_LOCATION = "DESACTIVAR_MY_LOCATION";

    public static final double LIMITES_UTB_LATITUD_SUROESTE = 10.368202;
    public static final double LIMITES_UTB_LONGITUD_SUROESTE = -75.466161;
    public static final double LIMITES_UTB_LATITUD_NORESTE = 10.371140;
    public static final double LIMITES_UTB_LONGITUD_NORESTE = -75.464468;

    public static final double DEFAULT_LATITUD = 10.370337;
    public static final double DEFAULT_LONGITUD = -75.465449;

    public static final float ZOOM_MINIMO = 18.5f;
    public static final float ZOOM_MAXIMO = 20.0f;

    public static final float DEFAULT_ZOOM = ZOOM_MINIMO;
    public static final float DEFAULT_ROTACION = 140f;


    public static final LatLng COORDENADAS_CENTRO_UNIVERSIDAD = new LatLng(DEFAULT_LATITUD, DEFAULT_LONGITUD);
    public static final LatLngBounds COORDENADAS_LIMITES_UNIVERSIDAD = new LatLngBounds(
        new LatLng(LIMITES_UTB_LATITUD_SUROESTE, LIMITES_UTB_LONGITUD_SUROESTE),
        new LatLng(LIMITES_UTB_LATITUD_NORESTE, LIMITES_UTB_LONGITUD_NORESTE)
    );

    private SupportMapFragment fragmentMapa;
    private LocationManager locationManager;
    private GoogleMap mapa;
    private ClienteJSON http;

    //private SharedPreferencesCompat preferenciasGlobales;
    private SharedPreferences preferenciasLocales;

    private int accion;
    private boolean regresandoDeSettings;

    // posición actual del mapa, no del usuario en el mapa
    private double latitudMapa;
    private double longitudMapa;

    // Recibidos de la Activity que inició este mapa con un Intent.
    private double latitudIntent;
    private double longitudIntent;

    private float zoomMapa;
    private float rotacionMapa;

    public static void lanzarMapa(Context activity, LatLng ubicacion) {
        lanzarMapa(activity, ubicacion.latitude, ubicacion.longitude);
    }

    public static void lanzarMapa(Context activity, double latitud, double longitud) {
        Intent intentMapa = new Intent(activity, MapaActivity.class);
        Bundle informacionInicio = new Bundle();

        informacionInicio.putInt(PROPIEDAD_ACCION, ACCION_MOSTRAR_UBICACION);
        informacionInicio.putDouble(PROPIEDAD_LATITUD_INTENT, latitud);
        informacionInicio.putDouble(PROPIEDAD_LONGITUD_INTENT, longitud);
        // ...

        intentMapa.putExtras(informacionInicio);
        activity.startActivity(intentMapa);
    }

    public static void lanzarMapa(Context activity) {
        Intent intentMapa = new Intent(activity, MapaActivity.class);
        Bundle informacionInicio = new Bundle();

        informacionInicio.putInt(PROPIEDAD_ACCION, ACCION_NINGUNA);
        // ...

        intentMapa.putExtras(informacionInicio);
        activity.startActivity(intentMapa);
    }

    @Override
    public void requestJSONExitoso(Object resultado, int codigoEstado) {

    }

    @Override
    public void requestJSONFallido(int codigoEstado) {

    }

    @Override
    public void requestJSONIniciado() {

    }

    @Override
    public void requestJSONFinalizado() {

    }

    @Override
    public void requestJSONReintentado(int numeroReintento) {

    }

    protected void procesarComando() {
        Toast.makeText(this, "DEBUG: funciona()", Toast.LENGTH_SHORT).show();

        CameraPosition inicial = (new CameraPosition.Builder()).target(COORDENADAS_LIMITES_UNIVERSIDAD.getCenter()).zoom(DEFAULT_ZOOM).bearing(DEFAULT_ROTACION).build();
        mapa.moveCamera(CameraUpdateFactory.newCameraPosition(inicial));
    }

    void mostrarDialogoPrecisionGPS() {
        final AppCompatActivity contexto = this;
        final Intent settings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_activity_mapa)
                .setMessage(getString(R.string.permiso_ubicacion_razon) + "\n\n" + getString(R.string.permiso_ubicacion_precision) + "\n\n" + getString(R.string.permiso_ubicacion_lanzar_settings))
                .setPositiveButton(R.string.button_abrir, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        regresandoDeSettings = true;
                        startActivity(settings);
                    }
                })
                .setNegativeButton(R.string.button_cancelar, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        preferenciasLocales.edit().putBoolean(PREFERENCIA_DESACTIVAR_MY_LOCATION, true).commit();
                        Toast.makeText(contexto, R.string.permiso_ubicacion_denegado, Toast.LENGTH_SHORT).show();
                    }
                }).create().show();
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void activarMyLocation() {
        String proveedorUbicacion;

        Toast.makeText(this, "DEBUG: activarMyLocationStart()", Toast.LENGTH_SHORT).show();

        if (mapa == null || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Error.
            return;
        }

        if (mapa.isMyLocationEnabled()) {
            Toast.makeText(this, "DEBUG: activarMyLocationStart(2if)", Toast.LENGTH_SHORT).show();
            preferenciasLocales.edit().putBoolean(PREFERENCIA_DESACTIVAR_MY_LOCATION, false).commit();
            procesarComando();
            return;
        }

        proveedorUbicacion = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : null;
        Toast.makeText(this, "DEBUG: activarMyLocationStart(4thwall)", Toast.LENGTH_SHORT).show();

        if (proveedorUbicacion != null) {
            Toast.makeText(this, "DEBUG: activarMyLocationStart(3if)", Toast.LENGTH_SHORT).show();
            preferenciasLocales.edit().putBoolean(PREFERENCIA_DESACTIVAR_MY_LOCATION, false).commit();
            mapa.setMyLocationEnabled(true);
            procesarComando();
            return;
        }

        if (!preferenciasLocales.getBoolean(PREFERENCIA_DESACTIVAR_MY_LOCATION, false)) {
            Toast.makeText(this, "DEBUG: activarMyLocationStart(4if)", Toast.LENGTH_SHORT).show();
            mostrarDialogoPrecisionGPS();
        }

        // Error.
        return;
    }

    public void prepararMapa() {
        mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mapa.setLatLngBoundsForCameraTarget(COORDENADAS_LIMITES_UNIVERSIDAD);
        mapa.setMinZoomPreference(ZOOM_MINIMO);
        mapa.setMaxZoomPreference(ZOOM_MAXIMO);

        Toast.makeText(this, "DEBUG: prepararMapa()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;

        prepararMapa();

        if (regresandoDeSettings) {
            regresoDeSettings();
        } else if (!mapa.isMyLocationEnabled()) {
            MapaActivityPermissionsDispatcher.activarMyLocationWithCheck(this);
        }

        Toast.makeText(this, "DEBUG: onMapReady()", Toast.LENGTH_SHORT).show();
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void mostrarDialogoPermisosUbicacion(final PermissionRequest solicitud) {
        // NOTE: Show a rationale to explain why the permission is needed, e.g. with a dialog.
        // Call proceed() or cancel() on the provided PermissionRequest to continue or abort
        new AlertDialog.Builder(this).setPositiveButton(R.string.button_permitir, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                solicitud.proceed();
            }
        }).setNegativeButton(R.string.button_denegar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                solicitud.cancel();
            }
        }).setCancelable(false).setMessage(R.string.permiso_ubicacion_razon).show();
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onPermisoUbicacionDenegadoPermanentemente() {
        Toast.makeText(this, R.string.permiso_ubicacion_no_preguntar, Toast.LENGTH_SHORT).show();
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onPermisoUbicacionDenegado() {
        // NOTE: Deal with a denied permission, e.g. by showing specific UI
        // or disabling certain functionality
        Toast.makeText(this, R.string.permiso_ubicacion_denegado, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MapaActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    public boolean regresoDeSettings() {
        if (mapa != null && regresandoDeSettings) {
            regresandoDeSettings = false;

            if (mapa.isMyLocationEnabled()) {
                Toast.makeText(this, R.string.permiso_ubicacion_precision_activado, Toast.LENGTH_SHORT);
            } else {
                Toast.makeText(this, R.string.permiso_ubicacion_precision_desactivado, Toast.LENGTH_SHORT);
            }

            procesarComando();
            return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mapa != null && regresandoDeSettings) {
            regresoDeSettings();
        }

        Toast.makeText(this, "DEBUG: onResume()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }

        if (preferenciasLocales == null) {
            preferenciasLocales = getPreferences(MODE_PRIVATE);
        }

        if (mapa == null) {
            fragmentMapa.getMapAsync(this);
        }
    }

    @Override
    protected void inicializarLayout() {
        setContentView(R.layout.activity_mapa);

        fragmentMapa = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.activity_mapa, fragmentMapa).commit();

        Toast.makeText(this, "DEBUG: inicializarLayout()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRestoreInstanceState (Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        regresandoDeSettings = savedInstanceState.getBoolean(PROPIEDAD_REGRESO_SETTINGS, false);
        accion = savedInstanceState.getInt(PROPIEDAD_ACCION, ACCION_NINGUNA);
        latitudIntent = savedInstanceState.getDouble(PROPIEDAD_LATITUD_INTENT, DEFAULT_LATITUD);
        longitudIntent = savedInstanceState.getDouble(PROPIEDAD_LONGITUD_INTENT, DEFAULT_LONGITUD);
        latitudMapa = savedInstanceState.getDouble(PROPIEDAD_LATITUD_MAPA, latitudIntent);
        longitudMapa = savedInstanceState.getDouble(PROPIEDAD_LONGITUD_MAPA, longitudIntent);
        zoomMapa = savedInstanceState.getFloat(PROPIEDAD_ZOOM, DEFAULT_ZOOM);
        rotacionMapa = savedInstanceState.getFloat(PROPIEDAD_ROTACION, DEFAULT_ROTACION);
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(PROPIEDAD_ACCION, accion);
        outState.putBoolean(PROPIEDAD_REGRESO_SETTINGS, regresandoDeSettings);
        outState.putDouble(PROPIEDAD_LATITUD_INTENT, latitudIntent);
        outState.putDouble(PROPIEDAD_LONGITUD_INTENT, longitudIntent);
        outState.putDouble(PROPIEDAD_LATITUD_MAPA, latitudMapa);
        outState.putDouble(PROPIEDAD_LONGITUD_MAPA, longitudMapa);
        outState.putFloat(PROPIEDAD_ZOOM, zoomMapa);
        outState.putFloat(PROPIEDAD_ROTACION, rotacionMapa);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle informacionInicio = getIntent().getExtras();

        if (informacionInicio != null) {
            accion = informacionInicio.getInt(PROPIEDAD_ACCION, ACCION_NINGUNA);
            regresandoDeSettings = informacionInicio.getBoolean(PROPIEDAD_REGRESO_SETTINGS, false);
            latitudIntent = informacionInicio.getDouble(PROPIEDAD_LATITUD_INTENT, DEFAULT_LATITUD);
            longitudIntent = informacionInicio.getDouble(PROPIEDAD_LONGITUD_INTENT, DEFAULT_LONGITUD);
            latitudMapa = informacionInicio.getDouble(PROPIEDAD_LATITUD_MAPA, latitudIntent);
            longitudMapa = informacionInicio.getDouble(PROPIEDAD_LONGITUD_MAPA, longitudIntent);
            zoomMapa = informacionInicio.getFloat(PROPIEDAD_ZOOM, DEFAULT_ZOOM);
            rotacionMapa = informacionInicio.getFloat(PROPIEDAD_ROTACION, DEFAULT_ROTACION);
        } else {
            accion = ACCION_NINGUNA;
            regresandoDeSettings = false;
            latitudIntent = DEFAULT_LATITUD;
            longitudIntent = DEFAULT_LONGITUD;
            latitudMapa = latitudIntent;
            longitudMapa = longitudIntent;
            zoomMapa = DEFAULT_ZOOM;
            rotacionMapa = DEFAULT_ROTACION;
        }

        Toast.makeText(this, "DEBUG: onCreate()", Toast.LENGTH_SHORT).show();
    }
}