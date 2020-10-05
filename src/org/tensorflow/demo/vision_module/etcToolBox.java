package org.tensorflow.demo.vision_module;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

public class etcToolBox {
    static public byte[] bitmapToByteArray( Bitmap bitmap ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
        byte[] byteArray = stream.toByteArray() ;
        return byteArray;
    }
}
