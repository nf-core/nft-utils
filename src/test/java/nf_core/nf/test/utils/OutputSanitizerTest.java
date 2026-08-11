package nf_core.nf.test.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OutputSanitizerTest {

    @Test
    void shouldAcceptUnstableKeyPresentInChannel() {
        Map<String, Object> channel = Map.of(
            "foo", "some-value",
            "bar", "another-value"
        );
        assertDoesNotThrow(() ->
            OutputSanitizer.validateUnstableKeys(
                List.of("foo"),
                channel
            )
        );
    }

    @Test
    void shouldRejectUnstableKeyNotPresentInChannel() {
        Map<String, Object> channel = Map.of(
            "foo", "some-value",
            "bar", "another-value"
        );
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> OutputSanitizer.validateUnstableKeys(
                List.of("baz"),
                channel
            )
        );
        assertEquals(
            "Unstable key 'baz' not present in channel",
            exception.getMessage()
        );
    }

    @Test
    void shouldAcceptMultipleValidUnstableKeys() {
        Map<String, Object> channel = Map.of(
            "foo", "value1",
            "bar", "value2",
            "baz", "value3"
        );
        assertDoesNotThrow(() ->
            OutputSanitizer.validateUnstableKeys(
                List.of("foo", "bar"),
                channel
            )
        );
    }

    @Test
    void shouldRejectWhenOneUnstableKeyIsMissing() {
        Map<String, Object> channel = Map.of(
            "foo", "value1",
            "bar", "value2"
        );
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> OutputSanitizer.validateUnstableKeys(
                List.of("foo", "missing"),
                channel
            )
        );
        assertEquals(
            "Unstable key 'missing' not present in channel",
            exception.getMessage()
        );
    }

    @Test
    void shouldAcceptEmptyUnstableKeys() {
        Map<String, Object> channel = Map.of(
            "foo", "value1"
        );
        assertDoesNotThrow(() ->
            OutputSanitizer.validateUnstableKeys(
                List.of(),
                channel
            )
        );
    }
}
