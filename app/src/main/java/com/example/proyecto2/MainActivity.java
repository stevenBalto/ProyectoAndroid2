package com.example.proyecto2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Actividad principal que muestra la lista de clientes registrados.
 * Permite buscar, agregar, editar y eliminar clientes.
 */
public class MainActivity extends AppCompatActivity {

    // --- COMPONENTES DE LA INTERFAZ --- //
    LinearLayout layoutLista;
    EditText txtBuscar; // Campo de texto para buscar clientes
    ListView listViewClientes; // Lista para mostrar los clientes
    TextView btnAgregar, btnEditar, btnEliminar, btnSalir; // Botones de acción

    // --- DATOS Y ADAPTADOR --- //
    // Lista que almacena los objetos Cliente
    ArrayList<Cliente> listaClientes = new ArrayList<>();
    // Adaptador personalizado para el ListView
    ClienteAdapter adapter;

    // --- BASE DE DATOS Y SELECCIÓN --- //
    DatabaseHelper db; // Ayudante de la base de datos
    String cedulaSeleccionada = null; // Cédula del cliente actualmente seleccionado en la lista
    int posicionSeleccionada = -1; // Posición del ítem seleccionado en la lista

    /**
     * Se ejecuta al crear la actividad. Inicializa la interfaz, configura los listeners
     * para los botones y el campo de búsqueda, y carga la lista inicial de clientes.
     * @param savedInstanceState Estado previamente guardado de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa el ayudante de la base de datos
        db = new DatabaseHelper(this);

        // --- ENLACE DE COMPONENTES DE LA UI --- //
        layoutLista = findViewById(R.id.layoutLista);
        txtBuscar = findViewById(R.id.txtBuscar);
        listViewClientes = findViewById(R.id.listViewClientes);

        btnAgregar = findViewById(R.id.btnAgregar);
        btnEditar = findViewById(R.id.btnEditar);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnSalir = findViewById(R.id.btnSalir);

        // --- CONFIGURACIÓN DE LISTENERS --- //

        // Listener para el campo de búsqueda que filtra la lista en tiempo real
        txtBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cada vez que el texto cambia, se vuelve a cargar la lista filtrada
                cargarClientes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Listener para clics en los ítems de la lista
        listViewClientes.setOnItemClickListener((parent, view, position, id) -> {
            posicionSeleccionada = position;   // Guarda la posición del ítem seleccionado
            Cliente c = listaClientes.get(position);
            cedulaSeleccionada = c.getCedula(); // Guarda la cédula del cliente seleccionado

            adapter.notifyDataSetChanged();    // Notifica al adaptador para que repinte la lista (y marque la selección)
            actualizarEstadoBotones(true); // Habilita los botones de editar y eliminar
        });

        // Carga inicial de todos los clientes
        cargarClientes("");

        // Listener para el botón "Agregar": abre la actividad de edición para un nuevo cliente
        btnAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ClientesActivity.class);
            startActivity(intent);
        });

        // Listener para el botón "Editar": abre la actividad de edición con los datos del cliente seleccionado
        btnEditar.setOnClickListener(v -> {
            if (cedulaSeleccionada != null) {
                Intent intent = new Intent(MainActivity.this, ClientesActivity.class);
                intent.putExtra("cedula", cedulaSeleccionada); // Pasa la cédula como extra
                startActivity(intent);
            } else {
                Toast.makeText(this, "Seleccione un cliente", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener para el botón "Eliminar": borra el cliente seleccionado de la base de datos
        btnEliminar.setOnClickListener(v -> {
            if (cedulaSeleccionada != null) {
                SQLiteDatabase writable = db.getWritableDatabase();
                writable.delete("cliente", "cedula=?", new String[]{cedulaSeleccionada});
                txtBuscar.setText(""); // Limpia la búsqueda
                cargarClientes(""); // Recarga la lista
                cedulaSeleccionada = null;
                Toast.makeText(this, "Cliente eliminado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Seleccione un cliente primero", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener para el botón "Salir": cierra la aplicación
        btnSalir.setOnClickListener(v -> finish());
    }

    /**
     * Se ejecuta cuando la actividad vuelve a estar en primer plano.
     * Recarga la lista de clientes para reflejar posibles cambios.
     */
    @Override
    protected void onResume() {
        super.onResume();
        cargarClientes(txtBuscar.getText().toString());
    }

