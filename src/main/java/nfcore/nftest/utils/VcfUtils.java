package nfcore.nftest.utils;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility methods for interacting with VCF files and the nft-vcf plugin.
 */
public final class VcfUtils {

  /**
   * Name of the VcfFile class provided by nft-vcf.
   */
  private static final String VARIANT_FILE_CLASS =
    "genepi.nf.test.vcf.VcfFile";

  /**
   * Prevents instantiation of this utility class.
   */
  private VcfUtils() {
  }

  /**
   * Gets the nft-vcf class.
   *
   * @return the nft-vcf class
   * @throws ClassNotFoundException if nft-vcf is not available
   */
  private static Class<?> getNftVcfClass() throws ClassNotFoundException {
    return Class.forName(VARIANT_FILE_CLASS);
  }

  /**
   * Checks whether a compatible version of nft-vcf is available.
   *
   * @return true if nft-vcf is available and compatible
   */
  @SuppressWarnings("PMD.UselessPureMethodCall")
  public static boolean isNftVcfAvailable() {
    try {
      Class<?> vcfFileClass = getNftVcfClass();
      vcfFileClass.getConstructor();
      vcfFileClass.getMethod("setVcfFilename", String.class);
      vcfFileClass.getMethod("getVariantsMD5");
      return true;
    } catch (ClassNotFoundException e) {
      System.err.println(
        "Could not find the VcfFile class of the nft-vcf plugin"
      );
      return false;
    } catch (NoSuchMethodException e) {
      System.err.println(
        "Installed nft-vcf version is incompatible with nft-utils. "
        + "Expected VcfFile(), setVcfFilename(String) and getVariantsMD5()."
      );
      return false;
    }
  }


  /**
   * Calculates the variants MD5 for VCF/BCF files recursively
   * for the given value.
   *
   * @param value value containing VCF/BCF files
   * @return value with variants MD5 replacements
   */
  public static Object vcfMD5(final Object value) {
    return OutputSanitizer.recursiveParse(value, strValue -> {
      Path path = Paths.get(strValue);

      if (!Files.exists(path)) {
        return strValue;
      }
      String extension = Utils.getExtension(path, false);
      if (!"vcf".equals(extension) && !"bcf".equals(extension)) {
        return strValue;
      }
      return path.getFileName().toString()
        + ":md5Variants,"
        + getVariantsMD5(path);
    });
  }

  /**
   * Calculates the variants MD5 using nft-vcf.
   *
   * @param path VCF/BCF file path
   * @return variants MD5
   */
  private static String getVariantsMD5(final Path path) {
    try {
      Class<?> vcfFileClass = getNftVcfClass();
      Object vcfFile = vcfFileClass
        .getConstructor()
        .newInstance();
      Method setVcfFilename = vcfFileClass.getMethod(
        "setVcfFilename", String.class
      );
      setVcfFilename.invoke(vcfFile, path.toString());
      Method getVariantsMD5 = vcfFileClass.getMethod("getVariantsMD5");

      return (String) getVariantsMD5.invoke(vcfFile);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(
        "The installed version of nft-vcf is incompatible with "
        + "nft-utils. Expected VcfFile(), setVcfFilename(String) "
        + "and getVariantsMD5().",
        e
      );
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to calculate variants MD5 for file: " + path, e
      );
    }
  }
}
