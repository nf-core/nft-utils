package nf_core.nf.test.utils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

public class BamUtils {

  private static final String ALIGNMENT_FILE_CLASS =
    "nvnieuwk.nf.test.bam.AlignmentFile";

  private static Class<?> getNftBamClass() {
    try {
      return Class.forName(ALIGNMENT_FILE_CLASS);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public static boolean isNftBamAvailable() {
    Class<?> clazz = getNftBamClass();
    if (clazz != null) {
      return true;
    }
    System.err.println(
      "Could not find the AlignmentFile class of the nft-bam plugin"
    );
    return false;
  }

  private static Path resolveReference(String refFile, Path referenceTmpDir) {
    if (refFile == null || refFile.isEmpty()) {
      return null;
    }
    if (refFile.startsWith("http://") || refFile.startsWith("https://")) {
      return Utils.downloadFile(refFile, referenceTmpDir);
    }
    Path path = Paths.get(refFile);
    if (!Files.exists(path)) {
      throw new RuntimeException(
        "Reference file does not exist: " + path
      );
    }
    return path;
  }

  public static Object bamMD5(Object value, String fasta, String fai) {
    try {
      Path referenceTmpDir = Files.createTempDirectory("nft-utils-reference-");
      Path pathFasta = resolveReference(fasta, referenceTmpDir);
      Path pathFai = resolveReference(fai, referenceTmpDir);
      return OutputSanitizer.recursiveParse(value, strValue -> {
        Path pathBam = Paths.get(strValue);

        if (!Files.exists(pathBam)) {
          return strValue;
        }
        String extension = Utils.getExtension(pathBam, false);
        if (!"bam".equals(extension) && !"sam".equals(extension) && !"cram".equals(extension)) {
          return strValue;
        }
        if ("cram".equals(extension) && (pathFasta == null || pathFai == null)) {
          throw new RuntimeException(
            "A pathBam FASTA file is required to calculate reads MD5 for CRAM file: "
              + pathBam
          );
        }
        return pathBam.getFileName().toString() + ":md5Reads," + getReadsMD5(pathBam, pathFasta);
      });
    } catch (IOException e) {
      throw new RuntimeException(
        "Failed to create temporary directory for BAM/CRAM reference files", e
      );
    }
  }

  private static String getReadsMD5(Path pathBam, Path pathFasta) {
    try {
      Class<?> alignmentFileClass = getNftBamClass();
      Constructor<?> constructor = alignmentFileClass.getConstructor(
        LinkedHashMap.class, Path.class, Path.class
      );
      Object alignmentFile = constructor.newInstance(
        new LinkedHashMap<String, Object>(), pathBam, pathFasta
      );
      Method getReadsMD5 = alignmentFileClass.getMethod("getReadsMD5");

      return (String) getReadsMD5.invoke(alignmentFile);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(
        "Failed to calculate reads MD5 for file: " + pathBam, e
      );
    }
  }
}
