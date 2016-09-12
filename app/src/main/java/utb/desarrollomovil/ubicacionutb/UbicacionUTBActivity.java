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
 * respectivo menú de Toolbar. También define el menú principal (Drawer) a un lado de la pantalla
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
     * Inicializa el Layout de la Activity.
     * Es la responsabilidad de cada Activity inicializar y definir su layout.
     */
    protected abstract void inicializarLayout();

    /**
     * Llamado desde constructores de clases hijas después de onCreate y después de mostrar el
     * layout principal de la Activity.
     */
    protected void inicializarMenus() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inicializarLayout();
        inicializarMenus();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Log.d(this.getClass().getName(), "Diálogo 'Acerca De' no implementado / enlazado.");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when an item in the navigation menu is selected.
     *
     * @param item The selected item
     * @return true to display the item as the selected item
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Intent siguienteAcitivty;

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_inicio) {
            siguienteAcitivty = new Intent(this, MainActivity.class);
            this.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_mapa) {
            siguienteAcitivty = new Intent(this, MapaActivity.class);
            this.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_categorias) {
            siguienteAcitivty = new Intent(this, CategoriasActivity.class);
            this.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_buscar) {
            siguienteAcitivty = new Intent(this, BuscarActivity.class);
            this.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_horario) {
            siguienteAcitivty = new Intent(this, UsuarioActivity.class);
            this.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_iniciar_sesion) {
            Log.d(this.getClass().getName(), "Diálogo de inicio de sesión no implementado / enlazado.");
        } else if (id == R.id.nav_cerrar_sesion) {
            Log.d(this.getClass().getName(), "Diálogo de cierre de sesión no implementado / enlazado.");
        } else if (id == R.id.nav_ajustes) {
            Log.d(this.getClass().getName(), "Diálogo de ajustes no implementado / enlazado.");
        }

        DrawerLayout drawer = (DrawerLayout) this.findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
