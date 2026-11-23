package nfcore.nftest.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Utility methods to remove unecessary output from a snapshot.
 */
public final class OutputSanitizer {

  /**
   * Prevents instantiation of this utility class.
   */
  private OutputSanitizer() {
  }

  /**
   * Validates that all specified keys are present in the given channel,
   * either as a channel name or as a field of the records it emits.
   *
   * @param keysList The list of keys that must be present in the channel.
   * @param channel The channel map to validate.
   * @throws RuntimeException If any of the specified keys is neither a
   *     channel name nor a record field.
   */
  static void validateKeysInChannel(
    final Collection<String> keysList,
    final Map<String, Object> channel) {

    Set<String> recordFields = recordFieldNames(channel);
    for (String keyList : keysList) {
      if (channel.containsKey(keyList) || recordFields.contains(keyList)) {
        continue;
      }
      throw new RuntimeException(
        "Key '" + keyList + "' not present in channel"
        + (recordFields.isEmpty() ? "" : " names or record fields")
      );
    }
  }

  /**
   * Collects the field names of the records emitted by a channel.
   *
   * nf-test deserializes the records of a typed process as maps, so a map
   * emitted directly on a channel is treated as a record. Tuples of untyped
   * processes are lists and do not match.
   *
   * @param channel The channel map to inspect.
   * @return The names of the fields of all records in the channel.
   */
  static Set<String> recordFieldNames(final Map<String, Object> channel) {
    Set<String> fields = new TreeSet<>();
    for (Object value : channel.values()) {
      if (!(value instanceof List)) {
        continue;
      }
      for (Object item : (List<?>) value) {
        if (item instanceof Map) {
          for (Object field : ((Map<?, ?>) item).keySet()) {
            fields.add(String.valueOf(field));
          }
        }
      }
    }
    return fields;
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
    // Can't do valid type checking here because
    // the channels type is not exposed from nf-test
    if (!className.equals("com.askimed.nf.test.lang.channels.Channels")) {
      throw new RuntimeException(
        "sanitizeOutput only supports channels as input, "
        + "pass it either `process.out` or `workflow.out`"
      );
    }

    Options opts = new Options("sanitizeOutput", options,
      "unstableKeys", "ignoreKeys", "unstablePatterns", "ignorePatterns",
      "readsMD5Keys", "variantsMD5Keys", "tableMD5Keys", "referenceFasta",
      "tableDoubleDigits", "tableRemoveComments");
    List<String> unstableKeys = opts.strings("unstableKeys", List.of());
    List<String> ignoreKeys = opts.strings("ignoreKeys", List.of());
    List<String> unstablePatterns = opts.strings("unstablePatterns", List.of());
    List<String> ignorePatterns = opts.strings("ignorePatterns", List.of());
    List<String> readsMD5Keys = opts.strings("readsMD5Keys", List.of());
    List<String> variantsMD5Keys = opts.strings("variantsMD5Keys", List.of());
    List<String> tableMD5Keys = opts.strings("tableMD5Keys", List.of());
    String referenceFasta = opts.string("referenceFasta", "");
    int tableDoubleDigits = opts.integer(
      "tableDoubleDigits", TableUtils.DEFAULT_DOUBLE_DIGITS
    );
    boolean tableRemoveComments = opts.bool(
      "tableRemoveComments", TableUtils.DEFAULT_REMOVE_COMMENTS
    );

    if (tableDoubleDigits < 0) {
      throw new IllegalArgumentException(
        "tableDoubleDigits must be greater than or equal to zero"
      );
    }

    validateKeyUsage(
      unstableKeys, ignoreKeys, readsMD5Keys,
      variantsMD5Keys, tableMD5Keys
    );

    Map<String, Function<Object, Object>> keySanitizers =
      new LinkedHashMap<>();
    unstableKeys.forEach(
      k -> keySanitizers.put(k, OutputSanitizer::fixUnstable)
    );
    ignoreKeys.forEach(k -> keySanitizers.put(k, v -> IGNORE));
    readsMD5Keys.forEach(
      k -> keySanitizers.put(k, v -> BamUtils.bamMD5(v, referenceFasta))
    );
    variantsMD5Keys.forEach(k -> keySanitizers.put(k, VcfUtils::vcfMD5));
    tableMD5Keys.forEach(k -> keySanitizers.put(k, v -> TableUtils.tableMD5(
      v, tableDoubleDigits, tableRemoveComments
    )));

    validateKeysInChannel(keySanitizers.keySet(), channel);

    if (!readsMD5Keys.isEmpty() && !BamUtils.isNftBamAvailable()) {
      System.err.println(
        "WARNING: A compatible version of nft-bam is not available. "
        + "Cannot calculate reads MD5 for BAM/SAM/CRAM files; "
        + "output may be unstable."
      );
      keySanitizers.keySet().removeAll(readsMD5Keys);
    }
    if (!variantsMD5Keys.isEmpty() && !VcfUtils.isNftVcfAvailable()) {
      System.err.println(
        "WARNING: A compatible version of nft-vcf is not available. "
        + "Cannot calculate variants MD5 for VCF files; "
        + "output may be unstable."
      );
      keySanitizers.keySet().removeAll(variantsMD5Keys);
    }

    // A channel name takes precedence over a record field of the same name
    Map<String, Function<Object, Object>> fieldSanitizers =
      new HashMap<>(keySanitizers);
    fieldSanitizers.keySet().removeAll(channel.keySet());
    Function<Object, Object> patternSanitizer =
      patternSanitizer(unstablePatterns, ignorePatterns);
    Function<Object, Object> channelSanitizer =
      v -> sanitizeRecords(v, fieldSanitizers, patternSanitizer);

    // nf-test lists named outputs twice, by index and by name:
    // drop one index entry per named output, keep unnamed outputs
    // shortcut: equal values (e.g. two empty channels) may drop the wrong
    // index; nf-test gives no name-to-index map, revisit if it exposes one
    TreeMap<String, Object> outputs = new TreeMap<>(channel);
    channel.forEach((name, value) -> {
      if (!isIndex(name)) {
        outputs.keySet().stream()
          .filter(key -> isIndex(key) && value.equals(outputs.get(key)))
          .findFirst()
          .ifPresent(outputs::remove);
      }
    });
    return sanitizeMap(outputs, keySanitizers, channelSanitizer);
  }

