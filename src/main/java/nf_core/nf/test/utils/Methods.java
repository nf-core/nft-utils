package nf_core.nf.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.Yaml;

public class Methods {

  // Read a Version YAML file and return a Map of Map
  public static Map<String, Map<String, Object>> readYamlFile(String filePath) {
    Yaml yaml = new Yaml();
    try (FileReader reader = new FileReader(filePath)) {
      Map<String, Map<String, Object>> data = yaml.load(reader);
      return data;
    } catch (IOException e) {
      System.err.println("Error reading YAML file: " + e.getMessage());
      return null;
    }
  }

  // Helper method to resolve wildcard patterns to actual file paths
  private static List<String> resolveWildcardPaths(String pathPattern) throws IOException {
    // If no wildcard, return single item list
    if (!pathPattern.contains("*") && !pathPattern.contains("?")) {
      return Arrays.asList(pathPattern);
    }

    Path pattern = Paths.get(pathPattern);
    Path parent = pattern.getParent();
    String fileName = pattern.getFileName().toString();

    // If parent is null, use current directory
    if (parent == null) {
      parent = Paths.get(".");
    }

    // Check if parent directory exists
    if (!Files.exists(parent) || !Files.isDirectory(parent)) {
      throw new IOException("Parent directory does not exist: " + parent);
    }

    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + fileName);

