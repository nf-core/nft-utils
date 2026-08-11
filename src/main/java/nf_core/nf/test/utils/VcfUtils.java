package nf_core.nf.test.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

public class VcfUtils {

  private static final String VARIANT_FILE_CLASS =
    "genepi.nf.test.vcf.VcfFile";

  private static Class<?> getNftVcfClass() {
    try {
      return Class.forName(VARIANT_FILE_CLASS);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public static boolean isNftVcfAvailable() {
    Class<?> clazz = getNftVcfClass();
    if (clazz != null) {
      return true;
    }
    System.err.println(
      "Could not find nft-vcf VcfFile"
    );
    return false;
  }

  public static Object vcfMD5(Object value) {
    return OutputSanitizer.recursiveParse(value, strValue -> {
      Path path = Paths.get(strValue);

      if (!Files.exists(path)) {
        return strValue;
      }
      String extension = Utils.getExtension(path, false);
      if (!"vcf".equals(extension) && !"bcf".equals(extension)) {
        return strValue;
      }
      return path.getFileName().toString() + ":md5Variants," + getVariantsMD5(path);
    });
  }

  private static String getVariantsMD5(Path path) {
    try {
      Class<?> vcfFileClass = getNftVcfClass();
      Object vcfFile = vcfFileClass
        .getConstructor()
        .newInstance();
      Method setVcfFilename = vcfFileClass.getMethod(
        "setVcfFilename", String.class
      );
      setVcfFilename.invoke(vcfFile,path.toString());
      Method getVariantsMD5 = vcfFileClass.getMethod("getVariantsMD5");

      return (String) getVariantsMD5.invoke(vcfFile);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(
        "Failed to calculate variants MD5 for file: " + path, e
      );
    }
  }
}
