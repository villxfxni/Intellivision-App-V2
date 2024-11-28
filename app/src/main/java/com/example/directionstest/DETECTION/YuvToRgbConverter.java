package com.example.directionstest.DETECTION;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;


import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class YuvToRgbConverter {

    @OptIn(markerClass = ExperimentalGetImage.class)
    public static void convert(@NonNull ImageProxy imageProxy, @NonNull Bitmap outputBitmap) {
        Image image = imageProxy.getImage();
        if (image == null || image.getFormat() != ImageFormat.YUV_420_888) {
            throw new UnsupportedOperationException("Unsupported image format");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        byte[] yArray = new byte[yBuffer.remaining()];
        byte[] uArray = new byte[uBuffer.remaining()];
        byte[] vArray = new byte[vBuffer.remaining()];
        yBuffer.get(yArray);
        uBuffer.get(uArray);
        vBuffer.get(vArray);

        int yRowStride = image.getPlanes()[0].getRowStride();
        int uvRowStride = image.getPlanes()[1].getRowStride();
        int uvPixelStride = image.getPlanes()[1].getPixelStride();

        int[] argb = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int yIndex = y * yRowStride + x;
                int uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride;

                int yValue = yArray[yIndex] & 0xFF;
                int uValue = uArray[uvIndex] & 0xFF;
                int vValue = vArray[uvIndex] & 0xFF;
                int r = Math.round(yValue + 1.402f * (vValue - 128));
                int g = Math.round(yValue - 0.344f * (uValue - 128) - 0.714f * (vValue - 128));
                int b = Math.round(yValue + 1.772f * (uValue - 128));

                r = Math.min(Math.max(r, 0), 255);
                g = Math.min(Math.max(g, 0), 255);
                b = Math.min(Math.max(b, 0), 255);

                argb[y * width + x] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }

        outputBitmap.setPixels(argb, 0, width, 0, 0, width, height);
    }
}
