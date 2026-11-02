package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NextflowOutputFilterTest {

  @Test
  void shouldRemoveAdditionalPatternMatches() {
    List<String> result = NextflowOutputFilter.filterNextflowOutput(
      "keep me\nsecret 1234 here\n.*drop line.*",
      List.of("\\d{4} ", ".*drop line.*"));
    assertEquals(List.of("keep me", "secret here"), result);
  }

  @Test
  void shouldSkipInvalidAdditionalPattern() {
    List<String> result = NextflowOutputFilter.filterNextflowOutput(
      "keep me", List.of("("));
    assertEquals(List.of("keep me"), result);
  }

  @Test
  void shouldAcceptOptionsMapAfterOutput() {
    List<String> result = NextflowOutputFilter.filterNextflowOutput(
      "b line\na line", Map.<String, Object>of("ignore", List.of("b ")));
    assertEquals(List.of("a line"), result);
  }
}
