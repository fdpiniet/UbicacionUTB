package utb.desarrollomovil.ubicacionutb;

import android.os.Bundle;

public class UbicacionActivity extends UbicacionUTBActivity {
    @Override
    protected void inicializarLayout() {
        setContentView(R.layout.activity_ubicacion);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
