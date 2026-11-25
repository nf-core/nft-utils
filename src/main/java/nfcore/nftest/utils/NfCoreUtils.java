package nfcore.nftest.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Utility methods for interacting with nf-core pipelines and modules.
 */
public final class NfCoreUtils {

  /**
   * Prevents instantiation of this utility class.
   */
  private NfCoreUtils() {
  }

  /**
   * Sets up a temporary nf-core library directory structure.
   * @param libDir The directory path where the nf-core library should
   * be created
   * @throws IOException if the library cannot be created
   */
  public static void nfcoreInitialise(final String libDir) throws IOException {
    System.out.println();
    System.out.println("Creating a temporary nf-core library at " + libDir);

    // state/ tracks the installed modules
    for (String dir : List.of("modules", "conf", "state")) {
      Files.createDirectories(Paths.get(libDir, dir));
    }

    Files.writeString(
      Paths.get(libDir, ".nf-core.yml"),
      "repository_type: \"pipeline\"\ntemplate:\n    name: test\n"
    );
  }

  /**
   * Installs nf-core modules from a list.
   *
   * @param libDir An nf-core library initialised by nfcoreSetup()
   * @param modules List of module names (strings) or module maps
   * with keys: name (required), sha (optional), remote (optional)
   * @throws IOException if a module fails to install
   */
  public static void nfcoreInstall(
      final String libDir,
      final List<?> modules)
      throws IOException {
    System.out.println("Installing nf-core modules...");

    if (modules == null || modules.isEmpty()) {
      throw new IllegalArgumentException(
        "Modules list not provided or is empty!"
      );
    }

    for (Object moduleObj : modules) {
      String name;
      String sha = null;
      String remote = null;
      // toString() turns Groovy GStrings into Strings
      if (moduleObj instanceof CharSequence) {
        name = moduleObj.toString();
      } else if (moduleObj instanceof Map) {
        Options module = new Options("nfcoreInstall", "module key",
          (Map<?, ?>) moduleObj, "name", "sha", "remote");
        name = module.string("name", null);
        sha = module.string("sha", null);
        remote = module.string("remote", null);

        if (name == null || name.isEmpty()) {
          throw new IllegalArgumentException("Module name is required");
        }
      } else {
        throw new IllegalArgumentException(
          "Unsupported module type: "
          + moduleObj.getClass().getSimpleName()
          + ". Expected String or Map."
        );
      }

      installModule(libDir, name, sha, remote);
    }
  }

  /**
   * Private helper method to install a single nf-core module.
   *
   * @param libDir The library directory
   * @param name The module name (required)
   * @param sha The SHA hash (optional)
   * @param remote The remote repository (optional)
   * @throws IOException if the install command fails or is interrupted, or
   *     the state file cannot be written
   */
  private static void installModule(
      final String libDir,
      final String name,
      final String sha,
      final String remote
    ) throws IOException {
    String cacheKey = createModuleCacheKey(name, sha, remote);
    File stateFile = new File(
      libDir + "/state/" + cacheKey + ".installed");

    if (stateFile.exists()) {
      System.out.println(
        "Module already installed (cached): " + name);
      return;
    }

    // Arguments, not a shell line: paths and names with spaces or shell
    // characters reach nf-core unchanged
    List<String> command = new ArrayList<>(
      List.of("nf-core", "--verbose", "modules")
    );
    if (remote != null && !remote.isEmpty()) {
      command.addAll(List.of("--git-remote", remote));
    }
    command.addAll(List.of("install", name));
    if (sha != null && !sha.isEmpty()) {
      command.addAll(List.of("--sha", sha));
    }

    Utils.runOrThrow(
      new ProcessBuilder(command).directory(new File(libDir)),
      "Failed to install module " + name
      + " with: " + String.join(" ", command)
    );
    System.out.println("Successfully installed module: " + name);
    Files.write(stateFile.toPath(), new byte[0], StandardOpenOption.CREATE);
  }

  /**
   * Create a cache key for a module based on its parameters.
   *
   * @param name The module name
   * @param sha The SHA hash (optional)
   * @param remote The remote repository (optional)
   * @return A hashed cache key string
   */
  private static String createModuleCacheKey(
      final String name,
      final String sha,
      final String remote) {
    StringBuilder key = new StringBuilder(name);
    if (sha != null && !sha.isEmpty()) {
      key.append("_").append(sha);
    }
    if (remote != null && !remote.isEmpty()) {
      key.append("_").append(remote);
    }

    return HashUtils.md5Hex(key.toString());
  }

  /**
   * Link a library's modules into a modules directory at the lowest possible
   * position.
   *
   * e.g. if `modules/nf-core` doesn't exist, link it
   * but if it does, link the tool directory inside it.
   *
   * @param libDir An nf-core library initialised by nfcoreInitialise()
   * @param modulesDir Location to make the library available at
   * @throws IOException if either directory does not exist or a link cannot
   *     be created
   */
  public static void nfcoreLink(
      final String libDir,
      final String modulesDir) throws IOException {
    Path destModulesDir = Paths.get(modulesDir);
    for (Path orgDir : libraryOrgDirs(libDir, destModulesDir)) {
      recurseLink(orgDir, destModulesDir);
    }
  }

