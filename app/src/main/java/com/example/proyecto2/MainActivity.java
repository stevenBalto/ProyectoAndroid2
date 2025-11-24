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

public class MainActivity extends AppCompatActivity {

    LinearLayout layoutLista;
    EditText txtBuscar;
    ListView listViewClientes;
    TextView btnAgregar, btnEditar, btnEliminar, btnSalir;

    ArrayList<Cliente> listaClientes = new ArrayList<>();
    ClienteAdapter adapter;

    DatabaseHelper db;
    String cedulaSeleccionada = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);

        layoutLista = findViewById(R.id.layoutLista);
        txtBuscar = findViewById(R.id.txtBuscar);
        listViewClientes = findViewById(R.id.listViewClientes);

        btnAgregar = findViewById(R.id.btnAgregar);
        btnEditar = findViewById(R.id.btnEditar);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnSalir = findViewById(R.id.btnSalir);

        txtBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cargarClientes(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        listViewClientes.setOnItemClickListener((parent, view, position, id) -> {
            Cliente c = listaClientes.get(position);
            cedulaSeleccionada = c.getCedula();
            actualizarEstadoBotones(true);
        });

        cargarClientes("");

        btnAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ClientesActivity.class);
            startActivity(intent);
        });

        btnEditar.setOnClickListener(v -> {
            if (cedulaSeleccionada != null) {
                Intent intent = new Intent(MainActivity.this, ClientesActivity.class);
                intent.putExtra("cedula", cedulaSeleccionada);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Seleccione un cliente", Toast.LENGTH_SHORT).show();
            }
        });

        btnEliminar.setOnClickListener(v -> {
            if (cedulaSeleccionada != null) {
                SQLiteDatabase writable = db.getWritableDatabase();
                writable.delete("cliente", "cedula=?", new String[]{cedulaSeleccionada});
                txtBuscar.setText("");
                cargarClientes("");
                cedulaSeleccionada = null;
                Toast.makeText(this, "Cliente eliminado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Seleccione un cliente primero", Toast.LENGTH_SHORT).show();
            }
        });

        btnSalir.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarClientes(txtBuscar.getText().toString());
    }

    private void actualizarEstadoBotones(boolean habilitados) {
        btnEditar.setEnabled(habilitados);
        btnEliminar.setEnabled(habilitados);

        if (habilitados) {
            btnEditar.setAlpha(1.0f);
            btnEliminar.setAlpha(1.0f);
        } else {
            btnEditar.setAlpha(0.5f);
            btnEliminar.setAlpha(0.5f);
        }
    }

    private void cargarClientes(String query) {
        listaClientes.clear();
        SQLiteDatabase readable = db.getReadableDatabase();
        Cursor c;

        if (query == null || query.trim().isEmpty()) {
            c = readable.rawQuery("SELECT cedula, nombre, telefono FROM cliente ORDER BY nombre", null);
        } else {
            String likeQuery = "%" + query + "%";
            c = readable.rawQuery("SELECT cedula, nombre, telefono FROM cliente WHERE nombre LIKE ? OR cedula LIKE ? ORDER BY nombre", new String[]{likeQuery, likeQuery});
        }

        while (c.moveToNext()) {
            Cliente cli = new Cliente(c.getString(0), c.getString(1), c.getString(2));
            listaClientes.add(cli);
        }
        c.close();

        adapter = new ClienteAdapter(this, listaClientes);
        listViewClientes.setAdapter(adapter);
        
        cedulaSeleccionada = null;
        actualizarEstadoBotones(false);
    }

    public class ClienteAdapter extends ArrayAdapter<Cliente> {
        public ClienteAdapter(Context context, ArrayList<Cliente> clientes) {
            super(context, 0, clientes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
             if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            Cliente cliente = getItem(position);
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);
            if(text1 != null) text1.setText(cliente.getNombre());
            if(text2 != null) text2.setText("Cédula: " + cliente.getCedula() + " | Tel: " + cliente.getTelefono());
            return convertView;
        }
    }

    public class Cliente {
        private String cedula, nombre, telefono;
        public Cliente(String cedula, String nombre, String telefono) {
            this.cedula = cedula;
            this.nombre = nombre;
            this.telefono = telefono;
        }
        public String getCedula() { return cedula; }
        public String getNombre() { return nombre; }
        public String getTelefono() { return telefono; }
    }
}