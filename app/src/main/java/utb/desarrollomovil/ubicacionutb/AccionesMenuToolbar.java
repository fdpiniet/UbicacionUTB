package utb.desarrollomovil.ubicacionutb;

import android.app.Activity;
import android.util.Log;
import android.view.MenuItem;

/**
 * Created by Fabian on 11/09/2016.
 *
 * Maneja eventos del menú lateral principal.
 *
 * Toda Activity con un toolbar que necesita mostrar el menú del mismo toolbar debe crear una
 * instancia de este objeto y manejar onOptionsItemSelected llamando al método con el mismo nombre
 * de un objeto de este tipo.
 *
 * Este método debe regresar true si una opción del menú de toolbar fue seleccionada, o debe
 * regresar false de lo contrario. Dentro del método sobreescrito con el mismo nombre en la
 * Activity que desea usar menú de toolar, dicho método debe regresar true si este método regresa
 * true, o debe regresar super.onOptionsItemSelected(item) en caso contrario. Esto es un requisito
 * de implementación del método onOptionsItemSelected de la clase Activity.
 *
 * TO-DO: buscar una manera mas limpia y elegante de manejar eventos onOptionsItemSelected fuera
 * de Activity.
 */
public class AccionesMenuToolbar {
    protected Activity contexto;

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

        return false;
    }

    protected AccionesMenuToolbar(Activity contexto) {
        this.contexto = contexto;
    }
}
