package nfcore.nftest.utils;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The named options passed to one function from a Groovy test script.
 * Unknown option names and values of the wrong type fail with the function
 * name. String options accept Groovy GStrings, {@code File}s and
 * {@code Path}s, read with {@code toString()}.
 */
final class Options {

  /** The function name, for error messages. */
  private final String function;

  /** What the names are, such as "option", for error messages. */
  private final String kind;

  /** The options passed, by name. */
  private final Map<String, Object> values = new HashMap<>();

  /**
   * Checks the option names passed to a function.
   *
   * @param functionName The function name, for error messages.
   * @param options      The options passed, or {@code null} for none.
   * @param valid        The options the function accepts.
   * @throws IllegalArgumentException on an unknown option
   */
  Options(
      final String functionName,
      final Map<?, ?> options,
      final String... valid) {
    this(functionName, "option", options, valid);
  }

  /**
   * Checks the names in a map passed to a function.
   *
   * @param functionName The function name, for error messages.
   * @param kindName     What the names are, such as "module key".
   * @param options      The map passed, or {@code null} for none.
   * @param valid        The names the function accepts.
   * @throws IllegalArgumentException on an unknown name
   */
  Options(
      final String functionName,
      final String kindName,
      final Map<?, ?> options,
      final String... valid) {
    this.function = functionName;
    this.kind = kindName;
    // String keys, so a GString key such as "${name}" is found too
    if (options != null) {
      options.forEach((key, value) -> values.put(String.valueOf(key), value));
    }
    Set<String> accepted = new TreeSet<>(List.of(valid));
    Set<String> unknown = new TreeSet<>(values.keySet());
    unknown.removeAll(accepted);
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException(
        "Unknown " + functionName + "() " + kind + "s: " + unknown
        + "; valid " + kind + "s: " + accepted
      );
    }
  }

  /**
   * Reads a string option: a string, Groovy GString, {@code File} or
   * {@code Path}.
   *
   * @param name         The option name.
   * @param defaultValue The value when the option is not passed.
   * @return The option value.
   * @throws IllegalArgumentException if the value is not a string
   */
  String string(final String name, final String defaultValue) {
    Object value = values.get(name);
    if (value == null) {
      return defaultValue;
    }
    if (!isText(value)) {
      throw invalid(name, "a String, File or Path", value);
    }
    return value.toString();
  }

  /**
   * Reads a boolean option.
   *
   * @param name         The option name.
   * @param defaultValue The value when the option is not passed.
   * @return The option value.
   * @throws IllegalArgumentException if the value is not a boolean
   */
  boolean bool(final String name, final boolean defaultValue) {
    return get(name, Boolean.class, defaultValue);
  }

  /**
   * Reads an integer option, from any whole number that fits an
   * {@code int}, such as {@code 3L}.
   *
   * @param name         The option name.
   * @param defaultValue The value when the option is not passed.
   * @return The option value.
   * @throws IllegalArgumentException if the value is not an integer
   */
  int integer(final String name, final int defaultValue) {
    Object value = values.get(name);
    if (value == null) {
      return defaultValue;
    }
    if (!(value instanceof Number number)) {
      throw invalid(name, "an Integer", value);
    }
    try {
      return new BigDecimal(number.toString()).intValueExact();
    } catch (ArithmeticException | NumberFormatException e) {
      throw invalid(name, "an Integer", value);
    }
  }

  /**
   * Reads an option that must be a list of strings, Groovy GStrings,
   * {@code File}s or {@code Path}s.
   *
   * @param name         The option name.
   * @param defaultValue The value when the option is not passed.
   * @return The option value.
   * @throws IllegalArgumentException if the value is not a list of strings
   */
  List<String> strings(final String name, final List<String> defaultValue) {
    Object value = values.get(name);
    if (value == null) {
      return defaultValue;
    }
    if (!(value instanceof List<?> list)
        || !list.stream().allMatch(Options::isText)) {
      throw invalid(name, "a list of String, File or Path", value);
    }
    return list.stream().map(Object::toString).toList();
  }

  /**
   * Reads an option that must be a list of the given type.
   *
   * @param <T>          The item type.
   * @param name         The option name.
   * @param type         The item type.
   * @param defaultValue The value when the option is not passed.
   * @return The option value.
   * @throws IllegalArgumentException if the value is not a list of
   *     {@code type}
   */
  <T> List<T> list(
      final String name,
      final Class<T> type,
      final List<? extends T> defaultValue) {
    Object value = values.get(name);
    if (value == null) {
      value = defaultValue;
    }
    if (!(value instanceof List<?> list)
        || !list.stream().allMatch(type::isInstance)) {
      throw invalid(name, "a list of " + type.getSimpleName(), value);
    }
    return list.stream().map(type::cast).toList();
  }

  /**
   * Reads an option of the given type.
   *
   * @param <T>          The option type.
   * @param name         The option name.
   * @param type         The option type.
   * @param defaultValue The value when the option is not passed.
   * @return The option value.
   * @throws IllegalArgumentException if the value is not a {@code type}
   */
  private <T> T get(
      final String name,
      final Class<T> type,
      final T defaultValue) {
    Object value = values.get(name);
    if (value == null) {
      return defaultValue;
    }
    if (!type.isInstance(value)) {
      throw invalid(name, "a " + type.getSimpleName(), value);
    }
    return type.cast(value);
  }

  /**
   * Tells whether a value is read as text: a string, Groovy GString,
   * {@code File} or {@code Path}.
   *
   * @param value The value.
   * @return {@code true} if the value is text.
   */
  private static boolean isText(final Object value) {
    return value instanceof CharSequence
      || value instanceof File
      || value instanceof Path;
  }

  /**
   * Builds the error for an option of the wrong type.
   *
   * @param name     The option name.
   * @param expected The expected type, for the message.
   * @param value    The value passed.
   * @return The exception to throw.
   */
  private IllegalArgumentException invalid(
      final String name,
      final String expected,
      final Object value) {
    return new IllegalArgumentException(
      function + "() " + kind + " '" + name + "' must be " + expected
      + ", got: " + value
    );
  }
}
