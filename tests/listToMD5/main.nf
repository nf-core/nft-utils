workflow TEST {
    main:
    def fill = "This needs to be filled in"
    def lines = [
        "This is a line",
        32,
        453.2,
        """
        Wow, a multiline string
        """,
        "${fill}"
    ]

    emit:
    lines_out = lines
}