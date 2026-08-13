package nfcore.nftest.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

public final class OutputSanitizer {

  private OutputSanitizer() {
  }

  static void validateKeysInChannel(
    final List<String> keysList,
    final Map<String, Object> channel) {

    for (String keyList : keysList) {
      if (!channel.containsKey(keyList)) {
        throw new RuntimeException(
          "Key '" + keyList
          + "' not present in channel"
        );
      }
    }
  }
  static void validateKeyUsage(
    final List<String> unstableKeys,
    final List<String> ignoreKeys,
    final List<String> readsMD5Keys,
    final List<String> variantsMD5Keys
  ) {
    Map<String, String> keyUsage = new HashMap<>();

    addKeyUsage(keyUsage, unstableKeys, "unstableKeys");
    addKeyUsage(keyUsage, ignoreKeys, "ignoreKeys");
    addKeyUsage(keyUsage, readsMD5Keys, "readsMD5Keys");
    addKeyUsage(keyUsage, variantsMD5Keys, "variantsMD5Keys");
  }

  private static void addKeyUsage(
    final Map<String, String> keyUsage,
    final List<String> keys,
    final String option
  ) {
    for (String key : keys) {
      if (keyUsage.containsKey(key)) {
        throw new RuntimeException(
          "Key '" + key + "' is used in both '"
          + keyUsage.get(key) + "' and '" + option + "'"
        );
      }

      keyUsage.put(key, option);
    }
  }

