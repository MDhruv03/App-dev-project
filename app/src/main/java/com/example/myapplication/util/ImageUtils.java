package com.example.myapplication.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Image utilities for loading, caching, and transforming images
 */
public class ImageUtils {
    
    /**
     * Load bitmap from URI with size constraints
     */
    public static Bitmap loadBitmapFromUri(Context context, Uri uri, int maxWidth, int maxHeight) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) return null;
            
            // Decode bounds first
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            input.close();
            
            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);
            
            // Decode actual bitmap
            input = context.getContentResolver().openInputStream(uri);
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            input.close();
            
            return bitmap;
        } catch (IOException e) {
            Logger.e("ImageUtils", "Error loading bitmap", e);
            return null;
        }
    }
    
    /**
     * Calculate optimal sample size for image loading
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    /**
     * Create circular bitmap
     */
    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        
        Rect rect = new Rect(0, 0, size, size);
        
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        
        return output;
    }
    
    /**
     * Create thumbnail
     */
    public static Bitmap createThumbnail(Bitmap source, int size) {
        if (source == null) return null;
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        float scale = Math.min(((float) size) / width, ((float) size) / height);
        
        int scaledWidth = Math.round(width * scale);
        int scaledHeight = Math.round(height * scale);
        
        return Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true);
    }
    
    /**
     * Save bitmap to file
     */
    public static File saveBitmapToFile(Context context, Bitmap bitmap, String fileName, int quality) {
        File file = new File(context.getCacheDir(), fileName);
        
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            return file;
        } catch (IOException e) {
            Logger.e("ImageUtils", "Error saving bitmap", e);
            return null;
        }
    }
    
    /**
     * Generate company logo placeholder with initials
     */
    public static Bitmap generateLogoPlaceholder(String companyName, int size, int backgroundColor) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(backgroundColor);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint);
        
        // Text
        String initials = getInitials(companyName);
        Paint textPaint = new Paint();
        textPaint.setColor(0xFFFFFFFF); // White
        textPaint.setTextSize(size * 0.4f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        float xPos = size / 2f;
        float yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2);
        
        canvas.drawText(initials, xPos, yPos, textPaint);
        
        return bitmap;
    }
    
    /**
     * Get initials from company name
     */
    private static String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return parts[0].substring(0, 1).toUpperCase() + 
                   parts[1].substring(0, 1).toUpperCase();
        } else {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
    }
    
    /**
     * Get color for company logo based on name hash
     */
    public static int getColorForCompany(String companyName) {
        int[] colors = {
            0xFF2196F3, // Blue
            0xFF4CAF50, // Green
            0xFFF44336, // Red
            0xFFFF9800, // Orange
            0xFF9C27B0, // Purple
            0xFF00BCD4, // Cyan
            0xFFE91E63, // Pink
            0xFF795548, // Brown
            0xFF607D8B  // Blue Grey
        };
        
        int hash = Math.abs(companyName.hashCode());
        return colors[hash % colors.length];
    }
}
