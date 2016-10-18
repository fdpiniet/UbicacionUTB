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
 * llamar el método get o set para hacer un request a un recurso JSON.
 *
 * La interfaz interna HandlerClienteJSON debe ser implementada por toda clase que desea ser
 * notificada cuando ocurren eventos emitidos por el request. Notablemente los events de éxito y
 * de fracaso.
 *
 * Se recomienda como mínimo implementar completamente requestJSONExitoso y requestJSONFallido.
 * El resto de los métodos de la interfaz HandlerClienteJSON pueden ser dejados en blanco, pero
 * es importante recalcar que el método requestJSONFinalizado es ejecutado al finalizar el
 * request, fracasado o no.
 *
 * NOTA: esta clase usa extensivamente la librería "Android Async HTTP" de James Smith ("loopj" en
 * Github). Disponible en:
 *      -- http://loopj.com/android-async-http/
 *      -- https://github.com/loopj/android-async-http
 */

public class ClienteJSON extends JsonHttpResponseHandler {
    // Instancia única y global a travez del cual se hará nrequests GET o POST.
    private final static AsyncHttpClient client = new AsyncHttpClient();

    // Referencia interna al handler que recibirá invocaciones en respuesta a eneetos de esta clase.
    private HandlerClienteJSON handler;

    /**
     * En esta intarfaz, un request es exitoso si su código de estado es exactamente 200.
     *
     * Si en onSuccess por alguna razón la respuesta es obtenida como un String y no como un
     * JSONObject o un JSONArray, entonces el caso será tratado como un error ya que esta clase
     * sólo trabaja con JSON. Esto no debería ocurrir.
     *
     * Tenga en cuenta que requestJSONFinalizado es llamado desde onFinish, significando que
     * es llamado al finalizar un request *con o sin* errores.
     *
     * JSONObject y JSONArray no comparten ancestros fuera de Object, así que debe realizarse
     * un type cast a JSONObject o a JSONArray, respectivamente.
     *
     * En requestJSONExitoso, resultado puede ser null. Se debe verificar su valor antes de usar.
     */
    public interface HandlerClienteJSON {
        void requestJSONExitoso(Object resultado, int codigoEstado);
        void requestJSONFallido(int codigoEstado);
        void requestJSONIniciado();
        void requestJSONFinalizado();
        void requestJSONReintentado(int numeroReintento);
    }

    /**
     * Ejecutado internamente si el request JSON fue exitoso.
     *
     * Si el request fue exitoso y si su código de estado HTTP es 200, entonces se invoca el
     * método requestJSONExitoso en el handler de eventos de esta instancia.
     *
     * Este método es llamado sólo si la respuesta es un objeto JSONObject. Si es un JSONArray,
     * por favor revise la versión onSuccess correspondiente a "JSONArray response".
     *
     * @param statusCode Código de estado HTTP. Puede ser 0 en errores del sistema.
     * @param headers Headers recibidos en la respuesta. Puede ser null.
     * @param response Respuesta recibida. Puede ser null. Es pasada a requestJSONExitoso como Object
     */
    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        super.onSuccess(statusCode, headers, response);

        if (statusCode == 200){
            handler.requestJSONExitoso(response, statusCode);
            return;
        }

