package com.google.webrtc.apmdemo.file;

import java.nio.ByteOrder;

public class BytesTransUtils {

    private String TAG = "BytesTransUtils";
    private static BytesTransUtils instance = null;

    private BytesTransUtils() {
        // Log.i(TAG, "instance BytesTransUtils");
    }

    public static BytesTransUtils getInstance() {
        if (instance == null) {
            instance = new BytesTransUtils();
        }

        return instance;
    }

    public boolean testCPU() {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            // System.out.println("is big ending");
            return true;
        } else {
            // System.out.println("is little ending");
            return false;
        }
    }

    public byte[] getBytes(short s, boolean bBigEnding) {
        byte[] buf = new byte[2];

        if (bBigEnding){
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x00ff);
                s >>= 8;
            }
        }
        else {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x00ff);
                s >>= 8;
            }
        }

        return buf;
    }

    public byte[] getBytes(int s, boolean bBigEnding) {
        byte[] buf = new byte[4];

        if (bBigEnding) {
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x000000ff);
                s >>= 8;
            }
        } else {
            System.out.println("1");
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x000000ff);
                s >>= 8;
            }
        }

        return buf;
    }

    public byte[] getBytes(long s, boolean bBigEnding) {
        byte[] buf = new byte[8];

        if (bBigEnding) {
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x00000000000000ff);
                s >>= 8;
            }
        }
        else {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x00000000000000ff);
                s >>= 8;
            }
        }

        return buf;
    }

    public short getShort(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }

        if (buf.length > 2) {
            throw new IllegalArgumentException("byte array size > 2 !");
        }

        short r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        }

        return r;
    }

    public int getInt(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }

        if (buf.length > 4) {
            throw new IllegalArgumentException("byte array size > 4 !");
        }

        int r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x000000ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x000000ff);
            }
        }

        return r;
    }

    public long getLong(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }

        if (buf.length > 8) {
            throw new IllegalArgumentException("byte array size > 8 !");
        }

        long r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x00000000000000ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x00000000000000ff);
            }
        }

        return r;
    }

    /*----------------------------------------------------------*/
    /* 对转换进行一个简单的封装 */
    /*----------------------------------------------------------*/
    public byte[] getBytes(int i) {
        return getBytes(i, this.testCPU());
    }

    public byte[] getBytes(short s) {
        return getBytes(s, this.testCPU());
    }

    public byte[] getBytes(long l) {
        return getBytes(l, this.testCPU());
    }

    public int getInt(byte[] buf) {
        return getInt(buf, this.testCPU());
    }

    public short getShort(byte[] buf) {
        return getShort(buf, this.testCPU());
    }

    public long getLong(byte[] buf) {
        return getLong(buf, this.testCPU());
    }

    /****************************************/
    public short[] Bytes2Shorts(byte[] buf) {
        byte bLength = 2;
        short[] s = new short[buf.length / bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = new byte[bLength];

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                temp[jLoop] = buf[iLoop * bLength + jLoop];
            }

            s[iLoop] = getShort(temp);
        }

        return s;
    }

    public byte[] Shorts2Bytes(short[] s) {
        byte bLength = 2;
        byte[] buf = new byte[s.length * bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = getBytes(s[iLoop]);

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                buf[iLoop * bLength + jLoop] = temp[jLoop];
            }
        }

        return buf;
    }

    /****************************************/
    public int[] Bytes2Ints(byte[] buf) {
        byte bLength = 4;
        int[] s = new int[buf.length / bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = new byte[bLength];

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                temp[jLoop] = buf[iLoop * bLength + jLoop];
            }

            s[iLoop] = getInt(temp);

            System.out.println("2out->"+s[iLoop]);
        }

        return s;
    }

    public byte[] Ints2Bytes(int[] s) {
        byte bLength = 4;
        byte[] buf = new byte[s.length * bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = getBytes(s[iLoop]);

            System.out.println("1out->"+s[iLoop]);

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                buf[iLoop * bLength + jLoop] = temp[jLoop];
            }
        }

        return buf;
    }

    /****************************************/
    public long[] Bytes2Longs(byte[] buf) {
        byte bLength = 8;
        long[] s = new long[buf.length / bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = new byte[bLength];

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                temp[jLoop] = buf[iLoop * bLength + jLoop];
            }

            s[iLoop] = getLong(temp);
        }

        return s;
    }

    public byte[] Longs2Bytes(long[] s) {
        byte bLength = 8;
        byte[] buf = new byte[s.length * bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = getBytes(s[iLoop]);

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                buf[iLoop * bLength + jLoop] = temp[jLoop];
            }
        }

        return buf;
    }

}

