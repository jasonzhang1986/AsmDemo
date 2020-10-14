package com.sogou.teemo.plugin;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Util {
    private static final String TAG         = "Util";
    public static final  int    BUFFER_SIZE = 16384;
    public static void addZipEntry(ZipOutputStream zipOutputStream, ZipEntry zipEntry, InputStream inputStream) throws Exception {
        try {
            zipOutputStream.putNextEntry(zipEntry);
            byte[] buffer = new byte[BUFFER_SIZE];
            int length = -1;
            while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                zipOutputStream.write(buffer, 0, length);
                zipOutputStream.flush();
            }
        } catch (ZipException e) {
            System.out.println("addZipEntry err!");
        } finally {
            closeQuietly(inputStream);

            zipOutputStream.closeEntry();
        }
    }

    /**
     * Close {@code target} quietly.
     *
     * @param obj
     *  Object to be closed.
     */
    public static void closeQuietly(@Nullable Object obj) {
        if (obj == null) {
            return;
        }
        if (obj instanceof Closeable) {
            try {
                ((Closeable) obj).close();
            } catch (Throwable ignored) {
                // ignore
            }
        } else if (obj instanceof AutoCloseable) {
            try {
                ((AutoCloseable) obj).close();
            } catch (Throwable ignored) {
                // ignore
            }
        } else if (obj instanceof ZipFile) {
            try {
                ((ZipFile) obj).close();
            } catch (Throwable ignored) {
                // ignore
            }
        } else {
            throw new IllegalArgumentException("obj " + obj + " is not closeable");
        }
    }
}
