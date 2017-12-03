package com.fungames.privateservercreationtool;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by Fabian on 03.12.2017.
 */

public class Compress extends AsyncTask {

    private ProgressDialog progressDialog;
    private FileInfo fileInfo;

    @Override
    protected Object doInBackground(Object[] objects) {
        fileInfo = (FileInfo) objects[0];
        progressDialog = (ProgressDialog) objects[1];
        File srcFile = fileInfo.getFile();
        File targetFile = new File(Environment.getExternalStorageDirectory(), "ModdedAPK/" + fileInfo.getFileName());
        new File(targetFile.getParent()).mkdirs();
        String targetPath = targetFile.getAbsolutePath();
        try {
            FileOutputStream dest = new FileOutputStream(targetFile);
            ZipOutputStream zipOutputStream = new ZipOutputStream(dest);
            int filesnumber = srcFile.listFiles().length;
            progressDialog.setMax(filesnumber);
            zipDirectoryHelper(srcFile, srcFile, zipOutputStream);
            zipOutputStream.close();
        }
        catch (IOException ioe) {
            Log.w("Compress", "Failed to create APK file: " + ioe.getMessage());
        }
        return null;
    }

    private void zipDirectoryHelper(File rootDirectory, File currentDirectory, ZipOutputStream out) throws IOException {
        byte[] data = new byte[2048];

        File[] files = currentDirectory.listFiles();
        if (files == null) {
            // no files were found or this is not a directory

        } else {
            int fileCount = 0;
            for (File file : files) {
                if (file.isDirectory()) {
                    zipDirectoryHelper(rootDirectory, file, out);
                } else {
                    FileInputStream fi = new FileInputStream(file);
                    // creating structure and avoiding duplicate file names
                    String name = file.getAbsolutePath().replace(rootDirectory.getAbsolutePath(), "");

                    ZipEntry entry = new ZipEntry(name);
                    out.putNextEntry(entry);
                    int count;
                    BufferedInputStream origin = new BufferedInputStream(fi,2048);
                    while ((count = origin.read(data, 0 , 2048)) != -1){
                        out.write(data, 0, count);
                    }
                    origin.close();
                }
                if(rootDirectory == currentDirectory) {
                    publishProgress(fileCount++);
                }
            }
        }

    }
    @Override
    protected void onProgressUpdate(Object[] values) {
        progressDialog.setProgress((Integer) values[0]);
    }

    @Override
    protected void onPostExecute(Object o) {
        progressDialog.dismiss();
        String[] args = new String[1];
        args[0] = fileInfo.getFilePath();
//        sign.src.orig.SignApk.main(args);
    }
}
