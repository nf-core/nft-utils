package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchiveExtractorTest {

  private static final int TAR_BLOCK = 512;

  @TempDir
  Path tempDir;

  private static String serve(
      final HttpServer server,
      final int status,
      final long announcedLength,
      final byte[] body) {
    server.createContext("/archive.tar", exchange -> {
      exchange.sendResponseHeaders(status, announcedLength);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    return "http://127.0.0.1:" + server.getAddress().getPort()
      + "/archive.tar";
  }

  private byte[] tarOf(final String... names)
      throws IOException, InterruptedException {
    Path source = Files.createDirectory(tempDir.resolve("source"));
    for (String name : names) {
      Files.write(source.resolve(name), new byte[TAR_BLOCK]);
    }
    Path archive = tempDir.resolve("source.tar");
    List<String> cmd = new ArrayList<>(List.of(
      "tar", "cf", archive.toString(), "-C", source.toString()
    ));
    cmd.addAll(List.of(names));
    assertEquals(0, new ProcessBuilder(cmd).start().waitFor());
    return Files.readAllBytes(archive);
  }

  @Test
  void shouldExtractTar() throws Exception {
    HttpServer server = HttpServer.create(
      new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      byte[] tar = tarOf("first");
      String url = serve(server, 200, tar.length, tar);
      Path dest = tempDir.resolve("dest");

      ArchiveExtractor.extract(url, dest.toString(), null);

      assertEquals(List.of("first"), Arrays.asList(dest.toFile().list()));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldExtractLocalPath() throws Exception {
    Path archive = tempDir.resolve("local.tar");
    Files.write(archive, tarOf("first"));
    Path dest = tempDir.resolve("dest");

    ArchiveExtractor.extract(archive.toString(), dest.toString(), null);

    assertEquals(List.of("first"), Arrays.asList(dest.toFile().list()));
    assertTrue(Files.exists(archive));
  }

  @Test
  void shouldExtractInTemporaryDirectoryByDefault() throws Exception {
    Path archive = tempDir.resolve("local.tar");
    Files.write(archive, tarOf("first"));

    Path dest = ArchiveExtractor.extract(archive.toString(), null, null);

    assertEquals(List.of("first"), Arrays.asList(dest.toFile().list()));
  }

  @Test
  void shouldTakeNamedOptions() throws Exception {
    Path archive = tempDir.resolve("local.data");
    Files.write(archive, tarOf("first"));
    Path dest = tempDir.resolve("dest");
    LinkedHashMap<String, Object> options = new LinkedHashMap<>();
    options.put("extractDir", dest);
    options.put("compression", "tar");

    assertEquals(dest, Methods.extract(options, archive.toString()));
    assertEquals(List.of("first"), Arrays.asList(dest.toFile().list()));
  }

  @Test
  void shouldFailOnMissingLocalPath() {
    String archive = tempDir.resolve("missing.tar").toString();
    String dest = tempDir.resolve("dest").toString();

    IOException e = assertThrows(
      IOException.class,
      () -> ArchiveExtractor.extract(archive, dest, null));

    assertTrue(e.getMessage().startsWith("Failed to extract"));
  }

  @Test
  void shouldFailWhenTarIsTruncatedAtMemberBoundary() throws Exception {
    HttpServer server = HttpServer.create(
      new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      byte[] tar = tarOf("first", "second");
      byte[] firstMember = Arrays.copyOf(tar, 2 * TAR_BLOCK);
      String url = serve(server, 200, tar.length, firstMember);
      String dest = tempDir.resolve("dest").toString();

      IOException e = assertThrows(
        IOException.class,
        () -> ArchiveExtractor.extract(url, dest, null));

      assertTrue(e.getMessage().startsWith("Failed to download http"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldReportHttpErrorAsDownloadFailure() throws Exception {
    HttpServer server = HttpServer.create(
      new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      String url = serve(server, 404, 0, new byte[0]);
      Path dest = tempDir.resolve("dest");

      IOException e = assertThrows(
        IOException.class,
        () -> ArchiveExtractor.extract(url, dest.toString(), null));

      assertTrue(e.getMessage().startsWith("Failed to download http"));
      assertEquals(0, dest.toFile().list().length);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldFailOnCorruptArchive() throws Exception {
    HttpServer server = HttpServer.create(
      new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      byte[] garbage = "not a tar archive".getBytes("UTF-8");
      String url = serve(server, 200, garbage.length, garbage);
      String dest = tempDir.resolve("dest").toString();

      IOException e = assertThrows(
        IOException.class,
        () -> ArchiveExtractor.extract(url, dest, null));

      assertTrue(e.getMessage().startsWith("Failed to extract"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldExtractWithCompressionAlias() throws Exception {
    HttpServer server = HttpServer.create(
      new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
      try (GZIPOutputStream gzip = new GZIPOutputStream(gzipped)) {
        gzip.write(tarOf("first"));
      }
      byte[] body = gzipped.toByteArray();
      String url = serve(server, 200, body.length, body);
      Path dest = tempDir.resolve("dest");

      ArchiveExtractor.extract(url, dest.toString(), "TGZ");

      assertEquals(List.of("first"), Arrays.asList(dest.toFile().list()));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldRejectUnsupportedCompression() {
    String dest = tempDir.resolve("dest").toString();
    for (String compression : List.of("rar", "tar.rar", "t")) {
      assertThrows(
        IllegalArgumentException.class,
        () -> ArchiveExtractor.extract("http://x", dest, compression));
    }
  }

  @Test
  void shouldRejectUnsupportedUrlExtension() {
    String dest = tempDir.resolve("dest").toString();
    for (String url : List.of(
        "http://x/data", "http://x/data.gz", "http://x/v1.2/notes.txt")) {
      assertThrows(
        IllegalArgumentException.class,
        () -> ArchiveExtractor.extract(url, dest, null));
    }
  }
}