  /**
   * Remove the links to a library from a modules directory.
   *
   * @param libDir An nf-core library initialised by nfcoreInitialise()
   * @param modulesDir Location the library was made available at
   * @throws IOException if either directory does not exist or a link cannot
   *     be read or removed
   */
  public static void nfcoreUnlink(
      final String libDir,
      final String modulesDir) throws IOException {
    Path destModulesDir = Paths.get(modulesDir);
    for (Path orgDir : libraryOrgDirs(libDir, destModulesDir)) {
      recurseUnlink(orgDir, destModulesDir);
    }
  }

  /**
   * List the organisation directories (e.g. nf-core) of a library, after
   * checking that the library and the modules directory exist.
   *
   * @param libDir An nf-core library initialised by nfcoreInitialise()
   * @param modulesDir Location to make the library available at
   * @return The organisation directories of the library
   * @throws IOException if either directory does not exist or cannot be listed
   */
  private static List<Path> libraryOrgDirs(
      final String libDir,
      final Path modulesDir) throws IOException {
    // Absolute, so links to it resolve from any directory
    Path libModulesDir = Paths.get(libDir, "modules").toAbsolutePath();
    if (!Files.isDirectory(libModulesDir)) {
      throw new IOException(
        "Library modules directory does not exist: " + libModulesDir
      );
    }
    if (!Files.exists(modulesDir)) {
      throw new IOException(
        "Modules directory does not exist: " + modulesDir.toAbsolutePath()
      );
    }
    return subDirectories(libModulesDir);
  }

  /**
   * List the directories inside a directory.
   *
   * @param dir The directory
   * @return Its sub-directories
   * @throws IOException if the directory cannot be listed
   */
  private static List<Path> subDirectories(final Path dir)
      throws IOException {
    try (Stream<Path> entries = Files.list(dir)) {
      return entries.filter(Files::isDirectory).toList();
    }
  }

  /**
   * Recursively iterate through a library and a target module directory
   * and link files at the lowest-available directory level.
   *
   * @param libDir The source library directory
   * @param destDir The destination directory
   * @throws IOException If file operations fail, or if a folder already links
   *     to another directory
   */
  private static void recurseLink(
      final Path libDir,
      final Path destDir) throws IOException {
    Path dest = destDir.resolve(libDir.getFileName());

    // Never write through a link: it may point into another library
    if (Files.isSymbolicLink(dest)) {
      if (!Files.exists(dest) || !Files.isSameFile(dest, libDir)) {
        throw new IOException(
          "Cannot link " + libDir + ": " + dest + " already links to "
          + Files.readSymbolicLink(dest)
        );
      }
      return;
    }
    if (!Files.exists(dest)) {
      Files.createSymbolicLink(dest, libDir);
      return;
    }
    for (Path subDir : subDirectories(libDir)) {
      recurseLink(subDir, dest);
    }
  }

  /**
   * Recursively iterate through a target module directory and remove symlinks
   * that point to anywhere within the library directory.
   *
   * @param libDir The source library directory
   * @param destDir The destination directory to traverse
   * @throws IOException If file operations fail
   */
  private static void recurseUnlink(
      final Path libDir,
      final Path destDir) throws IOException {
    if (!Files.isDirectory(destDir)) {
      return;
    }
    List<Path> files;
    try (Stream<Path> entries = Files.list(destDir)) {
      files = entries.toList();
    }
    for (Path file : files) {
      if (Files.isSymbolicLink(file)) {
        // A relative link target is relative to the folder of the link
        Path target = destDir.resolve(Files.readSymbolicLink(file));
        if (isWithinDirectory(target, libDir)) {
          Files.delete(file);
          System.out.println("Removed symlink: " + file.toAbsolutePath());
        }
      } else if (Files.isDirectory(file)) {
        recurseUnlink(libDir, file);
      }
    }
  }

  /**
   * Helper method to check if a path is within a given directory.
   *
   * @param path The path to check
   * @param directory The directory to check against
   * @return true if the path is within the directory, false otherwise
   */
  private static boolean isWithinDirectory(
      final Path path,
      final Path directory) {
    try {
      // getCanonicalFile resolves links, and paths that no longer exist
      return path.toFile().getCanonicalFile().toPath()
        .startsWith(directory.toFile().getCanonicalFile().toPath());
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Delete the temporary nf-core library.
   *
   * @param libDir The library directory path to delete
   * @throws IOException if a file in the library cannot be deleted
   */
  public static void nfcoreDeleteLibrary(final String libDir)
      throws IOException {
    System.out.println("Deleting temporary nf-core library: " + libDir);
    Utils.deleteDirectory(new File(libDir));
  }

}
