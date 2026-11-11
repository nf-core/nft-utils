package nfcore.nftest.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility methods for traversing directories and collecting file paths.
 */
public final class FileTraversalUtils {

  /**
   * Prevents instantiation of this utility class.
   */
  private FileTraversalUtils() {
  }

  /**
   * Number of fields expected in an AWS S3 CLI listing line.
   */
  private static final int AWS_S3_LIST_FIELDS = 4;

  /**
   * Index of the object key in an AWS S3 CLI listing line.
   */
  private static final int AWS_S3_KEY_INDEX = 3;

  /**
   * Filters relative paths: a path is accepted when it matches an include
   * matcher and no exclude matcher.
   */
  static final class PathFilter {

    /** Matchers of the paths to accept. */
    private final List<PathMatcher> include;

    /** Matchers of the paths to reject. */
    private final List<PathMatcher> exclude;

    /**
     * Creates a filter.
     *
     * @param includeMatchers Matchers of the paths to accept.
     * @param excludeMatchers Matchers of the paths to reject.
     */
    PathFilter(
        final List<PathMatcher> includeMatchers,
        final List<PathMatcher> excludeMatchers) {
      this.include = includeMatchers;
      this.exclude = excludeMatchers;
    }

    /**
     * Tests a path against the filter.
     *
     * @param relativePath The path to test.
     * @return {@code true} if the path is accepted.
     */
    boolean accepts(final Path relativePath) {
      return include.stream().anyMatch(m -> m.matches(relativePath))
        && exclude.stream().noneMatch(m -> m.matches(relativePath));
    }
  }

  /**
   * Compiles glob patterns into path matchers, skipping null or empty globs.
   *
   * @param globs The glob patterns, or {@code null}.
   * @return The path matchers.
   */
  static List<PathMatcher> buildMatchers(final List<String> globs) {
    List<PathMatcher> matchers = new ArrayList<>();
    if (globs == null) {
      return matchers;
    }
    for (String glob : globs) {
      if (glob != null && !glob.isEmpty()) {
        matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
      }
    }
    return matchers;
  }

  /**
   * Builds the exclude matchers from ignore globs and an optional ignore file.
   *
   * @param ignoreGlobs Glob patterns to exclude, or {@code null}.
   * @param ignoreFilePath Path to a file of additional glob patterns, or
   *     {@code null} or empty.
   * @return The path matchers.
   * @throws IOException If an error occurs while reading the ignore file.
   */
  private static List<PathMatcher> buildExcludeMatchers(
      final List<String> ignoreGlobs,
      final String ignoreFilePath)
      throws IOException {
    List<PathMatcher> matchers = buildMatchers(ignoreGlobs);
    if (ignoreFilePath != null && !ignoreFilePath.isEmpty()) {
      matchers.addAll(buildMatchers(readGlobsFromFile(ignoreFilePath)));
    }
    return matchers;
  }

