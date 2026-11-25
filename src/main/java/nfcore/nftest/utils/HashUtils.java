package nfcore.nftest.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Utility methods for computing MD5 hashes.
 */
public final class HashUtils {

  /**
   * Prevents instantiation of this utility class.
   */
  private HashUtils() {
  }

  /**
   * Computes an MD5 hash of the given string value.
   *
   * @param value The string to hash.
   * @return The MD5 digest as a lowercase hexadecimal string.
   */
  public static String md5Hex(final String value) {
    return listToMD5(List.of(value));
  }

  /**
   * Computes an MD5 hash from the string representation of each element in
   * a list, joined with newlines, so {@code ["ab"]} and {@code ["a", "b"]}
   * differ.
   *
   * @param input The list of objects to include in the MD5 calculation.
   * @return The MD5 digest as a hexadecimal string.
   */
  public static String listToMD5(final List<?> input) {
    final MessageDigest md5;
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      // Every Java platform must support MD5
      throw new IllegalStateException(e);
    }
    boolean first = true;
    for (final Object item : input) {
      if (!first) {
        md5.update((byte) '\n');
      }
      first = false;
      md5.update(String.valueOf(item).getBytes(StandardCharsets.UTF_8));
    }
    return HexFormat.of().formatHex(md5.digest());
  }
}
