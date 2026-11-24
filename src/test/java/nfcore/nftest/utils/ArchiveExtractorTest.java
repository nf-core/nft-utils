package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

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
  void shouldExtractFileUrlInPlace() throws Exception {
    Path archive = tempDir.resolve("local.tar");
    Files.write(archive, tarOf("first"));
    Path dest = tempDir.resolve("dest");

    ArchiveExtractor.extract(
      archive.toUri().toString(), dest.toString(), null);

    assertEquals(List.of("first"), Arrays.asList(dest.toFile().list()));
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
  void shouldRejectUnknownOption() {
    LinkedHashMap<String, Object> options = new LinkedHashMap<>();
    options.put("dest", tempDir.resolve("dest"));

    assertThrows(
      IllegalArgumentException.class,
      () -> Methods.extract(options, "http://x/data.tar"));
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
  void shouldDownloadEachCallIntoItsOwnDirectory() throws Exception {
    HttpServer server = HttpServer.create(
      new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      byte[] body = "data".getBytes(StandardCharsets.UTF_8);
      String url = serve(server, 200, body.length, body);

      Path first = ArchiveExtractor.downloadFromS3(url);
      Path second = ArchiveExtractor.downloadFromS3(url);

      assertEquals("archive.tar", first.getFileName().toString());
      assertNotEquals(first.getParent(), second.getParent());
      assertEquals("data", Files.readString(second));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldRejectDownloadWithoutFileName() {
    for (String uri : List.of("s3://bucket/dir/", "s3://bucket/dir/..")) {
      assertThrows(
        IllegalArgumentException.class,
        () -> ArchiveExtractor.downloadFromS3(uri));
    }
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
    assertFalse(Files.exists(Path.of(dest)));
  }

  @Test
  void shouldRejectUnsupportedUrlExtension() {
    String dest = tempDir.resolve("dest").toString();
    for (String url : List.of(
        "http://x/data", "http://x/v1.2/notes.txt")) {
      assertThrows(
        IllegalArgumentException.class,
        () -> ArchiveExtractor.extract(url, dest, null));
    }
  }

  private static byte[] gzip(final String content) throws IOException {
    ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(gzipped)) {
      gzip.write(content.getBytes(StandardCharsets.UTF_8));
    }
    return gzipped.toByteArray();
  }

  @Test
  void shouldGunzipPlainGzipFile() throws Exception {
    Path gz = tempDir.resolve("genome.fa.gz");
    // Two gzip members, like a bgzip file
    ByteArrayOutputStream members = new ByteArrayOutputStream();
    members.write(gzip(">chr1\n"));
    members.write(gzip("ACGT\n"));
    Files.write(gz, members.toByteArray());
    Path dest = tempDir.resolve("dest");

    ArchiveExtractor.extract(gz.toString(), dest.toString(), null);

    assertEquals(
      ">chr1\nACGT\n", Files.readString(dest.resolve("genome.fa")));
    assertTrue(Files.exists(gz));
  }

  @Test
  void shouldNameGunzippedDownloadAfterUrlPath() throws Exception {
    HttpServer server = HttpServer.create(
      new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      byte[] body = gzip("ACGT\n");
      server.createContext("/", exchange -> {
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
      });
      server.start();
      String url = "http://127.0.0.1:" + server.getAddress().getPort()
        + "/ref/Genome.FA.GZ";
      Path dest = tempDir.resolve("dest");

      ArchiveExtractor.extract(url, dest.toString(), null);

      assertEquals(List.of("Genome.FA"), Arrays.asList(dest.toFile().list()));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldDownloadUrlWithQueryString() throws Exception {
    HttpServer server = HttpServer.create(
      new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      byte[] body = gzip("ACGT\n");
      server.createContext("/ref/Genome.FA.GZ", exchange -> {
        if ("raw=true".equals(exchange.getRequestURI().getQuery())) {
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
        } else {
          exchange.sendResponseHeaders(404, -1);
        }
        exchange.close();
      });
      server.start();
      String url = "http://127.0.0.1:" + server.getAddress().getPort()
        + "/ref/Genome.FA.GZ?raw=true";
      Path dest = tempDir.resolve("dest");

      ArchiveExtractor.extract(url, dest.toString(), null);

      assertEquals("ACGT\n", Files.readString(dest.resolve("Genome.FA")));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void shouldGunzipWithCompressionAndKeepNameWithoutSuffix()
      throws Exception {
    Path gz = tempDir.resolve("reference");
    Files.write(gz, gzip("ACGT\n"));
    Path dest = tempDir.resolve("dest");

    ArchiveExtractor.extract(gz.toString(), dest.toString(), "GZIP");

    assertEquals("ACGT\n", Files.readString(dest.resolve("reference")));
  }

  @Test
  void shouldFailToGunzipNonGzipFile() throws Exception {
    Path gz = tempDir.resolve("genome.fa.gz");
    Files.writeString(gz, "not gzipped");
    String dest = tempDir.resolve("dest").toString();

    IOException e = assertThrows(
      IOException.class,
      () -> ArchiveExtractor.extract(gz.toString(), dest, null));

    assertTrue(e.getMessage().startsWith("Failed to extract"));
  }

  @Test
  void shouldDecompressPlainXzFile() throws Exception {
    Path xz = tempDir.resolve("genome.fa.xz");
    try (XZOutputStream out = new XZOutputStream(
        Files.newOutputStream(xz), new LZMA2Options())) {
      out.write("ACGT\n".getBytes(StandardCharsets.UTF_8));
    }
    Path named = tempDir.resolve("named");
    Path typed = tempDir.resolve("typed");

    ArchiveExtractor.extract(xz.toString(), named.toString(), null);
    ArchiveExtractor.extract(xz.toString(), typed.toString(), "XZ");

    assertEquals("ACGT\n", Files.readString(named.resolve("genome.fa")));
    assertEquals("ACGT\n", Files.readString(typed.resolve("genome.fa")));
    assertTrue(Files.exists(xz));
  }

  @Test
  void shouldFailToDecompressNonXzFile() throws Exception {
    Path xz = tempDir.resolve("genome.fa.xz");
    Files.writeString(xz, "not xz");
    String dest = tempDir.resolve("dest").toString();

    IOException e = assertThrows(
      IOException.class,
      () -> ArchiveExtractor.extract(xz.toString(), dest, null));

    assertTrue(e.getMessage().startsWith("Failed to extract"));
  }

  @Test
  void shouldLeaveNoFileWhenDecompressionFails() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (XZOutputStream out = new XZOutputStream(bytes, new LZMA2Options())) {
      out.write("ACGT\n".repeat(1000).getBytes(StandardCharsets.UTF_8));
    }
    byte[] full = bytes.toByteArray();
    Path xz = tempDir.resolve("genome.fa.xz");
    Files.write(xz, Arrays.copyOf(full, full.length - 8));
    Path dest = tempDir.resolve("dest");

    assertThrows(
      IOException.class,
      () -> ArchiveExtractor.extract(xz.toString(), dest.toString(), null));

    try (var files = Files.list(dest)) {
      assertEquals(List.of(), files.toList());
    }
  }

  @Test
  void shouldRejectDecompressedNameResolvingToDirectory() {
    String dest = tempDir.resolve("dest").toString();
    for (String url : List.of("http://x/a/%2E%2E%2Egz", "http://x/a/..gz")) {
      assertThrows(
        IllegalArgumentException.class,
        () -> ArchiveExtractor.extract(url, dest, null));
    }
  }

  @Test
  void shouldKeepHashAndQuestionMarkInLocalNames() throws Exception {
    Path gz = tempDir.resolve("sample#1?.fa.gz");
    Files.write(gz, gzip("ACGT\n"));
    Path dest = tempDir.resolve("dest");

    ArchiveExtractor.extract(gz.toString(), dest.toString(), null);

    assertEquals(List.of("sample#1?.fa"), Arrays.asList(dest.toFile().list()));
  }

  @Test
  void shouldReturnFileFromTar() throws Exception {
    Path archive = tempDir.resolve("data.tar");
    Files.write(archive, tarOf("a.txt", "b.txt"));
    Path dest = tempDir.resolve("dest");

    assertEquals(
      dest.resolve("b.txt"),
      ArchiveExtractor.extractPath(
        archive.toString(), dest.toString(), null, "b.txt"));
  }

  @Test
  void shouldReturnFolderFromTar() throws Exception {
    Path source = Files.createDirectories(tempDir.resolve("source/db"));
    Files.writeString(source.resolve("index.txt"), "x");
    Path archive = tempDir.resolve("db.tar");
    assertEquals(0, new ProcessBuilder(
      "tar", "cf", archive.toString(),
      "-C", tempDir.resolve("source").toString(), "db"
    ).start().waitFor());
    Path dest = tempDir.resolve("dest");

    Path db = ArchiveExtractor.extractPath(
      archive.toString(), dest.toString(), null, "db");

    assertEquals(dest.resolve("db"), db);
    assertTrue(Files.exists(db.resolve("index.txt")));
  }

  @Test
  void shouldReturnGunzippedFile() throws Exception {
    Path gz = Files.write(tempDir.resolve("genome.fa.gz"), gzip("ACGT\n"));
    Path dest = tempDir.resolve("dest");

    Path fasta = ArchiveExtractor.extractPath(
      gz.toString(), dest.toString(), null, "genome.fa");

    assertEquals(dest.resolve("genome.fa"), fasta);
    assertEquals("ACGT\n", Files.readString(fasta));
  }

  @Test
  void shouldFailWhenPathIsNotInArchive() throws Exception {
    Path archive = tempDir.resolve("data.tar");
    Files.write(archive, tarOf("a.txt"));
    String dest = tempDir.resolve("dest").toString();

    assertThrows(IOException.class, () -> ArchiveExtractor.extractPath(
      archive.toString(), dest, null, "missing.txt"));
  }

  @Test
  void shouldRejectPathOutsideExtractDirectory() {
    String dest = tempDir.resolve("dest").toString();
    for (String path : List.of("../x", "/etc/passwd", "", ".")) {
      assertThrows(IllegalArgumentException.class,
        () -> ArchiveExtractor.extractPath("data.tar", dest, null, path));
    }
    assertFalse(Files.exists(Path.of(dest)));
  }
}