    // Find matching files in the parent directory
    try {
      List<String> matchingFiles = Files.list(parent)
          .filter(Files::isRegularFile)
          .filter(path -> matcher.matches(path.getFileName()))
          .sorted()
          .map(path -> path.toAbsolutePath().toString())
          .collect(Collectors.toList());

      if (matchingFiles.isEmpty()) {
        throw new IOException("No files found matching pattern: " + pathPattern);
      }

      return matchingFiles;
    } catch (IOException e) {
      throw new IOException("Error resolving wildcard pattern " + pathPattern + ": " + e.getMessage());
    }
  }

  // Removed the Nextflow entry from the Workflow entry
  // within the input Version YAML file
  public static Map<String, Map<String, Object>> removeNextflowVersion(CharSequence versionFile) {
    return removeFromYamlMap(versionFile, "Workflow", "Nextflow");
  }

  // Removed the Key2 entry from the Key1 entry
  // within the input Version YAML file
  // If Key2 is null or empty, clears all content from Key1
  // Processes all files matching wildcard patterns and merges results
  public static Map<String, Map<String, Object>> removeFromYamlMap(CharSequence versionFile, String Key1, String Key2) {
    String yamlFilePattern = versionFile.toString();
    Map<String, Map<String, Object>> mergedResult = new TreeMap<>();

    try {
      // Resolve wildcard patterns if present - now returns all matching files
      List<String> yamlFilePaths = resolveWildcardPaths(yamlFilePattern);

      for (String yamlFilePath : yamlFilePaths) {
        Map<String, Map<String, Object>> yamlData = readYamlFile(yamlFilePath);

        if (yamlData != null) {
          // Process each file's data
          if (yamlData.containsKey(Key1)) {
            if (Key2 == null || Key2.isEmpty()) {
              // Remove the entire Key1 entry
              yamlData.remove(Key1);
            } else {
              // Remove only the specific Key2 from Key1
              yamlData.get(Key1).remove(Key2);
            }
          }

          // Merge the processed data into the result
          for (Map.Entry<String, Map<String, Object>> entry : yamlData.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> value = entry.getValue();

            if (mergedResult.containsKey(key)) {
              // If key already exists, merge the inner maps
              mergedResult.get(key).putAll(value);
            } else {
              // If key doesn't exist, add it (also sorted)
              mergedResult.put(key, new TreeMap<>(value));
            }
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Error resolving file path pattern: " + e.getMessage());
      return null;
    }

    return mergedResult;
  }

  // Overloaded method for clearing all content from Key1
  public static Map<String, Map<String, Object>> removeFromYamlMap(CharSequence versionFile, String Key1) {
    return removeFromYamlMap(versionFile, Key1, null);
  }

  // wrapper functions for getAllFilesFromDir with default options
  public static List getAllFilesFromDir(String path) throws IOException {
    return getAllFilesFromDir(new LinkedHashMap<String, Object>(), path);
  }

  // wrapper functions for getAllFilesFromDir with named options
  public static List getAllFilesFromDir(LinkedHashMap<String, Object> options, String outdir) throws IOException {
    if (outdir == null || outdir.isEmpty()) {
      throw new IllegalArgumentException("The 'outdir' parameter is required.");
    }
    // Check if path exists
    Path dirPath = Paths.get(outdir);
    if (!Files.exists(dirPath)) {
      throw new IllegalArgumentException("The specified path does not exist: " + outdir);
    }

    // Check if it's a directory
    if (!Files.isDirectory(dirPath)) {
      throw new IllegalArgumentException("The specified path is not a directory: " + outdir);
    }

    // Extract optional parameters from the map (use defaults if not provided)
    Boolean includeDir = (Boolean) options.getOrDefault("includeDir", false);
    List<String> ignoreGlobs = (List<String>) options.getOrDefault("ignore", new ArrayList<String>());
    String ignoreFilePath = (String) options.get("ignoreFile");
    Boolean relative = (Boolean) options.getOrDefault("relative", false);
    List<String> includeGlobs = (List<String>) options.getOrDefault("include", Arrays.asList("*", "**/*"));

    List<File> files = getAllFilesFromDir(outdir, includeDir, ignoreGlobs, ignoreFilePath, includeGlobs);

    if (relative) {
      return getRelativePath(files, outdir);
    } else {
      return files;
    }
  }

  // Return all files in a directory and its sub-directories
  // matching or not matching supplied glob
  public static List<File> getAllFilesFromDir(
      String outdir,
      boolean includeDir,
      List<String> ignoreGlobs,
      String ignoreFilePath,
      List<String> includeGlobs) throws IOException {
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
      excludeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
    }

    List<String> allIncludeGlobs = new ArrayList<>();
    if (includeGlobs != null) {
      allIncludeGlobs.addAll(includeGlobs);
    }

    List<PathMatcher> includeMatchers = new ArrayList<>();
    for (String glob : allIncludeGlobs) {
      includeMatchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
    }

    Files.walkFileTree(
        directory,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (isIncluded(file) && !isExcluded(file)) {
              output.add(file.toFile());
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            // Exclude output which is the root output folder from nf-test
            if (includeDir && (isIncluded(dir) && !isExcluded(dir)
                && !dir.getFileName().toString().equals("output"))) {
              output.add(dir.toFile());
            }
            return FileVisitResult.CONTINUE;
          }

          private boolean isExcluded(Path path) {
            return excludeMatchers.stream()
                .anyMatch(matcher -> matcher.matches(directory.relativize(path)));
          }

          private boolean isIncluded(Path path) {
            return includeMatchers.stream()
                .anyMatch(matcher -> matcher.matches(directory.relativize(path)));
          }
        });

    return output.stream().sorted(Comparator.comparing(File::getPath)).collect(Collectors.toList());
  }

  private static List<String> readGlobsFromFile(String filePath) throws IOException {
    List<String> globs = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
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

  public static List<String> getRelativePath(List<File> filePaths, String baseDir) {
    Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();

    return filePaths
        .stream()
        .map(filePath -> {
          Path path = Paths.get(filePath.toURI()).toAbsolutePath().normalize();
          return basePath.relativize(path).toString();
        })
        .collect(Collectors.toList());
  }

  public static String listToMD5(ArrayList<Object> input) throws UnsupportedEncodingException {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      Iterator<Object> inputIterator = input.iterator();
      while (inputIterator.hasNext()) {
        md5.update(inputIterator.next().toString().getBytes("UTF-8"));
      }
      byte[] digest = md5.digest();

      // Convert byte array to hex string
      StringBuilder hexString = new StringBuilder();
      for (byte b : digest) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 algorithm not available", e);
    }
  }

  /**
   * Creates the modules directory and .nf-core.yml configuration file
   *
   * @param libDir The directory path to initialise an nf-core library at
   */
  public static void nfcoreInitialise(String libDir) {
    NfCoreUtils.nfcoreInitialise(libDir);
  }

  /**
   * Installs nf-core modules from a list
   *
   * @param libDir  An nf-core library initialised by nfcoreInitialise()
   * @param modules List of module names (strings) or module maps with keys: name
   *                (required), sha (optional), remote (optional)
   */
  public static void nfcoreInstall(String libDir, List<?> modules) {
    NfCoreUtils.nfcoreInstall(libDir, modules);
  }

  /**
   * Creates a symbolic link from the installed nf-core modules to the base
   * directory
   *
   * @param libDir     An nf-core library initialised by nfcoreSetup()
   * @param modulesDir Location to make the library available at
   */
  public static void nfcoreLink(String libDir, String modulesDir) {
    NfCoreUtils.nfcoreLibraryLinker(libDir, modulesDir, "link");
  }

  /**
   * Remove all linked modules from a modules directory
   *
   * @param libDir     An nf-core library initialised by nfcoreSetup()
   * @param modulesDir Location to make the library available at
   */
  public static void nfcoreUnlink(String libDir, String modulesDir) {
    NfCoreUtils.nfcoreLibraryLinker(libDir, modulesDir, "unlink");
  }

  /**
   * Delete the temporary nf-core library
   *
   * @param libDir The library directory path to delete
   */
  public static void nfcoreDeleteLibrary(String libDir) {
    NfCoreUtils.nfcoreDeleteLibrary(libDir);
  }

  /**
   * Filters Nextflow stdout/stderr output to remove variable content that makes
   * snapshots unstable.
   * This method removes common patterns like timestamps, execution IDs, memory
   * usage, and other
   * runtime-specific information to make test snapshots reproducible.
   *
   * @param output The stdout or stderr output (String or List) to filter
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output) {
    return filterNextflowOutput(output, null, true, false);
  }

  /**
   * Filters Nextflow stdout/stderr output with optional sorting.
   *
   * @param output The stdout or stderr output (String or List) to filter
   * @param sorted Whether to sort the output lines alphabetically
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output, boolean sorted) {
    return filterNextflowOutput(output, null, sorted, false);
  }

  /**
   * Filters Nextflow stdout/stderr output with optional sorting and ANSI code
   * handling.
   *
   * @param output   The stdout or stderr output (String or List) to filter
   * @param sorted   Whether to sort the output lines alphabetically
   * @param keepAnsi Whether to keep ANSI escape codes (colors, formatting)
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output, boolean sorted, boolean keepAnsi) {
    return filterNextflowOutput(output, null, sorted, keepAnsi);
  }

  /**
   * Filters Nextflow stdout/stderr output with custom patterns, optional
   * sorting, and ANSI code handling.
   *
   * @param output             The stdout or stderr output (String or List) to
   *                           filter
   * @param additionalPatterns List of additional regex patterns to remove from
   *                           the output
   * @param sorted             Whether to sort the output lines alphabetically
   * @param keepAnsi           Whether to keep ANSI escape codes (colors,
   *                           formatting)
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output, List<String> additionalPatterns, boolean sorted,
      boolean keepAnsi) {
    if (output == null) {
      return new ArrayList<>();
    }

    List<String> outputLines;
    if (output instanceof List) {
      // Handle workflow.stdout and workflow.stderr which are Lists
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
      // Split string into lines
      outputLines = Arrays.asList(outputString.split("\n"));
    } else {
      // Convert any other type to string and split into lines
      String outputString = output.toString();
      outputLines = Arrays.asList(outputString.split("\n"));
    }

    // Filter each line
    List<String> filteredLines = new ArrayList<>();
    for (String line : outputLines) {
      String filtered = line;

      // Strip ANSI escape codes unless keepAnsi is true (colors, formatting, etc.)
      if (!keepAnsi) {
        filtered = filtered.replaceAll("\\x1B\\[[0-9;]*[A-Za-z]", "");
      }

      // Replace username value in patterns like "userName : max"
      String userName = System.getenv("USER");
      if (userName != null && !userName.isEmpty()) {
        filtered = filtered.replaceAll("(userName\\s*:\\s*)" + java.util.regex.Pattern.quote(userName), "$1[USER]");
      }

      // Remove timestamp patterns

      // ISO 8601 related formats:
      // YYY-MM-DDTHH:mm:ss
      // YYY-MM-DD HH:mm:ss
      // YYY-MM-DD_HH-mm-ss
      filtered = filtered.replaceAll(
          "\\d{4}-\\d{2}-\\d{2}[T\\s_]\\d{2}[:-]\\d{2}[:-]\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?",
          "[TIMESTAMP]");
      // US date format: MM/DD/YYY HH:mm:ss
      filtered = filtered.replaceAll("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}:\\d{2}", "[TIMESTAMP]");

      // Remove Nextflow process execution hashes (format: [xx/yyyyyy])
      filtered = filtered.replaceAll("\\[[0-9a-f]{2}/[0-9a-f]{6}\\]", "[NXF_HASH]");

      // Remove NFT_HASH work dir (format: [xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx])
      filtered = filtered.replaceAll("\\b[0-9a-f]{30,32}\\b", "[NFT_HASH]");

      // Remove revision hashes (format: revision: abc1234)
      filtered = filtered.replaceAll("revision: [0-9a-f]{10}", "revision: [REVISION]");

      // Remove Nextflow version update notifications
      filtered = filtered.replaceAll(".*Nextflow\\s+\\d+\\.\\d+\\.\\d+.*is available.*", "");
      filtered = filtered.replaceAll(".*Please consider updating your version.*", "");

      // Replace absolute paths with [PATH] placeholder using a more general approach
      filtered = filterAbsolutePaths(filtered);

      // Remove run name
      // List of all adjectives used by Nextflow
      String[] adjectives = {
          "admiring",
          "adoring",
          "agitated",
          "amazing",
          "angry",
          "astonishing",
          "awesome",
          "backstabbing",
          "berserk",
          "big",
          "boring",
          "chaotic",
          "cheeky",
          "cheesy",
          "clever",
          "compassionate",
          "condescending",
          "confident",
          "cranky",
          "crazy",
          "curious",
          "deadly",
          "desperate",
          "determined",
          "distracted",
          "distraught",
          "disturbed",
          "dreamy",
          "drunk",
          "ecstatic",
          "elated",
          "elegant",
          "evil",
          "exotic",
          "extravagant",
          "fabulous",
          "fervent",
          "festering",
          "focused",
          "friendly",
          "furious",
          "gigantic",
          "gloomy",
          "golden",
          "goofy",
          "grave",
          "happy",
          "high",
          "hopeful",
          "hungry",
          "infallible",
          "insane",
          "intergalactic",
          "irreverent",
          "jolly",
          "jovial",
          "kickass",
          "lethal",
          "lonely",
          "loving",
          "loquacious",
          "mad",
          "magical",
          "maniac",
          "marvelous",
          "mighty",
          "modest",
          "nasty",
          "naughty",
          "nauseous",
          "nice",
          "nostalgic",
          "peaceful",
          "pedantic",
          "pensive",
          "prickly",
          "reverent",
          "ridiculous",
          "romantic",
          "sad",
          "scruffy",
          "serene",
          "sharp",
          "shrivelled",
          "sick",
          "silly",
          "sleepy",
          "small",
          "soggy",
          "special",
          "spontaneous",
          "stoic",
          "stupefied",
          "suspicious",
          "tender",
          "thirsty",
          "tiny",
          "trusting",
          "voluminous",
          "wise",
          "zen"
      };

      String[] scientificNames = {
          "agnesi",
          "albattani",
          "allen",
          "almeida",
          "ampere",
          "angela",
          "archimedes",
          "ardinghelli",
          "aryabhata",
          "austin",
          "avogadro",
          "babbage",
          "baekeland",
          "banach",
          "bardeen",
          "bartik",
          "bassi",
          "becquerel",
          "bell",
          "bernard",
          "bhabha",
          "bhaskara",
          "blackwell",
          "bohr",
          "boltzmann",
          "booth",
          "borg",
          "bose",
          "boyd",
          "brahmagupta",
          "brattain",
          "brazil",
          "brenner",
          "brown",
          "cajal",
          "cantor",
          "caravaggio",
          "carlsson",
          "carson",
          "celsius",
          "chandrasekhar",
          "church",
          "colden",
          "cori",
          "coulomb",
          "cray",
          "crick",
          "curie",
          "curran",
          "curry",
          "cuvier",
          "dalembert",
          "darwin",
          "davinci",
          "descartes",
          "dijkstra",
          "dubinsky",
          "easley",
          "edison",
          "einstein",
          "ekeblad",
          "elion",
          "engelbart",
          "escher",
          "euclid",
          "euler",
          "faggin",
          "faraday",
          "fermat",
          "fermi",
          "feynman",
          "fourier",
          "franklin",
          "galileo",
          "gates",
          "gauss",
          "gautier",
          "gilbert",
          "goldberg",
          "goldstine",
          "goldwasser",
          "golick",
          "goodall",
          "gutenberg",
          "hamilton",
          "hawking",
          "heisenberg",
          "heyrovsky",
          "hilbert",
          "hirsch",
          "hodgkin",
          "hoover",
          "hopper",
          "hugle",
          "hypatia",
          "jang",
          "jennings",
          "jepsen",
          "joliot",
          "jones",
          "kalam",
          "kalman",
          "kare",
          "kay",
          "keller",
          "khorana",
          "kilby",
          "kimura",
          "kirch",
          "knuth",
          "koch",
          "kowalevski",
          "lagrange",
          "lalande",
          "lamarck",
          "lamarr",
          "lamport",
          "laplace",
          "lattes",
          "lavoisier",
          "leakey",
          "leavitt",
          "legentil",
          "leibniz",
          "lichterman",
          "linnaeus",
          "liskov",
          "lorenz",
          "lovelace",
          "lumiere",
          "magritte",
          "mahavira",
          "majorana",
          "mandelbrot",
          "marconi",
          "maxwell",
          "mayer",
          "mccarthy",
          "mcclintock",
          "mclean",
          "mcnulty",
          "meitner",
          "mendel",
          "meninsky",
          "mercator",
          "mestorf",
          "meucci",
          "miescher",
          "minsky",
          "mirzakhani",
          "monod",
          "montalcini",
          "moriondo",
          "morse",
          "murdock",
          "neumann",
          "newton",
          "nightingale",
          "nobel",
          "noether",
          "northcutt",
          "noyce",
          "ochoa",
          "panini",
          "pare",
          "pasteur",
          "pauling",
          "payne",
          "perlman",
          "pesquet",
          "picasso",
          "pike",
          "planck",
          "plateau",
          "poincare",
          "poisson",
          "poitras",
          "ptolemy",
          "raman",
          "ramanujan",
          "ride",
          "ritchie",
          "roentgen",
          "rosalind",
          "rubens",
          "rutherford",
          "saha",
          "salas",
          "sammet",
          "sanger",
          "sax",
          "shannon",
          "shaw",
          "shirley",
          "shockley",
          "sinoussi",
          "snyder",
          "solvay",
          "spence",
          "stallman",
          "stone",
          "stonebraker",
          "swanson",
          "swartz",
          "swirles",
          "tesla",
          "thompson",
          "torricelli",
          "torvalds",
          "tuckerman",
          "turing",
          "varahamihira",
          "venter",
          "visvesvaraya",
          "volhard",
          "volta",
          "waddington",
          "watson",
          "wegener",
          "wescoff",
          "wiles",
          "williams",
          "wilson",
          "wing",
          "woese",
          "wozniak",
          "wright",
          "yalow",
          "yonath"
      };

      // Remove Nextflow run names using specific adjective_scientificname patterns
      // Use more efficient approach with Set lookups instead of large regex
      // alternations

      // First, match bracketed run names: [adjective_scientificname]
      filtered = java.util.regex.Pattern.compile("\\[([a-z]+)_([a-z]+)\\]")
          .matcher(filtered)
          .replaceAll(matchResult -> {
            String adjective = matchResult.group(1);
            String scientificName = matchResult.group(2);
            // Check if both parts are in our known lists
            if (java.util.Arrays.asList(adjectives).contains(adjective) &&
                java.util.Arrays.asList(scientificNames).contains(scientificName)) {
              return "[RUN_NAME]";
            }
            return matchResult.group(0); // Return original if not a match
          });

      // Then, match unbracketed run names: adjective_scientificname
      filtered = java.util.regex.Pattern.compile("\\b([a-z]+)_([a-z]+)\\b")
          .matcher(filtered)
          .replaceAll(matchResult -> {
            String adjective = matchResult.group(1);
            String scientificName = matchResult.group(2);
            // Check if both parts are in our known lists
            if (java.util.Arrays.asList(adjectives).contains(adjective) &&
                java.util.Arrays.asList(scientificNames).contains(scientificName)) {
              return "[RUN_NAME]";
            }
            return matchResult.group(0); // Return original if not a match
          });

      // Replace nf-core pipeline versions (e.g., "nf-core/xxx yyyy")
      filtered = filtered.replaceAll("(nf-core/[^\\s]+\\s+)\\d+\\.\\d+(?:\\.\\d+)?[a-zA-Z]*", "$1[VERSION]");

      // Replace NEXTFLOW versions
      filtered = filtered.replaceAll("N E X T F L O W  ~  version \\d+\\.\\d+\\.\\d+",
          "N E X T F L O W  ~  version [VERSION]");

      // Only add non-empty lines (filter out empty lines)
      if (!filtered.trim().isEmpty()) {
        filteredLines.add(filtered);
      }
    }

    // Sort and remove duplicates if requested
    if (sorted) {
      // Separate lines that should be sorted from those that should preserve order
      List<String> sortableLines = new ArrayList<>();
      List<String> preserveOrderLines = new ArrayList<>();

      for (String line : filteredLines) {
        if (line.contains("Staging foreign file") ||
            line.contains("Submitted process") ||
            line.startsWith("Creating env using conda:") ||
            line.startsWith("Pulling Singularity image") ||
            line.startsWith("ERROR ~") ||
            line.startsWith("WARN:") ||
            (line.contains("Check ") && line.contains(" file for details"))) {
          sortableLines.add(line);
        } else {
          preserveOrderLines.add(line);
        }
      }

      // Sort only the sortable lines
      Collections.sort(sortableLines);

      // Combine lists: preserve-order lines first, then sorted lines
      List<String> combinedLines = new ArrayList<>();
      combinedLines.addAll(preserveOrderLines);
      combinedLines.addAll(sortableLines);

      // Remove duplicates while preserving the new order
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
   * Filters Nextflow stdout/stderr output with custom patterns.
   * This overloaded method allows specifying additional patterns to filter.
   *
   * @param output             The stdout or stderr output (String or List) to
   *                           filter
   * @param additionalPatterns List of additional regex patterns to remove from
   *                           the output
   * @return The filtered output as a List<String> with unstable patterns removed
   */
  public static List<String> filterNextflowOutput(Object output, List<String> additionalPatterns) {
    return filterNextflowOutput(output, additionalPatterns, true, false);
  }

  /**
   * Filters Nextflow stdout/stderr output using Groovy's named parameter syntax.
   * This allows calling: filterNextflowOutput(output, sorted: false, keepAnsi:
   * true)
   *
   * @param output  The stdout or stderr output (String or List) to filter
   * @param options Map containing filtering options (automatically created by
   *                Groovy named params):
   *                - additionalPatterns: List<String> of additional regex
   *                patterns (optional)
   *                - sorted: Boolean whether to sort the output (default: true)
   *                - keepAnsi: Boolean whether to keep ANSI codes (default:
   *                false)
   * @return The filtered output as a List<String> with unstable patterns removed
   */

  // Handle Groovy named parameters: filterNextflowOutput(output, keepAnsi:
  // true)
  // Groovy converts this to: filterNextflowOutput([keepAnsi: true], output)
  public static List<String> filterNextflowOutput(LinkedHashMap<String, Object> options, Object output) {
    if (options == null) {
      options = new LinkedHashMap<>();
    }

    // Extract options with defaults
    List<String> additionalPatterns = (List<String>) options.get("additionalPatterns");
    Boolean sorted = (Boolean) options.get("sorted");
    Boolean keepAnsi = (Boolean) options.get("keepAnsi");

    // Apply defaults
    if (sorted == null)
      sorted = true;
    if (keepAnsi == null)
      keepAnsi = false;

    return filterNextflowOutput(output, additionalPatterns, sorted, keepAnsi);
  }

  public static List<String> filterNextflowOutput(Object output, Map<String, Object> options) {
    if (options == null) {
      options = new HashMap<>();
    }

    // Extract options with defaults
    List<String> additionalPatterns = (List<String>) options.get("additionalPatterns");
    Boolean sorted = (Boolean) options.get("sorted");
    Boolean keepAnsi = (Boolean) options.get("keepAnsi");

    // Apply defaults
    if (sorted == null)
      sorted = true;
    if (keepAnsi == null)
      keepAnsi = false;

    return filterNextflowOutput(output, additionalPatterns, sorted, keepAnsi);
  }

  /**
   * Filters absolute paths in the given text and replaces them with [PATH]
   * placeholder.
   *
   * @param text The text to filter
   * @return The filtered text with various directory paths replaced with [PATH]
   */
  private static String filterAbsolutePaths(String text) {
    String filtered = text;

    // Collect all paths to replace, then sort by length (longest first)
    // This ensures more specific paths are replaced before their parent paths
    List<String> pathsToReplace = new ArrayList<>();

    // Get the current working directory
    String workingDir = System.getProperty("user.dir");
    if (workingDir != null) {
      pathsToReplace.add(workingDir);
    }

    // Check for various environment variables
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
      if (envValue != null && !envValue.isEmpty()) {
        pathsToReplace.add(envValue);
      }
    }

    // Handle default NXF_HOME case: if NXF_HOME is null, Nextflow uses
    // $HOME/.nextflow
    String nxfHome = System.getenv("NXF_HOME");
    if (nxfHome == null || nxfHome.isEmpty()) {
      String home = System.getenv("HOME");
      if (home != null && !home.isEmpty()) {
        pathsToReplace.add(home + "/.nextflow");
      }
    }

    // Remove duplicates and sort paths by length to avoid partial replacements
    pathsToReplace = pathsToReplace.stream()
        .distinct()
        .sorted((a, b) -> Integer.compare(b.length(), a.length()))
        .collect(java.util.stream.Collectors.toList());

    // Replace all paths with [PATH] in order of longest first
    for (String path : pathsToReplace) {
      filtered = filtered.replace(path, "[PATH]");
    }

    return filtered;
  }
}
