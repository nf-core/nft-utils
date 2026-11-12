package nfcore.nftest.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Utility methods for filtering Nextflow stdout/stderr output to remove
 * variable content that makes test snapshots unstable.
 */
public final class NextflowOutputFilter {

  /** Matches ANSI escape codes. */
  private static final Pattern ANSI =
    Pattern.compile("\\x1B\\[[0-9;]*[A-Za-z]");

  /** Matches the bracketed run name of the Nextflow launching line. */
  private static final Pattern RUN_NAME =
    Pattern.compile("\\[([^\\]]+)\\]");

  /** Rules applied to a line before paths and the run name are replaced. */
  private static final List<UnaryOperator<String>> RULES_BEFORE_PATHS = List.of(
    rule(
      "\\d{4}-\\d{2}-\\d{2}[T\\s_]\\d{2}[:-]\\d{2}[:-]\\d{2}"
      + "(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?",
      "[TIMESTAMP]"),
    rule("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}", "[TIMESTAMP]"),
    rule("\\[[0-9a-f]{2}/[0-9a-f]{6}\\]", "[NXF_HASH]"),
    rule("\\b[0-9a-f]{30,32}\\b", "[NFT_HASH]"),
    rule("revision: [0-9a-f]{10}", "revision: [REVISION]"),
    rule(".*Nextflow\\s+\\d+\\.\\d+\\.\\d+.*is available.*", ""),
    rule(".*Please consider updating your version.*", "")
  );

  /** Rules applied to a line after paths and the run name are replaced. */
  private static final List<UnaryOperator<String>> RULES_AFTER_RUN_NAME =
    List.of(
      rule(".*containerEngine.*", ""),
      rule(
        "apptainer|charliecloud|conda|docker|mamba|podman|shifter"
        + "|singularity|wave",
        "[CONTAINER]"),
      rule(
        "(nf-core/[^\\s]+\\s+)\\d+\\.\\d+(?:\\.\\d+)?[a-zA-Z]*",
        "$1[VERSION]"),
      rule(
        "N E X T F L O W  ~  version \\d+\\.\\d+\\.\\d+(-edge)?",
        "N E X T F L O W  ~  version [VERSION]")
    );

  /** The current user name, or an empty string. */
  private static final String USER_NAME = System.getProperty("user.name", "");

  /** Matches the user name in a {@code userName :} line. */
  private static final Pattern USER_NAME_FIELD =
    Pattern.compile("(userName\\s*:\\s*)" + Pattern.quote(USER_NAME));

  /** Environment-specific paths, longest first. */
  private static final List<String> PATHS_TO_REPLACE = pathsToReplace();

  /**
   * Prevents instantiation of this utility class.
   */
  private NextflowOutputFilter() {
  }

  /**
   * Compiles a regex replacement.
   *
   * @param regex       The regex to replace
   * @param replacement The replacement
   * @return The replacement as a function of the line
   */
  private static UnaryOperator<String> rule(
      final String regex,
      final String replacement) {
    Pattern pattern = Pattern.compile(regex);
    return line -> pattern.matcher(line).replaceAll(replacement);
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
    List<Pattern> removals = compilePatterns(additionalPatterns);
    List<String> filteredLines = new ArrayList<>();
    String capturedRunName = null;

    for (String line : toLines(output)) {
      if (!keepLine(line, ignore, include)) {
        continue;
      }

      String filtered = line;
      if (!keepAnsi) {
        filtered = ANSI.matcher(filtered).replaceAll("");
      }

      if (capturedRunName == null) {
        capturedRunName = captureRunName(filtered);
      }

      filtered = filterLinePattern(filtered, capturedRunName);

      for (Pattern removal : removals) {
        filtered = removal.matcher(filtered).replaceAll("");
      }

      if (!filtered.trim().isEmpty()) {
        filteredLines.add(filtered);
      }
    }

    if (sorted) {
      return sortOrderIndependentLines(filteredLines);
    }
    return filteredLines;
  }

  /**
   * Compiles regex patterns, warning about and skipping invalid ones.
   *
   * @param patterns The regex patterns, or {@code null}
   * @return The compiled patterns
   */
  private static List<Pattern> compilePatterns(final List<String> patterns) {
    List<Pattern> compiled = new ArrayList<>();
    if (patterns == null) {
      return compiled;
    }
    for (String pattern : patterns) {
      try {
        compiled.add(Pattern.compile(pattern));
      } catch (PatternSyntaxException e) {
        System.err.println("Warning: Invalid regex pattern '" + pattern
            + "': " + e.getMessage());
      }
    }
    return compiled;
  }

