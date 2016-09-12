package utb.desarrollomovil.ubicacionutb;

import android.os.Bundle;

public class MainActivity extends UbicacionUTBActivity {
    /**
     * Inicializa el Layout de la Activity.
     * Es la responsabilidad de cada Activity inicializar y definir su layout.
     */
    @Override
    protected void inicializarLayout() {
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
