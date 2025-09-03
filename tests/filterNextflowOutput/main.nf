include { FASTQC } from './modules/local/fastqc'

params.failure = false
params.outdir  = "results"
workflow {
  input = channel.of(
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

  log.info(
    """
\033[2m----------------------------------------------------\033[0m-
                                        \033[0;32m,--.\033[0;30m/\033[0;32m,-.\033[0m
\033[0;34m        ___     __   __   __   ___     \033[0;32m/,-._.--~\'\033[0m
\033[0;34m  |\\ | |__  __ /  ` /  \\ |__) |__         \033[0;33m}  {\033[0m
\033[0;34m  | \\| |       \\__, \\__/ |  \\ |___     \033[0;32m\\`-._,-`-,\033[0m
                                        \033[0;32m`._,._,\'\033[0m

\033[0;37m      ____\033[0m
\033[0;37m    .´ _  `.\033[0m
\033[0;37m   /  \033[0;32m|\\\033[0m`-_ \\\033[0m     \033[0;34m __        __   ___     \033[0m
\033[0;37m  |   \033[0;32m| \\\033[0m  `-|\033[0m    \033[0;34m|__`  /\\  |__) |__  |__/\033[0m
\033[0;37m   \\ \033[0;32m|   \\\033[0m  /\033[0m     \033[0;34m.__| /¯¯\\ |  \\ |___ |  \\\033[0m
\033[0;37m    `\033[0;32m|\033[0m____\033[0;32m\\\033[0m´\033[0m
-\033[2m----------------------------------------------------\033[0m-
nf-core/pipeline1: 1.0dev
nf-core/pipeline2: 1.0
nf-core/pipeline3: 1.1.0dev
nf-core/pipeline4: 1.1.0
        """
  )

  log.info("outdir: ${params.outdir}")
  log.info("HOME: ${System.getenv("HOME")}")
  log.info("NFT_WORKDIR: ${System.getenv("NFT_WORKDIR")}")
  log.info("NXF_CACHE_DIR: ${System.getenv("NXF_CACHE_DIR")}")
  log.info("NXF_CONDA_CACHEDIR: ${System.getenv("NXF_CONDA_CACHEDIR")}")
  log.info("NXF_HOME: ${System.getenv("NXF_HOME")}")
  log.info("NXF_SINGULARITY_CACHEDIR: ${System.getenv("NXF_SINGULARITY_CACHEDIR")}")
  log.info("NXF_TEMP: ${System.getenv("NXF_TEMP")}")
  log.info("NXF_WORK: ${System.getenv("NXF_WORK")}")

  println("println message")
  log.info("\033[0;34mlog.info message\033[0m")
  log.warn("\033[0;33mlog.warn message\033[0m")
  log.error("\033[0;31mlog.error message\033[0m")

  println("println with path: ${test_file}")
  log.info("\033[0;34mlog.info with path: ${test_file}\033[0m")
  log.warn("\033[0;33mlog.warn with path: ${test_file}\033[0m")
  log.error("\033[0;31mlog.error with path: ${test_file}\033[0m")
  if (params.failure) {
    System.err.println("\033[1;31mSystem error with path: ${test_file}\033[0m")
    System.err.println("\033[1;31mSystem error message\033[0m")
    error("\033[1;31mError with path: ${test_file}\033[0m")
  }
}
