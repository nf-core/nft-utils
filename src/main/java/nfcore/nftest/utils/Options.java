package nfcore.nftest.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The named options passed to one function from a Groovy test script.
 * Unknown option names and values of the wrong type fail with the function
 * name; {@code toString()} turns Groovy GStrings into Strings.
 */
final class Options {

  /** The function name, for error messages. */
  private final String function;

  /** The options passed. */
  private final Map<?, ?> values;

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
      final Set<String> valid) {
    this.function = functionName;
    this.values = options == null ? Map.of() : options;
    Set<String> unknown = new TreeSet<>();
    this.values.keySet().forEach(key -> unknown.add(String.valueOf(key)));
    unknown.removeAll(valid);
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException(
        "Unknown " + functionName + "() options: " + unknown
        + "; valid options: " + new TreeSet<>(valid)
      );
    }
  }

  /**
   * Reads an option as a string.
   *
   * @param name         The option name.
   * @param defaultValue The value when the option is not passed.
   * @return The option value.
   */
  String string(final String name, final String defaultValue) {
    Object value = values.get(name);
    return value == null ? defaultValue : value.toString();
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
   * Reads an integer option.
   *
   * @param name         The option name.
   * @param defaultValue The value when the option is not passed.
   * @return The option value.
   * @throws IllegalArgumentException if the value is not an integer
   */
  int integer(final String name, final int defaultValue) {
    return get(name, Integer.class, defaultValue);
  }

  /**
   * Reads an option that must be a list of strings.
   *
   * @param name         The option name.
   * @param defaultValue The value when the option is not passed.
   * @return The option value.
   * @throws IllegalArgumentException if the value is not a list of strings
   */
  List<String> strings(final String name, final List<String> defaultValue) {
    if (values.get(name) == null) {
      return defaultValue;
    }
    return list(name, CharSequence.class).stream()
      .map(Object::toString).toList();
  }

  /**
   * Reads an option that must be a list of the given type, defaulting to an
   * empty list.
   *
   * @param <T>  The item type.
   * @param name The option name.
   * @param type The item type.
   * @return The option value.
   * @throws IllegalArgumentException if the value is not a list of
   *     {@code type}
   */
  <T> List<T> list(final String name, final Class<T> type) {
    Object value = values.get(name);
    if (value == null) {
      value = List.of();
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
      function + "() option '" + name + "' must be " + expected
      + ", got: " + value
    );
  }
}
