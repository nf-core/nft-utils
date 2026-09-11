package nfcore.nftest.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
   * Retrieves all files from the specified directory using default options.
   *
   * @param path The path to the directory to traverse.
   * @return A list of files found in the directory.
   * @throws IOException If an error occurs while traversing the directory.
   */
  public static List<?> getAllFilesFromDir(
      final String path)
      throws IOException {
    return getAllFilesFromDir(new LinkedHashMap<String, Object>(), path);
  }

  /**
   * Retrieves files from an output directory using options provided in a map.
   *
   * @param options Options controlling directory traversal and filtering.
   * @param outdir The root output directory to traverse.
   * @return A list of matching files or relative paths when {@code relative}
   *     is enabled.
   * @throws IOException If an error occurs while traversing the directory or
   *     reading the ignore patterns file.
   * @throws IllegalArgumentException If {@code outdir} is null, empty, does
   *     not exist, or is not a directory.
   */
  public static List<?> getAllFilesFromDir(
      final LinkedHashMap<String, Object> options,
      final String outdir)
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

    Boolean includeDir = (Boolean) options
      .getOrDefault("includeDir", false);
    List<String> ignoreGlobs = (List<String>) options
      .getOrDefault("ignore", new ArrayList<String>());
    String ignoreFilePath = (String) options
      .get("ignoreFile");
    Boolean relative = (Boolean) options
      .getOrDefault("relative", false);
    List<String> includeGlobs = (List<String>) options
      .getOrDefault("include", Arrays.asList("*", "**/*"));

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
    List<File> output = new ArrayList<>();
    Path directory = Paths.get(outdir);

    List<String> allIgnoreGlobs = new ArrayList<>();
    if (ignoreGlobs != null) {
      allIgnoreGlobs.addAll(ignoreGlobs);
    }
    if (ignoreFilePath != null && !ignoreFilePath.isEmpty()) {
      allIgnoreGlobs.addAll(readGlobsFromFile(ignoreFilePath));
    }

    List<PathMatcher> excludeMatchers = new ArrayList<>();
    for (String glob : allIgnoreGlobs) {
      excludeMatchers.add(
        FileSystems.getDefault().getPathMatcher("glob:" + glob)
      );
    }

    List<String> allIncludeGlobs = new ArrayList<>();
    if (includeGlobs != null) {
      allIncludeGlobs.addAll(includeGlobs);
    }

    List<PathMatcher> includeMatchers = new ArrayList<>();
    for (String glob : allIncludeGlobs) {
      includeMatchers.add(
        FileSystems.getDefault().getPathMatcher("glob:" + glob)
      );
    }

    Files.walkFileTree(
        directory,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(
              final Path file,
              final BasicFileAttributes attrs) {
            if (isIncluded(file) && !isExcluded(file)) {
              output.add(file.toFile());
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(
              final Path dir,
              final BasicFileAttributes attrs) {
            Path fileName = dir.getFileName();
            if (
                includeDir
                && isIncluded(dir)
                && !isExcluded(dir)
                && fileName != null
                && !fileName.toString().equals("output")) {
              output.add(dir.toFile());
            }
            return FileVisitResult.CONTINUE;
          }

          private boolean isExcluded(final Path path) {
            return excludeMatchers
              .stream()
              .anyMatch(matcher -> matcher.matches(directory.relativize(path)));
          }

          private boolean isIncluded(final Path path) {
            return includeMatchers
              .stream()
              .anyMatch(matcher -> matcher.matches(directory.relativize(path)));
          }
        });

    return output
      .stream()
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
   * Lists all files at the given path, returning sorted relative paths.
   *
   * @param path The path to list - a local directory or an S3 URI
   * @return A sorted list of relative file paths under {@code path}
   * @throws IOException if the path cannot be walked or the AWS CLI fails
   * @throws InterruptedException if the AWS CLI process is interrupted
   */
  public static List<String> getAllFilesFromPath(
      final String path)
      throws IOException, InterruptedException {
    return getAllFilesFromPath(new LinkedHashMap<String, Object>(), path);
  }

  /**
   * Lists all files at the given path (local or cloud), returning sorted
   * relative paths, with filtering options.
   *
   * @param options Named options map (Groovy named params)
   * @param path    The path to list - a local directory or a cloud URI
   * @return A sorted list of relative file paths under {@code path}
   * @throws IOException if the path cannot be walked or the AWS CLI fails
   * @throws InterruptedException if the AWS CLI process is interrupted
   */
  public static List<String> getAllFilesFromPath(
      final LinkedHashMap<String, Object> options,
      final String path)
      throws IOException, InterruptedException {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException(
        "The 'path' parameter is required."
      );
    }

    List<String> ignoreGlobs =
        (List<String>) options.getOrDefault(
          "ignore", new ArrayList<String>()
        );
    List<String> includeGlobs =
        (List<String>) options.getOrDefault(
          "include", Arrays.asList("**", "*")
        );
    Boolean includeDir = (Boolean) options
      .getOrDefault("includeDir", false);
    String ignoreFilePath = (String) options.get("ignoreFile");
    Boolean noSignRequest = (Boolean) options
      .getOrDefault("noSignRequest", false);

    List<String> allIgnoreGlobs = new ArrayList<>(ignoreGlobs);
    if (ignoreFilePath != null && !ignoreFilePath.isEmpty()) {
      allIgnoreGlobs.addAll(readGlobsFromFile(ignoreFilePath));
    }

    List<PathMatcher> excludeMatchers = new ArrayList<>();
    for (String glob : allIgnoreGlobs) {
      if (glob != null && !glob.isEmpty()) {
        excludeMatchers.add(FileSystems
          .getDefault()
          .getPathMatcher("glob:" + glob)
        );
      }
    }

    List<PathMatcher> includeMatchers = new ArrayList<>();
    for (String glob : includeGlobs) {
      if (glob != null && !glob.isEmpty()) {
        includeMatchers.add(FileSystems
          .getDefault()
          .getPathMatcher("glob:" + glob)
        );
      }
    }

    if (path.startsWith("s3://")) {
      return ArchiveDownloader.getAllFilesFromS3ViaCli(
        path, includeMatchers, excludeMatchers,
        includeDir, noSignRequest
      );
    }

    Path root = Paths.get(path);
    List<String> files = new ArrayList<>();

    Files.walkFileTree(
        root,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(
              final Path file,
              final BasicFileAttributes attrs) {
            String relative = root.relativize(file).toString();
            if (relative.isEmpty()) {
              return FileVisitResult.CONTINUE;
            }
            Path relLocal = Paths.get(relative);
            boolean included =
                includeMatchers.isEmpty()
                    || includeMatchers
                      .stream()
                      .anyMatch(m -> m.matches(relLocal));
            boolean excluded = excludeMatchers
              .stream()
              .anyMatch(m -> m.matches(relLocal));
            if (included && !excluded) {
              files.add(relative);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(
              final Path dir,
              final BasicFileAttributes attrs) {
            if (dir.equals(root)) {
              return FileVisitResult.CONTINUE;
            }
            if (includeDir) {
              String relative = root.relativize(dir).toString();
              if (!relative.isEmpty()) {
                Path relLocal = Paths.get(relative);
                boolean included =
                    includeMatchers.isEmpty()
                        || includeMatchers
                          .stream()
                          .anyMatch(m -> m.matches(relLocal));
                boolean excluded = excludeMatchers
                  .stream()
                  .anyMatch(m -> m.matches(relLocal));
                if (included && !excluded) {
                  files.add(relative);
                }
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });

    return files.stream().sorted().collect(Collectors.toList());
  }
}