        handler.requestJSONFallido(statusCode);
    }

    /**
     * Ejecutado internamente si el request JSON fue exitoso.
     *
     * Si el request fue exitoso y si su código de estado HTTP es 200, entonces se invoca el
     * método requestJSONExitoso en el handler de eventos de esta instancia.
     *
     * Este método es llamado sólo si la respuesta es un objeto JSONObject. Si es un JSONArray,
     * por favor revise la versión onSuccess correspondiente a "JSONArray response".
     *
     * @param statusCode Código de estado HTTP. Puede ser 0 en errores del sistema.
     * @param headers Headers recibidos en la respuesta. Puede ser null.
     * @param response Respuesta recibida. Puede ser null. Es pasada a requestJSONExitoso como Object
     */
    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
        super.onSuccess(statusCode, headers, response);

        if (statusCode == 200){
            handler.requestJSONExitoso(response, statusCode);
            return;
        }

        handler.requestJSONFallido(statusCode);
    }

    /**
     * Ejecutado internamente si un request JSON fue "exitoso", pero si la respuesta no es un
     * JSONObject o un JSONArray.
     *
     * Internamente, consideramos esto un caso de error. Puede indicar la presencia de un JSON mal
     * formado o incorrectamente procesado. Como resultado, este método invoca requestJSONFallido
     * en el handler de eventos de esta instancia.
     *
     * @param statusCode Código de estado HTTP. Puede ser 0 en errores del sistema.
     * @param headers Headers recibidos en la respuesta. Puede ser null.
     * @param responseString Respuesta recibida. Puede ser null.
     */
    @Override
    public void onSuccess(int statusCode, Header[] headers, String responseString) {
        super.onSuccess(statusCode, headers, responseString);
        handler.requestJSONFallido(statusCode);
    }

    /**
     * Ejecutado en caso de error si la respuesta de error resulta ser un JSONObject válido.
     * Se llama el método requestJSONFallido en el handler de eventos de esta instancia.
     *
     * @param statusCode Código de error. Puede ser 0 en errores del sistema.
     * @param headers Headers recibidos en la respuesta. Puede ser null.
     * @param throwable Excepción correspondiente al error del request.
     * @param errorResponse Datos recibidos de respuesta. Puede ser null.
     */
    @Override
    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        super.onFailure(statusCode, headers, throwable, errorResponse);
        handler.requestJSONFallido(statusCode);
    }

    /**
     * Ejecutado en caso de error si la respuesta de error resulta ser un JSONArray válido.
     * Se llama el método requestJSONFallido en el handler de eventos de esta instancia.
     *
     * @param statusCode Código de error. Puede ser 0 en errores del sistema.
     * @param headers Headers recibidos en la respuesta. Puede ser null.
     * @param throwable Excepción correspondiente al error del request.
     * @param errorResponse Datos recibidos de respuesta. Puede ser null.
     */
    @Override
    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
        super.onFailure(statusCode, headers, throwable, errorResponse);
        handler.requestJSONFallido(statusCode);
    }

    /**
     * Ejecutado en caso de error si la respuesta de error resulta es un String.
     * Se llama el método requestJSONFallido en el handler de eventos de esta instancia.
     *
     * @param statusCode Código de error. Puede ser 0 en errores del sistema.
     * @param headers Headers recibidos en la respuesta. Puede ser null
     * @param responseString Datos recibidos de respuesta. Puede ser null.                .
     * @param throwable Excepción correspondiente al error del request.
     */
    @Override
    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
        super.onFailure(statusCode, headers, responseString, throwable);
        handler.requestJSONFallido(statusCode);
    }

    /** Llamado al comenzar request. Invoca requestJSONIniciado en handler de eventos de instancia. */
    @Override
    public void onStart() {
        super.onStart();
        handler.requestJSONIniciado();
    }

    /** Llamado al terminar request; *exitosamente o no.* Invoca requestJSONFinalizado en handler. */
    @Override
    public void onFinish() {
        super.onFinish();
        handler.requestJSONFinalizado();
    }

    /**
     * Llamado después de cada reintento. Invoca requestJSONReintentado en handler de eventos.
     *
     * @param retryNo Número de reintento.
     */
    @Override
    public void onRetry(int retryNo) {
        super.onRetry(retryNo);
        handler.requestJSONReintentado(retryNo);
    }

    /**
     * Inicia una solicitud HTTP GET.
     *
     * @param url URL de recurso JSON.
     */
    public void get(String url) {
        get(url, null);
    }

    /**
     * Inicia una solicitud HTTP POST.
     *
     * @param url URL de recurso JSON.
     */
    public void post(String url) {
        post(url, null);
    }

    /**
     * Inicia una solicitud HTTP GET usando ciertos parámetros.
     *
     * @param url URL de recurso JSON.
     * @param parametros Parámetros de request.
     */
    public void get(String url, RequestParams parametros) {
        client.get(url, parametros, this);
    }

    /**
     * Inicia una solicitud HTTP POST usando ciertos parámetros.
     *
     * @param url URL de recurso JSON.
     * @param parametros Parámetros de request.
     */
    public void post(String url, RequestParams parametros) {
        client.post(url, parametros, this);
    }

    /**
     * Crea un nuevo ClienteJSON, registrando a handlerRespuesta como handler de todos los evnetos
     * relevantes a requests.
     *
     * @param handlerRespuestas Handler de eventos de requests.
     */
    public ClienteJSON(HandlerClienteJSON handlerRespuestas) {
        this.handler = handlerRespuestas;
    }
}
