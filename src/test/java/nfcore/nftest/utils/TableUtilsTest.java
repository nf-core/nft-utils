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
    final TableUtils.TableUtilsClass input = new TableUtils.TableUtilsClass(
      List.of("z_column", "a_column", "m_column"),
      List.of(
        List.of("z", "a", "m")
      )
    );

    final TableUtils.TableUtilsClass normalized =
      TableUtils.normalizeTable(input, 6);

    assertEquals(
      List.of("a_column", "m_column", "z_column"),
      normalized.columns()
    );

    assertEquals(
      List.of("a", "m", "z"),
      normalized.rows().get(0)
    );
  }

  @Test
  void shouldRoundFloatingPointValues() {
    final TableUtils.TableUtilsClass input = new TableUtils.TableUtilsClass(
      List.of("value"),
      List.of(
        List.of("1.123456789"),
        List.of("2.987654321"),
        List.of("10.000000001")
      )
    );

    final TableUtils.TableUtilsClass normalized =
      TableUtils.normalizeTable(input, 6);

    assertEquals("1.123457", normalized.rows().get(0).get(0));
    assertEquals("10.0", normalized.rows().get(1).get(0));
    assertEquals("2.987654", normalized.rows().get(2).get(0));
  }

  @Test
  void shouldSimplifyAbsolutePaths() {
    final TableUtils.TableUtilsClass input = new TableUtils.TableUtilsClass(
      List.of("path"),
      List.of(
        List.of("/home/user/work/results/a.csv"),
        List.of("/home/user/work/results/b.csv")
      )
    );

    final TableUtils.TableUtilsClass normalized =
      TableUtils.normalizeTable(input, 6);

    assertEquals("a.csv", normalized.rows().get(0).get(0));
    assertEquals("b.csv", normalized.rows().get(1).get(0));
  }

  @Test
  void shouldNormalizeRows() {
    final TableUtils.TableUtilsClass input = new TableUtils.TableUtilsClass(
      List.of("sample", "value"),
      List.of(
        List.of("B", "2.00002123"),
        List.of("C", "3.046135468"),
        List.of("A", "1.016854841")
      )
    );

    final TableUtils.TableUtilsClass normalized =
      TableUtils.normalizeTable(input, 6);

    assertEquals(
      List.of("A", "1.016855"),
      normalized.rows().get(0)
    );
    assertEquals(
      List.of("B", "2.000021"),
      normalized.rows().get(1)
    );
    assertEquals(
      List.of("C", "3.046135"),
      normalized.rows().get(2)
    );
  }

  @Test
  void shouldNormalizeRowsAndColumnsTogether() {
    final TableUtils.TableUtilsClass input = new TableUtils.TableUtilsClass(
      List.of("value", "sample", "path"),
      List.of(
        List.of("2.123", "B", "/usr/local/bin/folder/"),
        List.of("1.456", "A", "/usr/local/bin/folder/files.py"),
        List.of("3.789", "C", "C:/This/Is/another/paht/test.txt")
      )
    );

    final TableUtils.TableUtilsClass normalized =
      TableUtils.normalizeTable(input, 2);

    assertEquals(
      List.of("path", "sample", "value"),
      normalized.columns()
    );

    assertEquals(
      List.of("files.py", "A", "1.46"),
      normalized.rows().get(0)
    );
    assertEquals(
      List.of("folder", "B", "2.12"),
      normalized.rows().get(1)
    );
    assertEquals(
      List.of("test.txt", "C", "3.79"),
      normalized.rows().get(2)
    );
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
