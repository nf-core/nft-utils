package nfcore.nftest.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility methods to remove unecessary output from a snapshot.
 */
public final class OutputSanitizer {

  /** Default number of decimal places for table double values. */
  private static final int DEFAULT_TABLE_DOUBLE_DIGITS =
    TableUtils.DEFAULT_DOUBLE_DIGITS;

  /** Default decision to keep comments in table. */
  private static final boolean DEFAULT_TABLE_REMOVE_COMMENTS = false;

  /**
   * Prevents instantiation of this utility class.
   */
  private OutputSanitizer() {
  }

  /**
   * Validates that all specified keys are present in the given channel.
   *
   * @param keysList The list of keys that must be present in the channel.
   * @param channel The channel map to validate.
   * @throws RuntimeException If any of the specified keys is not present in
   *     the channel.
   */
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

  /**
   * Validates that keys are not used in conflicting sanitization categories.
   *
   * @param unstableKeys Keys configured for unstable output handling.
   * @param ignoreKeys Keys configured to be ignored.
   * @param readsMD5Keys Keys configured for reads MD5 calculation.
   * @param variantsMD5Keys Keys configured for variants MD5 calculation.
   * @param tableMD5Keys Keys configured for table MD5 calculation.
   * @throws RuntimeException If a key is configured in more than one category.
   */
  static void validateKeyUsage(
    final List<String> unstableKeys,
    final List<String> ignoreKeys,
    final List<String> readsMD5Keys,
    final List<String> variantsMD5Keys,
    final List<String> tableMD5Keys
  ) {
    Map<String, String> keyUsage = new HashMap<>();

    addKeyUsage(keyUsage, unstableKeys, "unstableKeys");
    addKeyUsage(keyUsage, ignoreKeys, "ignoreKeys");
    addKeyUsage(keyUsage, readsMD5Keys, "readsMD5Keys");
    addKeyUsage(keyUsage, variantsMD5Keys, "variantsMD5Keys");
    addKeyUsage(keyUsage, tableMD5Keys, "tableMD5Keys");
  }

  /**
   * Records the usage of each key and detects conflicting configuration.
   *
   * @param keyUsage Map tracking each key and the option in which it is used.
   * @param keys Keys associated with the current option.
   * @param option Name of the option associated with the keys.
   * @throws RuntimeException If a key has already been registered for another
   *     option.
   */
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
    if (!className.equals("com.askimed.nf.test.lang.channels.Channels")) {
      throw new RuntimeException(
        "sanitizeOutput only supports channels as input, "
        + "pass it either `process.out` or `workflow.out`"
      );
    }

    SanitizeOptions opts = new SanitizeOptions(options);

    validateKeyUsage(
      opts.getUnstableKeys(), opts.getIgnoreKeys(),
      opts.getReadsMD5Keys(), opts.getVariantsMD5Keys(),
      opts.getTableMD5Keys()
    );

    validateKeysInChannel(opts.getUnstableKeys(), channel);
    validateKeysInChannel(opts.getIgnoreKeys(), channel);
    validateKeysInChannel(opts.getReadsMD5Keys(), channel);
    validateKeysInChannel(opts.getVariantsMD5Keys(), channel);
    validateKeysInChannel(opts.getTableMD5Keys(), channel);

