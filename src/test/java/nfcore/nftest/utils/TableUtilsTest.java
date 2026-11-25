package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TableUtilsTest {

  @Test
  void shouldNormalizeColumns() {
    assertEquals(
      new TableUtils.Table(
        List.of("a_column", "m_column", "z_column"),
        List.of(List.of("a", "m", "z"))),
      TableUtils.normalizeTable(new TableUtils.Table(
        List.of("z_column", "a_column", "m_column"),
        List.of(List.of("z", "a", "m"))), 6));
  }

  @Test
  void shouldRoundFloatingPointValues() {
    assertEquals(
      new TableUtils.Table(
        List.of("value"),
        List.of(List.of("1.123457"), List.of("10.0"), List.of("2.987654"))),
      TableUtils.normalizeTable(new TableUtils.Table(
        List.of("value"),
        List.of(
          List.of("1.123456789"),
          List.of("2.987654321"),
          List.of("10.000000001"))), 6));
  }

  @Test
  void shouldSimplifyAbsolutePaths() {
    assertEquals(
      new TableUtils.Table(
        List.of("path"),
        List.of(List.of("a.csv"), List.of("b.csv"))),
      TableUtils.normalizeTable(new TableUtils.Table(
        List.of("path"),
        List.of(
          List.of("/home/user/work/results/a.csv"),
          List.of("/home/user/work/results/b.csv"))), 6));
  }

  @Test
  void shouldNormalizeRows() {
    assertEquals(
      new TableUtils.Table(
        List.of("sample", "value"),
        List.of(
          List.of("A", "1.016855"),
          List.of("B", "2.000021"),
          List.of("C", "3.046135"))),
      TableUtils.normalizeTable(new TableUtils.Table(
        List.of("sample", "value"),
        List.of(
          List.of("B", "2.00002123"),
          List.of("C", "3.046135468"),
          List.of("A", "1.016854841"))), 6));
  }

  @Test
  void shouldNormalizeRowsAndColumnsTogether() {
    assertEquals(
      new TableUtils.Table(
        List.of("path", "sample", "value"),
        List.of(
          List.of("files.py", "A", "1.46"),
          List.of("folder", "B", "2.12"),
          List.of("test.txt", "C", "3.79"))),
      TableUtils.normalizeTable(new TableUtils.Table(
        List.of("value", "sample", "path"),
        List.of(
          List.of("2.123", "B", "/usr/local/bin/folder/"),
          List.of("1.456", "A", "/usr/local/bin/folder/files.py"),
          List.of("3.789", "C", "C:/This/Is/another/paht/test.txt"))), 2));
  }

  @TempDir
  Path dir;

  private String normalize(final String name, final byte[] content,
      final boolean removeComments) throws IOException {
    Path file = Files.write(dir.resolve(name), content);
    return TableUtils.normalizeTable(file, 6, removeComments);
  }

  private static byte[] utf8(final String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void shouldReadTableBelowLeadingComments() throws IOException {
    byte[] table = utf8("# made by tool, v1\nb\ta\n2\t1\n");
    assertEquals("# made by tool, v1\na,b\n1,2\n",
      normalize("t.tsv", table, false));
    assertEquals("a,b\n1,2\n", normalize("t.tsv", table, true));
  }

  @Test
  void shouldReadHashHeaderAsHeader() throws IOException {
    byte[] table = utf8("##source=tool\n#chrom\tstart\tend\nchr2\t5\t9\n"
      + "chr1\t1\t2\n");
    assertEquals("##source=tool\n\"#chrom\",end,start\nchr1,2,1\nchr2,9,5\n",
      normalize("t.tsv", table, false));
  }

  @Test
  void shouldPadShortRowsAndBlankLines() throws IOException {
    assertEquals("\"\",a,b,c\n\"\",,,\n\"\",1,2,\n7,4,5,6\n",
      normalize("t.csv", utf8("a,b,c\n1,2\n\n4,5,6,7\n"), false));
  }

  @Test
  void shouldKeepValuesTooLargeToRound() throws IOException {
    assertEquals("a\n0.123457\n1.5E13\n",
      normalize("t.csv", utf8("a\n1.5e13\n0.1234567\n"), false));
  }

  @Test
  void shouldReadGzippedAndNonUtf8Tables() throws IOException {
    ByteArrayOutputStream gz = new ByteArrayOutputStream();
    try (GZIPOutputStream out = new GZIPOutputStream(gz)) {
      out.write(utf8("a,b\n1,2\n"));
    }
    assertEquals("a,b\n1,2\n", normalize("t.csv.gz", gz.toByteArray(), false));
    assertEquals("a\n\uFFFD\n",
      normalize("t.csv", new byte[] {'a', '\n', (byte) 0xff, '\n'}, false));
  }
}
