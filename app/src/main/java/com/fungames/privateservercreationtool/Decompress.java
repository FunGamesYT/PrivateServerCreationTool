package com.fungames.privateservercreationtool;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Fabian on 26.11.2017.
 */

public class Decompress extends AsyncTask {
    private static final String APP_SHARED_PREFERENCES = "AppSettings";
    private final MainActivity mainActivity;
    private ProgressBar progressBar;
    private TextView currentFileTextView;
    private static final String CARD_STATS_LIST = "CardStatsList";
    private static final String TEXTURES_LIST = "TexturesList";
    private String selectedPath;
    private String selectedFile;
    private Set<FileInfo> cardStatsItems = new HashSet<>();
    private Set<FileInfo> texturesItems = new HashSet<>();

    public Decompress(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        progressBar = (ProgressBar) mainActivity.findViewById(R.id.decryptProgressBar);
        currentFileTextView = (TextView) mainActivity.findViewById(R.id.currentFile);
        try {
            selectedPath = (String) objects[0];
            selectedFile = (String) objects[1];
            final ZipFile apk = new ZipFile(selectedPath);
            final Set<ZipEntry> files = new HashSet<ZipEntry>(Collections.list(apk.entries()));
            byte[] buffer = new byte[1024];
            Integer counter = 0;
            progressBar.setMax(files.size());
            for (ZipEntry zipEntry : files) {
//                if (counter > 150) {
//                    continue;
//                }
                if (zipEntry.isDirectory()) {
                    continue;
                }
                InputStream in = apk.getInputStream(zipEntry);
                String targetFileName = zipEntry.getName();

                File targetFile = new File(Environment.getExternalStorageDirectory(), ".PSCT/" + selectedFile + "/" + targetFileName);
                String targetPath = targetFile.getAbsolutePath();
                Log.i("FileOpen", "Unzipping to " + targetFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(targetFile.getParent()).mkdirs();

                // read the first block of the file to see if it is LZMA compressed
                int len = in.read(buffer);
                boolean lzmaFile = len > 0 && buffer[0] == ']' && (targetFileName.endsWith(".csv") || targetFileName.endsWith(".sc")); // most probably a LZMA file
                boolean sclzmaFile = len > 0 && buffer[0] == 'S' && buffer[1] == 'C' && targetFileName.endsWith(".sc"); // most probably a .SC LZMA file
                OutputStream out;

                if (lzmaFile || sclzmaFile) {
                    // write to a temporary byte buffer output stream
                    out = new ByteArrayOutputStream();
                    publishProgress(counter, "Decrypting " + targetFileName, targetPath);
                } else {
                    out = new FileOutputStream(targetFile);
                    publishProgress(counter, "Decompressing " + targetFileName, targetPath);
                }
                if (targetFileName.endsWith(".csv")) {
                    String[] targetFileNameFragments = targetFileName.split("/");
                    cardStatsItems.add(new FileInfo(targetFileNameFragments[targetFileNameFragments.length - 1], targetPath));
                } else if (targetFileName.endsWith("_tex.sc")) {
                    String[] targetFileNameFragments = targetFileName.split("/");
                    texturesItems.add(new FileInfo(targetFileNameFragments[targetFileNameFragments.length - 1], targetPath));
                }

                boolean firstBlock = true;
                do {
                    if (firstBlock && (lzmaFile || sclzmaFile) && len > 9) {
                        int startoffset = sclzmaFile ? 26 : 0;
                        // the header of the lzma file is corrupt. It needs to be 13 bytes but the last 4 bytes are missing
                        out.write(buffer, startoffset, 9);
                        out.write(0); // add the missing 4 bytes at the end of the lzma header
                        out.write(0);
                        out.write(0);
                        out.write(0);
                        out.write(buffer, startoffset + 9, len - startoffset - 9);
                    } else {
                        out.write(buffer, 0, len);
                    }
                    firstBlock = false;

                }
                while ((len = in.read(buffer)) > 0);
                out.close();

                if (lzmaFile || sclzmaFile) {
                    // now we can use the LzmaDecompressor. It will read the temporary byte buffer as input stream
                    byte[] byteArray = ((ByteArrayOutputStream) out).toByteArray();
                    ByteArrayInputStream lzmaIn = new ByteArrayInputStream(byteArray);
                    FileOutputStream lzmaOut = new FileOutputStream(targetFile);
                    LzmaDecompress(lzmaIn, targetFileName, lzmaOut);
                }
                //close this ZipEntry
                in.close();
                counter++;
            }
        } catch (IOException e) {
            Log.e("FileOpen", "Error while reading file: " + e.getMessage());
        }
        return null;
    }

    private void LzmaDecompress(InputStream in, String fileName, FileOutputStream out) {
        // LZMA decoding
        try {
            int propertiesSize = 5;
            byte[] properties = new byte[propertiesSize];
            if (in.read(properties, 0, propertiesSize) != propertiesSize)
                throw new Exception("input .lzma file is too short");
            SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
            if (!decoder.SetDecoderProperties(properties))
                throw new Exception("Incorrect stream properties");
            long outSize = 0;
            for (int i = 0; i < 8; i++) {
                int v = in.read();
                if (v < 0)
                    throw new Exception("Can't read stream size");
                outSize |= ((long) v) << (8 * i);
            }
            if (!decoder.Code(in, out, outSize)) {
                throw new Exception("Error in data stream");
            }
        } catch (Exception e) {
            Log.e("FileDecompress", "Failed to decompress LZMA file " + fileName + ": " + e.getMessage());
        }
    }


    @Override
    protected void onPostExecute(Object o) {
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        currentFileTextView.setText("");
        // save the task list to preferences
        writeToSharedPreferences();
    }

    private void writeToSharedPreferences() {
        SharedPreferences shared = mainActivity.getSharedPreferences(APP_SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        Set<String> csi = new HashSet<>();
        for (FileInfo fi : cardStatsItems) {
            csi.add(fi.getFilePath());
        }
        editor.putStringSet(CARD_STATS_LIST, csi);
        Set<String> ti = new HashSet<>();
        for (FileInfo fi : texturesItems) {
            ti.add(fi.getFilePath());
        }
        editor.putStringSet(TEXTURES_LIST, ti);
        editor.apply();
    }

    public void readFromSharedPreference() {
        SharedPreferences shared = mainActivity.getSharedPreferences(APP_SHARED_PREFERENCES, MODE_PRIVATE);
        Set<String> csi = shared.getStringSet(CARD_STATS_LIST, null);
        if (csi != null) {
            cardStatsItems.clear();
            for (String path : csi) {
                String[] targetFileNameFragments = path.split("/");
                String fileName = targetFileNameFragments[targetFileNameFragments.length - 1];
                FileInfo item = new FileInfo(fileName, path);
                cardStatsItems.add(item);
            }
        }
        Set<String> ti = shared.getStringSet(TEXTURES_LIST, null);
        if (ti != null) {
            texturesItems.clear();
            for (String path : ti) {
                String[] targetFileNameFragments = path.split("/");
                String fileName = targetFileNameFragments[targetFileNameFragments.length - 1];
                FileInfo item = new FileInfo(fileName, path);
                texturesItems.add(item);
            }
        }
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        progressBar.setProgress((Integer) values[0]);
        currentFileTextView.setText((String) values[1]);

    }

    public Set<FileInfo> getCardStatsItems() {
        return cardStatsItems;
    }

    public Set<FileInfo> getTexturesItems() {
        return texturesItems;
    }
}

