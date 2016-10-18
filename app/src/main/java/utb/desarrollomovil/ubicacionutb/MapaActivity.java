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
import android.view.View;
import android.widget.LinearLayout;
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
 * Las operaciones soportadas por el mapa hasta ahora son (escrito en 2016/10/17):
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
public class MapaActivity extends UbicacionUTBActivity implements OnMapReadyCallback, ClienteJSON.HandlerClienteJSON {
    // "Enumeración" de acciones soportadas por el mapa.
    public static final int ACCION_NINGUNA = 0;
    public static final int ACCION_CENTRAR = 1;
    public static final int ACCION_MOSTRAR_UBICACION = 2;

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
     *   -- PROOPIEDAD_ZOOM: (float) nivel de zoom actual, del punto de vista del mapa.
     *   -- PROOPIEDAD_ROTACION: (float) rotación en grados, del punto de vista del mapa (bearing.)
     */
    public static final String PROPIEDAD_ACCION = "ACCION";
    public static final String PROPIEDAD_REGRESO_SETTINGS = "REGRESO_SETTINGS";
    public static final String PROPIEDAD_LATITUD_MAPA = "LATITUD_MAPA";
    public static final String PROPIEDAD_LONGITUD_MAPA = "LONGITUD_MAPA";
    public static final String PROPIEDAD_LATITUD_INTENT = "LATITUD_INTENT";
    public static final String PROPIEDAD_LONGITUD_INTENT = "LONGITUD_INTENT";
    public static final String PROPIEDAD_ZOOM = "ZOOM_MAPA";
    public static final String PROPIEDAD_ROTACION = "ROTACION_MAPA";

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

    // Posición recibida de la Activity que inició este mapa con un Intent.
    private double latitudIntent;
    private double longitudIntent;

    // Posición actual del mapa, de su punto de vista actual. No confundir con posición del usuario.
    private double latitudMapa;
    private double longitudMapa;

    // Niveles de zoom y rotación actuales, del punto de vista del mapa.
    private float zoomMapa;
    private float rotacionMapa;

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
        informacionInicio.putInt(PROPIEDAD_ACCION, ACCION_NINGUNA);
        // ... agregar más propiedades aquí si llega a ser necesario.

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

