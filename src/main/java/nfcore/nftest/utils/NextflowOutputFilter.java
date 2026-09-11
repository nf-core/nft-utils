package nfcore.nftest.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility methods for filtering Nextflow stdout/stderr output to remove
 * variable content that makes test snapshots unstable.
 */
public final class NextflowOutputFilter {

  /**
   * Prevents instantiation of this utility class.
   */
  private NextflowOutputFilter() {
  }

  /**
   * Filters Nextflow stdout/stderr output to remove variable content that makes
   * snapshots unstable.
   *
   * @param output The stdout or stderr output (String or List) to filter
   * @return The filtered output as a List&lt;String&gt; with unstable patterns
   *     removed
   */
  public static List<String> filterNextflowOutput(final Object output) {
    return filterNextflowOutput(output, null, true, false, null, null);
  }

  /**
   * Filters Nextflow stdout/stderr output with optional sorting.
   *
   * @param output The stdout or stderr output (String or List) to filter
   * @param sorted Whether to sort the output lines alphabetically
   * @return The filtered output as a List&lt;String&gt; with unstable patterns
   *     removed
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final boolean sorted) {
    return filterNextflowOutput(output, null, sorted, false, null, null);
  }

  /**
   * Filters Nextflow stdout/stderr output with optional sorting and ANSI code
   * handling.
   *
   * @param output   The stdout or stderr output (String or List) to filter
   * @param sorted   Whether to sort the output lines alphabetically
   * @param keepAnsi Whether to keep ANSI escape codes (colors, formatting)
   * @return The filtered output as a List&lt;String&gt; with unstable patterns
   *     removed
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final boolean sorted,
      final boolean keepAnsi) {
    return filterNextflowOutput(output, null, sorted, keepAnsi, null, null);
  }

  /**
   * Filters Nextflow stdout/stderr output with custom patterns.
   *
   * @param output             The stdout or stderr output (String or List) to
   *                           filter
   * @param additionalPatterns List of additional regex patterns to remove
   * @return The filtered output as a List&lt;String&gt; with unstable patterns
   *     removed
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final List<String> additionalPatterns) {
    return filterNextflowOutput(
      output, additionalPatterns,
      true, false, null, null
    );
  }

  /**
   * Filters Nextflow stdout/stderr output with custom patterns, optional
   * sorting, and ANSI code handling.
   *
   * @param output             The stdout or stderr output (String or List) to
   *                           filter
   * @param additionalPatterns List of additional regex patterns to remove
   * @param sorted             Whether to sort the output lines alphabetically
   * @param keepAnsi           Whether to keep ANSI escape codes
   * @param ignore             List of strings to filter out
   * @param include            List of strings to include
   * @return The filtered output as a List&lt;String&gt; with unstable patterns
   *     removed
   */
  public static List<String> filterNextflowOutput(
      final Object output,
      final List<String> additionalPatterns,
      final boolean sorted,
      final boolean keepAnsi,
      final List<String> ignore,
      final List<String> include) {
    if (output == null) {
      return new ArrayList<>();
    }

    List<String> outputLines;
    if (output instanceof List) {
      List<?> outputList = (List<?>) output;
      if (outputList.isEmpty()) {
        return new ArrayList<>();
      }
      outputLines = outputList.stream()
          .map(Object::toString)
          .collect(Collectors.toList());
    } else if (output instanceof String) {
      String outputString = (String) output;
      if (outputString.isEmpty()) {
        return new ArrayList<>();
      }
      outputLines = Arrays.asList(outputString.split("\n"));
    } else {
      String outputString = output.toString();
      outputLines = Arrays.asList(outputString.split("\n"));
    }

    List<String> filteredLines = new ArrayList<>();
    String capturedRunName = null;

    for (String line : outputLines) {
      String filtered = line;

      if (ignore != null && !ignore.isEmpty()) {
        boolean shouldIgnore = false;
        for (String ignoreString : ignore) {
          if (filtered.contains(ignoreString)) {
            shouldIgnore = true;
            break;
          }
        }
        if (shouldIgnore) {
          continue;
        }
      }

      if (include != null && !include.isEmpty()) {
        boolean shouldInclude = false;
        for (String includeString : include) {
          if (filtered.contains(includeString)) {
            shouldInclude = true;
            break;
          }
        }
        if (!shouldInclude) {
          continue;
        }
      }

      if (!keepAnsi) {
        filtered = filtered.replaceAll(
          "\\x1B\\[[0-9;]*[A-Za-z]", "");
      }

      if (
          capturedRunName == null
          && filtered.contains("Launching")
          && filtered.contains("[")
          && filtered.contains("]")) {
        java.util.regex.Pattern runNamePattern =
          java.util.regex.Pattern.compile("\\[([^\\]]+)\\]");
        java.util.regex.Matcher matcher = runNamePattern.matcher(filtered);
        if (matcher.find()) {
          capturedRunName = matcher.group(1);
        }
      }

      filtered = filterLinePattern(filtered, capturedRunName);

      if (additionalPatterns != null && !additionalPatterns.isEmpty()) {
        for (String pattern : additionalPatterns) {
          try {
            filtered = filtered.replaceAll(pattern, "[FILTERED]");
          } catch (Exception e) {
            System.err.println("Warning: Invalid regex pattern '" + pattern
                + "': " + e.getMessage());
          }
        }
      }

      if (!filtered.trim().isEmpty()) {
        filteredLines.add(filtered);
      }
    }

    if (sorted) {
      List<String> sortableLines = new ArrayList<>();
      List<String> preserveOrderLines = new ArrayList<>();

      for (String line : filteredLines) {
        if (line.contains("Staging foreign file")
            || line.contains("Submitted process")
            || line.startsWith("Creating env using conda:")
            || line.startsWith("Pulling Singularity image")
            || line.startsWith("ERROR ~")
            || line.startsWith("WARN:")
            || (
              line.contains("Check ")
              && line.contains(" file for details")
            )) {
          sortableLines.add(line);
        } else {
          preserveOrderLines.add(line);
        }
      }

      Collections.sort(sortableLines);

      List<String> combinedLines = new ArrayList<>();
      combinedLines.addAll(preserveOrderLines);
      combinedLines.addAll(sortableLines);

      List<String> uniqueLines = new ArrayList<>();
      String lastLine = null;
      for (String line : combinedLines) {
        if (!line.equals(lastLine)) {
          uniqueLines.add(line);
          lastLine = line;
        }
      }
      filteredLines = uniqueLines;
    }

    return filteredLines;
  }

