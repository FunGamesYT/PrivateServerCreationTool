package com.fungames.privateservercreationtool;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Fabian on 26.11.2017.
 */

public class Decompress extends AsyncTask {
    private ProgressBar progressBar;
    private String selectedPath;
    private String selectedFile;

    public Decompress(ProgressBar progressDialog) {
        this.progressBar = progressDialog;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            selectedPath = (String) objects[0];
            selectedFile = (String) objects[1];
            final ZipFile apk = new ZipFile(selectedPath);
            final Set<ZipEntry> files = new HashSet<ZipEntry>(Collections.list(apk.entries()));
            byte[] buffer = new byte[1024];
            Integer counter = 0;
            progressBar.setMax(files.size());
            for (ZipEntry zipEntry : files) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                InputStream in = apk.getInputStream(zipEntry);
                String fileName = zipEntry.getName();
                File targetFile = new File(Environment.getExternalStorageDirectory(), ".PSCT/" + selectedFile + "/" + fileName);
                Log.i("FileOpen", "Unzipping to " + targetFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(targetFile.getParent()).mkdirs();

                // read the first block of the file to see if it is LZMA compressed
                int len = in.read(buffer);
                boolean lzmaFile = len > 0 && buffer[0] == ']' && (fileName.endsWith(".csv") || fileName.endsWith(".sc")); // most probably a LZMA file
                boolean sclzmaFile = len > 0 && buffer[0] == 'S' && buffer[1] == 'C' && fileName.endsWith(".sc"); // most probably a .SC LZMA file
                OutputStream out;
                if (lzmaFile || sclzmaFile) {
                    // write to a temporary byte buffer output stream
                    out = new ByteArrayOutputStream();
                } else {
                    out = new FileOutputStream(targetFile);
                }
                boolean firstBlock = true;
                do {
                    if (firstBlock && lzmaFile && len > 9) {
                        // the header of the lzma file is corrupt. It needs to be 13 bytes but the last 4 bytes are missing
                        out.write(buffer, 0, 9);
                        out.write(0); // add the missing 4 bytes at the end of the lzma header
                        out.write(0);
                        out.write(0);
                        out.write(0);
                        out.write(buffer, 9, len - 9);
                    } else {
                        out.write(buffer, 0, len);
                    }
                    firstBlock = false;

                }
                while ((len = in.read(buffer)) > 0);
                out.close();
  /*                  boolean scfirstBlock = true;
                    do {
                        if (scfirstBlock && sclzmaFile && len > 9) {
                            // the header of the lzma file is corrupt. It needs to be 13 bytes but the last 4 bytes are missing
                            out.write(buffer, 0, 9);
                            out.write(0); // add the missing 4 bytes at the end of the lzma header
                            out.write(0);
                            out.write(0);
                            out.write(0);
                            out.write(buffer, 9, len - 9);
                        } else {
                            out.write(buffer, 0, len);
                        }
                        firstBlock = false;

                    }
                    while ((len = in.read(buffer)) > 0);
                out.close();
*/

                if (lzmaFile) {
                    // now we can use the LzmaDecompressor. It will read the temporary byte buffer as input stream
                    byte[] byteArray = ((ByteArrayOutputStream) out).toByteArray();
                    ByteArrayInputStream lzmaIn = new ByteArrayInputStream(byteArray);
                    FileOutputStream lzmaOut = new FileOutputStream(targetFile);
                    LzmaDecompress(lzmaIn, fileName, lzmaOut);
                }
                //close this ZipEntry
                in.close();
                publishProgress(counter);
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

    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        progressBar.setProgress((Integer) values[0]);
    }
}
