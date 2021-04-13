package com.example.dyktafon11;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecording implements Runnable {
    private static boolean workingLoop;
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);
    ByteArrayOutputStream note = new ByteArrayOutputStream();

    public AudioRecording(){
        workingLoop = true;
    }
    @Override
    public void run() {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();

        while (workingLoop) {
            if (recordingInProgress.get()) {
                int result = recorder.read(buffer, BUFFER_SIZE);
                if (result < 0) {
                    throw new RuntimeException("Reading of audio buffer failed: " +
                            getBufferReadFailureReason(result));
                }
                if(isAudible(buffer.array()))
                    note.write(buffer.array(), 0, result);
                buffer.clear();
            }
        }
    }

    private static boolean isAudible(byte[] data) {
        double rms = getRootMeanSquared(data);
        return (128 > rms && rms > 40);
    }

    private static double getRootMeanSquared(byte[] data) {
        double ms = 0;
        for (byte datum : data) {
            ms += datum * datum;
        }
        ms /= data.length;
        return Math.sqrt(ms);
    }

    private String getBufferReadFailureReason(int errorCode) {
        switch (errorCode) {
            case AudioRecord.ERROR_INVALID_OPERATION:
                return "ERROR_INVALID_OPERATION";
            case AudioRecord.ERROR_BAD_VALUE:
                return "ERROR_BAD_VALUE";
            case AudioRecord.ERROR_DEAD_OBJECT:
                return "ERROR_DEAD_OBJECT";
            case AudioRecord.ERROR:
                return "ERROR";
            default:
                return "Unknown (" + errorCode + ")";
        }
    }

    public void startRecording(){
        recordingInProgress.set(true);
    }

    public void stopRecording(){
        recordingInProgress.set(false);
    }

    public void deleteRecording(){
        note.reset();
    }

    public boolean saveRecording(String filename){
        stopRecording();
        byte[] header = new byte[44];
        byte[] data = note.toByteArray();

        long totalDataLen = data.length + 36;
        int channel = 1;
        int format = 16;
        long bitrate = SAMPLING_RATE_IN_HZ * channel * format;

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
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

        String fullFilename = filename + ".wav";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fullFilename);
        FileOutputStream os;
        try {
            if(file.createNewFile())
                os = new FileOutputStream(file, true);
            else
                return false;
            os.write(header, 0, 44);
            os.write(data);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}