  /**
   * Splits output into lines.
   *
   * @param output The output (String or List), or {@code null}
   * @return The output lines
   */
  private static List<String> toLines(final Object output) {
    if (output == null) {
      return List.of();
    }
    if (output instanceof List) {
      return ((List<?>) output).stream()
          .map(Object::toString)
          .collect(Collectors.toList());
    }
    return Arrays.asList(output.toString().split("\n"));
  }

  /**
   * Applies the ignore and include lists to a line.
   *
   * @param line    The line to check
   * @param ignore  Strings rejecting the line, or {@code null}
   * @param include Strings of which the line must contain one, or {@code null}
   *                or empty to accept any line
   * @return {@code true} if the line should be kept
   */
  private static boolean keepLine(
      final String line,
      final List<String> ignore,
      final List<String> include) {
    if (ignore != null && ignore.stream().anyMatch(line::contains)) {
      return false;
    }
    return include == null
      || include.isEmpty()
      || include.stream().anyMatch(line::contains);
  }

  /**
   * Extracts the run name from the Nextflow launching line.
   *
   * @param line The line to inspect
   * @return The run name, or {@code null} if the line does not contain one
   */
  private static String captureRunName(final String line) {
    if (!line.contains("Launching")) {
      return null;
    }
    Matcher matcher = RUN_NAME.matcher(line);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
   * Checks whether a line can appear in a different position between runs.
   *
   * @param line The line to check
   * @return {@code true} if the position of the line is not stable
   */
  private static boolean isOrderIndependent(final String line) {
    return line.contains("Staging foreign file")
      || line.contains("Submitted process")
      || line.startsWith("Creating env using conda:")
      || line.startsWith("Pulling Singularity image")
      || line.startsWith("ERROR ~")
      || line.startsWith("WARN:")
      || (line.contains("Check ") && line.contains(" file for details"));
  }

  /**
   * Moves order-independent lines to the end in sorted order, keeps other
   * lines in place, and drops adjacent duplicates.
   *
   * @param lines The lines to sort
   * @return The sorted lines
   */
  private static List<String> sortOrderIndependentLines(
      final List<String> lines) {
    List<String> combinedLines = lines.stream()
        .filter(line -> !isOrderIndependent(line))
        .collect(Collectors.toList());
    List<String> sortableLines = lines.stream()
        .filter(NextflowOutputFilter::isOrderIndependent)
        .collect(Collectors.toList());
    Collections.sort(sortableLines);
    combinedLines.addAll(sortableLines);

    List<String> uniqueLines = new ArrayList<>();
    String lastLine = null;
    for (String line : combinedLines) {
      if (!line.equals(lastLine)) {
        uniqueLines.add(line);
        lastLine = line;
      }
    }
    return uniqueLines;
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

    if (!USER_NAME.isEmpty()) {
      filtered = USER_NAME_FIELD.matcher(filtered).replaceAll("$1[USER]");
    }

    filtered = applyRules(RULES_BEFORE_PATHS, filtered);
    filtered = filterAbsolutePaths(filtered);

    if (capturedRunName != null) {
      filtered = filtered.replace(
        "[" + capturedRunName + "]",
        "[RUN_NAME]");
      filtered = filtered.replace(
        capturedRunName,
        "[RUN_NAME]");
    }

    return applyRules(RULES_AFTER_RUN_NAME, filtered);
  }

  /**
   * Applies rules to a line, in order.
   *
   * @param rules The rules to apply
   * @param line  The line
   * @return The rewritten line
   */
  private static String applyRules(
      final List<UnaryOperator<String>> rules,
      final String line) {
    String filtered = line;
    for (UnaryOperator<String> rule : rules) {
      filtered = rule.apply(filtered);
    }
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
    for (String path : PATHS_TO_REPLACE) {
      filtered = filtered.replace(path, "[PATH]");
    }
    return filtered;
  }

  /**
   * Collects the environment-specific directories to hide from the output.
   *
   * @return The distinct directories, longest first
   */
  private static List<String> pathsToReplace() {
    List<String> paths = new ArrayList<>();

    String workingDir = System.getProperty("user.dir");
    if (workingDir != null) {
      paths.add(workingDir);
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
        paths.add(envValue);
      }
    }

    String nxfHome = System.getenv("NXF_HOME");
    if (nxfHome == null || nxfHome.isEmpty()) {
      String home = System.getProperty("user.home");
      if (home != null && !home.isEmpty() && !home.equals("~")) {
        paths.add(home + "/.nextflow");
      }
    }

    return paths.stream()
        .distinct()
        .sorted((a, b) -> Integer.compare(b.length(), a.length()))
        .collect(Collectors.toList());
  }
}
