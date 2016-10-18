package utb.desarrollomovil.ubicacionutb;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import permissions.dispatcher.*;
import utb.desarrollomovil.ubicacionutb.net.ClienteJSON;

/**
 * Defina una Activity que presenta un mapa con ubicaciones de distintos lugares de la Universidad.
 *
 * Se recomienda iniciar la activity llamando uno de los métodos públicos y estáticos llamados
 * lanzarActivity(...). Estos métodos se encargan de inicializar el mapa en distintos estados,
 * dependiendo de la información que se desea mostrar.
 *
 * En tiempo de ejecución, la Activity comprueba si se tienen suficientes permisos en varios
 * momentos. En especial, comprueba si se tienen permisos de ubicación en Android 6 y nás
 * reciente, y pide al usuario activar el modo GPS de alta precisión en todas las versiones de
 * Android soportadas. Si los permisos siguen siendo insuficientes, se mostrará un mensaje de
 * error en lugar del mapa, y seguirá siendo mostrado hasta que el usuario haga los ajustes
 * necesarios pra que funcione la aplicación.
 *
 * Si el usuario ha activado permisos de ubicación para la aplicación (en caso de ser Android >= 6)
 * pero no ha activado el GPS en modo alta precisión, la aplicación continuará en funcionamiento,
 * pero será incapáz de mostrar la ubicación actual del usuario. En este caso, la posición "inicial"
 * o "actual" del usuario será tratada como una posición correspondiente al centro del campus
 * de la Universidad (Ternera.)
 *
 * Las operaciones soportadas por el mapa hasta ahora son (escrito en 2016/10/17; 14:30):
 *
 *     -- Ninguna: sólo se muestra el mapa. Si la activity es iniciada con una operación "Ninguna",
 *        entonces el mapa será centrado sobre la universidad. (Es decir, se realiza "Centrar")
 *        Si la Activity es iniciada con una operación distinta a Ninguna, entonces no se realiza
 *        la operación "Centrar"; el mapa y su punto de vista permanece intacto.
 *     -- Centrar: centra el mapa sobre el usuario o sobre la Universidad, dependiendo de la
 *        configuración/disponibilidad del GPS.
 *     -- Mostrar Ubicación: muestra una ubicación en el mapa daad por su latitud y longitud.
 *
 * TODO: completar implementación de procesarComando. Hacer que efectue una operación distinta
 * TODO: dependiendo del valor de "accion", y luego asignar ACCION_NINGUNA a "accion."
 *
 * TODO: sobreescribir menú de toolbar y reemplazarlo por opciones de mapa.
 *
 * TODO: implementar más operaciones.
 */
