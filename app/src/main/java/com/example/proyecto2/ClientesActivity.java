package com.example.proyecto2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Actividad para registrar y editar la información de un cliente.
 * Permite capturar datos como cédula, nombre, teléfono, una fotografía,
 * una nota de audio y la ubicación geográfica.
 */
public class ClientesActivity extends AppCompatActivity {

    // --- CAMPOS DE LA INTERFAZ DE USUARIO --- //
    // Campos de texto para la información del cliente
    EditText txtCedula, txtNombre, txtTelefono;
    // Visor para la fotografía del cliente
    ImageView imgCliente;
    // Botones para acciones principales (guardar, cancelar, tomar foto)
    Button btnGuardar, btnCancelar, btnTomarFoto;
    // Botones para la grabación y reproducción de audio
    Button btnGrabar, btnDetenerGrab, btnReproducir, btnDetenerRep;

    // --- COMPONENTES PARA EL MAPA --- //
    // Campos de texto para mostrar latitud y longitud
    EditText txtLatitud, txtLongitud;
    // Vista del mapa
    MapView map;
    // Controlador para interactuar con el mapa (zoom, centrado, etc.)
    IMapController mapController;

    // --- COMPONENTES PARA LA FOTOGRAFÍA --- //
    // Almacena la imagen capturada como un Bitmap
    Bitmap imagenBitmap;
    // Lanzador de la actividad para tomar una foto
    ActivityResultLauncher<Intent> lanzadorFoto;

    // --- COMPONENTES PARA EL AUDIO --- //
    // Grabador de audio
    MediaRecorder mediaRecorder;
    // Reproductor de audio
    MediaPlayer mediaPlayer;
    // Ruta donde se guarda el archivo de audio
    String rutaAudio;
    // Banderas para controlar el estado de la grabación y reproducción
    boolean grabando = false;
    boolean reproduciendo = false;

    // --- COMPONENTES PARA LA BASE DE DATOS --- //
    // Ayudante para la gestión de la base de datos SQLite
    DatabaseHelper db;
    // Cédula del cliente que se está editando (null si es un nuevo cliente)
    String cedulaEditar = null;

