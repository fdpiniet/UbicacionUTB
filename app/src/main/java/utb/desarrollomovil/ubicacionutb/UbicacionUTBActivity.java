package utb.desarrollomovil.ubicacionutb;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by Fabian on 11/09/2016.
 *
 * Clase base de toda Activity de Ubicación UTB salvo por ActivityMapa. Define elementos que deben
 * estar presentes en toda Activity de la aplicación, sin excepción.
 *
 * En particular, define el Toolbar (barra superior con título de Activity de toda Activity y su
 * respectivo menú de Toolbar. También define el menú principal a un lado de la pantalla
 * del layout de cada Activity.
 *
 * Esta clase no asigna ningún layout a la Activity. Debe ser especificado por subclases usando
 * el método setContentView.
 *
 * Vea activity_main.xml por un ejemplo de la estructura que debe tener el layout de cada clase
 * hija de UbicacionUTBActivity para su correcto funcionamiento.
 */
public abstract class UbicacionUTBActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    /**
     * Inicializa el Layout de la Activity. Es la responsabilidad de cada UbicacionUTBActivity
     * definir e inicializar sus respectivos layouts.
     */
    protected abstract void inicializarLayout();

    /**
     * Llamado por clases hijas después de onCreate y después de mostrar el layout principal de
     * la Activity (después de inicializarLayout.)
     *
     * Esta implementación configura el toolbar y el posicionamiento de los dos menús principales
     * de la aplicación: el menu principal a la izquierda, y el menú del toolbar en la esquina
     * superior derecha. Sus layouts son insertados en el layout de la Activity y se asigna esta
     * misma clase como handler de sus distintos eventos.
     */
    protected void inicializarMenus() {
        // Asocia el toolbar y su respectivo menú con esta Activity.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configura el drawer de la activity; la ubicación del menú principal lateral.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // Registra esta clase como handler de eventos del menú principal.
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    /** Implementación por defecto: llama el método con el mismo nombre en su superclase. */
    @Override
    public void onRestart() {
        super.onRestart();
    }

    /** Implementación por defecto: llama el método con el mismo nombre en su superclase. */
    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     * Inicializa el layout de esta UbicacionUTBActivity llamando inicializarLayout y
     * inicializarMenus, en ese orden.
     *
     * Una subclase solo requiere implementar el método inicializarLayout.
     *
     * @param savedInstanceState Estado de ejecución anterior de la Activity, si existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inicializarLayout();
        inicializarMenus();
    }

    /** Implementación por defecto: llama el método con el mismo nombre en su superclase. */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /** Implementación por defecto: llama el método con el mismo nombre en su superclase. */
    @Override
    public void onPause() {
        super.onPause();
    }

    /** Implementación por defecto: llama el método con el mismo nombre en su superclase. */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /** Implementación por defecto: llama el método con el mismo nombre en su superclase. */
    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     *  Cierra el menú principal (lateral) si se presiona el boton atrás mientras que está abierto.
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Llena el menú del toolbar con opciones tomadas de res/menu/toolbar.xml
     *
     * @param menu El menú que será llenado de elementos.
     * @return Es true si y solo si el menú es mostraod despues de invocar este método.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    /**
     * Llamado cada vez que se presiona una opción en el menú del toolbar.
     *
     * @param item El elemento que fue presionado.
     * @return Si el evento generado al presionar el boton es consumido en el método, regresa true.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Si un elemento fue presionado, entonces cual fue? Se busca su ID.
        int id = item.getItemId();

        // Se toma un curso de acción dependiendo del ID.
        if (id == R.id.action_about) {
            Log.d(this.getClass().getName(), "Diálogo 'Acerca De' no implementado / enlazado.");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Implementación por defecto: llama el método con el mismo nombre en su superclase.
     *
     * @param savedInstanceState Estado anterior de pa Activity que está siendo restaurado.
     */
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * Implementación por defecto: llama el método con el mismo nombre en su superclase.
     *
     * @param outState Objeto sobre el cual se guardaran propiedades para uso futuro.
     */
    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Ejecutado cada vez que se selecciona un elemento en el menú principal lateral de la Activity.
     * Actua como un puente entre las Activities principales de la aplicación.
     *
     * @param item El objeto siendo seleccionado.
     * @return true Si el elemento tocado será mostrado como un elemento seleccionado.
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Referencia al drawer asociado con el elemento seleccioando.
        DrawerLayout drawer = (DrawerLayout) this.findViewById(R.id.drawer_layout);

        // Usado para iniciar otras activities dependiendo del curso de acción.
        Intent siguienteAcitivty;

        // Se presionó un elemento. Cual? Se identificará por ID.
        int id = item.getItemId();

        // Se toma un curso de acción distinto dependiendo del elemento seleccionado.
        if (id == R.id.nav_inicio) {
            // Se muestra MainActivity
            siguienteAcitivty = new Intent(this, MainActivity.class);
            drawer.closeDrawer(GravityCompat.START);
            this.startActivity(siguienteAcitivty);
            return true;

        } else if (id == R.id.nav_mapa) {
            // Se muestra el maapa, sin mostrar algún lugar en específico.
            drawer.closeDrawer(GravityCompat.START);
            MapaActivity.lanzarMapa(this);
            return true;

        } else if (id == R.id.nav_categorias) {
            // Se muesra un listado cetegórico de ubicaciones.
            siguienteAcitivty = new Intent(this, CategoriasActivity.class);
            drawer.closeDrawer(GravityCompat.START);
            this.startActivity(siguienteAcitivty);
            return true;

        } else if (id == R.id.nav_buscar) {
            // Se muestra formulario de búsqueda de ubicaciones.
            siguienteAcitivty = new Intent(this, BuscarActivity.class);
            drawer.closeDrawer(GravityCompat.START);
            this.startActivity(siguienteAcitivty);
            return true;

        } else if (id == R.id.nav_horario) {
            // Se muestra el horario de un usuario autenticado. La opción puede o no estar oculta.
            siguienteAcitivty = new Intent(this, UsuarioActivity.class);
            drawer.closeDrawer(GravityCompat.START);
            this.startActivity(siguienteAcitivty);
            return true;

        } else if (id == R.id.nav_iniciar_sesion) {
            // Se muestra formulario de inicio de sesión. La opción puede estar oculta.
            drawer.closeDrawer(GravityCompat.START);
            Log.d(this.getClass().getName(), "Diálogo de inicio de sesión no implementado / enlazado.");
            return true;

        } else if (id == R.id.nav_cerrar_sesion) {
            // Se muestra mensaje de confirmación de fin de sesión. La opción puede estar oculta.
            drawer.closeDrawer(GravityCompat.START);
            Log.d(this.getClass().getName(), "Diálogo de cierre de sesión no implementado / enlazado.");
            return true;

        } else if (id == R.id.nav_ajustes) {
            // Se muestra un menú con opciones de ajustes de la aplicación.
            drawer.closeDrawer(GravityCompat.START);
            Log.d(this.getClass().getName(), "Diálogo de ajustes no implementado / enlazado.");
            return true;
        }

        return false;
    }
}
