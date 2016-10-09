package utb.desarrollomovil.ubicacionutb;

import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import utb.desarrollomovil.ubicacionutb.net.ClienteJSON;

public class MapaActivity extends UbicacionUTBActivity implements OnMapReadyCallback, ClienteJSON.HandlerClienteJSON {
    private SupportMapFragment fragmentMapa;
    private LocationManager locationManager;
    private GoogleMap mapa;
    private ClienteJSON http;

    public void mostrarMapa() {
        /*LatLng ubicacionUTB = new LatLng(10.370337, -75.465449);

        mapa.addMarker(new MarkerOptions().position(ubicacionUTB)).setVisible(true);

        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUTB, 18));
        mapa.animateCamera(CameraUpdateFactory.zoomTo(18), 1, null);*/

        android.widget.Toast.makeText(this, "DEBUG: mostrarMapa()", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
        mostrarMapa();
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

    @Override
    protected void inicializarLayout() {
        setContentView(R.layout.activity_mapa);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        http = new ClienteJSON(this);

        fragmentMapa = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.activity_mapa, fragmentMapa).commit();
        fragmentMapa.getMapAsync(this);
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
}
