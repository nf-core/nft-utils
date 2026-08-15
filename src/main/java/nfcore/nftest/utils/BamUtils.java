package nfcore.nftest.utils;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

/**
 * Utility methods for interacting with BAM files and the nft-bam plugin.
 */
public final class BamUtils {
  /**
   * Utility methods for calculating BAM/SAM/CRAM read MD5 hashes using nft-bam.
   */
  private static final String METHODS_CLASS =
    "nvnieuwk.nf.test.bam.Methods";

  /**
   * Name of the Methods class provided by nft-bam.
   */
  private static final String ALIGNMENT_FILE_CLASS =
    "nvnieuwk.nf.test.bam.AlignmentFile";

  /**
   * Prevents instantiation of this utility class.
   */
  private BamUtils() {
  }

  /**
   * Gets the nft-bam Methods class.
   *
   * @return the nft-bam Methods class
   * @throws ClassNotFoundException if nft-bam is not available
   */
  private static Class<?> getNftBamMethodsClass()
      throws ClassNotFoundException {
    return Class.forName(METHODS_CLASS);
  }

  /**
   * Checks whether a compatible version of nft-bam is available.
   *
   * @return true if nft-bam is available and compatible
   */
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
        "Installed nft-bam version is incompatible with nft-utils. "
        + "The required bam(...) and getReadsMD5() methods were not found."
      );
      return false;
    }
  }

  /**
   * Calculates read MD5 hashes for BAM, SAM, and CRAM files recursively.
   *
   * @param value value containing alignment files
   * @param referenceFasta reference FASTA file for CRAM files
   * @return value with read MD5 replacements
   */
  public static Object bamMD5(final Object value, final String referenceFasta) {
    return OutputSanitizer.recursiveParse(value, strValue -> {
      Path pathBam = Paths.get(strValue);
      if (!Files.exists(pathBam)) {
        return strValue;
      }
      String extension = Utils.getExtension(pathBam, false);
      if (!"bam".equals(extension)
          && !"sam".equals(extension)
          && !"cram".equals(extension)) {
        return strValue;
      }
      if ("cram".equals(extension)
          && (referenceFasta == null || referenceFasta.isEmpty())) {
        throw new RuntimeException(
          "A reference FASTA file is required to calculate reads MD5 "
          + "for CRAM file: "
          + pathBam
        );
      }
      return pathBam.getFileName().toString()
        + ":md5Reads,"
        + getReadsMD5(strValue, referenceFasta);
    });
  }

  /**
   * Calculates the read MD5 using nft-bam.
   *
   * @param pathBam alignment file path
   * @param pathFasta reference FASTA path
   * @return read MD5
   */
  private static String getReadsMD5(
      final String pathBam,
      final String pathFasta) {
    try {
      Class<?> methodsClass = getNftBamMethodsClass();
      Method bamMethod = methodsClass.getMethod(
        "bam",
        LinkedHashMap.class,
        CharSequence.class,
        CharSequence.class
      );

      String fastaPath = pathFasta;
      if (fastaPath == null) {
        fastaPath = "";
      }
      Object alignmentFile = bamMethod.invoke(
        null,
        new LinkedHashMap<String, Object>(),
        pathBam.toString(),
        fastaPath
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
