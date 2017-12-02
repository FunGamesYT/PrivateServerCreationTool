package com.fungames.privateservercreationtool.ScLib;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ProgressBar;

import com.fungames.privateservercreationtool.FileInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.Stream;

/**
 * Created by Fabian on 30.11.2017.
 */

public class Textures extends AsyncTask
{
    private static int[] Convert5To8 = {
                0x00, 0x08, 0x10, 0x18, 0x20, 0x29, 0x31, 0x39, 0x41, 0x4A, 0x52, 0x5A, 0x62, 0x6A, 0x73, 0x7B, 0x83, 0x8B,
                0x94, 0x9C, 0xA4, 0xAC, 0xB4, 0xBD, 0xC5, 0xCD, 0xD5, 0xDE, 0xE6, 0xEE, 0xF6, 0xFF
    };
    private ProgressDialog progressDialog;

    @Override
    protected Object doInBackground(Object[] objects) {
        FileInfo fileInfo = (FileInfo) objects[0];
        progressDialog = (ProgressDialog) objects[1];
        File scFile = new File(fileInfo.getFilePath());
        try {
            FileInputStream inStream = new FileInputStream(scFile);
            String suffix = "";
            do {
                Bitmap bitmap = getBitmapBySc(inStream);
                if (bitmap == null) {
                    break;
                }
                File targetFile = new File(Environment.getExternalStorageDirectory(), "ExtractedPNGs/" + fileInfo.getFileName() + suffix + ".png");
                new File(targetFile.getParent()).mkdirs();
                FileOutputStream out = new FileOutputStream(targetFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                suffix += "_";
            } while (inStream.available() > 0);
            inStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TextureException e) {
            e.printStackTrace();
        }

        return null;
    }
    @Override
    protected void onPostExecute(Object o) {
        progressDialog.dismiss();
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        progressDialog.setProgress((Integer) values[0]);
    }

    /// <summary>
    ///     Exports the texture from a SC File
    /// </summary>
    /// <param name="inStream"></param>
    /// <returns></returns>
    public Bitmap getBitmapBySc(InputStream inStream) throws TextureException {
        byte id = 0;
        try {
            id = (byte) inStream.read();
            inStream.skip(4);
            int pxFomat = inStream.read();
            if (pxFomat == -1) {
                return null;
            }
            byte[] byteArr = new byte[2];
            inStream.read(byteArr);
            int width = (byteArr[1] & 0xFF) * 0x100 + (byteArr[0] & 0xFF);
            inStream.read(byteArr);
            int height = (byteArr[1] & 0xFF) * 0x100 + (byteArr[0] & 0xFF);

            int mtWidth = id == 1 ? width : width % 32;
            int ttWidth = id == 1 ? width - mtWidth : (width - mtWidth) / 32;
            int mtHeigth = id == 1 ? height : height % 32;
            int ttHeigth = id == 1 ? height - mtHeigth : (height - mtHeigth) / 32;

            int[][] pixelArray = new int[height][width];
            if (ttHeigth == 0) {
                progressDialog.setMax(ttWidth + mtHeigth);
            }
            else {
                progressDialog.setMax(ttHeigth);
            }
            for (int index = 0; index < ttHeigth + 1; index++) {
                int lHeigth = 32;

                if (index == ttHeigth) {
                    lHeigth = mtHeigth;
                }


                for (int t = 0; t < ttWidth; t++) {
                    for (int y = 0; y < lHeigth; y++) {
                        for (int x = 0; x < 32; x++) {
                            int xOffset = t * 32;
                            int yOffset = index * 32;

                            pixelArray[y + yOffset][x + xOffset] = getColorByPxFormat(inStream, pxFomat);
                        }
                    }
                    if (ttHeigth == 0) {
                        publishProgress(t);
                    }
                }

                for (int y = 0; y < lHeigth; y++) {
                    for (int x = 0; x < mtWidth; x++) {
                        int pxOffsetX = ttWidth * 32, pxOffsetY = index * 32;
                        pixelArray[y + pxOffsetY][x + pxOffsetX] = getColorByPxFormat(inStream, pxFomat);
                    }
                    if (ttHeigth == 0) {
                        publishProgress(ttWidth + y);
                    }
                }
                if (ttHeigth > 0) {
                    publishProgress(index);
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);


            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    bitmap.setPixel(column, row, pixelArray[row][column]);
                }
            }

            return bitmap;
        } catch (IOException e) {
            throw new TextureException(e);
        }
    }

    private int getColorByPxFormat(InputStream inStream, int pxFormat) throws TextureException {
        int color;
        byte[] byteArr = new byte[2];
        try {
        switch (pxFormat)
        {
            case 0: {
                int r = 0;
                r = inStream.read();
                int g = inStream.read();
                int b = inStream.read();
                int a = inStream.read();
                color = Color.argb(a, r, g, b);
                break;
            }
            case 2: {
                inStream.read(byteArr);
                color = (byteArr[1] & 0xFF) * 0x100 + (byteArr[0] & 0xFF);
                int r = ((color >> 12) & 0xF) << 4;
                int g = ((color >> 8) & 0xF) << 4;
                int b = ((color >> 4) & 0xF) << 4;
                int a = (color & 0xF) << 4;
                color = Color.argb(a, r, g, b);
                break;
            }
            case 3: {
                inStream.read(byteArr);
                color = (byteArr[1] & 0xFF) * 0x100 + (byteArr[0] & 0xFF);
                int r = Convert5To8[(color >> 11) & 0x1F];
                int g = Convert5To8[(color >> 6) & 0x1F];
                int b = Convert5To8[(color >> 1) & 0x1F];
                int a = (color & 0x0001) == 1 ? 0xFF : 0x00;
                color = Color.argb(a, r, g, b);
                break;
            }
            case 4: {
                inStream.read(byteArr);
                color = (byteArr[1] & 0xFF) * 0x100 + (byteArr[0] & 0xFF);
                int r = ((color >> 11) & 0x1F) << 3;
                int g = ((color >> 5) & 0x3F) << 2;
                int b = (color & 0X1F) << 3;
                color = Color.argb(0, r, g, b);
                break;
            }
            case 6: {
                inStream.read(byteArr);
                color = (byteArr[1] & 0xFF) * 0x100 + (byteArr[0] & 0xFF);
                int rgb = color >> 8;
                color = Color.argb(color >> 0xFF, rgb, rgb, rgb);
                break;
            }
            case 10: {
                inStream.read(byteArr);
                color = (byteArr[1] & 0xFF) * 0x100 + (byteArr[0] & 0xFF);
                color = Color.argb(0, color, color, color);
                break;
            }

            default: {
                throw new TextureException("Unknown pixelformat.");
            }
        }

        return color;
        } catch (IOException e) {
            throw new TextureException(e);
        }
    }

}
