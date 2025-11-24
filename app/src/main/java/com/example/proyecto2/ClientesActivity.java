package com.example.proyecto2;


import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class ClientesActivity extends AppCompatActivity {

    LinearLayout layoutLista;
    View layoutFormulario;
    EditText txtBuscar, txtCedula, txtNombre, txtTelefono;
    ListView listViewClientes;
    TextView btnAgregar, btnEditar, btnEliminar, btnSalir;
    Button btnGuardar, btnCancelar;

    ArrayList<Cliente> listaClientes = new ArrayList<>();
    ClienteAdapter adapter;

    DatabaseHelper db;
    String cedulaSeleccionada = null;
    ImageView imgCliente;
    ActivityResultLauncher<Intent> lanzadorTomarFoto;
    Bitmap imagenBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        db = new DatabaseHelper(this);

        layoutLista = findViewById(R.id.layoutLista);
        layoutFormulario = findViewById(R.id.layoutFormulario);

        txtBuscar = findViewById(R.id.txtBuscar);
        txtCedula = findViewById(R.id.txtCedula);
        txtNombre = findViewById(R.id.txtNombre);
        txtTelefono = findViewById(R.id.txtTelefono);
        // APUNTA EL IMG CLIENTE
        imgCliente = findViewById(R.id.imgCliente);


        listViewClientes = findViewById(R.id.listViewClientes);

        btnAgregar = findViewById(R.id.btnAgregar);
        btnEditar = findViewById(R.id.btnEditar);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnSalir = findViewById(R.id.btnSalir);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

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

        btnAgregar.setOnClickListener(v -> mostrarFormulario(null));
        btnEditar.setOnClickListener(v -> {
            if (cedulaSeleccionada != null) {
                Cliente c = buscarPorCedula(cedulaSeleccionada);
                mostrarFormulario(c);
            }
        });
        btnEliminar.setOnClickListener(v -> {
            if (cedulaSeleccionada != null) {
                SQLiteDatabase writable = db.getWritableDatabase();
                writable.delete("cliente", "cedula=?", new String[]{cedulaSeleccionada});
                txtBuscar.setText("");
                cargarClientes("");
            }
        });

        btnSalir.setOnClickListener(v -> {
            if (layoutFormulario.getVisibility() == View.VISIBLE) {
                mostrarLista();
            } else {
                finish();
            }
        });

        btnGuardar.setOnClickListener(v -> guardarCliente());
        btnCancelar.setOnClickListener(v -> mostrarLista());

        // Handle back press using the new OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutFormulario.getVisibility() == View.VISIBLE) {
                    mostrarLista();
                } else {
                    finish();
                }
            }
        });
        lanzadorTomarFoto = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultado -> {
                    if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                        Bundle extras = resultado.getData().getExtras();
                        if (extras != null) {
                            imagenBitmap = (Bitmap) extras.get("data"); // miniatura de la cámara
                            imgCliente.setImageBitmap(imagenBitmap);
                        }
                    }
                }
        );
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

    private void mostrarFormulario(Cliente c) {
        layoutLista.setVisibility(View.GONE);
        layoutFormulario.setVisibility(View.VISIBLE);

        if (c == null) {
            txtCedula.setText("");
            txtNombre.setText("");
            txtTelefono.setText("");
            txtCedula.setEnabled(true);

            imagenBitmap = null;
            imgCliente.setImageBitmap(null);
        } else {
            txtCedula.setText(c.getCedula());
            txtNombre.setText(c.getNombre());
            txtTelefono.setText(c.getTelefono());
            txtCedula.setEnabled(false);

            // cargar foto desde la BD
            imagenBitmap = obtenerFotoCliente(c.getCedula());
            if (imagenBitmap != null) {
                imgCliente.setImageBitmap(imagenBitmap);
            } else {
                imgCliente.setImageBitmap(null);
            }

        }

    }
    private Bitmap obtenerFotoCliente(String cedula) {
        SQLiteDatabase readable = db.getReadableDatabase();
        Cursor cursor = readable.rawQuery(
                "SELECT Imagen FROM cliente WHERE cedula = ?",
                new String[]{cedula}
        );

        Bitmap bitmap = null;
        if (cursor.moveToFirst()) {
            byte[] bytes = cursor.getBlob(0);
            if (bytes != null) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        }
        cursor.close();
        return bitmap;
    }
    private void mostrarLista() {
        layoutFormulario.setVisibility(View.GONE);
        layoutLista.setVisibility(View.VISIBLE);
    }

    private void guardarCliente() {
        String ced = txtCedula.getText().toString().trim();
        String nom = txtNombre.getText().toString().trim();
        String tel = txtTelefono.getText().toString().trim();

        // si quieres obligar a tener foto:
        // if (ced.isEmpty() || nom.isEmpty() || imagenBitmap == null) return;
        if (ced.isEmpty() || nom.isEmpty()) return;

        SQLiteDatabase writable = db.getWritableDatabase();
        boolean esUpdate = !txtCedula.isEnabled();  // si está deshabilitada, es edición

        ContentValues values = new ContentValues();
        values.put("nombre", nom);
        values.put("telefono", tel);

        // Convertir la foto a BLOB si hay una foto tomada
        if (imagenBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imagenBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            values.put("Imagen", stream.toByteArray());   // 👈 nombre de la columna BLOB en tu tabla
        }

        if (esUpdate) {
            writable.update("cliente", values, "cedula=?", new String[]{ced});
        } else {
            values.put("cedula", ced);
            writable.insert("cliente", null, values);
        }

        txtBuscar.setText("");
        cargarClientes("");
        mostrarLista();
    }
    private void cargarClientes(String query) {
        listaClientes.clear();

        SQLiteDatabase readable = db.getReadableDatabase();
        Cursor c;

        if (query.trim().isEmpty()) {
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

    private Cliente buscarPorCedula(String cedula) {
        for (Cliente c : listaClientes) {
            if (c.getCedula().equals(cedula)) return c;
        }
        return null;
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

            text1.setText(cliente.getNombre());
            text2.setText("Cédula: " + cliente.getCedula() + " | Tel: " + cliente.getTelefono());

            return convertView;
        }

    }
    public void tomarFoto(View vista) {
        Intent intentTomarFoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intentTomarFoto.resolveActivity(getPackageManager()) != null) {
            lanzadorTomarFoto.launch(intentTomarFoto);
        } else {
            Toast.makeText(this, "No hay app de cámara disponible", Toast.LENGTH_SHORT).show();
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