    List<String> readsMD5Keys = opts.getReadsMD5Keys();
    if (!readsMD5Keys.isEmpty() && !BamUtils.isNftBamAvailable()) {
      System.err.println(
        "WARNING: A compatible version of nft-bam is not available. "
        + "Cannot calculate reads MD5 for BAM/SAM/CRAM files; "
        + "output may be unstable."
      );
      readsMD5Keys = List.of();
    }
    List<String> variantsMD5Keys = opts.getVariantsMD5Keys();
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
        continue;
      }
      if (opts.getIgnoreKeys().contains(key)) {
        continue;
      }

      if (opts.getUnstableKeys().contains(key)) {
        output.put(key, fixUnstable(value));
      } else if (readsMD5Keys.contains(key)) {
        output.put(key, BamUtils.bamMD5(
          value, opts.getReferenceFasta()));
      } else if (variantsMD5Keys.contains(key)) {
        output.put(key, VcfUtils.vcfMD5(value));
      } else if (opts.getTableMD5Keys().contains(key)) {
        output.put(key, TableUtils.tableMD5(
          value,
          opts.getTableDoubleDigits(),
          opts.getTableRemoveComments()));
      } else {
        output.put(key, checkPattern(
          value, opts.getUnstablePatterns(),
          opts.getIgnorePatterns()));
      }
    }
    return output;
  }

  /**
   * Holds the parsed and validated sanitization options.
   */
  private static final class SanitizeOptions {
    /** Keys whose values should be replaced with file names. */
    private final List<String> unstableKeys;
    /** Keys to exclude from output entirely. */
    private final List<String> ignoreKeys;
    /** Glob patterns matching unstable file paths. */
    private final List<String> unstablePatterns;
    /** Glob patterns matching values to ignore. */
    private final List<String> ignorePatterns;
    /** Keys whose BAM/SAM/CRAM values should be hashed. */
    private final List<String> readsMD5Keys;
    /** Keys whose VCF/BCF values should be hashed. */
    private final List<String> variantsMD5Keys;
    /** Keys whose table values should be hashed. */
    private final List<String> tableMD5Keys;
    /** Reference FASTA path for BAM MD5 calculation. */
    private final String referenceFasta;
    /** Decimal places for table floating-point normalization. */
    private final int tableDoubleDigits;
    /** Whether leading comment lines are removed from tables. */
    private final boolean tableRemoveComments;

    /**
     * Creates sanitization options from a map.
     *
     * @param options The options map from Groovy named params.
     */
    @SuppressWarnings("unchecked")
    SanitizeOptions(final HashMap<String, Object> options) {
      this.unstableKeys = (List<String>) options
        .getOrDefault("unstableKeys", List.of());
      this.ignoreKeys = (List<String>) options
        .getOrDefault("ignoreKeys", List.of());
      this.unstablePatterns = (List<String>) options
        .getOrDefault("unstablePatterns", List.of());
      this.ignorePatterns = (List<String>) options
        .getOrDefault("ignorePatterns", List.of());
      this.readsMD5Keys = (List<String>) options
        .getOrDefault("readsMD5Keys", List.of());
      this.variantsMD5Keys = (List<String>) options
        .getOrDefault("variantsMD5Keys", List.of());
      this.tableMD5Keys = (List<String>) options
        .getOrDefault("tableMD5Keys", List.of());
      this.referenceFasta = (String) options
        .getOrDefault("referenceFasta", "");
      this.tableDoubleDigits = (int) options
        .getOrDefault(
          "tableDoubleDigits", DEFAULT_TABLE_DOUBLE_DIGITS);
      this.tableRemoveComments = (boolean) options
        .getOrDefault(
          "tableRemoveComments", DEFAULT_TABLE_REMOVE_COMMENTS);

      if (this.tableDoubleDigits < 0) {
        throw new IllegalArgumentException(
          "tableDoubleDigits must be >= 0"
        );
      }
    }

    /**
     * Returns keys whose values should be replaced with file names.
     *
     * @return the unstable keys
     */
    List<String> getUnstableKeys() {
      return unstableKeys;
    }

    /**
     * Returns keys to exclude from output entirely.
     *
     * @return the ignore keys
     */
    List<String> getIgnoreKeys() {
      return ignoreKeys;
    }

    /**
     * Returns glob patterns matching unstable file paths.
     *
     * @return the unstable patterns
     */
    List<String> getUnstablePatterns() {
      return unstablePatterns;
    }

    /**
     * Returns glob patterns matching values to ignore.
     *
     * @return the ignore patterns
     */
    List<String> getIgnorePatterns() {
      return ignorePatterns;
    }

    /**
     * Returns keys whose BAM/SAM/CRAM values should be hashed.
     *
     * @return the reads MD5 keys
     */
    List<String> getReadsMD5Keys() {
      return readsMD5Keys;
    }

    /**
     * Returns keys whose VCF/BCF values should be hashed.
     *
     * @return the variants MD5 keys
     */
    List<String> getVariantsMD5Keys() {
      return variantsMD5Keys;
    }

    /**
     * Returns keys whose table values should be hashed.
     *
     * @return the table MD5 keys
     */
    List<String> getTableMD5Keys() {
      return tableMD5Keys;
    }

    /**
     * Returns the reference FASTA path for BAM MD5.
     *
     * @return the reference FASTA path
     */
    String getReferenceFasta() {
      return referenceFasta;
    }

    /**
     * Returns decimal places for table float normalization.
     *
     * @return the number of decimal digits
     */
    int getTableDoubleDigits() {
      return tableDoubleDigits;
    }

    /**
     * Returns whether leading comment lines are removed from tables.
     *
     * @return true to remove comment lines
     */
    boolean getTableRemoveComments() {
      return tableRemoveComments;
    }
  }

  /**
   * Marker object used to indicate that a value should be excluded from the
   * recursively parsed result.
   */
  private static final Object IGNORE = new Object();

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
  static Object recursiveParse(
      final Object value,
      final Function<String, Object> applyFct) {
    if (value instanceof String) {
      String strValue = (String) value;
      if (strValue.isEmpty()) {
        return IGNORE;
      }
      java.nio.file.Path path = Paths.get(strValue);
      if (Files.isDirectory(path)) {
        ArrayList<Object> fixedList = new ArrayList<>();
        try (var children = Files.list(path)) {
          children
            .sorted()
            .forEach(child -> {
              Object parsed = recursiveParse(child.toString(), applyFct);
              if (parsed != IGNORE) {
                fixedList.add(parsed);
              }
            });

          return fixedList;
        } catch (java.io.IOException e) {
          throw new RuntimeException(
            "Failed to read directory: " + path, e
          );
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
   * Values matching {@code ignorePatterns} are excluded, while values matching
   * {@code unstablePatterns} have their paths reduced to file names. A value
   * matching both patterns causes a {@link RuntimeException}.
   *
   * @param value The value to sanitize.
   * @param unstablePatterns The glob patterns identifying unstable values.
   * @param ignorePatterns The glob patterns identifying values to ignore.
   * @return The sanitized value.
   */
  static Object checkPattern(
      final Object value,
      final List<String> unstablePatterns,
      final List<String> ignorePatterns) {

    List<PathMatcher> ignoreMatchers = ignorePatterns.stream()
      .map(pattern -> FileSystems.getDefault()
        .getPathMatcher("glob:" + pattern))
      .collect(Collectors.toList());

    List<PathMatcher> unstableMatchers = unstablePatterns.stream()
      .map(pattern -> FileSystems.getDefault()
        .getPathMatcher("glob:" + pattern))
      .collect(Collectors.toList());

    return recursiveParse(value, strValue -> {
      boolean matchIgnore = ignoreMatchers.stream()
        .anyMatch(matcher -> matcher.matches(Paths.get(strValue)));
      boolean matchUnstable = unstableMatchers.stream()
        .anyMatch(matcher -> matcher.matches(Paths.get(strValue)));
      if (matchIgnore && matchUnstable) {
        throw new RuntimeException(
          "Value '" + strValue
          + "' matches both ignorePatterns and unstablePatterns"
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