  /** Matches an output index channel key. */
  private static final Pattern INDEX = Pattern.compile("\\d+");

  /**
   * Checks whether a channel key is an output index, such as {@code "0"}.
   *
   * @param key The channel key.
   * @return {@code true} if the key is an output index.
   */
  private static boolean isIndex(final String key) {
    return INDEX.matcher(key).matches();
  }

  /**
   * Sanitizes the records emitted on a channel field by field.
   *
   * Fields with a registered sanitizer use it, and all other fields, as well
   * as channel items that are not records, use the default sanitizer. With
   * no field sanitizers, the result equals applying the default sanitizer to
   * the whole value.
   *
   * @param value The channel value to sanitize.
   * @param fieldSanitizers Map from record field name to its sanitizer.
   * @param defaultSanitizer The sanitizer for everything else.
   * @return The sanitized channel value.
   */
  static Object sanitizeRecords(
      final Object value,
      final Map<String, Function<Object, Object>> fieldSanitizers,
      final Function<Object, Object> defaultSanitizer) {
    if (!(value instanceof List)) {
      return defaultSanitizer.apply(value);
    }
    return sanitizeList((List<?>) value, item -> item instanceof Map
      ? sanitizeMap((Map<?, ?>) item, fieldSanitizers, defaultSanitizer)
      : defaultSanitizer.apply(item));
  }

