package nf_core.nf.test.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
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
    final List<String> bamMD5Keys,
    final List<String> vcfMD5Keys
  ) {
    Map<String, String> keyUsage = new HashMap<>();

    addKeyUsage(keyUsage, unstableKeys, "unstableKeys");
    addKeyUsage(keyUsage, ignoreKeys, "ignoreKeys");
    addKeyUsage(keyUsage, bamMD5Keys, "bamMD5Keys");
    addKeyUsage(keyUsage, vcfMD5Keys, "vcfMD5Keys");
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

    List<String> bamMD5Keys =
      (List<String>) options.getOrDefault("bamMD5Keys", List.of());

    List<String> vcfMD5Keys =
      (List<String>) options.getOrDefault("vcfMD5Keys", List.of());

    String referenceFasta = (String) options.getOrDefault("referenceFasta", "");

    validateKeyUsage(unstableKeys, ignoreKeys, bamMD5Keys, vcfMD5Keys);

    validateKeysInChannel(unstableKeys, channel);
    validateKeysInChannel(ignoreKeys, channel);
    validateKeysInChannel(bamMD5Keys, channel);
    validateKeysInChannel(vcfMD5Keys, channel);

    if (!bamMD5Keys.isEmpty() && !BamUtils.isNftBamAvailable()) {
      System.err.println(
        "WARNING: A compatible version of nft-bam is not available. "
        + "Cannot calculate reads MD5 for BAM/SAM/CRAM files; "
        + "output may be unstable."
      );
      bamMD5Keys = List.of();
    }
    if (!vcfMD5Keys.isEmpty() && !VcfUtils.isNftVcfAvailable()) {
      System.err.println(
        "WARNING: A compatible version of nft-vcf is not available. "
        + "Cannot calculate variants MD5 for VCF files; "
        + "output may be unstable."
      );
      vcfMD5Keys = List.of();
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
      } else if (bamMD5Keys.contains(key)) {
        output.put(key, BamUtils.bamMD5(value, referenceFasta));
      } else if (vcfMD5Keys.contains(key)) {
        output.put(key, VcfUtils.vcfMD5(value));
      } else {
        output.put(key, value);
      }
    }
    return output;
  }

  static Object recursiveParse(final Object value, final Function<String, Object> applyFct) {
    if (value instanceof String) {
      String strValue = (String) value;
      java.nio.file.Path path = Paths.get(strValue);
      if (Files.isDirectory(path)) {
        ArrayList<Object> fixedList = new ArrayList<>();
        try {
          Files.list(path)
            .sorted()
            .forEach(child -> fixedList.add(
              recursiveParse(child.toString(), applyFct)
            ));

          return fixedList;
        } catch (java.io.IOException e) {
          throw new RuntimeException("Failed to read directory: " + path, e);
        }
      }
      return applyFct.apply(strValue);
    } else if (value instanceof ArrayList || value instanceof Vector) {
      List<?> listValue = (List<?>) value;
      ArrayList<Object> fixedList = new ArrayList<>();
      for (Object item : listValue) {
        fixedList.add(recursiveParse(item, applyFct));
      }
      return fixedList;
    } else if (value instanceof Map) {
      Map<?, ?> mapValue = (Map<?, ?>) value;
      Map<Object, Object> fixedMap = new TreeMap<>();
      for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
        fixedMap.put(
          entry.getKey(),
          recursiveParse(entry.getValue(), applyFct)
        );
      }
      return fixedMap;
    } else {
      return value;
    }
  }

  private static Object fixUnstable(final Object value) {
    return recursiveParse(value, strValue -> {
      java.nio.file.Path path = Paths.get(strValue);
      if (Files.exists(path)) {
        return path.getFileName().toString();
      }
      return strValue;
    });
  }
}
