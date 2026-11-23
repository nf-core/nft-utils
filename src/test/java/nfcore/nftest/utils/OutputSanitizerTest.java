package nfcore.nftest.utils;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class OutputSanitizerTest {

    @Test
    void shouldAcceptKeyPresentInChannel() {
        Map<String, Object> channel = Map.of(
            "foo", "some-value",
            "bar", "another-value"
        );
        assertDoesNotThrow(() ->
            OutputSanitizer.validateKeysInChannel(
                List.of("foo"),
                channel
            )
        );
    }

    @Test
    void shouldRejectKeyNotPresentInChannel() {
        Map<String, Object> channel = Map.of(
            "foo", "some-value",
            "bar", "another-value"
        );
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> OutputSanitizer.validateKeysInChannel(
                List.of("baz"),
                channel
            )
        );
        assertEquals(
            "Key 'baz' not present in channel",
            exception.getMessage()
        );
    }

    @Test
    void shouldAcceptMultipleValidKeysList() {
        Map<String, Object> channel = Map.of(
            "foo", "value1",
            "bar", "value2",
            "baz", "value3"
        );
        assertDoesNotThrow(() ->
            OutputSanitizer.validateKeysInChannel(
                List.of("foo", "bar"),
                channel
            )
        );
    }

    @Test
    void shouldRejectWhenOneKeyIsMissing() {
        Map<String, Object> channel = Map.of(
            "foo", "value1",
            "bar", "value2"
        );
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> OutputSanitizer.validateKeysInChannel(
                List.of("foo", "missing"),
                channel
            )
        );
        assertEquals(
            "Key 'missing' not present in channel",
            exception.getMessage()
        );
    }

    @Test
    void shouldAcceptEmptyKeysList() {
        Map<String, Object> channel = Map.of(
            "foo", "value1"
        );
        assertDoesNotThrow(() ->
            OutputSanitizer.validateKeysInChannel(
                List.of(),
                channel
            )
        );
    }

    @Test
    void shouldAcceptMutuallyExclusiveKeysList() {
        assertDoesNotThrow(() ->
            OutputSanitizer.validateKeyUsage(
                List.of("KeyA", "KeyB"),
                List.of("KeyC", "KeyD"),
                List.of(),
                List.of("KeyE", "KeyF"),
                List.of("")
            )
        );
    }

    @Test
    void shouldRejectWhenOneKeyIsUsedMoreThanOnce() {
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> OutputSanitizer.validateKeyUsage(
                List.of("KeyA", "KeyB"),
                List.of("KeyC", "KeyA"),
                List.of(),
                List.of("KeyA", "KeyF"),
                List.of()
            )
        );
        assertEquals(
            "Key 'KeyA' is used in both 'unstableKeys' and 'ignoreKeys'",
            exception.getMessage()
        );
    }

    @Test
    void patternSanitizerFailMatchBothIgnoreAndUnstable() {
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> OutputSanitizer.patternSanitizer(
                List.of("*.bam", "*.vcf"),
                List.of("*.vcf")
            ).apply(List.of("MyFile.bam", "MyFile.vcf"))
        );

        assertEquals(
            "Value 'MyFile.vcf' matches both ignorePatterns and unstablePatterns",
            exception.getMessage()
        );
    }

  @Test
  void patternSanitizerShouldIgnoreTxtFileAndSimplifyBamInList() {
    Object outputCheck = OutputSanitizer.patternSanitizer(
      List.of("**/*.bam"),
      List.of("**/*.txt")
    ).apply(List.of(
      "/tmp/some.directory/example.bam",
      "/tmp/some.directory/example.txt"
    ));
    assertEquals(
      List.of("example.bam"),
      outputCheck
    );
  }

  @Test
  void patternSanitizerShouldMatchDoubleStarSlashGlobsAtTopLevel() {
    Object outputCheck = OutputSanitizer.patternSanitizer(
      List.of("**/*.bam"),
      List.of("**/*.txt")
    ).apply(List.of("example.bam", "example.txt", "sub/other.txt"));
    assertEquals(
      List.of("example.bam"),
      outputCheck
    );
  }

  @Test
  void patternSanitizerShouldIgnoreTxtFileAndSimplifyBamInMap() {
    Object outputCheck = OutputSanitizer.patternSanitizer(
      List.of("**/*.bam"),
      List.of("**/*.txt")
    ).apply(Map.of(
      "bam", "/tmp/some.directory/example.bam",
      "txt", "/tmp/some.directory/example.txt"
    ));
    assertEquals(
      Map.of("bam", "example.bam"),
      outputCheck
    );
  }

  @Test
  void shouldAcceptKeyPresentAsRecordField() {
    Map<String, Object> channel = Map.of(
      "0", List.of(Map.of("id", "test", "log", "/tmp/test.log"))
    );
    assertDoesNotThrow(() ->
      OutputSanitizer.validateKeysInChannel(List.of("0", "log"), channel)
    );
  }

  @Test
  void shouldRejectKeyAbsentFromChannelAndRecords() {
    Map<String, Object> channel = Map.of(
      "0", List.of(Map.of("id", "test", "log", "/tmp/test.log"))
    );
    RuntimeException exception = assertThrows(
      RuntimeException.class,
      () -> OutputSanitizer.validateKeysInChannel(List.of("bam"), channel)
    );
    assertEquals(
      "Key 'bam' not present in channel names or record fields",
      exception.getMessage()
    );
  }

  @Test
  void recordFieldNamesShouldIgnoreTuples() {
    Map<String, Object> channel = Map.of(
      "html", List.of(List.of(Map.of("id", "test"), "/tmp/test.html")),
      "0", List.of(Map.of("id", "test", "log", "/tmp/test.log"))
    );
    assertEquals(
      Set.of("id", "log"),
      OutputSanitizer.recordFieldNames(channel)
    );
    assertEquals(
      Set.of(),
      OutputSanitizer.recordFieldNames(Map.of("html", channel.get("html")))
    );
  }

  @Test
  void sanitizeRecordsShouldApplyFieldSanitizers() {
    Function<Object, Object> drop =
      OutputSanitizer.patternSanitizer(List.of(), List.of("**"));
    Object sanitized = OutputSanitizer.sanitizeRecords(
      List.of(
        Map.of(
          "id", "test",
          "log", "/tmp/dir/test.log",
          "report", "/tmp/dir/test.txt"
        ),
        "/tmp/dir/other.txt"
      ),
      Map.of("log", v -> "renamed", "report", drop),
      v -> v
    );
    assertEquals(
      List.of(Map.of("id", "test", "log", "renamed"), "/tmp/dir/other.txt"),
      sanitized
    );
  }
}