@RuntimePermissions
public class MapaActivity extends UbicacionUTBActivity implements OnMapReadyCallback, ClienteJSON.HandlerClienteJSON, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    // "Enumeración" de acciones soportadas por el mapa.
    public static final int ACCION_NO_ESPECIFICADA = 0;
    public static final int ACCION_NINGUNA = 1;
    public static final int ACCION_CENTRAR = 2;
    public static final int ACCION_MOSTRAR_UBICACION = 3;

    // "Enumeración" de posible estados del GPS.
    public static final int ESTADO_GPS_DESCONOCIDO = 0;
    public static final int ESTADO_GPS_DESACTIVADO_O_WIFI = 1;
    public static final int ESTADO_GPS_ALTA_PRECISION = 2;

    /*
     * Nombres inmutables de propiedades semi-permanentes almacenadas en Bundles.
     *
     * Las propiedades son leídas en los siguientes lugares (métodos):
     *
     *   -- Desde onCreate se lee el Bundle contenido en el Intent que inició la Activity. Los
     *      bundles que provienen de otras Activities pueden contener instrucciones para el mapa.
     *   -- En onCreate y en onRestoreInstanceState, el parámetro savedInstanceState es un Bundle
     *      que contiene propiedades que fueron almacenadas antes de "matar" la Activity.
     *
     * Las propiedades son escritas en los siguientes lugares:
     *
     *   -- En onSaveInstanceState, se pueden almacenarp ropiedades en el Bundle outState. outState
     *      será enviado a onCreate y a onRestoreInstanceState.
     *   -- En los Intent que son usados para iniciar esta Activity, los "extras" son propiedades
     *      en un Bundle interno del Intent que son enviados a esta Activity. En esta clase, están
     *      disponibles en getIntent().getExtras().
     *
     * Descripciones de las propiedades:
     *
     *   -- PROPIEDAD_ACCION: (int) la acción a ejecutar cuando la Activity esté lista.
     *   -- PROPIEDAD_REGRESO_SETTINGS: (boolean) indica si el usuario está regresando de Settings.
     *   -- PROPIEDAD_LATITUD_MAPA: (double) latitud de la cámara (punto de vista) en el mapa.
     *   -- PROPIEDAD_LONGITUD_MAPA: (double) longitud de la cámara (punto de vista) en el mapa.
     *   -- PROPIEDAD_LATITUD_INTENT: (double) latitud recibida por Intent. Usada por acciones.
     *   -- PROPIEDAD_LONGITUD_INTENT: (double) longitud recibida por Intent. Usada por acciones.
     *   -- PROPIEDAD_ZOOM: (float) nivel de zoom actual, del punto de vista del mapa.
     *   -- PROPIEDAD_ROTACION: (float) rotación en grados, del punto de vista del mapa (bearing.)
     *   -- PROPIEDAD_ESTADO_GPS: (int) último estado GPS conocido. Ver ESTADO_GPS_*
     *   -- PROPIEDAD_ACTUALIZAR_MY_LOCATION: (boolean) true si estadoGPS ha cambiado recientemente.
     */
    public static final String PROPIEDAD_ACCION = "ACCION";
    public static final String PROPIEDAD_REGRESO_SETTINGS = "REGRESO_SETTINGS";
    public static final String PROPIEDAD_LATITUD_MAPA = "LATITUD_MAPA";
    public static final String PROPIEDAD_LONGITUD_MAPA = "LONGITUD_MAPA";
    public static final String PROPIEDAD_LATITUD_INTENT = "LATITUD_INTENT";
    public static final String PROPIEDAD_LONGITUD_INTENT = "LONGITUD_INTENT";
    public static final String PROPIEDAD_ZOOM = "ZOOM_MAPA";
    public static final String PROPIEDAD_ROTACION = "ROTACION_MAPA";
    public static final String PROPIEDAD_ESTADO_GPS = "ESTADO_PROVEEDOR_GPS";
    public static final String PROPIEDAD_ACTUALIZAR_MY_LOCATION = "ACTUALIZAR_MY_LOCATION";

    /*
     * Las siguientes constantes espeficícan nombres inmutables de "preferencias compartidas" (es
     * decir, "globales") que persisten a travéz de distintas ejecuciones de la aplicación.
     */
    // Por el momento no tenemos propiedades "globales", pero aquí un ejemplo de un posible nombre.
    //public static final String PREFERENCIA_GLOBAL_NOMBRE = "NOMBRE";

    /*
     * Las siguientes constantes corresponden a "preferencias locales" (es decir, prefrencias que
     * están disponibles para esta Activity y para ninguna otra) que persisten a travéz de distintas
     * ejecuciones de la aplicación.
     *
     * Descripción de propiedades:
     *
     *   -- PREFERENCIA_DESACTIVAR_MY_LOCATION: myLocation es una propiedad de todos GoogleMaps que
     *      permite mostrar la ubicación actual del usuario al presionar un botón en pantalla.
     *
     *      Esto está disponible solo si el GPS está activado (independientemente de los permisos
     *      de la aplicación). Para no solicitar al usuario que active el GPS a cada instante, se
     *      guarda esta propiedad indicando que el usuario no necesita o no desea myLocation.
     *      Mientras que su valor sea true, no se le pedirá al usuario que active el GPS.
     *
     *      La próxima vez que el usuario regrese a la Activity después de activar el GPS, entonces
     *      este valor tomará un valor de false y myLocation será activado automáticamente.
     */
    public static final String PREFERENCIA_DESACTIVAR_MY_LOCATION = "DESACTIVAR_MY_LOCATION";

    /*
     * Especifícan los límites de la universidad usando dos puntos: un punto sur-oeste, y un punto
     * en el nor-oriente.
     *
     * Las coordenadas son usadas por el mapa en prepararMapa para colocar límites geográficos en los
     * que la aplicación trabajará; una vez puestos, la aplicación no podrá mostrar ubicaciones
     * que estén fuera de estas coordenadas.
     *
     * Si se usa myLocation para mostrar la ubicación del usuario mientras el usuario est´fuera
     * de la universidad, entonces se mostrará el punto mas cercano al usuario dentro de las
     * coordenadas, sin salirse de su rango. Con esto se asegura que toda posición mostrada siempre
     * estará dentro del a Universidad, sin importar la posición actual del usuario.
     */
    public static final double LIMITES_UTB_LATITUD_SUROESTE = 10.368202;
    public static final double LIMITES_UTB_LONGITUD_SUROESTE = -75.466161;
    public static final double LIMITES_UTB_LATITUD_NORESTE = 10.371140;
    public static final double LIMITES_UTB_LONGITUD_NORESTE = -75.464468;

    // Especifícan los niveles de zoom mínimos y máximos permitidos en el mapa.
    public static final float ZOOM_MINIMO = 18.5f;
    public static final float ZOOM_MAXIMO = 20.0f;

    /*
     * Coordenadas por defecto usadas cuando la posición del usuario (o cualquie otra posición) no
     * es válida o simplemente no está disponible. Correpsonde al centro del campus de Ternera.
     */
    public static final double DEFAULT_LATITUD = 10.370337;
    public static final double DEFAULT_LONGITUD = -75.465449;

    // Niveles de zoom y rotación por defecto.
    public static final float DEFAULT_ZOOM = ZOOM_MINIMO;
    public static final float DEFAULT_ROTACION = 140f;

    /*
     * Referencias inmutables a objetos que representan coordenadas y límites de coordenadas creados
     * usando los límites definidos anterioremnte.
     */
    public static final LatLng COORDENADAS_CENTRO_UNIVERSIDAD = new LatLng(DEFAULT_LATITUD, DEFAULT_LONGITUD);
    public static final LatLngBounds COORDENADAS_LIMITES_UNIVERSIDAD = new LatLngBounds(
        new LatLng(LIMITES_UTB_LATITUD_SUROESTE, LIMITES_UTB_LONGITUD_SUROESTE),
        new LatLng(LIMITES_UTB_LATITUD_NORESTE, LIMITES_UTB_LONGITUD_NORESTE)
    );

    // Referencia a elementos da la interfáz de usuario.
    private LinearLayout contenedorFragmentMapa;
    private SupportMapFragment fragmentMapa;
    private LinearLayout mensajeNoMapa;

    // Objetos críticos para el funcionamiento de las operaciones principales del mapa.
    private LocationManager locationManager;
    private GoogleMap mapa;
    private GoogleApiClient serviciosGooglePlay;
    private ClienteJSON http;

    // Preferencias globales y locales. Persistencia de datos.
    private SharedPreferences preferenciasGlobales;
    private SharedPreferences preferenciasLocales;

    /*
     * La siguientes propiedades reflejan el estado de la Activity en tiempo de ejecución.
     * -----------------------------------------------------------------------------------
     */

    // Accion a tomar. Ver constantes ACCION_*.
    private int accion;

    // True si se está regresando de la pantalla Settings después de pedirle permisos al usuario.
    private boolean regresandoDeSettings;

    // Último estado conocido del GPS. No siempre corresponde al valor actual del GPS.
    private int estadoGPS;

    // Si es true, entonces se debe activar o desactivar MyLocation.
    private boolean actualizarMyLocation;

    /*
     * Posición recibida de la Activity que inició este mapa con un Intent.
     */
    private double latitudIntent;
    private double longitudIntent;

    /*
     * La posición que se usará para realizar operaciones/acciones en el mapa.
     *
     * No se debe confundir con las coordenadas recibidas por intent y tampoco se debe confundir
     * con las coordenadas actuales de la camara en el mapa, que pueden ser obtenidas con
     * mapa.getCameraPosition().target.{latitude,longitude}
     *
     * Debe ser alterado antes de realizar nuevas operaciones.
     */
    private double latitudMapa;
    private double longitudMapa;

    // Niveles de zoom y rotación que las operaciones/acciones usarán. Alterar antes de hacer accioenes.
    private float zoomMapa;
    private float rotacionMapa;

    // Si es true, entonces el primer desplazamiento de cámara será hecho instantaneamente, sin aniamr.
    private boolean primerMovimiento;

    /**
     * Lanza esta Activity desde otra Activity mostrando las coordenadas LatLng ubicacion.
     *
     * @param activity Referencia a la Activity que desea iniciar el mapa.
     * @param ubicacion Objeto con posiciones a mostrar en el mapa; latitud y longitud.
     */
    public static void lanzarMapa(Context activity, LatLng ubicacion) {
        lanzarMapa(activity, ubicacion.latitude, ubicacion.longitude);
    }

    /**
     * Lanza esta Activity desde otra Activity mostrando las coordenadas latitud y longitud.
     *
     * @param activity Referencia a la Activity que desea iniciar el mapa.
     * @param latitud Latitud del lugar a mostrar.
     * @param longitud Longitud del lugar a mostrar.
     */
    public static void lanzarMapa(Context activity, double latitud, double longitud) {
        Intent intentMapa = new Intent(activity, MapaActivity.class);
        Bundle informacionInicio = new Bundle();

        // Instruyendo al mapa a mostrar una ubicación específica.
        informacionInicio.putInt(PROPIEDAD_ACCION, ACCION_MOSTRAR_UBICACION);
        informacionInicio.putDouble(PROPIEDAD_LATITUD_INTENT, latitud);
        informacionInicio.putDouble(PROPIEDAD_LONGITUD_INTENT, longitud);
        // ... agregar más propiedades aquí si llega a ser necesario.

        intentMapa.putExtras(informacionInicio);
        activity.startActivity(intentMapa);
    }

    /**
     * Lanza esta Activity desde otra Activity mostrando la ubicación actual del usuario dentro de
     * la Universidad (si está disponible) o una posición central dentro del campus (Ternera.)
     *
     * @param activity Referencia a la Activity que desea iniciar el mapa.
     */
    public static void lanzarMapa(Context activity) {
        Intent intentMapa = new Intent(activity, MapaActivity.class);
        Bundle informacionInicio = new Bundle();

        // Instruyendo al mapa a no mostrar una ubicación específica.
        informacionInicio.putInt(PROPIEDAD_ACCION, ACCION_NO_ESPECIFICADA);
        // ... agregar más propiedades aquí si llega a ser necesario.

        intentMapa.putExtras(informacionInicio);
        activity.startActivity(intentMapa);
    }

    /** TODO: implementar y documentar cuando sea necesario hacer requests JSON. */
    @Override
    public void requestJSONExitoso(Object resultado, int codigoEstado) {

    }

    /** TODO: implementar y documentar cuando sea necesario hacer requests JSON. */
    @Override
    public void requestJSONFallido(int codigoEstado) {

    }

    /** TODO: implementar y documentar cuando sea necesario hacer requests JSON. */
    @Override
    public void requestJSONIniciado() {

    }

    /** TODO: implementar y documentar cuando sea necesario hacer requests JSON. */
    @Override
    public void requestJSONFinalizado() {

    }

    /** TODO: implementar y documentar cuando sea necesario hacer requests JSON. */
    @Override
    public void requestJSONReintentado(int numeroReintento) {

    }

    /**
     * Centra la cámara sobre la posición actual del usuario, o mueve la cámara al centro de la
     * Universidad si la ubicación del usuario no está disponible.
     */
    protected void accionCentrar() {
        // Primero se intenta determinar la posición del usuario.
        Location ultimaUbicacionConocida = null;
        if (serviciosGooglePlay != null && serviciosGooglePlay.isConnected()) {
            ultimaUbicacionConocida = LocationServices.FusedLocationApi.getLastLocation(serviciosGooglePlay);
        }

        if (ultimaUbicacionConocida != null) {
            latitudMapa = ultimaUbicacionConocida.getLatitude();
            longitudMapa = ultimaUbicacionConocida.getLongitude();
        } else {
            latitudMapa = DEFAULT_LATITUD;
            longitudMapa = DEFAULT_LONGITUD;
        }

        accionMostrarUbicacion();
    }

    /**
     * Mueve la cámara (con suavizado) hacia una ubicación dada por su latitud y longitud; la
     * latitud es tomada de la variable latitudMapa y la longitud de longitudMapa, mientras que el
     * zoom actual y la rotación de la cámara son tomados de zoomMapa y de rotacionMapa,
     * respectivamente.
     */
    protected void accionMostrarUbicacion() {
        // Configurando animación de movimiento de cámara.
        CameraPosition nuevaPosicionCamara = new CameraPosition.Builder()
                .target(new LatLng(latitudMapa, longitudMapa))
                .zoom(zoomMapa)
                .bearing(rotacionMapa)
        .build();

        // Se está especificando una posición inicial ose está moviendo hacia otra ubicación?
        if (primerMovimiento) {
            primerMovimiento = false;

            // Se trata de una posición inicial.
            mapa.moveCamera(CameraUpdateFactory.newCameraPosition(nuevaPosicionCamara));
            return;
        }

        // No? Entonces se trata de otro desplazamiento de cámara.
        mapa.animateCamera(CameraUpdateFactory.newCameraPosition(nuevaPosicionCamara));
    }

    /**
     * Procesa la acción "acción" y posteriormente cambia su valor a ACCION_NINGUNA, dejando el mapa
     * listo pare realizar otras operaciones.
     *
     * Si la operación nes ACCION_NO_ESPECIFICADA, entonces ejecuta ACCION_CENTRAR (es la acción
     * por defecto, por así decirlo.) Si la acción es ACCION_NINGUNA, entonces no se hace nada.
     *
     * TODO: terminar. Definir e implementar más acciones.
     */
    protected void procesarComando() {
        switch(accion) {
            case ACCION_CENTRAR:
                accionCentrar();
                break;
            case ACCION_MOSTRAR_UBICACION:
                accionMostrarUbicacion();
                break;
            case ACCION_NO_ESPECIFICADA:
            default:
                // Si la acción es inválida o no especificada, entonces se centra cámara en el mapa.
                accionCentrar();
                break;
            case ACCION_NINGUNA:
                // Nada que hacer.

        }

        accion = ACCION_NINGUNA;
    }

    /**
     * Muestra un diálogo solicitando al usuario que cambie la configuración de su GPS a modo de
     * alta precisión.
     *
     * Si el usuario presiona "Abrir", entonces es enviado a la pantalla de Settings de su
     * dispositivo para que cambie la configuración del GPS.
     *
     *  -- Si al volver a esta Activity desde la pantalla Settings (ya sea presionando el botón
     *     atrás o usando el menú de tareas) el GPS sigue en un modo distinto a alta precisión,
     *     entonces myLocation permanecerá desactivado hasta que se active el modo alta precisión.
     *  -- Si al volver a esta Activity desde la pantalla de Settings el GPS está activado, entonces
     *     myLocation será activado.
     *  -- En ambos casos, al final se ejecuta procesarComando()
     *
     * Pero si el usuario presiona "Cancelar", entonces simplemente myLocation permanecerá
     * desactivado hasta que el usuario manualmente active el modo de alta precisión. Después de
     * esto, se ejecuta procesarComando()
     */
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
                    activarMyLocation();
                }
            }).create().show();
    }

    /**
     * Determina el estado actual del GPS y asigna dicho estado a estadoGPS.
     *
     * Si el estado actual del GPS no coincide con el último estado conocido, entonces la función
     * regresa true. En otras palabras, regresa true i el valor de estadoGPS fue cambiado a un
     * valor distinto.
     *
     * Idealmente, se debe llamar al inicio de onResume. De esta manera, es posible mantener
     * estadoGPS tan actualizado como sea posible y se puede alter MyLocation como sea necesario
     * (desde otroe métodos, ya que este método no altera NyLocation de ninguna manera.) Su
     * resultado debe ser asignado a la variable actualizarMyLocation como true.
     *
     * Antes de llamar este método, tenga en cuenta que también regresará true si estadoGPS cambia
     * de ESTADO_GPS_DESCONOCIDO a un estado distinto.
     */
    public boolean actualizarEstadoGPS() {
        String proveedorUbicacion = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : null;
        int nuevoEstado = ESTADO_GPS_DESCONOCIDO;

        if (proveedorUbicacion != null) {
            nuevoEstado = ESTADO_GPS_ALTA_PRECISION;
        } else {
            nuevoEstado = ESTADO_GPS_DESACTIVADO_O_WIFI;
        }

        if (estadoGPS != nuevoEstado) {
            estadoGPS = nuevoEstado;
            actualizarMyLocation = true;
            return true;
        }

        actualizarMyLocation = false;
        return false;
    }

    /**
     * Intenta activar MyLocation y luego ejecuta procesarComando()
     *
     * Si el mapa no está aún cargado, o si no se tienen permisos de ACCESS_FINE_LOCATION, entonces
     * el mapa es reemplazado por un mensaje de error. Es un error crítico.
     *
     * Si el estado del GPS ha cambiado a un estado distinto al último estado conocido, entonces
     * se activa o desactiva MyLocation. Cuando el estado cambia a ESTADO_GPS_ALTA_PRECISION,
     * entonces MyLocation es activado. Si cambia a desactivado o desconocido, entonces se
     * desactiva MyLocation.
     *
     * Cuando el GPS está desactivado, se presenta al usuario un diálogo solicitando que se
     * active el GPS en modo alta precisión en Settings. Si al regresar a la Activity desde
     * settings el GPS continúa en un modo distinto a alta precisión, entonces la aplicación
     * recordará este cambio y no presentará el diálogo al usuario. En este caso, MyLocation
     * permancerá desactivado. Esto actualiza la _preferencia_ DESACTIVAR_MY_LOCATION a true.
     *
     * Si al regresar de Settings estadoGPS está activado, entonces MyLocation será activado y
     * se actualizará la _preferencia_ DESACTIVAR_;Y_LOCATION con un valor de false.
     *
     * En todos casos (excepto por el caso de permiso insuficiente o de mapa nulo), entonces se
     * ejecutará procesarComando(). Es importante notar que procesarComando(), en los casos en que
     * el usuario es enviado a Settings, será invocado solo cuando el usuario regrese a la Activity.
     *
     * TODO: renombrar método? Su nombre no parece describir exactamente todo lo que hace.
     */
    public void activarMyLocation() {
        // Error ctítico si no hay un mapa o si no se cuenta con suficientes permisos.
        if (mapa == null || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            errorPermisos();
            return;
        }

        // Si se necesita actualizar estadoGPS y por extensión MyLocation, entonces:
        if (actualizarMyLocation) {
            actualizarMyLocation = false;

            if (estadoGPS == ESTADO_GPS_ALTA_PRECISION) {
                preferenciasLocales.edit().putBoolean(PREFERENCIA_DESACTIVAR_MY_LOCATION, false).commit();

                // Si el estado ahora es alta precisión, se activa MyLocation.
                if (!mapa.isMyLocationEnabled()) {
                    mapa.setMyLocationEnabled(true);
                }
            } else {
                // Se desactiva MyLocation.
                if (mapa.isMyLocationEnabled()) {
                    mapa.setMyLocationEnabled(false);
                }

                // Diálogo si GPS no está en modo alta precisión y DESACTIVAR_MY_LOCATION es false
                if (!preferenciasLocales.getBoolean(PREFERENCIA_DESACTIVAR_MY_LOCATION, false)) {
                    mostrarDialogoPrecisionGPS();
                    return;
                }

                /*
                 * Si el diálogo no es presentado ya que no es necesario (es decir, si el GPS
                 * cambió de ESTADO_GPS_DESACTIVADO_O_WIFI a ESTADO_GPS_DESCONOCIDO o vice-versa),
                 * entonces no es necesario cambiar la _preferencia_ DESACTIVAR_MY_LOCATION ya que
                 * el estado simplemente no es ESTADO_GPS_ALTA_PRECISION.
                 *
                 * En este caso, esto implica que la aplicación recuerda que el usuario no desea
                 * activar el modo de alta precisión y se debe continuar la operación sin MyLocation
                 */
            }
        }

        // Si no hubo error, entonces se ejecuta un comando pendiente.
        procesarComando();
    }

    /**
     * Realiza configuración inicial del mapa, incluyendo sus límites geográficos y de zoom.
     * Hace el mapa visible.
     */
    public void prepararMapa() {
        // Haciendo el mapa visible.
        contenedorFragmentMapa.setVisibility(View.VISIBLE);
        mensajeNoMapa.setVisibility(View.GONE);

        // Configurando mapa.
        mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mapa.setLatLngBoundsForCameraTarget(COORDENADAS_LIMITES_UNIVERSIDAD);
        mapa.setMinZoomPreference(ZOOM_MINIMO);
        mapa.setMaxZoomPreference(ZOOM_MAXIMO);
    }

    /**
     * Ejecutado automáticamente cuando el mapa ha terminado de cargar y se hace presentable.
     *
     * @param googleMap El mapa que acaba de reportar que está listo.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Almacenando referencia a mapa y configurándolo, haciéndolo visible.
        mapa = googleMap;

        prepararMapa();

        // Se toma un curso de acción dependiendo de MyLocation y regresandoDeSettings.
        if (regresandoDeSettings) {
            // Si se está regresando de Settings, se toma un curso de acción basándose en MyLocation
            regresoDeSettings();
        } else {
            // Si MyLocation no está activado,
            activarMyLocation();
        }
    }

    /**
     * Diálogo mostrado al usuario cuando se requieren permisos de ACCESS_FINE_LOCATION-
     *
     * @param solicitud Objeto solicitud de permisos.
     */
    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void mostrarDialogoPermisosUbicacion(final PermissionRequest solicitud) {
        new AlertDialog.Builder(this).setPositiveButton(R.string.button_permitir, new DialogInterface.OnClickListener() {
            /** Botón "Permitir" */
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                solicitud.proceed();
            }
        }).setNegativeButton(R.string.button_denegar, new DialogInterface.OnClickListener() {
            /** Botón "Denegar" */
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {
                solicitud.cancel();
            }
        }).setCancelable(false).setMessage(R.string.permiso_ubicacion_razon).show();
    }

    /**
     * Llamado cuando se encuentra un error crítico de permisos.
     * Elimina el mapa si está disponible, y muestra un mensaje de error en su lugar.
     */
    void errorPermisos() {
        contenedorFragmentMapa.setVisibility(View.GONE);
        mensajeNoMapa.setVisibility(View.VISIBLE);
    }

    /**
     * Llamado cuando el usuario rechaza los permisos selecciona "nunca volver a preguntar".
     *
     * Muestra un mensaje de error en lugar del mapa ya que no se pueden continuar sin permisos
     * de ACCESS_FINE_LOCATION, y al mismo tiempo recuerda que el usuario no desea volver a activar
     * los permisos.
     *
     * Para que la aplicación funcione, el usuario debe navegar por su sí mismo a Settings y
     * conceder permisos de Ubicación a esta aplicación.
     */
    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onPermisoUbicacionDenegadoPermanentemente() {
        Toast.makeText(this, R.string.permiso_ubicacion_no_preguntar, Toast.LENGTH_SHORT).show();
        errorPermisos();
    }

    /**
     * Llamado cuando el usuario rechaza los permisos sin seleccionar "nunca volver a preguntar".
     *
     * Muestra un mensaje de error ya que no se puede continuar sin permiso ACCESS_FINE_LOCATION.
     * El métodono recuerda la elección, así que se pediraán permisos la próxima vez que el
     * usuario entre a la Activity.
     */
    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onPermisoUbicacionDenegado() {
        errorPermisos();
    }

    /**
     * Delega el manejo de permisos a la librería PermissionDispatcher. Delega el chequeo de
     * permisos a una clase generada dinámicamente por la misma librería.
     *
     * @param requestCode Código identificador de petición.
     * @param permissions Arreglo de permisos. Ver Manifest.permission.*
     * @param grantResults Arreglo de resultados de peticiones de permisos.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MapaActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    /**
     * Método ejecutado cuando el usuario regresa a la Activity desde la pantalla de Settings.
     *
     * En adición a actualizar el valor de regresandoDeSettings, el método intenta activar MyLocation
     * nuevamente si el mapa está listo para ser usado.
     *
     * activarMyLocation() toma un curso de acción dependiendo de su valor actual.
     *
     * TODO: renombrar este método? **Indirectamente toma un curso de accíón.**
     */
    public void regresoDeSettings() {
        if (regresandoDeSettings && mapa != null) {
            regresandoDeSettings = false;

            activarMyLocation();
            return;
        }

        return;
    }

    /**
     * Parte del ciclo de vida de una Activity. Ejecutado al regresar a la Activity despueés de
     * haberse hecho parcialmente invisible o después de onStart o después de onPause. Siempre
     * actualiza el valor de estadoGPS dependiendo del estado actual del GPS del dispositivo.
     *
     * Si el mapa está listo y se está regresando de Settings, entonces se toma un curso de
     * acción. De lo contrario, no se hace más nada.
     */
    @Override
    public void onResume() {
        super.onResume();

        actualizarEstadoGPS();

        if (mapa != null) {
            if (regresandoDeSettings) {
                regresoDeSettings();
            } else if (actualizarMyLocation) {
                activarMyLocation();
            }
        }
    }

    /**
     * Carga el mapa, solicitando permisos en Android >= 6 si llega a ser necesario.
     * La comprobación de permisos es manejada por la librería PermissionDispatcher.
     */
    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void cargarMapa() {
        if (mapa == null) {
            fragmentMapa.getMapAsync(this);
        }
    }

    /**
     * Maneja evento Connected con GooglePlayServices.
     *
     * TODO: implementar manejo de error? Es realmente necesario? Existe isConnected()
     *
     * @param bundle Datos recibidos.
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {}

    /**
     * Maneja el evento ConnectionSuspended con GooglePlayServices. Por el momento no hace nada.
     *
     * TODO: implementar manejo de error? Es realmente necesario? Existe isConnected()
     *
     * @param i Código de causa de suspensión de conexión.
     */
    @Override
    public void onConnectionSuspended(int i) {}

    /**
     * Maneja evento ConnectionFailed de GooglePlayServices. Por el momento no hace nada.
     *
     * TODO: implementar manejo de error? Es realmente necesario? Existe isConnected()
     *
     * @param connectionResult Objeto con detalles sobre el resultado de conexión, incluyendo error.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    /**
     * Parte del ciclo de vida de toda Activity. Es llamado cuando la Activity no es visible,
     * después de haber llamado onResume.
     *
     * Suspende la conexión con GooglePlayServices.
     */
    @Override
    public void onStop() {
        // Se desconecta de GooglePlayServices. Se reintentará re-conectar en onStart()
        if (serviciosGooglePlay != null && serviciosGooglePlay.isConnected()) {
            serviciosGooglePlay.disconnect();
        }

        super.onStop();
    }

    /**
     * Parte del ciclo de vida de toda Activity. Es llamado después de onCreate, o después de
     * onRestart si la Activity está siendo re-creada después de haber sido matada por Android.
     *
     * Inicializa locationManager, preferenciasLocales y el mapa individualmente si no están
     * disponibles.
     *
     * El mapa es inicializado condicionalmente dependiendo de los permisos de la aplicación en
     * Android >= 6. La comprobación de permmisos es manejada por la librería PermissionDispatcher.
     */
    @Override
    public void onStart() {
        // Se conecta a GooglePlayServices para acceder a su LocationService.
        if (serviciosGooglePlay != null && !serviciosGooglePlay.isConnected() && !serviciosGooglePlay.isConnecting()) {
            serviciosGooglePlay.connect();
        }

        super.onStart();

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }

        if (preferenciasLocales == null) {
            preferenciasLocales = getPreferences(MODE_PRIVATE);
        }

        // ParmissionDispatcher
        MapaActivityPermissionsDispatcher.cargarMapaWithCheck(this);
    }

    /**
     * Sobreescrito de UbicacionUTBActivity. Inicializa el Layout de esta Activity.
     */
    @Override
    protected void inicializarLayout() {
        setContentView(R.layout.activity_mapa);

        fragmentMapa = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapa_fragment);

        contenedorFragmentMapa = (LinearLayout) findViewById(R.id.mapa_contenedor);
        mensajeNoMapa = (LinearLayout) findViewById(R.id.mapa_mensaje_error);
    }

    /**
     * Restaura propiedades que fueron guardadas antes de que Android matara esta Activity.
     *
     * Es llamado después de onStart y savedInstanceState siempre existe en este método, si es
     * invocado (no siempre es invocado.) savedInstanceState también está disponible en onCreate,
     * pero puede ser null si la Activity está siendo creada sin un estado anterior almacenado.
     *
     * @param savedInstanceState Estado anterior de la Activity que está siendo reiniciada.
     */
    @Override
    public void onRestoreInstanceState (Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        accion = savedInstanceState.getInt(PROPIEDAD_ACCION, ACCION_NO_ESPECIFICADA);
        regresandoDeSettings = savedInstanceState.getBoolean(PROPIEDAD_REGRESO_SETTINGS, false);
        estadoGPS = savedInstanceState.getInt(PROPIEDAD_ESTADO_GPS, ESTADO_GPS_DESCONOCIDO);
        regresandoDeSettings = savedInstanceState.getBoolean(PROPIEDAD_ACTUALIZAR_MY_LOCATION, false);
        latitudIntent = savedInstanceState.getDouble(PROPIEDAD_LATITUD_INTENT, DEFAULT_LATITUD);
        longitudIntent = savedInstanceState.getDouble(PROPIEDAD_LONGITUD_INTENT, DEFAULT_LONGITUD);
        latitudMapa = savedInstanceState.getDouble(PROPIEDAD_LATITUD_MAPA, latitudIntent);
        longitudMapa = savedInstanceState.getDouble(PROPIEDAD_LONGITUD_MAPA, longitudIntent);
        zoomMapa = savedInstanceState.getFloat(PROPIEDAD_ZOOM, DEFAULT_ZOOM);
        rotacionMapa = savedInstanceState.getFloat(PROPIEDAD_ROTACION, DEFAULT_ROTACION);
    }

    /**
     * Almacena algunas propiedades antes de que la Activity sea matada por Android.
     *
     * Los datos almacenados en outState en este método siempre están disponibles en
     * onRestoreInstanceState. También, si onCreate es llamado después de que Android
     * destruyera esta Activity, entonces su savedInstanceState no será null.
     *
     * @param outState Objeto sobre el cual se guardarán propiedades para uso futuro.
     */
    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(PROPIEDAD_ACCION, accion);
        outState.putBoolean(PROPIEDAD_REGRESO_SETTINGS, regresandoDeSettings);
        outState.putInt(PROPIEDAD_ESTADO_GPS, estadoGPS);
        outState.putBoolean(PROPIEDAD_ACTUALIZAR_MY_LOCATION, actualizarMyLocation);
        outState.putDouble(PROPIEDAD_LATITUD_INTENT, latitudIntent);
        outState.putDouble(PROPIEDAD_LONGITUD_INTENT, longitudIntent);
        outState.putDouble(PROPIEDAD_LATITUD_MAPA, latitudMapa);
        outState.putDouble(PROPIEDAD_LONGITUD_MAPA, longitudMapa);
        outState.putFloat(PROPIEDAD_ZOOM, zoomMapa);
        outState.putFloat(PROPIEDAD_ROTACION, rotacionMapa);
    }

    /**
     * Crea una conexión con GooglePlayServices e inicializa datos que usados por esta Activity.
     *
     * Si la Activity fue iniciada con un Intent con datos "extra" asociados, entonces se usarán
     * esos datos en la Activity. De lo contrario, entonces se usarán valores por defecto
     * razonables.
     *
     * @param savedInstanceState Estado de ejecución anterior de la Activity, si existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle informacionInicio = getIntent().getExtras();

        if (informacionInicio != null) {
            accion = informacionInicio.getInt(PROPIEDAD_ACCION, ACCION_NO_ESPECIFICADA);
            regresandoDeSettings = informacionInicio.getBoolean(PROPIEDAD_REGRESO_SETTINGS, false);
            estadoGPS = informacionInicio.getInt(PROPIEDAD_ESTADO_GPS, ESTADO_GPS_DESCONOCIDO);
            actualizarMyLocation = informacionInicio.getBoolean(PROPIEDAD_ACTUALIZAR_MY_LOCATION, false);
            latitudIntent = informacionInicio.getDouble(PROPIEDAD_LATITUD_INTENT, DEFAULT_LATITUD);
            longitudIntent = informacionInicio.getDouble(PROPIEDAD_LONGITUD_INTENT, DEFAULT_LONGITUD);
            latitudMapa = informacionInicio.getDouble(PROPIEDAD_LATITUD_MAPA, latitudIntent);
            longitudMapa = informacionInicio.getDouble(PROPIEDAD_LONGITUD_MAPA, longitudIntent);
            zoomMapa = informacionInicio.getFloat(PROPIEDAD_ZOOM, DEFAULT_ZOOM);
            rotacionMapa = informacionInicio.getFloat(PROPIEDAD_ROTACION, DEFAULT_ROTACION);
            primerMovimiento = true;
        } else {
            accion = ACCION_NO_ESPECIFICADA;
            regresandoDeSettings = false;
            actualizarMyLocation = false;
            estadoGPS = ESTADO_GPS_DESCONOCIDO;
            latitudIntent = DEFAULT_LATITUD;
            longitudIntent = DEFAULT_LONGITUD;
            latitudMapa = latitudIntent;
            longitudMapa = longitudIntent;
            zoomMapa = DEFAULT_ZOOM;
            rotacionMapa = DEFAULT_ROTACION;
            primerMovimiento = true;
        }

        // Crea instancia de GooglePlayServices, pero no intenta crear una conexión.
        if (serviciosGooglePlay == null) {
            serviciosGooglePlay = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
            .build();
        }
    }
}