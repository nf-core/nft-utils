package nfcore.nftest.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
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
   * Bit mask used to convert a signed byte to its unsigned representation.
   */
  private static final int BYTE_MASK = 0xff;

  /**
   * Converts a byte to its two-digit lowercase hexadecimal representation.
   *
   * @param b The byte to convert.
   * @return The byte as a two-digit hexadecimal string.
   */
  private static String byteToHex(final byte b) {
    final String hex = Integer.toHexString(BYTE_MASK & b);
    if (hex.length() == 1) {
      return "0" + hex;
    }
    return hex;
  }

  /**
   * Computes an MD5 hash of the given string value.
   *
   * @param value The string to hash.
   * @return The MD5 digest as a lowercase hexadecimal string.
   * @throws NoSuchAlgorithmException If the MD5 algorithm is not available.
   */
  public static String md5Hex(final String value)
      throws NoSuchAlgorithmException {
    final MessageDigest digest = MessageDigest.getInstance("MD5");
    final byte[] hash = digest.digest(
      value.getBytes(StandardCharsets.UTF_8)
    );

    final StringBuilder result = new StringBuilder();
    for (final byte b : hash) {
      result.append(byteToHex(b));
    }
    return result.toString();
  }

  /**
   * Computes an MD5 hash from the string representation of each element in
   * a list.
   *
   * @param input The list of objects to include in the MD5 calculation.
   * @return The MD5 digest as a hexadecimal string.
   * @throws NoSuchAlgorithmException If the MD5 algorithm is not available.
   */
  public static String listToMD5(final List<?> input)
      throws NoSuchAlgorithmException {
    final MessageDigest md5 = MessageDigest.getInstance("MD5");
    final Iterator<?> inputIterator = input.iterator();
    while (inputIterator.hasNext()) {
      final String item = inputIterator.next().toString();
      md5.update(item.getBytes(StandardCharsets.UTF_8));
    }
    final byte[] digest = md5.digest();

    final StringBuilder hexString = new StringBuilder();
    for (final byte b : digest) {
      hexString.append(byteToHex(b));
    }
    return hexString.toString();
  }
}
