package nf_core.nf.test.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
                List.of("KeyE", "KeyF")
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
                List.of("KeyA", "KeyF")
            )
        );
        assertEquals(
            "Key 'KeyA' is used in both 'unstableKeys' and 'ignoreKeys'",
            exception.getMessage()
        );
    }
}
