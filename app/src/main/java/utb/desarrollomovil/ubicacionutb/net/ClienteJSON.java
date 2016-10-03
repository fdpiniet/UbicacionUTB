package utb.desarrollomovil.ubicacionutb.net;

import com.loopj.android.http.*;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Fabian on 3/10/2016.
 *
 * Simplifica el manejo de requests HTML y de lectura de JSON.
 *
 * Para usar la clase, se debe crear una instancia desde una Activity (u otra clase Java) y se debe
 * llamar el método get o set para hacer un request HTTP a un recurso JSON.
 *
 * La interfaz interna HandlerClienteJSON debe ser implementada por toda clase que desea ser
 * notificada cuando ocurren eventos emitidos por el request. Notablemente los events de éxito y
 * de fracaso.
 *
 * Se recomienda como mínimo implementar completamente requestJSONExitoso y requestJSONFallido.
 * El resto de los métodos de la interfaz HandlerClienteJSON pueden ser dejados en blanco, pero
 * es importante recalcar que el método requestJSONFinalizado es ejecutado al finalizar el
 * request, fracasado o no.
 */

public class ClienteJSON extends JsonHttpResponseHandler {
    private final static AsyncHttpClient client = new AsyncHttpClient();

    private HandlerClienteJSON handler;

    /**
     * En esta intarfaz, un request es exitoso si su código de estado es exactamente 200.
     *
     * Si en onSuccess por alguna razón la respuesta es obtenida como un String y no como un
     * JSONObject o un JSONArray, entonces el caso será tratado como un error ya que esta clase
     * sólo trabaja con JSON.
     *
     * Tenga en cuenta que requestJSONFinalizado es llamado desde onFinish, significando que
     * es llamado al finalizar un request *con o sin* errores.
     *
     * JSONObject y JSONArray no comparten ancestros fuera de Object, así que debe realizarse
     * un type cast a JSONObject o a JSONArray, respectivamente.
     *
     * resultado puede ser null.
     */
    public interface HandlerClienteJSON {
        void requestJSONExitoso(Object resultado, int codigoEstado);
        void requestJSONFallido(int codigoEstado);
        void requestJSONIniciado();
        void requestJSONFinalizado();
        void requestJSONReintentado(int numeroReintento);
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        super.onSuccess(statusCode, headers, response);

        if (statusCode == 200){
            handler.requestJSONExitoso(response, statusCode);
            return;
        }

        handler.requestJSONFallido(statusCode);
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
        super.onSuccess(statusCode, headers, response);

        if (statusCode == 200){
            handler.requestJSONExitoso(response, statusCode);
            return;
        }

        handler.requestJSONFallido(statusCode);
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, String responseString) {
        super.onSuccess(statusCode, headers, responseString);
        handler.requestJSONFallido(statusCode);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        super.onFailure(statusCode, headers, throwable, errorResponse);
        handler.requestJSONFallido(statusCode);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
        super.onFailure(statusCode, headers, throwable, errorResponse);
        handler.requestJSONFallido(statusCode);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
        super.onFailure(statusCode, headers, responseString, throwable);
        handler.requestJSONFallido(statusCode);
    }

    /** Llamado al comenzar request. */
    @Override
    public void onStart() {
        super.onStart();
        handler.requestJSONIniciado();
    }

    /** Llamado al terminar request; *exitosamente o no* */
    @Override
    public void onFinish() {
        super.onFinish();
        handler.requestJSONFinalizado();
    }

    @Override
    public void onRetry(int retryNo) {
        super.onRetry(retryNo);
        handler.requestJSONReintentado(retryNo);
    }

    public void get(String url) {
        client.get(url, this);
    }

    public void post(String url) {
        client.post(url, this);
    }

    public ClienteJSON(HandlerClienteJSON handlerRespuestas) {
        this.handler = handlerRespuestas;
    }
}
