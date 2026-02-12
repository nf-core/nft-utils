include { FASTQC } from './modules/local/fastqc'

params {
    failure         : Boolean = false
    outdir          : String = "results"
    monochrome_logs : Boolean = false
}

workflow {
    def colors = getColors(params.monochrome_logs)
    def input = channel.of(
        'sample_1',
        'sample_2',
        'sample_3',
    )

    FASTQC(input)

    // Create a file in the output directory

    // Write to files for validation
    def output_dir = params.outdir

    // Create output directory
    new File(output_dir).mkdirs()

    // Write citation text
    new File("${output_dir}/test.txt").text = "This is a test file"

    def test_file = file("${output_dir}/test.txt")

    // Print a complex nf-core/pipeline logo (ie SAREK for example)
    log.info(
        """
${colors.dim}----------------------------------------------------${colors.reset}-
                                        ${colors.green},--.${colors.black}/${colors.green},-.${colors.reset}
${colors.blue}        ___     __   __   __   ___     ${colors.green}/,-._.--~\'${colors.reset}
${colors.blue}  |\\ | |__  __ /  ` /  \\ |__) |__         ${colors.yellow}}  {${colors.reset}
${colors.blue}  | \\| |       \\__, \\__/ |  \\ |___     ${colors.green}\\`-._,-`-,${colors.reset}
                                        ${colors.green}`._,._,\'${colors.reset}

${colors.white}      ____${colors.reset}
${colors.white}    .´ _  `.${colors.reset}
${colors.white}   /  ${colors.green}|\\${colors.reset}`-_ \\${colors.reset}     ${colors.blue} __        __   ___     ${colors.reset}
${colors.white}  |   ${colors.green}| \\${colors.reset}  `-|${colors.reset}    ${colors.blue}|__`  /\\  |__) |__  |__/${colors.reset}
${colors.white}   \\ ${colors.green}|   \\${colors.reset}  /${colors.reset}     ${colors.blue}.__| /¯¯\\ |  \\ |___ |  \\${colors.reset}
${colors.white}    `${colors.green}|${colors.reset}____${colors.green}\\${colors.reset}´${colors.reset}
-${colors.dim}----------------------------------------------------${colors.reset}-
nf-core/pipeline1 1.0dev
nf-core/pipeline2 1.0
nf-core/pipeline3 1.1.0dev
nf-core/pipeline4 1.1.0
container: docker
image: singularity
virtualenv: conda
profile: test,docker,singularity,conda
runName[false]: test_run_name
runName[false]: amazing_mercury
runName[false]: [amazing_mercury]
runName[true]: ${workflow.runName}
        """
    )

    // Print various ENVs and PATHs
    log.info("HOME                      : ${System.getenv("HOME")}")
    log.info("NXF_CACHE_DIR             : ${System.getenv("NXF_CACHE_DIR")}")
    log.info("NXF_CONDA_CACHEDIR        : ${System.getenv("NXF_CONDA_CACHEDIR")}")
    log.info("NXF_HOME                  : ${System.getenv("NXF_HOME")}")
    log.info("NXF_SINGULARITY_CACHEDIR  : ${System.getenv("NXF_SINGULARITY_CACHEDIR")}")
    log.info("NXF_SINGULARITY_LIBRARYDIR: ${System.getenv("NXF_SINGULARITY_LIBRARYDIR")}")
    log.info("NXF_TEMP                  : ${System.getenv("NXF_TEMP")}")
    log.info("NXF_WORK                  : ${System.getenv("NXF_WORK")}")
    log.info("userName                  : ${System.getenv("USER")}")
    log.info("outdir                    : ${params.outdir}")

    println("println message")
    log.info("${colors.blue}log.info message${colors.reset}")
    log.warn("${colors.yellow}log.warn message${colors.reset}")
    log.error("${colors.red}log.error message${colors.reset}")

    println("println with path: ${test_file}")
    log.info("${colors.blue}log.info with path: ${test_file}${colors.reset}")
    log.warn("${colors.yellow}log.warn with path: ${test_file}${colors.reset}")
    log.error("${colors.red}log.error with path: ${test_file}${colors.reset}")
    if (params.failure) {
        System.err.println("${colors.bred}System error with path: ${test_file}${colors.reset}")
        System.err.println("${colors.bred}System error message${colors.reset}")
        error("${colors.bred}Error with path: ${test_file}${colors.reset}")
    }
}

def getColors(monochrome_logs = true) {

    def colorsDict = [:]

    // Reset / Meta
    colorsDict['reset'] = monochrome_logs ? '' : "\033[0m"
    colorsDict['bold'] = monochrome_logs ? '' : "\033[1m"
    colorsDict['dim'] = monochrome_logs ? '' : "\033[2m"
    colorsDict['underlined'] = monochrome_logs ? '' : "\033[4m"
    colorsDict['blink'] = monochrome_logs ? '' : "\033[5m"
    colorsDict['reverse'] = monochrome_logs ? '' : "\033[7m"
    colorsDict['hidden'] = monochrome_logs ? '' : "\033[8m"

    // Regular Colors
    colorsDict['black'] = monochrome_logs ? '' : "\033[0;30m"
    colorsDict['red'] = monochrome_logs ? '' : "\033[0;31m"
    colorsDict['green'] = monochrome_logs ? '' : "\033[0;32m"
    colorsDict['yellow'] = monochrome_logs ? '' : "\033[0;33m"
    colorsDict['blue'] = monochrome_logs ? '' : "\033[0;34m"
    colorsDict['purple'] = monochrome_logs ? '' : "\033[0;35m"
    colorsDict['cyan'] = monochrome_logs ? '' : "\033[0;36m"
    colorsDict['white'] = monochrome_logs ? '' : "\033[0;37m"

    // Bold
    colorsDict['bblack'] = monochrome_logs ? '' : "\033[1;30m"
    colorsDict['bred'] = monochrome_logs ? '' : "\033[1;31m"
    colorsDict['bgreen'] = monochrome_logs ? '' : "\033[1;32m"
    colorsDict['byellow'] = monochrome_logs ? '' : "\033[1;33m"
    colorsDict['bblue'] = monochrome_logs ? '' : "\033[1;34m"
    colorsDict['bpurple'] = monochrome_logs ? '' : "\033[1;35m"
    colorsDict['bcyan'] = monochrome_logs ? '' : "\033[1;36m"
    colorsDict['bwhite'] = monochrome_logs ? '' : "\033[1;37m"

    return colorsDict
}
