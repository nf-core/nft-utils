package nf_core.nf.test.utils;

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
      "Could not find nft-bam AlignmentFile"
    );
    return false;
  }

  public static Object bamMD5(Object value) {
    return OutputSanitizer.recursiveParse(value, strValue -> {
      Path path = Paths.get(strValue);

      if (!Files.exists(path)) {
        return strValue;
      }
      String extension = Utils.getExtension(path);
      if (!"bam".equals(extension) && !"sam".equals(extension)) {
        return strValue;
      }
      return path.getFileName().toString() + ":md5Reads," + getReadsMD5(path);
    });
  }

  private static String getReadsMD5(Path path) {
    try {
      Class<?> alignmentFileClass = getNftBamClass();
      Constructor<?> constructor = alignmentFileClass.getConstructor(
        LinkedHashMap.class, Path.class, Path.class
      );
      Object alignmentFile = constructor.newInstance(
        new LinkedHashMap<String, Object>(), path, null
      );
      Method getReadsMD5 = alignmentFileClass.getMethod("getReadsMD5");

      return (String) getReadsMD5.invoke(alignmentFile);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(
        "Failed to calculate reads MD5 for file: " + path, e
      );
    }
  }
}
