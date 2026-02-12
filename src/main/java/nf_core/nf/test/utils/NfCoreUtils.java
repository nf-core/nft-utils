package nf_core.nf.test.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NfCoreUtils {

  /**
   * Sets up a temporary nf-core library directory structure
   * @param libDir The directory path where the nf-core library should be created
   */
  public static void nfcoreInitialise(String libDir) {
    System.out.println("\n");
    System.out.println("Creating a temporary nf-core library at " + libDir);
    try {
      // Create modules directory
      File modulesDir = new File(libDir + "/modules");
      modulesDir.mkdirs();

      // Create state directory for tracking installed modules
      File stateDir = new File(libDir + "/state");
      stateDir.mkdirs();

      // Create .nf-core.yml file
      File nfcoreYml = new File(libDir + "/.nf-core.yml");
      try (FileWriter writer = new FileWriter(nfcoreYml)) {
        writer.write("repository_type: \"pipeline\"\n");
        writer.write("template:\n");
        writer.write("    name: test\n");
      }
    } catch (IOException e) {
      System.err.println("Error setting up nf-core: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Installs nf-core modules from a list
   * @param libDir An nf-core library initialised by nfcoreSetup()
   * @param modules List of module names (strings) or module maps with keys: name (required), sha (optional), remote (optional)
   */
  @SuppressWarnings("unchecked")
  public static void nfcoreInstall(String libDir, List<?> modules) {
    System.out.println("Installing nf-core modules...");

    if (modules == null || modules.isEmpty()) {
      throw new IllegalArgumentException("Modules list not provided or is empty!");
    }

    for (Object moduleObj : modules) {
      if (moduleObj instanceof String) {
        installModule(libDir, (String) moduleObj, null, null);
      } else if (moduleObj instanceof LinkedHashMap || moduleObj instanceof Map) {
        Map<String, String> moduleMap = (Map<String, String>) moduleObj;
        String name = moduleMap.get("name");
        String sha = moduleMap.get("sha");
        String remote = moduleMap.get("remote");

        if (name == null || name.isEmpty()) {
          throw new IllegalArgumentException("Module name is required");
        }

        installModule(libDir, name, sha, remote);
      } else {
        throw new RuntimeException(
          "Unsupported module type: " + moduleObj.getClass().getSimpleName() + ". Expected String or Map."
        );
      }
    }
  }

  /**
   * Private helper method to install a single nf-core module
   * @param libDir The library directory
   * @param name The module name (required)
   * @param sha The SHA hash (optional)
   * @param remote The remote repository (optional)
   */
  private static void installModule(String libDir, String name, String sha, String remote) {
    try {
      // Create a cache key based on module parameters
      String cacheKey = createModuleCacheKey(name, sha, remote);
      File stateFile = new File(libDir + "/state/" + cacheKey + ".installed");

      // Check if module is already installed
      if (stateFile.exists()) {
        System.out.println("Module already installed (cached): " + name);
        return;
      }

      StringBuilder command = new StringBuilder("cd " + libDir + " && nf-core --verbose modules");

      if (remote != null && !remote.isEmpty()) {
        command.append(" --git-remote ").append(remote);
      }

      command.append(" install ").append(name);

      if (sha != null && !sha.isEmpty()) {
        command.append(" --sha ").append(sha);
      }

      ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command.toString());
      Utils.ProcessResult result = Utils.runProcess(processBuilder);

      // Spit out nf-core tools stderr if install fails
      if (result.exitCode != 0) {
        System.err.println("Error installing module " + name + ": exit code " + result.exitCode + "\n");
        System.out.println("Installation command: \n" + command.toString());
        System.err.println("nf-core tools output: \n");
        System.err.println(result.stderr);
      } else {
        System.out.println("Successfully installed module: " + name);
        // Write state file to mark module as installed
        writeModuleStateFile(stateFile);
      }
    } catch (IOException | InterruptedException e) {
      System.err.println("Error installing module " + name + ": " + e.getMessage());
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Create a cache key for a module based on its parameters
   * @param name The module name
   * @param sha The SHA hash (optional)
   * @param remote The remote repository (optional)
   * @return A hashed cache key string
   */
  private static String createModuleCacheKey(String name, String sha, String remote) {
    StringBuilder key = new StringBuilder(name);
    if (sha != null && !sha.isEmpty()) {
      key.append("_").append(sha);
    }
    if (remote != null && !remote.isEmpty()) {
      key.append("_").append(remote);
    }

    try {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
      byte[] messageDigest = md.digest(key.toString().getBytes());
      StringBuilder hexString = new StringBuilder();
      for (byte b : messageDigest) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 algorithm not available on this system", e);
    }
  }

  /**
   * Write a state file to mark a module as installed
   * @param stateFile The state file to create
   */
  private static void writeModuleStateFile(File stateFile) {
    try {
      if (!stateFile.exists()) {
        stateFile.createNewFile();
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not write module state file", e);
    }
  }

  /**
   * Traverse through a modules directory and link modules at the lowest possible position
   * e.g. if `modules/nf-core` doesn't exist, link it
   * but if it does, link the tool directory inside it
   * @param libDir An nf-core library initialised by nfcoreSetup()
   * @param modulesDir Location to make the library available at
   */
  public static void nfcoreLibraryLinker(String libDir, String modulesDir, String mode) {
    try {
      File libModulesDir = new File(libDir + "/modules");
      File destModulesDir = new File(modulesDir);

      // Capitalise mode string for error messages
      String CapMode = mode.substring(0, 1).toUpperCase() + mode.substring(1);

      if (!libModulesDir.exists() || !libModulesDir.isDirectory()) {
        System.err.println("Warning: Library modules directory does not exist: " + libModulesDir.getAbsolutePath());
        System.err.println(CapMode + "ing halted!");
        return;
      }

      if (!destModulesDir.exists()) {
        System.err.println("Warning: Modules directory does not exist: " + destModulesDir.getAbsolutePath());
        System.err.println(CapMode + "ing halted!");
        return;
      }

      // Starting at the organisation-dir (e.g. nf-core) - link it if it doesn't exist, otherwise
      // go a step deeper and link everything inside it
      for (File orgDir : libModulesDir.listFiles()) {
        if (orgDir.isDirectory()) {
          if ("link".equals(mode)) {
            recurseLink(orgDir, destModulesDir);
          } else if ("unlink".equals(mode)) {
            recurseUnlink(orgDir, destModulesDir);
          } else {
            throw new RuntimeException("Error: mode is not 'link' or 'unlink'!");
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Error creating symlinks: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Recursively iterate through a library and a target module directory and link
   * files at the lowest-available directory level
   * @param libDir The source library directory
   * @param destDir The destination directory
   * @throws IOException If file operations fail
   */
  private static void recurseLink(File libDir, File destDir) throws IOException {
    String itemName = libDir.getName();
    File destItem = new File(destDir, itemName);

    if (!destItem.exists()) {
      Files.createSymbolicLink(destItem.toPath(), libDir.toPath());
      return;
    }

    for (File subDir : libDir.listFiles()) {
      if (subDir.isDirectory()) {
        recurseLink(subDir, destItem);
      }
    }
  }

  /**
   * Recursively iterate through a target module directory and remove symlinks
   * that point to anywhere within the library directory
   * @param libDir The source library directory
   * @param destDir The destination directory to traverse
   * @throws IOException If file operations fail
   */
  private static void recurseUnlink(File libDir, File destDir) throws IOException {
    if (!destDir.exists() || !destDir.isDirectory()) {
      return;
    }

    File[] files = destDir.listFiles();
    if (files == null) {
      return;
    }

    for (File file : files) {
      if (Files.isSymbolicLink(file.toPath())) {
        try {
          File linkTarget = Files.readSymbolicLink(file.toPath()).toFile();
          if (!linkTarget.isAbsolute()) {
            linkTarget = new File(file.getParentFile(), linkTarget.getPath()).getCanonicalFile();
          }

          if (isWithinDirectory(linkTarget, libDir)) {
            Files.delete(file.toPath());
            System.out.println("Removed symlink: " + file.getAbsolutePath());
          }
        } catch (IOException e) {
          System.err.println("Warning: Could not read symlink target for " + file.getAbsolutePath());
        }
      } else if (file.isDirectory()) {
        recurseUnlink(libDir, file);
      }
    }
  }

  /**
   * Helper method to check if a file is within a given directory
   * @param file The file to check
   * @param directory The directory to check against
   * @return true if the file is within the directory, false otherwise
   */
  private static boolean isWithinDirectory(File file, File directory) {
    try {
      String filePath = file.getCanonicalPath();
      String dirPath = directory.getCanonicalPath();
      return filePath.startsWith(dirPath);
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Delete the temporary nf-core library
   * @param libDir The library directory path to delete
   */
  public static void nfcoreDeleteLibrary(String libDir) {
    System.out.println("Deleting temporary nf-core library: " + libDir);

    try {
      // Delete modules directory
      File modulesLibDir = new File(libDir);
      deleteDirectory(modulesLibDir);
    } catch (Exception e) {
      System.err.println("Error during cleanup: " + e.getMessage());
    }
  }

  /**
   * Helper method to recursively delete a directory and all its contents
   * @param directory The directory to delete
   */
  private static void deleteDirectory(File directory) {
    if (directory.exists()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            deleteDirectory(file);
          } else {
            file.delete();
          }
        }
      }
      directory.delete();
    }
  }
}