  /**
   * Sanitizes the output channel based on the provided options.
   *
   * @param options A HashMap containing options for sanitization.
   * @param channel A TreeMap representing the output channel to be sanitized.
   * @return A sanitized TreeMap of the output channel.
   */
  public static TreeMap<String, Object> sanitizeOutput(
      final HashMap<String, Object> options,
      final TreeMap<String, Object> channel) {
    String className = channel.getClass().getName();
    // Can't do valid type checking here because
    // the channels type is not exposed from nf-test
    if (!className.equals("com.askimed.nf.test.lang.channels.Channels")) {
      throw new RuntimeException(
        "sanitizeOutput only supports channels as input, "
        + "pass it either `process.out` or `workflow.out`"
      );
    }

    // Fetch options
    List<String> unstableKeys =
      (List<String>) options.getOrDefault("unstableKeys", List.of());

    List<String> ignoreKeys =
      (List<String>) options.getOrDefault("ignoreKeys", List.of());

    List<String> unstablePattern =
      (List<String>) options.getOrDefault("unstablePattern", List.of());

    List<String> ignorePattern =
      (List<String>) options.getOrDefault("ignorePattern", List.of());

    List<String> readsMD5Keys =
      (List<String>) options.getOrDefault("readsMD5Keys", List.of());

    List<String> variantsMD5Keys =
      (List<String>) options.getOrDefault("variantsMD5Keys", List.of());

    String referenceFasta = (String) options.getOrDefault("referenceFasta", "");

    validateKeyUsage(unstableKeys, ignoreKeys, readsMD5Keys, variantsMD5Keys);

    validateKeysInChannel(unstableKeys, channel);
    validateKeysInChannel(ignoreKeys, channel);
    validateKeysInChannel(readsMD5Keys, channel);
    validateKeysInChannel(variantsMD5Keys, channel);

    if (!readsMD5Keys.isEmpty() && !BamUtils.isNftBamAvailable()) {
      System.err.println(
        "WARNING: A compatible version of nft-bam is not available. "
        + "Cannot calculate reads MD5 for BAM/SAM/CRAM files; "
        + "output may be unstable."
      );
      readsMD5Keys = List.of();
    }
    if (!variantsMD5Keys.isEmpty() && !VcfUtils.isNftVcfAvailable()) {
      System.err.println(
        "WARNING: A compatible version of nft-vcf is not available. "
        + "Cannot calculate variants MD5 for VCF files; "
        + "output may be unstable."
      );
      variantsMD5Keys = List.of();
    }

    TreeMap<String, Object> output = new TreeMap<String, Object>();
    Integer channelSize = (Integer) channel.size();
    for (Map.Entry<String, Object> entry : channel.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.matches("^\\d+$") && channelSize > 1) {
        // Skip numeric keys if there is more than one entry in the channel
        continue;
      }
      if (ignoreKeys.contains(key)) {
        continue;
      }

      if (unstableKeys.contains(key)) {
        output.put(key, fixUnstable(value));
      } else if (readsMD5Keys.contains(key)) {
        output.put(key, BamUtils.bamMD5(value, referenceFasta));
      } else if (variantsMD5Keys.contains(key)) {
        output.put(key, VcfUtils.vcfMD5(value));
      } else {
        output.put(key, checkPattern(value, unstablePattern, ignorePattern));
      }
    }
    return output;
  }

  /**
   * Recursively parses a value and applies the provided function
   * to string values.
   *
   * Directories are traversed recursively, lists are parsed element
   * by element, and maps are parsed value by value.
   * Values that resolve to the {@code IGNORE} marker are excluded
   * from the resulting collections.
   *
   * @param value The value to parse.
   * @param applyFct The function to apply to string values.
   * @return The recursively parsed value.
  */
  private static final Object IGNORE = new Object();
  static Object recursiveParse(
      final Object value,
      final Function<String, Object> applyFct) {
    if (value instanceof String) {
      String strValue = (String) value;
      java.nio.file.Path path = Paths.get(strValue);
      if (Files.isDirectory(path)) {
        ArrayList<Object> fixedList = new ArrayList<>();
        try {
          Files.list(path)
            .sorted()
            .forEach(child -> {
              Object parsed = recursiveParse(child.toString(), applyFct);
              if (parsed != IGNORE) {
                fixedList.add(parsed);
              }
            });

          return fixedList;
        } catch (java.io.IOException e) {
          throw new RuntimeException("Failed to read directory: " + path, e);
        }
      }
      return applyFct.apply(strValue);
    } else if (value instanceof List) {
      List<?> listValue = (List<?>) value;
      ArrayList<Object> fixedList = new ArrayList<>();
      for (Object item : listValue) {
        Object parsed = recursiveParse(item, applyFct);
        if (parsed != IGNORE) {
          fixedList.add(parsed);
        }
      }
      return fixedList;
    } else if (value instanceof Map) {
      Map<?, ?> mapValue = (Map<?, ?>) value;
      Map<Object, Object> fixedMap = new TreeMap<>();
      for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
        Object parsed = recursiveParse(entry.getValue(), applyFct);
        if (parsed != IGNORE) {
          fixedMap.put(entry.getKey(), parsed);
        }
      }
      return fixedMap;
    } else {
      return value;
    }
  }

  /**
   * Recursively sanitizes unstable file paths by replacing existing paths
   * with their file names.
   *
   * @param value The value to sanitize.
   * @return The sanitized value with file paths reduced to file names.
   */
  private static Object fixUnstable(final Object value) {
    return recursiveParse(value, strValue -> {
      java.nio.file.Path path = Paths.get(strValue);
      if (Files.exists(path)) {
        return path.getFileName().toString();
      }
      return strValue;
    });
  }

  /**
   * Recursively applies the configured unstable and ignore patterns to a value.
   *
   * Values matching {@code ignorePattern} are excluded, while values matching
   * {@code unstablePattern} have their paths reduced to file names. A value
   * matching both patterns causes a {@link RuntimeException}.
   *
   * @param value The value to sanitize.
   * @param unstablePattern The regular expression patterns identifying unstable
   * values.
   * @param ignorePattern The regular expression patterns identifying values to
   * ignore.
   * @return The sanitized value.
   */
  static Object checkPattern(
    final Object value,
    final List<String> unstablePattern,
    final List<String> ignorePattern) {

    return recursiveParse(value, strValue -> {
      boolean matchIgnore = ignorePattern.stream()
        .anyMatch(strValue::matches);
      boolean matchUnstable = unstablePattern.stream()
        .anyMatch(strValue::matches);
      if (matchIgnore && matchUnstable) {
        throw new RuntimeException(
          "Value '" + strValue
          + "' matches both ignorePattern and unstablePattern"
        );
      }
      if (matchIgnore) {
        return IGNORE;
      }
      if (matchUnstable) {
        return Paths.get(strValue).getFileName().toString();
      }
      return strValue;
    });
  }
}