  /**
   * Filters Nextflow output using Groovy's named parameter syntax.
   *
   * @param options LinkedHashMap of named options (Groovy named params)
   * @param output  The stdout or stderr output to filter
   * @return The filtered output as a List&lt;String&gt;
   */
  public static List<String> filterNextflowOutput(
      final LinkedHashMap<String, Object> options,
      final Object output) {
    final Map<String, Object> optionsFixed;
    if (options == null) {
      optionsFixed = new HashMap<>();
    } else {
      optionsFixed = options;
    }

    List<String> additionalPatterns = (List<String>) optionsFixed
      .get("additionalPatterns");
    Boolean sorted = (Boolean) optionsFixed.get("sorted");
    Boolean keepAnsi = (Boolean) optionsFixed.get("keepAnsi");
    List<String> ignore = (List<String>) optionsFixed.get("ignore");
    List<String> include = (List<String>) optionsFixed.get("include");

    if (sorted == null) {
      sorted = true;
    }
    if (keepAnsi == null) {
      keepAnsi = false;
    }
    return filterNextflowOutput(
      output, additionalPatterns,
      sorted, keepAnsi, ignore, include
    );
  }

  /**
   * Sanitizes a Nextflow output line by replacing non-deterministic values with
   * stable placeholders.
   *
   * @param line The output line to sanitize.
   * @param capturedRunName The run name captured from the Nextflow launching
   *     line, or {@code null} if no run name was captured.
   * @return The sanitized output line.
   */
  public static String filterLinePattern(
      final String line,
      final String capturedRunName) {
    String filtered = line;

    String userName = System.getProperty("user.name");
    if (userName != null && !userName.isEmpty()) {
      filtered = filtered.replaceAll(
        "(userName\\s*:\\s*)" + java.util.regex.Pattern.quote(userName),
        "$1[USER]");
    }

    filtered = filtered.replaceAll(
        "\\d{4}-\\d{2}-\\d{2}[T\\s_]\\d{2}[:-]\\d{2}[:-]\\d{2}"
        + "(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?",
        "[TIMESTAMP]");
    filtered = filtered.replaceAll(
      "\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}",
      "[TIMESTAMP]");

    filtered = filtered.replaceAll(
      "\\[[0-9a-f]{2}/[0-9a-f]{6}\\]",
      "[NXF_HASH]");

    filtered = filtered.replaceAll(
      "\\b[0-9a-f]{30,32}\\b",
      "[NFT_HASH]");

    filtered = filtered.replaceAll(
      "revision: [0-9a-f]{10}",
      "revision: [REVISION]");

    filtered = filtered.replaceAll(
      ".*Nextflow\\s+\\d+\\.\\d+\\.\\d+.*is available.*",
      "");
    filtered = filtered.replaceAll(
      ".*Please consider updating your version.*",
      "");

    filtered = filterAbsolutePaths(filtered);

    if (capturedRunName != null) {
      filtered = filtered.replace(
        "[" + capturedRunName + "]",
        "[RUN_NAME]");
      filtered = filtered.replace(
        capturedRunName,
        "[RUN_NAME]");
    }

    filtered = filtered.replaceAll(".*containerEngine.*", "");

    filtered = filtered.replaceAll("apptainer", "[CONTAINER]");
    filtered = filtered.replaceAll("charliecloud", "[CONTAINER]");
    filtered = filtered.replaceAll("conda", "[CONTAINER]");
    filtered = filtered.replaceAll("docker", "[CONTAINER]");
    filtered = filtered.replaceAll("mamba", "[CONTAINER]");
    filtered = filtered.replaceAll("podman", "[CONTAINER]");
    filtered = filtered.replaceAll("shifter", "[CONTAINER]");
    filtered = filtered.replaceAll("singularity", "[CONTAINER]");
    filtered = filtered.replaceAll("wave", "[CONTAINER]");

    filtered = filtered.replaceAll(
      "(nf-core/[^\\s]+\\s+)\\d+\\.\\d+(?:\\.\\d+)?[a-zA-Z]*",
      "$1[VERSION]");

    filtered = filtered.replaceAll(
      "N E X T F L O W  ~  version \\d+\\.\\d+\\.\\d+(-edge)?",
      "N E X T F L O W  ~  version [VERSION]");

    return filtered;
  }