  /**
   * Sanitizes the items of a list, dropping the items that resolve to the
   * {@code IGNORE} marker.
   *
   * @param items The items to sanitize.
   * @param sanitizer The sanitizer of each item.
   * @return The sanitized items, in order.
   */
  private static List<Object> sanitizeList(
      final List<?> items,
      final Function<Object, Object> sanitizer) {
    ArrayList<Object> fixedList = new ArrayList<>();
    for (Object item : items) {
      Object parsed = sanitizer.apply(item);
      if (parsed != IGNORE) {
        fixedList.add(parsed);
      }
    }
    return fixedList;
  }

  /**
   * Sanitizes the values of a map, keeping its keys and dropping the values
   * that resolve to the {@code IGNORE} marker.
   *
   * @param <K> The type of the map keys.
   * @param map The map to sanitize.
   * @param keySanitizers Map from key to its sanitizer.
   * @param defaultSanitizer The sanitizer for the other keys.
   * @return The sanitized map, sorted by key.
   */
  private static <K> TreeMap<K, Object> sanitizeMap(
      final Map<K, ?> map,
      final Map<String, Function<Object, Object>> keySanitizers,
      final Function<Object, Object> defaultSanitizer) {
    TreeMap<K, Object> fixedMap = new TreeMap<>();
    for (Map.Entry<K, ?> entry : map.entrySet()) {
      Object parsed = keySanitizers
        .getOrDefault(String.valueOf(entry.getKey()), defaultSanitizer)
        .apply(entry.getValue());
      if (parsed != IGNORE) {
        fixedMap.put(entry.getKey(), parsed);
      }
    }
    return fixedMap;
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
   * Absolute directories are traversed recursively, lists are parsed element
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
      java.nio.file.Path path = outputPath(strValue);
      if (path != null && Files.isDirectory(path)) {
        try (var children = Files.list(path)) {
          return sanitizeList(
            children.sorted().map(java.nio.file.Path::toString).toList(),
            v -> recursiveParse(v, applyFct)
          );
        } catch (java.io.IOException e) {
          throw new RuntimeException(
            "Failed to read directory: " + path, e
          );
        }
      }
      return applyFct.apply(strValue);
    } else if (value instanceof List) {
      return sanitizeList((List<?>) value, v -> recursiveParse(v, applyFct));
    } else if (value instanceof Map) {
      return sanitizeMap(
        (Map<?, ?>) value, Map.of(), v -> recursiveParse(v, applyFct)
      );
    } else {
      return value;
    }
  }

  /**
   * The path of an output file or folder. Outputs are absolute paths; a
   * relative value such as {@code ""} or {@code "src"} is a val, env or
   * stdout output, even if it names a file of the working directory.
   *
   * @param value The string value.
   * @return The path, or {@code null} if the value is not an output path.
   */
  private static java.nio.file.Path outputPath(final String value) {
    return value.startsWith("/") ? Paths.get(value) : null;
  }

  /**
   * Recursively sanitizes unstable file paths by replacing existing output
   * paths with their file names.
   *
   * @param value The value to sanitize.
   * @return The sanitized value with file paths reduced to file names.
   */
  static Object fixUnstable(final Object value) {
    return recursiveParse(value, strValue -> {
      java.nio.file.Path path = outputPath(strValue);
      if (path != null && Files.exists(path)) {
        return path.getFileName().toString();
      }
      return strValue;
    });
  }

  /**
   * Builds a sanitizer that recursively applies the unstable and ignore
   * patterns to a value.
   *
   * Values matching {@code ignorePatterns} are excluded, while values matching
   * {@code unstablePatterns} have their paths reduced to file names. A value
   * matching both patterns causes a {@link RuntimeException}.
   *
   * @param unstablePatterns The glob patterns identifying unstable values.
   * @param ignorePatterns The glob patterns identifying values to ignore.
   * @return The sanitizer.
   */
  static Function<Object, Object> patternSanitizer(
      final List<String> unstablePatterns,
      final List<String> ignorePatterns) {
    List<PathMatcher> unstableMatchers = Utils.buildMatchers(unstablePatterns);
    List<PathMatcher> ignoreMatchers = Utils.buildMatchers(ignorePatterns);
    return value -> recursiveParse(value, strValue -> {
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