  /**
   * Walks a directory tree and collects the paths accepted by the filter.
   *
   * @param root The directory to walk.
   * @param filter The filter, applied to paths relative to {@code root}.
   * @param keepDir Selects the directories that can be collected.
   * @return The accepted paths, in traversal order.
   * @throws IOException If an error occurs while traversing the directory.
   */
  private static List<Path> walk(
      final Path root,
      final PathFilter filter,
      final Predicate<Path> keepDir)
      throws IOException {
    List<Path> matches = new ArrayList<>();
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(
              final Path file,
              final BasicFileAttributes attrs) {
            if (filter.accepts(root.relativize(file))) {
              matches.add(file);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(
              final Path dir,
              final BasicFileAttributes attrs) {
            if (keepDir.test(dir) && filter.accepts(root.relativize(dir))) {
              matches.add(dir);
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return matches;
  }

  /**
   * Retrieves files from an output directory, optionally as paths relative to
   * it.
   *
   * @param outdir The root output directory to traverse.
   * @param includeDir Whether directories should be included in the result.
   * @param ignoreGlobs Glob patterns identifying files or directories to
   *     exclude.
   * @param ignoreFilePath Path to a file containing additional ignore glob
   *     patterns, or {@code null}.
   * @param includeGlobs Glob patterns identifying files or directories to
   *     include.
   * @param relative Whether to return paths relative to {@code outdir}.
   * @return A list of matching files, or relative paths when {@code relative}
   *     is enabled.
   * @throws IOException If an error occurs while traversing the directory or
   *     reading the ignore patterns file.
   * @throws IllegalArgumentException If {@code outdir} is null, empty, does
   *     not exist, or is not a directory.
   */
  public static List<?> getAllFilesFromDir(
      final String outdir,
      final boolean includeDir,
      final List<String> ignoreGlobs,
      final String ignoreFilePath,
      final List<String> includeGlobs,
      final boolean relative)
      throws IOException {
    if (outdir == null || outdir.isEmpty()) {
      throw new IllegalArgumentException(
        "The 'outdir' parameter is required."
      );
    }
    Path dirPath = Paths.get(outdir);
    if (!Files.exists(dirPath)) {
      throw new IllegalArgumentException(
        "The specified path does not exist: " + outdir
      );
    }

    if (!Files.isDirectory(dirPath)) {
      throw new IllegalArgumentException(
        "The specified path is not a directory: " + outdir
      );
    }

    List<File> files = getAllFilesFromDir(
      outdir, includeDir, ignoreGlobs,
      ignoreFilePath, includeGlobs);

    if (relative) {
      return getRelativePath(files, outdir);
    } else {
      return files;
    }
  }

  /**
   * Recursively retrieves files and optionally directories from an output
   * directory, applying include and exclude glob patterns.
   *
   * @param outdir The root output directory to traverse.
   * @param includeDir Whether directories should be included in the result.
   * @param ignoreGlobs Glob patterns identifying files or directories to
   *     exclude.
   * @param ignoreFilePath Path to a file containing additional ignore glob
   *     patterns.
   * @param includeGlobs Glob patterns identifying files or directories to
   *     include.
   * @return A sorted list of matching files and, if enabled, directories.
   * @throws IOException If an error occurs while traversing the directory or
   *     reading the ignore patterns file.
   */
  public static List<File> getAllFilesFromDir(
      final String outdir,
      final boolean includeDir,
      final List<String> ignoreGlobs,
      final String ignoreFilePath,
      final List<String> includeGlobs)
      throws IOException {
    PathFilter filter = new PathFilter(
      buildMatchers(includeGlobs),
      buildExcludeMatchers(ignoreGlobs, ignoreFilePath)
    );
    Predicate<Path> keepDir = dir -> {
      Path fileName = dir.getFileName();
      return includeDir
        && fileName != null
        && !fileName.toString().equals("output");
    };

    return walk(Paths.get(outdir), filter, keepDir)
      .stream()
      .map(Path::toFile)
      .sorted(Comparator.comparing(File::getPath))
      .collect(Collectors.toList());
  }

  /**
   * Reads glob patterns from a file, ignoring empty lines and
   * surrounding whitespace.
   *
   * @param filePath The path to the file containing glob patterns.
   * @return A list of glob patterns read from the file.
   * @throws IOException If an error occurs while reading the file.
   */
  static List<String> readGlobsFromFile(
      final String filePath)
      throws IOException {
    List<String> globs = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(
        Paths.get(filePath),
        StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty()) {
          globs.add(line);
        }
      }
    }
    return globs;
  }

  /**
   * Converts a list of file paths to paths relative to the specified base
   * directory.
   *
   * @param filePaths The file paths to convert.
   * @param baseDir The base directory used to calculate relative paths.
   * @return A list of paths relative to the specified base directory.
   */
  public static List<String> getRelativePath(
      final List<File> filePaths,
      final String baseDir) {
    Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();

    return filePaths
        .stream()
        .map(filePath -> {
          Path path = Paths.get(filePath.toURI()).toAbsolutePath().normalize();
          return basePath.relativize(path).toString();
        })
        .collect(Collectors.toList());
  }

  /**
   * Get all file paths from a Nextflow channel output.
   *
   * @param channel the channel output to process
   * @return a flattened list containing only absolute file paths
   */
  public static List<String> getAllFilesFromChannel(
      final Object channel) {
    List<String> result = new ArrayList<>();

    if (channel == null) {
      return result;
    }

    flattenAndFilter(channel, result);

    return result;
  }

  /**
   * Helper method to recursively flatten nested collections and filter items.
   *
   * @param obj The object to recursively flatten and filter.
   * @param result The list to which matching absolute paths are added.
   */
  static void flattenAndFilter(
      final Object obj,
      final List<String> result) {
    if (obj == null) {
      return;
    }

    if (obj instanceof Map) {
      return;
    }

    if (obj instanceof Iterable) {
      for (Object item : (Iterable) obj) {
        flattenAndFilter(item, result);
      }
    } else if (obj instanceof String) {
      String str = (String) obj;
      if (str.startsWith("/")) {
        result.add(str);
      }
    } else if (obj.getClass().isArray()) {
      int length = java.lang.reflect.Array.getLength(obj);
      for (int i = 0; i < length; i++) {
        flattenAndFilter(java.lang.reflect.Array.get(obj, i), result);
      }
    }
  }

  /**
   * Lists all files at the given path (local or cloud), returning sorted
   * relative paths.
   *
   * @param path           The path to list - a local directory or a cloud URI
   * @param ignoreGlobs    Glob patterns to exclude
   * @param includeGlobs   Glob patterns to include; empty includes everything
   * @param includeDir     Whether directories should be included
   * @param ignoreFilePath Path to a file of additional ignore globs, or
   *                       {@code null}
   * @param noSignRequest  Whether to list S3 with {@code --no-sign-request}
   * @return A sorted list of relative file paths under {@code path}
   * @throws IOException if the path cannot be walked or the AWS CLI fails
   * @throws InterruptedException if the AWS CLI process is interrupted
   */
  public static List<String> getAllFilesFromPath(
      final String path,
      final List<String> ignoreGlobs,
      final List<String> includeGlobs,
      final boolean includeDir,
      final String ignoreFilePath,
      final boolean noSignRequest)
      throws IOException, InterruptedException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException(
        "The 'path' parameter is required."
      );
    }

    // An empty include list means everything here, unlike getAllFilesFromDir
    List<PathMatcher> includeMatchers = buildMatchers(includeGlobs);
    if (includeMatchers.isEmpty()) {
      includeMatchers = buildMatchers(List.of("**"));
    }
    PathFilter filter = new PathFilter(
      includeMatchers,
      buildExcludeMatchers(ignoreGlobs, ignoreFilePath)
    );

    if (path.startsWith("s3://")) {
      return getAllFilesFromS3ViaCli(
        path, filter, includeDir, noSignRequest
      );
    }

    Path root = Paths.get(path);
    return walk(root, filter, dir -> includeDir && !dir.equals(root))
      .stream()
      .map(match -> root.relativize(match).toString())
      .filter(relative -> !relative.isEmpty())
      .sorted()
      .collect(Collectors.toList());
  }