    /**
     * Habilita o deshabilita los botones de "Editar" y "Eliminar".
     * También ajusta su apariencia visual para indicar si están activos.
     * @param habilitados true para habilitar los botones, false para deshabilitarlos.
     */
    private void actualizarEstadoBotones(boolean habilitados) {
        btnEditar.setEnabled(habilitados);
        btnEliminar.setEnabled(habilitados);

        if (habilitados) {
            // Opacidad completa cuando están habilitados
            btnEditar.setAlpha(1.0f);
            btnEliminar.setAlpha(1.0f);
        } else {
            // Opacidad reducida cuando están deshabilitados
            btnEditar.setAlpha(0.5f);
            btnEliminar.setAlpha(0.5f);
        }
    }

    /**
     * Carga los clientes desde la base de datos y los muestra en el ListView.
     * Puede filtrar los resultados según un texto de búsqueda.
     * @param query El texto para buscar por nombre o cédula. Si es vacío, carga todos los clientes.
     */
    private void cargarClientes(String query) {
        listaClientes.clear(); // Limpia la lista actual
        SQLiteDatabase readable = db.getReadableDatabase();
        Cursor c;

        // Construye la consulta SQL según si hay un texto de búsqueda o no
        if (query == null || query.trim().isEmpty()) {
            // Sin búsqueda: trae todos los clientes ordenados por nombre
            c = readable.rawQuery("SELECT cedula, nombre, telefono FROM cliente ORDER BY nombre", null);
        } else {
            // Con búsqueda: busca por nombre o cédula usando LIKE
            String likeQuery = "%" + query + "%";
            c = readable.rawQuery("SELECT cedula, nombre, telefono FROM cliente WHERE nombre LIKE ? OR cedula LIKE ? ORDER BY nombre", new String[]{likeQuery, likeQuery});
        }

        // Itera sobre los resultados del cursor y los agrega a la lista
        while (c.moveToNext()) {
            Cliente cli = new Cliente(c.getString(0), c.getString(1), c.getString(2));
            listaClientes.add(cli);
        }
        c.close(); // Cierra el cursor para liberar recursos

        // Crea y establece el adaptador para el ListView
        adapter = new ClienteAdapter(this, listaClientes);
        listViewClientes.setAdapter(adapter);
        
        // Resetea la selección y deshabilita los botones de acción
        cedulaSeleccionada = null;
        posicionSeleccionada = -1;
        actualizarEstadoBotones(false);
    }

    /**
     * Adaptador personalizado para mostrar los clientes en el ListView.
     * Define cómo se visualiza cada ítem de la lista.
     */
    public class ClienteAdapter extends ArrayAdapter<Cliente> {
        public ClienteAdapter(Context context, ArrayList<Cliente> clientes) {
            super(context, 0, clientes);
        }

        /**
         * Obtiene la vista para un ítem específico de la lista.
         * @param position La posición del ítem en el conjunto de datos.
         * @param convertView La vista antigua para reutilizar, si es posible.
         * @param parent El grupo de vistas padre al que esta vista será adjuntada.
         * @return La vista que muestra los datos en la posición especificada.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Reutiliza la vista si es posible, si no, la infla desde el layout
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            // Obtiene el objeto Cliente para esta posición
            Cliente cliente = getItem(position);

            // Obtiene las referencias a los TextView del layout del ítem
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            // Asigna los datos del cliente a los TextView
            text1.setText(cliente.getNombre());
            text2.setText("Cédula: " + cliente.getCedula() + " | Tel: " + cliente.getTelefono());

            // --- MARCAR ÍTEM SELECCIONADO --- //
            // Cambia el color de fondo del ítem si es el que está seleccionado
            if (position == posicionSeleccionada) {
                convertView.setBackgroundColor(0xFFE0F7FA);  // Color celeste suave
            } else {
                convertView.setBackgroundColor(0xFFFFFFFF);  // Color blanco
            }

            return convertView;
        }

    }

    /**
     * Clase modelo que representa a un cliente.
     * Contiene la información básica: cédula, nombre y teléfono.
     */
    public class Cliente {
        private String cedula, nombre, telefono;

        /**
         * Constructor para crear un nuevo objeto Cliente.
         * @param cedula La cédula del cliente.
         * @param nombre El nombre del cliente.
         * @param telefono El teléfono del cliente.
         */
        public Cliente(String cedula, String nombre, String telefono) {
            this.cedula = cedula;
            this.nombre = nombre;
            this.telefono = telefono;
        }

        // --- MÉTODOS GETTER --- //
        public String getCedula() { return cedula; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
    }
}
