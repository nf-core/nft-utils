package nfcore.nftest.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * Utility methods for interacting with CSV files.
 */
public final class CsvUtils {

  /** Default number of decimal places for CSV double values. */
  public static final int DEFAULT_DOUBLE_DIGITS = 6;

  /**
   * Prevents instantiation of this utility class.
   */
  private CsvUtils() {
  }

  /**
   * Calculates the normalized MD5 for CSV/TSV/TXT files recursively.
   *
   * @param value value containing CSV/TSV/TXT files
   * @param digits the number of decimal places to retain for floating-point
   * values
   * @return value with normalized MD5 replacements
   */
  public static Object csvMD5(final Object value, final int digits) {
    return OutputSanitizer.recursiveParse(value, strValue -> {
      final Path path = Paths.get(strValue);

      if (!Files.exists(path)) {
        return strValue;
      }

      final String extension = Utils.getExtension(path, false);

      if (
        !"csv".equals(extension)
        && !"tsv".equals(extension)
        && !"txt".equals(extension)
      ) {
        return strValue;
      }

      return path.getFileName().toString()
        + ":md5NormedCsv,"
        + getCsvMD5(path, digits);
    });
  }


  /**
   * Normalizes a table by sorting columns, normalizing values, and sorting
   * rows.
   *
   * @param table the table to normalize
   * @param digits the number of decimal places to retain for floating-point
   * values
   * @return the normalized table
   */
  static CsvTable normalizeTable(final CsvTable table, final int digits) {
    CsvTable tableNormed = normalizeColumns(table);
    tableNormed = normalizeValues(tableNormed, digits);
    tableNormed = normalizeRows(tableNormed);
    return tableNormed;
  }

