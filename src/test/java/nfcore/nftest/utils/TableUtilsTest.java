package nfcore.nftest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

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
}
