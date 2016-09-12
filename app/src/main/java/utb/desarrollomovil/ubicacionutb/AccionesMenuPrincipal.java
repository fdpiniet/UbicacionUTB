package utb.desarrollomovil.ubicacionutb;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;

/**
 * Created by Fabian on 11/09/2016.
 *
 * Maneja eventos del menú lateral principal.
 *
 * Toda Activity que necesita mostrar el menú principal debe crear una instancia de este objeto
 * y pasarla como argumento al método setNavigationItemSelectedListener del NavigationView de
 * dicho menú lateral.
 */
public class AccionesMenuPrincipal implements NavigationView.OnNavigationItemSelectedListener {
    protected Activity contexto;

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
            siguienteAcitivty = new Intent(contexto, MainActivity.class);
            contexto.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_mapa) {
            siguienteAcitivty = new Intent(contexto, MapaActivity.class);
            contexto.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_categorias) {
            siguienteAcitivty = new Intent(contexto, CategoriasActivity.class);
            contexto.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_buscar) {
            siguienteAcitivty = new Intent(contexto, BuscarActivity.class);
            contexto.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_horario) {
            siguienteAcitivty = new Intent(contexto, UsuarioActivity.class);
            contexto.startActivity(siguienteAcitivty);
        } else if (id == R.id.nav_iniciar_sesion) {
            Log.d(contexto.getClass().getName(), "Diálogo de inicio de sesión no implementado / enlazado.");
        } else if (id == R.id.nav_cerrar_sesion) {
            Log.d(contexto.getClass().getName(), "Diálogo de cierre de sesión no implementado / enlazado.");
        } else if (id == R.id.nav_ajustes) {
            Log.d(contexto.getClass().getName(), "Diálogo de ajustes no implementado / enlazado.");
        }

        DrawerLayout drawer = (DrawerLayout) contexto.findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected AccionesMenuPrincipal(Activity contexto) {
        this.contexto = contexto;
    }
}
