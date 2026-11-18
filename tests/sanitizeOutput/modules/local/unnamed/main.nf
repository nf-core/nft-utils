process UNNAMED {
    output:
    path "a.txt"
    path "b.txt"
    path "c.txt", emit: c

    script:
    """
    echo a > a.txt
    echo b > b.txt
    echo c > c.txt
    """
}