  /**
   * Normalizes a delimited text file and returns its canonical
   * CSV representation.
   *
   * The file is read with automatic separator detection, normalized by
   * sorting columns and rows, normalizing values, and simplifying absolute
   * paths, then serialized as deterministic CSV.
   *
   * @param path path to the CSV, TSV, or semicolon-separated text file
   * @param digits the number of decimal places to retain for floating-point
   * values
   * @return the normalized CSV representation of the file
   * @throws RuntimeException if the file cannot be read, normalized, or
   * rendered
   */
  public static String normalizeCsv(final Path path, final int digits) {
    try {
      CsvTable table = readTable(path);
      table = normalizeTable(table, digits);
      return toCanonicalCsv(table);
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to normalize delimited file: " + path,
        e
      );
    }
  }

  /**
   * Calculates an MD5 from a canonical representation of the CSV.
   *
   * @param path CSV/TSV/TXT file
   * @param digits the number of decimal places to retain for floating-point
   * values
   * @return normalized MD5
   */
  private static String getCsvMD5(final Path path, final int digits) {
    try {
      return md5(normalizeCsv(path, digits));
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to calculate normalized CSV MD5 for file: " + path,
        e
      );
    }
  }

  /**
   * Reads a delimited text file using Apache Commons CSV.
   *
   * The separator is detected automatically from the file contents,
   * allowing CSV, TSV, and semicolon-separated files to be read regardless
   * of their file extension.
   *
   * @param path path to the delimited text file
   * @return the parsed table
   * @throws IOException if the file cannot be read or parsed
   */
  private static CsvTable readTable(final Path path)
    throws IOException {

    final char separator = detectSeparator(path);
    final String content = Files.readString(
      path,
      StandardCharsets.UTF_8
    );
    final CSVFormat format = CSVFormat.DEFAULT.builder()
      .setDelimiter(separator)
      .setIgnoreEmptyLines(false)
      .build();

    try (
      CSVParser parser = CSVParser.builder()
        .setReader(new StringReader(content))
        .setFormat(format)
        .get()
    ) {
      final List<List<String>> rows = new ArrayList<>();
      for (final CSVRecord record : parser) {
        final List<String> row = new ArrayList<>();
        for (String value : record) {
          row.add(value);
        }
        rows.add(row);
      }
      if (rows.isEmpty()) {
        return new CsvTable(List.of(), rows);
      }
      final List<String> columns = rows.remove(0);
      return new CsvTable(columns, rows);
    }
  }

  /**
   * Detects the separator used by a delimited text file.
   *
   * The first non-empty line of the file is inspected and the separator
   * occurring most frequently is selected.
   *
   * The following separators are
   * supported:
   *
   * <ul>
   *   <li>comma ({@code ,})</li>
   *   <li>tab ({@code \t})</li>
   *   <li>semicolon ({@code ;})</li>
   * </ul>
   *
   * If the file does not contain any of the supported separators, a
   * comma is returned as the default separator.
   *
   * @param path path to the delimited text file
   * @return the detected separator character, or a comma if no supported
   *         separator is found
   * @throws IOException if the file cannot be read
   */
  private static char detectSeparator(final Path path)
    throws IOException {

    final List<Character> separators = List.of(',', '\t', ';');

    try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
      final String line = lines
        .filter(value -> !value.isBlank())
        .findFirst()
        .orElse("");

      return separators.stream()
        .max(Comparator.comparingInt(separator ->
          countOccurrences(line, separator)
        ))
        .orElse(',');
    }
  }

  /**
   * Counts the number of occurrences of the specified character in a string.
   *
   * @param value the string to search
   * @param character the character to count
   * @return the number of occurrences of the specified character
   */
  private static int countOccurrences(
    final String value,
    final char character
  ) {
    return (int) value.chars()
      .filter(c -> c == character)
      .count();
  }

  /**
   * Reorders the columns of a table alphabetically by column name.
   *
   * @param table the table whose columns should be reordered
   * @return a new table with columns sorted alphabetically by name
   */
  private static CsvTable normalizeColumns(final CsvTable table) {
    final List<Integer> indexes = new ArrayList<>();

    for (int i = 0; i < table.columns().size(); i++) {
      indexes.add(i);
    }

    indexes.sort(Comparator.comparing(
      i -> table.columns().get(i)
    ));

    final List<String> columns = indexes.stream()
      .map(i -> table.columns().get(i))
      .collect(Collectors.toList());

    final List<List<String>> rows = new ArrayList<>();

    for (List<String> row : table.rows()) {
      final List<String> normalizedRow = new ArrayList<>();
      for (Integer index : indexes) {
        normalizedRow.add(row.get(index));
      }
      rows.add(normalizedRow);
    }
    return new CsvTable(columns, rows);
  }

  /**
   * Normalizes table values by rounding floating-point values to the
   * configured precision and simplifying absolute paths.
   *
   * @param table the table whose values should be normalized
   * @param digits the number of decimal places to retain for floating-point
   * values
   * @return the normalized table
   */
  private static CsvTable normalizeValues(
    final CsvTable table,
    final int digits
  ) {
    final List<List<String>> rows = new ArrayList<>();
    for (List<String> row : table.rows()) {
      final List<String> normalizedRow = new ArrayList<>();
      for (String value : row) {
        if (isAbsolutePath(value)) {
          normalizedRow.add(simplifyPath(value));
        } else if (isDouble(value)) {
          normalizedRow.add(
            Double.toString(
              round(
                Double.parseDouble(value),
                digits
              )
            )
          );
        } else {
          normalizedRow.add(value);
        }
      }
      rows.add(normalizedRow);
    }
    return new CsvTable(table.columns(), rows);
  }

  /**
   * Determines whether a string represents a floating-point number.
   *
   * @param value the string to check
   * @return {@code true} if the value can be parsed as a double
   */
  private static boolean isDouble(final String value) {
    if (value == null || value.isBlank()) {
      return false;
    }

    try {
      Double.parseDouble(value);
      return value.contains(".")
        || value.contains("e")
        || value.contains("E");
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Rounds a floating-point value to the specified number of decimal places.
   *
   * @param value the floating-point value to round
   * @param scale the number of decimal places to retain
   * @return the rounded value
   */
  private static double round(
    final double value,
    final int scale
  ) {
    final double multiplier = Math.pow(10, scale);
    return Math.round(value * multiplier) / multiplier;
  }

  /**
   * Simplifies absolute paths in a string by replacing each path with its
   * normalized representation.
   *
   * @param value the path to simplify
   * @return the simplified path containing only the final path component
   */
  private static String simplifyPath(final String value) {
    final String normalized = value.replace('\\', '/');
    final String[] parts = normalized.split("/");
    if (parts.length <= 1) {
      return normalized;
    }
    return parts[parts.length - 1];
  }

  /**
   * Determines whether a string represents an absolute Unix or Windows path.
   *
   * @param value the string to check
   * @return {@code true} if the value is an absolute Unix or Windows path,
   *         otherwise {@code false}
   */
  private static boolean isAbsolutePath(final String value) {
    return value.startsWith("/")
      || value.matches("^[A-Za-z]:[\\\\/].*");
  }

  /**
   * Sorts the rows of a table deterministically using all columns as
   * sort keys.
   *
   * @param table the table whose rows should be sorted
   * @return the table with rows sorted by all columns
   */
  private static CsvTable normalizeRows(final CsvTable table) {
    final List<List<String>> rows = new ArrayList<>(table.rows());
    rows.sort(
      Comparator.comparing(
        row -> String.join("\u0000", row)
      )
    );
    return new CsvTable(table.columns(), rows);
  }

  /**
   * Creates a deterministic CSV representation of a table.
   *
   * The original file bytes are not hashed directly because differences
   * in line endings, quoting, or formatting could otherwise produce different
   * hashes for equivalent table contents.
   *
   * Line endings are normalized to Unix-style line feeds ({@code \n}) to
   * ensure a consistent representation across platforms.
   *
   * @param table the table to serialize
   * @return the table serialized as UTF-8 CSV with normalized line endings
   * @throws IOException if the table cannot be serialized
   */
  private static String toCanonicalCsv(final CsvTable table)
    throws IOException {
    final StringWriter output = new StringWriter();
    final CSVFormat format = CSVFormat.DEFAULT.builder()
      .setRecordSeparator("\n")
      .build();
    try (CSVPrinter printer = new CSVPrinter(output, format)) {
      printer.printRecord(table.columns());
      for (List<String> row : table.rows()) {
        printer.printRecord(row);
      }
    }
    return output.toString()
      .replace("\r\n", "\n")
      .replace("\r", "\n");
  }

  /**
   * Calculates the MD5 hash of a string using UTF-8 encoding.
   *
   * @param value the string to hash
   * @return the MD5 hash as a lowercase hexadecimal string
   * @throws NoSuchAlgorithmException if the MD5 algorithm is not available
   */
  private static String md5(final String value)
    throws NoSuchAlgorithmException {
    return HashUtils.md5Hex(value);
  }

  /**
   * Represents the contents of a delimited text file as column names
   * and rows of string values.
   *
   * This class is used internally to represent tabular data without
   * relying on an external table library.
   */
  static final class CsvTable {
    /**
     * The column names of the table.
     */
    private final List<String> columns;

    /**
     * The rows of the table, with each row represented as a list of values.
     */
    private final List<List<String>> rows;

    /**
     * Creates a CSV table.
     *
     * @param columnNames column names
     * @param rowsNames table rows
     */
    CsvTable(
      final List<String> columnNames,
      final List<List<String>> rowsNames
    ) {
      this.columns = columnNames;
      this.rows = rowsNames;
    }

    /**
     * Returns the column names.
     *
     * @return the column names
     */
    List<String> columns() {
      return columns;
    }

    /**
     * Returns the rows names.
     *
     * @return the rows names
     */
    List<List<String>> rows() {
      return rows;
    }
  }
}
