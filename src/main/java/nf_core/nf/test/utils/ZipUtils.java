package nf_core.nf.test.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

  public static void unzip(String zipFilePath, String destDirectory) throws IOException {
    File destDir = new File(destDirectory);
    if (!destDir.exists()) {
      destDir.mkdirs();
    }

    try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
      ZipEntry entry = zipIn.getNextEntry();
      while (entry != null) {
        String filePath = destDirectory + File.separator + entry.getName();
        if (!entry.isDirectory()) {
          extractFile(zipIn, filePath);
        } else {
          File dir = new File(filePath);
          dir.mkdirs();
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
    }
  }

  private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
    File file = new File(filePath);

    // Create parent directories if they don't exist
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }

    // If the entry is a directory, just create it and return
    if (filePath.endsWith(File.separator)) {
      file.mkdirs();
      return;
    }

    // Extract the file
    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
      byte[] bytesIn = new byte[4096];
      int read;
      while ((read = zipIn.read(bytesIn)) != -1) {
        bos.write(bytesIn, 0, read);
      }
    }
  }

  public static void zip(String sourceDirPath, String zipFilePath) throws IOException {
    Path zipFile = Files.createFile(Paths.get(zipFilePath));
    try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      Path sourceDir = Paths.get(sourceDirPath);
      Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
          Path targetFile = sourceDir.relativize(file);
          zipOut.putNextEntry(new ZipEntry(targetFile.toString()));
          byte[] bytes = Files.readAllBytes(file);
          zipOut.write(bytes, 0, bytes.length);
          zipOut.closeEntry();
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

}
