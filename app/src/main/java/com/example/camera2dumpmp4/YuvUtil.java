package com.example.camera2dumpmp4;

import android.graphics.ImageFormat;
import android.media.Image;
import java.nio.ByteBuffer;

public class YuvUtil {
    public static final int NoneType = -1;
    public static final int YUV420P = 0;
    public static final int YUV420SP = 1;
    public static final int NV21 = 2;
    public static byte[] getBytesFromImageAsType(Image image, int type, boolean black_white) {
        if (null == image) {
            return null;
        }
        // 获取源数据，如果是YUV格式的数据planes.length = 3
        // plane[i]里面的实际数据可能存在byte[].length <= capacity (缓冲区总大小)
        final Image.Plane[] planes = image.getPlanes();
        if (planes.length != 3) {
            return null;
        }
        // 数据有效宽度，一般的，图片width <= rowStride，这也是导致byte[].length <= capacity的原因
        // 所以只取width部分
        int width = image.getWidth();
        int height = image.getHeight();
        //此处用来装填最终的YUV数据，需要1.5倍的图片大小，因为Y U V 比例为 4:1:1
        byte[] yuvBytes = new byte[width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        // 临时存储uv数据的
        byte[] uBytes = new byte[width * height / 4];
        byte[] vBytes = new byte[width * height / 4];
        if ((null == yuvBytes) || (null == uBytes) || (null == vBytes)) {
            return null;
        }
        // 源数组的装填到的位置
        int srcIndex = 0;
        // 目标数组的装填到的位置
        int dstIndex = 0;
        int uIndex = 0;
        int vIndex = 0;
        int pixelsStride = 0, rowStride = 0;
        for (int planeNo = 0;planeNo < planes.length;planeNo++) {
            pixelsStride = planes[planeNo].getPixelStride();
            rowStride = planes[planeNo].getRowStride();
            ByteBuffer buffer = planes[planeNo].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            if (null == bytes) {
                return null;
            }
            buffer.get(bytes);
            srcIndex = 0;
            switch (planeNo) {
                case 0:
                    for (int i = 0; i < height; i++) {
                        System.arraycopy(bytes, srcIndex, yuvBytes, dstIndex, width);
                        srcIndex += rowStride;
                        dstIndex += width;
                    }
                    break;
                case 1:
                    for (int i = 0; i < height / 2; i++) {
                        for (int j = 0; j < width / 2; j++) {
                            uBytes[uIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (2 == pixelsStride) {
                            srcIndex += rowStride - width;
                        } else if (1 == pixelsStride) {
                            srcIndex += rowStride - width / 2;
                        }
                        else {
                            return null;
                        }
                    }
                    break;
                case 2:
                    for (int i = 0; i < height / 2; i++) {
                        for (int j = 0; j < width / 2; j++) {
                            vBytes[vIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (2 == pixelsStride) {
                            srcIndex += rowStride - width;
                        } else if (1 == pixelsStride) {
                            srcIndex += rowStride - width / 2;
                        }
                        else {
                            return null;
                        }
                    }
                    break;
            }
        }
        final byte char_128 = (byte) 128;
        switch (type) {
            case YUV420P:
                System.arraycopy(uBytes, 0, yuvBytes, dstIndex, uBytes.length);
                System.arraycopy(vBytes, 0, yuvBytes, dstIndex + uBytes.length, vBytes.length);
                break;
            case YUV420SP:
                if (true == black_white) {
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = char_128;
                        yuvBytes[dstIndex++] = char_128;
                    }
                }
                else {
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = uBytes[i];
                        yuvBytes[dstIndex++] = vBytes[i];
                    }
                }
                break;
            case NV21:
                if (true == black_white) {
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = char_128;
                        yuvBytes[dstIndex++] = char_128;
                    }
                }
                else {
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = vBytes[i];
                        yuvBytes[dstIndex++] = uBytes[i];
                    }
                }
                break;
            default:
                return null;
        }
        return yuvBytes;
    }
}