    /**
     * Procesa la acción "acción" y posteriormente cambia su valor a ACCION_NINGUNA, dejando el mapa
     * listo pare realizar otras operaciones.
     *
     * No hace nada si la acción es ACCION_NINGUNA.
     *
     * TODO: terminar. Definir e implementar más acciones.
     */
    protected void procesarComando() {
        Toast.makeText(this, "DEBUG: funciona()", Toast.LENGTH_SHORT).show();

        CameraPosition inicial = (new CameraPosition.Builder()).target(COORDENADAS_LIMITES_UNIVERSIDAD.getCenter()).zoom(DEFAULT_ZOOM).bearing(DEFAULT_ROTACION).build();
        mapa.moveCamera(CameraUpdateFactory.newCameraPosition(inicial));
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
                        procesarComando();
                    }
                }).create().show();
    }

    /**
     * Intenta activar MyLocation y luego ejecuta procesarComando()
     *
     * Si el mapa no está aún cargado, o si no se tienen permisos de ACCESS_FINE_LOCATION, entonces
     * el mapa es reemplazado por un mensaje de error. Es un error crítico.
     *
     * Si MyLocation está activado, entonces se actualiza la _preferencia_ DESACTIVAR_MY_LOCATION
     * a false y se llama procesarComando()
     *
     * Si está activado el proveedor de GPS en modo alta precisión, entonces se actualiza el valor
     * de la _preferencia_ DESACTIVAR_MY_LOCATION a false, se activa MyLocation y se llama
     * el método procesarComando()
     *
     * Si ninguna de las anteriores condiciones se cumple y el valor de la _preferencia_
     * DESACTIVAR_MY_LOCATION es false, entonces se le solicita al usuario que active el modo
     * alta precisión del GPS en un diálogo.
     *
     *      -- En este díalogo, si el usuario presiona cancelar, entonces se actualiza le
     *         _preferencia_ DESACTIVAR_MY_LOCATION a true, se desactiva MyLocation y se ejecuta
     *         el método procesarComando().
     *      -- Si el usuario presiona "Abrir", entonces la Activity es reiniciada, y, después de
     *         reiniciar:
     *              -- Si el GPS está encendido, se activa MyLocation y se llama procesarComando()
     *              -- Si el GPS está desactivado, entonces MyLocation permanecerá desactivado y
     *                 se llamará procesarComando().
     *
     * Si no se cumple ninguna de las anteriores condiciones (es decir, si la _preferencia_
     * de DESACTIVAR_MY_LOCATION es true), entonces no se presenta ningún diálogo. MyLocation
     * permanceré "permanentemente" desactivado hasta que el usuario manualmente active el modo
     * alta precisión de su GPS. Se concluye invocando procesarComando().
     *
     * TODO: renombrar método? Su nombre no parece describir exactamente todo lo que hace...
     */
    public void activarMyLocation() {
        String proveedorUbicacion;

        Toast.makeText(this, "DEBUG: activarMyLocationStart()", Toast.LENGTH_SHORT).show();

        // Error ctítico si no hay un mapa o si no se cuenta con suficientes permisos.
        if (mapa == null || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            errorPermisos();
            return;
        }

        // No es necesario hacer nada si MyLocation está activado.
        if (mapa.isMyLocationEnabled()) {
            Toast.makeText(this, "DEBUG: activarMyLocationStart(2if)", Toast.LENGTH_SHORT).show();
            preferenciasLocales.edit().putBoolean(PREFERENCIA_DESACTIVAR_MY_LOCATION, false).commit();
            procesarComando();
            return;
        }

        // Se consulta el proveedor de ubicaciones.
        proveedorUbicacion = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : null;
        Toast.makeText(this, "DEBUG: activarMyLocationStart(4thwall)", Toast.LENGTH_SHORT).show();

        // Si el GPS está en modo alta precisión, entonces se activa MyLocation.
        if (proveedorUbicacion != null) {
            Toast.makeText(this, "DEBUG: activarMyLocationStart(3if)", Toast.LENGTH_SHORT).show();
            preferenciasLocales.edit().putBoolean(PREFERENCIA_DESACTIVAR_MY_LOCATION, false).commit();
            mapa.setMyLocationEnabled(true);
            procesarComando();
            return;
        }

        // Mostrar diálogo si GPS no está en modo alta precisión y DESACTIVAR_MY_LOCATION es false
        if (!preferenciasLocales.getBoolean(PREFERENCIA_DESACTIVAR_MY_LOCATION, false)) {
            Toast.makeText(this, "DEBUG: activarMyLocationStart(4if)", Toast.LENGTH_SHORT).show();
            mostrarDialogoPrecisionGPS();
            return;
        }

        // Si no se cumple nada de lo anterior, entonces se procesa el comando **sin MyLocation**
        procesarComando();
        return;
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

        Toast.makeText(this, "DEBUG: prepararMapa()", Toast.LENGTH_SHORT).show();
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

        Toast.makeText(this, "DEBUG: onMapReady()", Toast.LENGTH_SHORT).show();
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
        if (mapa != null && regresandoDeSettings) {
            regresandoDeSettings = false;

            activarMyLocation();
            return;
        }

        return;
    }

    /**
     * Parte del ciclo de vida de una Activity. Ejecutado al regresar a la Activity despueés de
     * haberse hecho parcialmente invisible o después de onStart o después de onPause.
     *
     * Si el mapa está listo y se está regresando de Settings, entonces se toma un curso de
     * acción. De lo contrario, no se hace nada.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mapa != null && regresandoDeSettings) {
            regresoDeSettings();
        }

        Toast.makeText(this, "DEBUG: onResume()", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "DEBUG: inicializarLayout()", Toast.LENGTH_SHORT).show();

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

        regresandoDeSettings = savedInstanceState.getBoolean(PROPIEDAD_REGRESO_SETTINGS, false);
        accion = savedInstanceState.getInt(PROPIEDAD_ACCION, ACCION_NINGUNA);
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
        outState.putDouble(PROPIEDAD_LATITUD_INTENT, latitudIntent);
        outState.putDouble(PROPIEDAD_LONGITUD_INTENT, longitudIntent);
        outState.putDouble(PROPIEDAD_LATITUD_MAPA, latitudMapa);
        outState.putDouble(PROPIEDAD_LONGITUD_MAPA, longitudMapa);
        outState.putFloat(PROPIEDAD_ZOOM, zoomMapa);
        outState.putFloat(PROPIEDAD_ROTACION, rotacionMapa);
    }

    /**
     * Inicializa datos que serán usados por esta Activity.
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