package nf_core.nf.test.utils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

public class BamUtils {
  private static final String METHODS_CLASS =
    "nvnieuwk.nf.test.bam.Methods";

  private static final String ALIGNMENT_FILE_CLASS =
    "nvnieuwk.nf.test.bam.AlignmentFile";

  private static Class<?> getNftBamMethodsClass()
      throws ClassNotFoundException {
    return Class.forName(METHODS_CLASS);
  }

  public static boolean isNftBamAvailable() {
    try {
      Class<?> methodsClass = getNftBamMethodsClass();
      methodsClass.getMethod(
        "bam",
        LinkedHashMap.class,
        CharSequence.class,
        CharSequence.class
      );
      Class<?> alignmentFileClass = Class.forName(ALIGNMENT_FILE_CLASS);
      alignmentFileClass.getMethod("getReadsMD5");
      return true;
    } catch (ClassNotFoundException e) {
      System.err.println(
        "Could not find the nft-bam plugin."
      );
      return false;
    } catch (NoSuchMethodException e) {
      System.err.println(
        "Installed nft-bam version is incompatible with nft-utils. " +
        "The required bam(...) and getReadsMD5() methods were not found."
      );
      return false;
    }
  }

  public static Object bamMD5(Object value, String referenceFasta) {
    return OutputSanitizer.recursiveParse(value, strValue -> {
      Path pathBam = Paths.get(strValue);
      if (!Files.exists(pathBam)) {
        return strValue;
      }
      String extension = Utils.getExtension(pathBam, false);
      if (!"bam".equals(extension) && !"sam".equals(extension) && !"cram".equals(extension)) {
        return strValue;
      }
      if ("cram".equals(extension) && (referenceFasta == null || referenceFasta.isEmpty())) {
        throw new RuntimeException(
          "A reference FASTA file is required to calculate reads MD5 " +
          "for CRAM file: " + pathBam
        );
      }
      return pathBam.getFileName().toString()
        + ":md5Reads,"
        + getReadsMD5(strValue, referenceFasta);
    });
  }

  private static String getReadsMD5(String pathBam, String pathFasta) {
    try {
      Class<?> methodsClass = getNftBamMethodsClass();
      Method bamMethod = methodsClass.getMethod(
        "bam",
        LinkedHashMap.class,
        CharSequence.class,
        CharSequence.class
      );
      Object alignmentFile = bamMethod.invoke(
        null,
        new LinkedHashMap<String, Object>(),
        pathBam.toString(),
        pathFasta == null ? "" : pathFasta
      );
      Class<?> alignmentFileClass = Class.forName(ALIGNMENT_FILE_CLASS);
      Method getReadsMD5 = alignmentFileClass.getMethod("getReadsMD5");
      return (String) getReadsMD5.invoke(alignmentFile);
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to calculate reads MD5 for file: " + pathBam,
        e
      );
    }
  }
}