  /**
   * Filters absolute paths in the given text and replaces them with [PATH]
   * placeholder.
   *
   * @param text The text to filter
   * @return The filtered text with various directory paths replaced with
   *     [PATH]
   */
  static String filterAbsolutePaths(final String text) {
    String filtered = text;

    List<String> pathsToReplace = new ArrayList<>();

    String workingDir = System.getProperty("user.dir");
    if (workingDir != null) {
      pathsToReplace.add(workingDir);
    }

    String[] envVars = {
        "HOME",
        "NFT_WORKDIR",
        "NXF_CACHE_DIR",
        "NXF_CONDA_CACHEDIR",
        "NXF_HOME",
        "NXF_SINGULARITY_CACHEDIR",
        "NXF_SINGULARITY_LIBRARYDIR",
        "NXF_TEMP",
        "NXF_WORK"
    };

    for (String envVar : envVars) {
      String envValue = System.getenv(envVar);
      if (envValue != null && !envValue.isEmpty() && !envValue.equals("~")) {
        pathsToReplace.add(envValue);
      }
    }

    String nxfHome = System.getenv("NXF_HOME");
    if (nxfHome == null || nxfHome.isEmpty()) {
      String home = System.getProperty("user.home");
      if (home != null && !home.isEmpty() && !home.equals("~")) {
        pathsToReplace.add(home + "/.nextflow");
      }
    }

    pathsToReplace = pathsToReplace.stream()
        .distinct()
        .sorted((a, b) -> Integer.compare(b.length(), a.length()))
        .collect(Collectors.toList());

    for (String path : pathsToReplace) {
      filtered = filtered.replace(path, "[PATH]");
    }

    return filtered;
  }
}
