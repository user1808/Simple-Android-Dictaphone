package com.example.dyktafon11;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ListActivity extends AppCompatActivity {
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private ListView listView;
    private File[] allFileList;
    private MediaPlayer mediaPlayer;
    private ArrayList<String> audioFilesMetadata;
    private EditText firstText, secondText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        audioFilesMetadata = getIntent().getStringArrayListExtra("METADATA");
        listView = (ListView) findViewById(R.id.listView);

        firstText = findViewById(R.id.first_entry);
        secondText = findViewById(R.id.second_entry);

        Button connectButton = findViewById(R.id.connectbutton);
        connectButton.setOnClickListener(v -> {
            if(buttonConnectNotes()) {
                Toast.makeText(getBaseContext(), "Połączono!", Toast.LENGTH_LONG).show();
                showListOfNotes();
            }
        });

        showListOfNotes();
    }

    private void showListOfNotes(){
        File dataDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        allFileList = dataDirectory.listFiles();
        if(allFileList != null) {
            String[] infoAboutFiles = new String[allFileList.length];
            for (int i = 0; i < allFileList.length; i++) {
                infoAboutFiles[i] = createInfoAboutFile(i);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, infoAboutFiles);
            listView.setAdapter(adapter);
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(String.valueOf(allFileList[position]));
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            File file = allFileList[position];
            int indexToDelete = -1;
            for(int i = 0; i < audioFilesMetadata.size() ; i++){
                String fileMetadata = audioFilesMetadata.get(i);
                String currentFilename = fileMetadata.split("#")[3];
                if(currentFilename.equals(file.getName())){
                    indexToDelete = i;
                }
            }

            if(indexToDelete != -1)
                audioFilesMetadata.remove(indexToDelete);

            boolean flag = file.delete();
            showListOfNotes();
            return flag;
        });
    }

    private String createInfoAboutFile(int index){
        File file = allFileList[index];
        String filename = file.getName();
        String authorName = "";
        String authorSurname = "";
        String description = "";

        for(String fileMetadata : audioFilesMetadata){
            String[] currentFileMetadata = fileMetadata.split("#");
            if((currentFileMetadata[2] + ".wav").equals(filename)){
                authorName = currentFileMetadata[0];
                authorSurname = currentFileMetadata[1];
                description = currentFileMetadata[3];
            }
        }

        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String lastModifiedDate = format.format(new Date(file.lastModified()));

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file.getPath());
        long durationMs = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        return "Tytuł: " + filename + " Autor: " + authorName + " " + authorSurname
                + "\n" + description + "\nData: " + lastModifiedDate + "\nCzas trwania w ms:" +
                durationMs;
    }

    public void onBackPressed(){
        buttonBack(getCurrentFocus());
    }

    public void buttonBack(View view) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    public boolean buttonConnectNotes(){
        String firstFilename = firstText.getText().toString();
        String secondFilename = secondText.getText().toString();

        if(firstFilename.length() == 0 || secondFilename.length() == 0){
            Toast.makeText(getBaseContext(), "Wpisz nazwe obu plików!", Toast.LENGTH_LONG).show();
            return false;
        }

        boolean firstFileFound = false;
        boolean secondFileFound = false;
        int firstFilePosition = -1;
        int secondFilePosition = -1;

        for(int i = 0; i < allFileList.length; i++){
            String currentFilename = allFileList[i].getName();
            if(currentFilename.equals(firstFilename)){
                firstFileFound = true;
                firstFilePosition = i;
            }
            if(currentFilename.equals(secondFilename)){
                secondFileFound = true;
                secondFilePosition = i;
            }
        }

        if(firstFileFound && secondFileFound){
            if(firstFilePosition == secondFilePosition){
                Toast.makeText(getBaseContext(), "Wybrałeś/aś ten sam plik", Toast.LENGTH_LONG).show();
                return false;
            } else {
                File olderFile, newerFile;
                if(new Date(allFileList[firstFilePosition].lastModified()).getTime() > new Date(allFileList[secondFilePosition].lastModified()).getTime()) {
                    olderFile = allFileList[secondFilePosition];
                    newerFile = allFileList[firstFilePosition];
                } else {
                    olderFile = allFileList[firstFilePosition];
                    newerFile = allFileList[secondFilePosition];
                }

                String newerFileMetadata = "";

                for(int i = 0; i < audioFilesMetadata.size(); i++){
                    String[] currentMetadata = audioFilesMetadata.get(i).split("#");

                    if(currentMetadata[2].equals(newerFile.getName())){
                        newerFileMetadata = audioFilesMetadata.get(i);
                    }
                }

                audioFilesMetadata.remove(newerFileMetadata);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(allFileList[firstFilePosition]));
                    int read;
                    byte[] buff = new byte[1024];
                    while ((read = in.read(buff)) > 0) {
                        out.write(buff, 0, read);
                    }
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] firstFileBytes = out.toByteArray();

                out = new ByteArrayOutputStream();
                try {
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(allFileList[secondFilePosition]));
                    int read;
                    byte[] buff = new byte[1024];
                    while ((read = in.read(buff)) > 0) {
                        out.write(buff, 0, read);
                    }
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] secondFileBytes = out.toByteArray();
                String newFilename = olderFile.getName();

                if(allFileList[firstFilePosition].delete() && allFileList[secondFilePosition].delete()){
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), newFilename);
                    FileOutputStream os = null;
                    try {
                        os = new FileOutputStream(file, true);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    byte[] header = new byte[44];
                    int firstFileByteArrayLength = firstFileBytes.length - 44;
                    int secondFileByteArrayLength = secondFileBytes.length - 44;
                    byte[] data = new byte[firstFileByteArrayLength + secondFileByteArrayLength];
                    System.arraycopy(firstFileBytes, 44, data, 0, firstFileByteArrayLength);
                    System.arraycopy(secondFileBytes, 44, data, firstFileByteArrayLength, secondFileByteArrayLength);
                    long totalDataLength = data.length + 36;
                    int channel = 1;
                    int format = 16;
                    long bitrate = SAMPLING_RATE_IN_HZ * format * channel;

                    header[0] = 'R';
                    header[1] = 'I';
                    header[2] = 'F';
                    header[3] = 'F';
                    header[4] = (byte) (totalDataLength & 0xff);
                    header[5] = (byte) ((totalDataLength >> 8) & 0xff);
                    header[6] = (byte) ((totalDataLength >> 16) & 0xff);
                    header[7] = (byte) ((totalDataLength >> 24) & 0xff);
                    header[8] = 'W';
                    header[9] = 'A';
                    header[10] = 'V';
                    header[11] = 'E';
                    header[12] = 'f';
                    header[13] = 'm';
                    header[14] = 't';
                    header[15] = ' ';
                    header[16] = (byte) format;
                    header[17] = 0;
                    header[18] = 0;
                    header[19] = 0;
                    header[20] = 1;
                    header[21] = 0;
                    header[22] = (byte) channel;
                    header[23] = 0;
                    header[24] = (byte) (SAMPLING_RATE_IN_HZ & 0xff);
                    header[25] = (byte) ((SAMPLING_RATE_IN_HZ >> 8) & 0xff);
                    header[26] = (byte) ((SAMPLING_RATE_IN_HZ >> 16) & 0xff);
                    header[27] = (byte) ((SAMPLING_RATE_IN_HZ >> 24) & 0xff);
                    header[28] = (byte) ((bitrate / 8) & 0xff);
                    header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
                    header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
                    header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
                    header[32] = (byte) ((channel * format) / 8);
                    header[33] = 0;
                    header[34] = 16;
                    header[35] = 0;
                    header[36] = 'd';
                    header[37] = 'a';
                    header[38] = 't';
                    header[39] = 'a';
                    header[40] = (byte) (data.length  & 0xff);
                    header[41] = (byte) ((data.length >> 8) & 0xff);
                    header[42] = (byte) ((data.length >> 16) & 0xff);
                    header[43] = (byte) ((data.length >> 24) & 0xff);

                    try {
                        assert os != null;
                        os.write(header, 0 ,44);
                        os.write(data);
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            }
        } else {
            Toast.makeText(getBaseContext(), "Wybierz dwa poprawne pliki!", Toast.LENGTH_LONG).show();
        }
        return false;
    }
}
