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
    Path stateFile = Paths.get(libDir, "state", cacheKey + ".installed");

    if (Files.exists(stateFile)) {
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
    Files.write(stateFile, new byte[0], StandardOpenOption.CREATE);
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
    linkInto(libraryModulesDir(libDir, destModulesDir), destModulesDir);
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
    Path libModulesDir =
      libraryModulesDir(libDir, destModulesDir).toRealPath();
    List<Path> links;
    // Links are not followed, so only links inside the modules directory,
    // which is resolved first as it may itself be a link
    try (Stream<Path> walk = Files.walk(destModulesDir.toRealPath())) {
      links = walk.filter(Files::isSymbolicLink).toList();
    }
    for (Path link : links) {
      // A relative link target is relative to the folder of the link
      Path target = link.resolveSibling(Files.readSymbolicLink(link));
      // getCanonicalFile resolves links, and paths that no longer exist
      if (target.toFile().getCanonicalFile().toPath()
          .startsWith(libModulesDir)) {
        Files.delete(link);
        System.out.println("Removed symlink: " + link);
      }
    }
  }

  /**
   * The modules directory of a library, after checking that it and the
   * modules directory to link it into exist.
   *
   * @param libDir An nf-core library initialised by nfcoreInitialise()
   * @param modulesDir Location to make the library available at
   * @return The absolute modules directory of the library
   * @throws IOException if either directory does not exist
   */
  private static Path libraryModulesDir(
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
    return libModulesDir;
  }

  /**
   * Link the directories of a library directory into a destination
   * directory, at the lowest level not already present there.
   *
   * @param libDir The source library directory
   * @param destDir The destination directory
   * @throws IOException If file operations fail, or if a folder already links
   *     to another directory
   */
  private static void linkInto(
      final Path libDir,
      final Path destDir) throws IOException {
    List<Path> subDirs;
    try (Stream<Path> entries = Files.list(libDir)) {
      subDirs = entries.filter(Files::isDirectory).toList();
    }
    for (Path subDir : subDirs) {
      Path dest = destDir.resolve(subDir.getFileName());
      // Never write through a link: it may point into another library
      if (Files.isSymbolicLink(dest)) {
        if (!Files.exists(dest) || !Files.isSameFile(dest, subDir)) {
          throw new IOException(
            "Cannot link " + subDir + ": " + dest + " already links to "
            + Files.readSymbolicLink(dest)
          );
        }
      } else if (!Files.exists(dest)) {
        Files.createSymbolicLink(dest, subDir);
      } else {
        linkInto(subDir, dest);
      }
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