    /**
     * Método que se ejecuta al crear la actividad.
     * Se encarga de inicializar la interfaz, configurar los componentes
     * (mapa, foto, audio) y cargar los datos si se trata de una edición.
     * @param savedInstanceState Estado previamente guardado de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- CONFIGURACIÓN DE OSMDROID (MAPA) --- //
        // Carga la configuración de osmdroid
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        // Establece un agente de usuario para las peticiones de tiles del mapa
        Configuration.getInstance().setUserAgentValue(getPackageName());
        // Define la ruta base para el almacenamiento en caché de osmdroid
        File basePath = new File(getCacheDir(), "osmdroid");
        basePath.mkdirs();
        Configuration.getInstance().setOsmdroidBasePath(basePath);

        // Define la ruta para la caché de los tiles del mapa
        File tileCache = new File(basePath, "tiles");
        tileCache.mkdirs();
        Configuration.getInstance().setOsmdroidTileCache(tileCache);
        
        // Asocia la actividad con su layout
        setContentView(R.layout.activity_clientes);

        // --- SOLICITUD DE PERMISOS --- //
        // Verifica y solicita permisos para grabar audio y acceder al almacenamiento
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, 3000);
        }


        // Inicializa el ayudante de la base de datos
        db = new DatabaseHelper(this);

        // --- INICIALIZACIÓN DE COMPONENTES DE LA UI --- //
        txtCedula = findViewById(R.id.txtCedula);
        txtNombre = findViewById(R.id.txtNombre);
        txtTelefono = findViewById(R.id.txtTelefono);
        imgCliente = findViewById(R.id.imgCliente);

        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);

        // Componentes de audio
        btnGrabar = findViewById(R.id.btnGrabarAudio);
        btnDetenerGrab = findViewById(R.id.btnDetenerGrab);
        btnReproducir = findViewById(R.id.btnReproducirAudio);
        btnDetenerRep = findViewById(R.id.btnDetenerRep);

        // Estado inicial de los botones de audio
        btnDetenerGrab.setEnabled(false);
        btnReproducir.setEnabled(false);
        btnDetenerRep.setEnabled(false);

        // Inicializa grabador y reproductor de audio
        mediaRecorder = new MediaRecorder();
        mediaPlayer = new MediaPlayer();
        // Define la ruta donde se guardará el audio
        rutaAudio = getExternalFilesDir(null).getAbsolutePath() + "/audioCliente.3gp";

        // Componentes del mapa
        txtLatitud = findViewById(R.id.txtLatitud);
        txtLongitud = findViewById(R.id.txtLongitud);
        map = findViewById(R.id.map);

        // Configuración inicial del mapa
        map.setTileSource(TileSourceFactory.MAPNIK); // Fuente de los tiles
        map.setMultiTouchControls(true); // Habilita gestos multitáctiles

        mapController = map.getController();
        mapController.setZoom(18); // Nivel de zoom inicial

        // Punto geográfico inicial para centrar el mapa
        GeoPoint puntoInicial = new GeoPoint(10.61822, -85.43707);
        mapController.setCenter(puntoInicial);
        manejarClickMapa(puntoInicial); // Coloca un marcador en el punto inicial

        // --- LISTENER PARA EVENTOS DEL MAPA --- //
        // Gestiona los clics en el mapa para actualizar la ubicación
        MapEventsOverlay mapEvents = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
                // Al hacer un clic simple, actualiza la ubicación
                manejarClickMapa(geoPoint);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint geoPoint) {
                // Al hacer un clic largo, también actualiza la ubicación
                manejarClickMapa(geoPoint);
                return true;
            }
        });

        map.getOverlays().add(mapEvents);

        // Muestra las coordenadas iniciales en los campos de texto
        txtLatitud.setText(String.valueOf(puntoInicial.getLatitude()));
        txtLongitud.setText(String.valueOf(puntoInicial.getLongitude()));

        // --- CONFIGURACIÓN PARA TOMAR FOTO --- //
        // Prepara el lanzador para la actividad de la cámara y procesa el resultado
        lanzadorFoto = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultado -> {
                    // Si la foto se tomó correctamente...
                    if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                        // ...obtiene la imagen y la muestra en el ImageView
                        imagenBitmap = (Bitmap) resultado.getData().getExtras().get("data");
                        imgCliente.setImageBitmap(imagenBitmap);
                    }
                }
        );

        // --- VERIFICACIÓN DE MODO EDICIÓN --- //
        // Si la actividad se inició con un extra "cedula", significa que es una edición
        if (getIntent().hasExtra("cedula")) {
            cedulaEditar = getIntent().getStringExtra("cedula");
            cargarDatos(cedulaEditar); // Carga los datos del cliente
            // Bloquea el campo de cédula para que no se pueda editar
            txtCedula.setFocusable(false);
            txtCedula.setClickable(false);
        }

        // --- LISTENERS DE BOTONES --- //
        btnGuardar.setOnClickListener(v -> guardar());
        btnCancelar.setOnClickListener(v -> finish()); // Cierra la actividad
    }

    /**
     * Inicia la cámara para capturar una fotografía.
     * @param view La vista que origina el evento (el botón de tomar foto).
     */
    public void tomarFoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        lanzadorFoto.launch(intent);
    }

    /**
     * Inicia la grabación de una nota de audio.
     * Configura el MediaRecorder y comienza a grabar desde el micrófono.
     * @param v La vista que origina el evento (el botón de grabar).
     */
    public void iniciarGrabacion(View v) {
        try {
            mediaRecorder.reset(); // Reinicia el grabador

            // Configuración de la fuente de audio, formato, y archivo de salida
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(rutaAudio);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);

            mediaRecorder.prepare();
            mediaRecorder.start(); // Comienza la grabación

            grabando = true;

            // Actualiza el estado de los botones
            btnGrabar.setEnabled(false);
            btnDetenerGrab.setEnabled(true);

            Toast.makeText(this, "Grabando audio...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace(); // Manejo de errores
        }
    }

    /**
     * Detiene la grabación de audio en curso.
     * @param v La vista que origina el evento (el botón de detener grabación).
     */
    public void detenerGrabacion(View v) {
        if (grabando) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            grabando = false;

            // Actualiza el estado de los botones
            btnDetenerGrab.setEnabled(false);
            btnReproducir.setEnabled(true);

            Toast.makeText(this, "Audio grabado", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Inicia la reproducción de la nota de audio grabada.
     * @param v La vista que origina el evento (el botón de reproducir).
     */
    public void iniciarReproduccion(View v) {
        try {
            mediaPlayer.setDataSource(rutaAudio); // Establece el archivo a reproducir
            mediaPlayer.prepare();
            mediaPlayer.start(); // Inicia la reproducción

            reproduciendo = true;

            // Actualiza el estado de los botones
            btnReproducir.setEnabled(false);
            btnDetenerRep.setEnabled(true);

        } catch (Exception e) {
            e.printStackTrace(); // Manejo de errores
        }
    }

    /**
     * Detiene la reproducción de audio en curso.
     * @param v La vista que origina el evento (el botón de detener reproducción).
     */
    public void detenerReproduccion(View v) {
        if (reproduciendo) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            reproduciendo = false;

            // Actualiza el estado de los botones
            btnDetenerRep.setEnabled(false);
            btnGrabar.setEnabled(true);

            btnReproducir.setEnabled(true);
        }
    }

    /**
     * Lee el archivo de audio y lo convierte en un arreglo de bytes.
     * @return Arreglo de bytes del audio, o null si ocurre un error.
     */
    private byte[] obtenerBytesAudio() {
        File archivo = new File(rutaAudio);
        if (!archivo.exists()) return null;

        try {
            byte[] bytes = new byte[(int) archivo.length()];
            FileInputStream fis = new FileInputStream(archivo);
            fis.read(bytes);
            fis.close();
            return bytes;

        } catch (Exception e) {
            return null; // Manejo de errores
        }
    }

    /**
     * Gestiona los clics en el mapa. Actualiza las coordenadas y coloca un marcador.
     * @param p El punto geográfico donde se hizo clic.
     */
    private void manejarClickMapa(GeoPoint p) {

        // Actualiza los campos de texto con las nuevas coordenadas
        txtLatitud.setText(String.valueOf(p.getLatitude()));
        txtLongitud.setText(String.valueOf(p.getLongitude()));

        // Limpia el mapa de marcadores y overlays anteriores
        map.getOverlays().clear();

        // Es necesario volver a agregar el listener de eventos después de limpiar los overlays
        MapEventsOverlay mapEvents = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
                manejarClickMapa(geoPoint);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint geoPoint) {
                manejarClickMapa(geoPoint);
                return true;
            }
        });
        map.getOverlays().add(mapEvents);

        // Coloca un nuevo marcador en la posición del clic
        colocarMarcador(p);

        // Refresca el mapa para mostrar los cambios
        map.invalidate();
    }

    /**
     * Coloca un marcador en el mapa en la posición especificada.
     * @param p El punto geográfico donde se colocará el marcador.
     */
    private void colocarMarcador(GeoPoint p) {
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(m);
    }

    /**
     * Guarda la información del cliente en la base de datos.
     * Si 'cedulaEditar' no es nulo, actualiza el cliente existente.
     * Si es nulo, inserta un nuevo cliente.
     */
    private void guardar() {
        // Obtiene los datos de los campos de texto
        String ced = txtCedula.getText().toString().trim();
        String nom = txtNombre.getText().toString().trim();
        String tel = txtTelefono.getText().toString().trim();
        String lat = txtLatitud.getText().toString().trim();
        String lon = txtLongitud.getText().toString().trim();

        // Validación de campos obligatorios
        if (ced.isEmpty() || nom.isEmpty()) {
            Toast.makeText(this, "Hay campos obligatorios vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtiene una referencia escribible de la base de datos
        SQLiteDatabase writable = db.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Prepara los valores a insertar/actualizar
        values.put("nombre", nom);
        values.put("telefono", tel);
        values.put("latitud", lat);
        values.put("longitud", lon);

        // Si hay una imagen, la convierte a bytes y la agrega
        if (imagenBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imagenBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            values.put("imagen", stream.toByteArray());
        }

        // Si hay un audio, lo convierte a bytes y lo agrega
        byte[] audioBytes = obtenerBytesAudio();
        if (audioBytes != null) {
            values.put("audio", audioBytes);
        }

        // Decide si actualizar o insertar
        if (cedulaEditar != null) {
            // Actualiza el cliente existente
            writable.update("cliente", values, "cedula=?", new String[]{cedulaEditar});
            Toast.makeText(this, "Cliente actualizado", Toast.LENGTH_SHORT).show();
        } else {
            // Inserta un nuevo cliente
            values.put("cedula", ced);
            long r = writable.insert("cliente", null, values);
            if (r == -1) {
                // Si la inserción falla (ej. cédula duplicada), muestra un error
                Toast.makeText(this, "Error: cédula ya existe", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Cliente registrado", Toast.LENGTH_SHORT).show();
        }

        finish(); // Cierra la actividad después de guardar
    }


    /**
     * Carga los datos de un cliente existente desde la base de datos para editarlos.
     * @param ced La cédula del cliente a cargar.
     */
    private void cargarDatos(String ced) {
        SQLiteDatabase readable = db.getReadableDatabase();
        Cursor c = readable.rawQuery(
                "SELECT nombre, telefono, imagen, audio, latitud, longitud FROM cliente WHERE cedula=?",
                new String[]{ced}
        );

        if (c.moveToFirst()) {
            // Asigna los valores recuperados a los campos de la interfaz
            txtCedula.setText(ced);
            txtNombre.setText(c.getString(0));
            txtTelefono.setText(c.getString(1));

            // Carga la foto si existe
            byte[] imgBytes = c.getBlob(2);
            if (imgBytes != null) {
                imagenBitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                imgCliente.setImageBitmap(imagenBitmap);
            }

            // Carga el audio si existe
            byte[] audBytes = c.getBlob(3);
            if (audBytes != null) {
                try {
                    // Escribe los bytes del audio a un archivo temporal para poder reproducirlo
                    File archivo = new File(rutaAudio);
                    FileOutputStream fos = new FileOutputStream(archivo);
                    fos.write(audBytes);
                    fos.close();
                } catch (Exception e) {}
                btnReproducir.setEnabled(true);
            }

            // Carga la ubicación en el mapa
            txtLatitud.setText(c.getString(4));
            txtLongitud.setText(c.getString(5));

            GeoPoint punto = new GeoPoint(
                    Double.parseDouble(c.getString(4)),
                    Double.parseDouble(c.getString(5))
            );

            // Centra el mapa y coloca un marcador en la ubicación del cliente
            manejarClickMapa(punto);
            mapController.setCenter(punto);

        }

        c.close(); // Cierra el cursor para liberar recursos
    }

    /**
     * Reanuda el mapa cuando la actividad vuelve a estar en primer plano.
     */
    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    /**
     * Pausa el mapa cuando la actividad ya no está en primer plano.
     */
    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}
