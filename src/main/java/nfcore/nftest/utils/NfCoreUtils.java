package nfcore.nftest.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    System.out.println("\n");
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
  @SuppressWarnings("unchecked")
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
      if (moduleObj instanceof String) {
        name = (String) moduleObj;
      } else if (moduleObj instanceof Map) {
        Map<?, ?> moduleMap = (Map<?, ?>) moduleObj;
        name = (String) moduleMap.get("name");
        sha = (String) moduleMap.get("sha");
        remote = (String) moduleMap.get("remote");

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
    File destModulesDir = new File(modulesDir);
    for (File orgDir : libraryOrgDirs(libDir, destModulesDir)) {
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
    File destModulesDir = new File(modulesDir);
    for (File orgDir : libraryOrgDirs(libDir, destModulesDir)) {
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
  private static List<File> libraryOrgDirs(
      final String libDir,
      final File modulesDir) throws IOException {
    File libModulesDir = new File(libDir, "modules");
    if (!libModulesDir.isDirectory()) {
      throw new IOException(
        "Library modules directory does not exist: "
        + libModulesDir.getAbsolutePath()
      );
    }
    if (!modulesDir.exists()) {
      throw new IOException(
        "Modules directory does not exist: "
        + modulesDir.getAbsolutePath()
      );
    }
    File[] entries = libModulesDir.listFiles();
    if (entries == null) {
      throw new IOException(
        "Unable to list library modules directory: "
        + libModulesDir.getAbsolutePath()
      );
    }
    return Arrays.stream(entries).filter(File::isDirectory).toList();
  }

  /**
   * Recursively iterate through a library and a target module directory
   * and link files at the lowest-available directory level.
   *
   * @param libDir The source library directory
   * @param destDir The destination directory
   * @throws IOException If file operations fail
   */
  private static void recurseLink(
      final File libDir,
      final File destDir) throws IOException {
    String itemName = libDir.getName();
    File destItem = new File(destDir, itemName);

    if (!destItem.exists()) {
      Files.createSymbolicLink(destItem.toPath(), libDir.toPath());
      return;
    }
    File[] subDirs = libDir.listFiles();
    if (subDirs == null) {
      throw new IOException(
        "Unable to list directory: " + libDir.getAbsolutePath()
      );
    }
    for (File subDir : subDirs) {
      if (subDir.isDirectory()) {
        recurseLink(subDir, destItem);
      }
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
      final File libDir,
      final File destDir) throws IOException {
    if (!destDir.exists() || !destDir.isDirectory()) {
      return;
    }

    File[] files = destDir.listFiles();
    if (files == null) {
      return;
    }

    for (File file : files) {
      if (Files.isSymbolicLink(file.toPath())) {
        File linkTarget = Files.readSymbolicLink(file.toPath()).toFile();
        if (!linkTarget.isAbsolute()) {
          linkTarget = new File(file.getParentFile(), linkTarget.getPath())
            .getCanonicalFile();
        }

        if (isWithinDirectory(linkTarget, libDir)) {
          Files.delete(file.toPath());
          System.out.println("Removed symlink: " + file.getAbsolutePath());
        }
      } else if (file.isDirectory()) {
        recurseUnlink(libDir, file);
      }
    }
  }

  /**
   * Helper method to check if a file is within a given directory.
   *
   * @param file The file to check
   * @param directory The directory to check against
   * @return true if the file is within the directory, false
   * otherwise
   */
  private static boolean isWithinDirectory(
      final File file,
      final File directory) {
    try {
      String filePath = file.getCanonicalPath();
      String dirPath = directory.getCanonicalPath();
      return filePath.startsWith(dirPath);
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
