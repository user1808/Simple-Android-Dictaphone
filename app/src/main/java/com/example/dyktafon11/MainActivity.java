package com.example.dyktafon11;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private AudioRecording audioRecording = null;
    private Button startButton;
    private Button stopButton;
    private EditText name, surname, title, info;
    private ArrayList<String> audioFilesMetadata;
    final static int MY_PERMISSIONS_CODE = 200;
    private boolean micPermission = false, writePermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_CODE);
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_CODE) {
            for(int i = 0; i < permissions.length; i++){
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if(permission.equals(Manifest.permission.RECORD_AUDIO)){
                    micPermission = grantResult == PackageManager.PERMISSION_GRANTED;
                }

                if(permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    writePermission = grantResult == PackageManager.PERMISSION_GRANTED;
                }
            }

            if(micPermission && writePermission){
                init();
            } else {
                Toast.makeText(getBaseContext(), "Potrzebuje tych uprawnień!", Toast.LENGTH_LONG).show();
                finishAndRemoveTask();
                System.exit(0);
            }
        }
    }

    /**
     * Metoda inicjująca wszystkie niezbędne składniki aplikacji, takie jak: lista metadanych,
     * obiekt klasy nagrywającej dźwięk, wszystkie przyciski oraz edittexty
     */
    private void init(){
        audioFilesMetadata = new ArrayList<>();
        audioRecording = new AudioRecording();

        name = findViewById(R.id.name_entry);
        surname = findViewById(R.id.surname_entry);
        title = findViewById(R.id.title_entry);
        info = findViewById(R.id.info_entry);

        startButton = findViewById(R.id.button_start);
        startButton.setOnClickListener(v -> {
            audioRecording.startRecording();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        });

        stopButton = findViewById(R.id.button_stop);
        stopButton.setOnClickListener(v -> {
            audioRecording.stopRecording();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        });

        Button deleteButton = findViewById(R.id.button_delete);
        deleteButton.setOnClickListener(v -> {
            audioRecording.deleteRecording();
            cleanMetadataTexts();
            stopRecordingAndChangeEnablingButtons();
        });

        Button saveButton = findViewById(R.id.button_save);
        saveButton.setOnClickListener(v -> {
            stopRecordingAndChangeEnablingButtons();
            String authorName = name.getText().toString();
            String authorSurname = surname.getText().toString();
            String filename = title.getText().toString();
            String description = info.getText().toString();
            if(filename.isEmpty()){
                Toast.makeText(getBaseContext(), "Plik musi mieć tytuł",
                        Toast.LENGTH_LONG).show();
            } else {
                if(authorName.contains("#") || authorSurname.contains("#") || filename.contains("#") || description.contains("#"))
                    Toast.makeText(getBaseContext(), "Żadne z metadanych nie może zawierać znaku #", Toast.LENGTH_LONG);
                else {
                    if (audioRecording.saveRecording(filename)) {
                        String fileMetadata = (authorName.length() > 0 ? authorName : " ") + "#" +
                                (authorSurname.length() > 0 ? authorSurname : " ") + "#" +
                                filename + "#" +
                                (description.length() > 0 ? description : " ");
                        audioFilesMetadata.add(fileMetadata);
                        cleanMetadataTexts();
                        Toast.makeText(getBaseContext(), "Zapisano!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getBaseContext(), "Istnieje juz plik o takim tytule!",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        Button listButton = findViewById(R.id.button_list);
        listButton.setOnClickListener(v -> {
            stopRecordingAndChangeEnablingButtons();
            Intent intent = new Intent(getApplicationContext(), ListActivity.class);
            intent.putExtra("METADATA", audioFilesMetadata);
            startActivity(intent);
        });

        new Thread(audioRecording, "Audio Recording Thread").start();
    }

    /**
     * Metoda czyszcząca zawartość edittextow
     */
    private void cleanMetadataTexts(){
        name.setText(null);
        surname.setText(null);
        title.setText(null);
        info.setText(null);
    }

    /**
     * Metoda zatrzymująca nagrywanie oraz przełączająca przyciski
     */
    private void stopRecordingAndChangeEnablingButtons(){
        if (!startButton.isEnabled()) {
            audioRecording.stopRecording();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }
}