  /**
   * Lists files under an S3 prefix using the AWS CLI
   * ({@code aws s3 ls --recursive}).
   *
   * @param s3Path The S3 path or prefix to list.
   * @param filter The include and exclude filter for relative paths.
   * @param includeDir Whether directory markers should be included.
   * @param noSignRequest Whether to use {@code --no-sign-request}.
   * @return A sorted list of S3 object keys matching the filters.
   * @throws IOException If an I/O error occurs.
   * @throws InterruptedException If the thread is interrupted.
   */
  private static List<String> getAllFilesFromS3ViaCli(
      final String s3Path,
      final PathFilter filter,
      final boolean includeDir,
      final boolean noSignRequest)
      throws IOException, InterruptedException {

    String normalizedPath = s3Path;
    if (!s3Path.endsWith("/")) {
      normalizedPath += "/";
    }
    String bucketAndPrefix = normalizedPath.substring("s3://".length());
    int firstSlash = bucketAndPrefix.indexOf('/');
    String prefix;
    if (firstSlash >= 0) {
      prefix = bucketAndPrefix.substring(firstSlash + 1);
    } else {
      prefix = "";
    }

    List<String> cmd = new ArrayList<>(
      Arrays.asList("aws", "s3", "ls", "--recursive")
    );
    if (noSignRequest) {
      cmd.add("--no-sign-request");
    }
    cmd.add(normalizedPath);

    // stderr is inherited: an undrained pipe can block the CLI
    Process process = new ProcessBuilder(cmd)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
      .start();

    List<String> files = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(
          process.getInputStream(),
          StandardCharsets.UTF_8
        )
    )) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.trim().split("\\s+", AWS_S3_LIST_FIELDS);
        if (parts.length < AWS_S3_LIST_FIELDS) {
          continue;
        }

        String fullKey = parts[AWS_S3_KEY_INDEX];
        String relativePath;
        if (!prefix.isEmpty() && fullKey.startsWith(prefix)) {
          relativePath = fullKey.substring(prefix.length());
        } else {
          relativePath = fullKey;
        }

        if (relativePath.isEmpty()) {
          continue;
        }

        boolean isDir = relativePath.endsWith("/");
        if (isDir && !includeDir) {
          continue;
        }

        final String pathString;
        if (isDir) {
          pathString = relativePath.substring(0, relativePath.length() - 1);
        } else {
          pathString = relativePath;
        }
        if (filter.accepts(Paths.get(pathString))) {
          files.add(relativePath);
        }
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IOException(
          "AWS CLI returned exit code " + exitCode
          + " when listing: " + normalizedPath);
    }

    return files.stream().sorted().collect(Collectors.toList());
  }
}
