process TESTCSV {
    tag "${meta.id}"
    label 'process_single'

    input:
    val meta

    output:
    tuple val(meta), path("test.txt"), path("test.tsv"), path("test.csv"), path("test.tbl"), emit: normalized
    tuple val(meta), path("test.txt"), path("test.tsv"), path("test.csv"), path("test.tbl"), emit: not_normalize

    script:
    """
    # CSV format
    echo "sample,path,value" > test.csv
    echo "#A,/usr/local/bin/folder/,1.03401255" >> test.csv
    echo "C,C:/This/Is/another/path/test.txt,3.046135999" >> test.csv
    echo "B,/usr/local/bin/folder/files.py,2.00002123" >> test.csv

    # TXT format
    echo "value;sample;path" > test.txt
    echo "2.00002123;B;/usr/local/bin/folder/files.py" >> test.txt
    echo "1.03401255;#A;/usr/local/bin/folder/" >> test.txt
    echo "3.046135768;C;C:/This/Is/another/path/test.txt" >> test.txt

    # TSV already normed format
    echo "path\tsample\tvalue" > test.tsv
    echo "folder\t#A\t1.034012" >> test.tsv
    echo "files.py\tB\t2.000021" >> test.tsv
    echo "test.txt\tC\t3.046136" >> test.tsv

    # TBL with comments
    echo "  " > test.tbl
    echo "  #Mon Sep 28 05:35:45 PM CEST 2026" >> test.tbl
    echo "#Other label" >> test.tbl
    echo "path\tsample\tvalue" >> test.tbl
    echo "folder\t#A\t1.034012" >> test.tbl
    echo "files.py\tB\t2.000021" >> test.tbl
    echo "test.txt\tC\t3.046136" >> test.tbl
    """
}
