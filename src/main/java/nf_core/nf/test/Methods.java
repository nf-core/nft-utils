package nf_core.nf.test.utils;

public class Methods {
  
    // Recursively list all files in a directory and its sub-directories, matching or not matching supplied regexes
    public static getAllFilesFromDir(dir, List<String> includeRegexes = null, List<String> excludeRegexes = null) {
        def output = []
        new File(dir).eachFileRecurse() { file ->
            boolean matchesInclusion = (includeRegexes == null || includeRegexes.any { regex -> file.name.toString() ==~ regex })
            boolean matchesExclusion = (excludeRegexes == null || !excludeRegexes.any { regex -> file.name.toString() ==~ regex })

            if (matchesInclusion && matchesExclusion) {
                output.add(file)
            }
        }
        return output.sort { it.name }
    }

    // Static (global) things useful for supplying to getAllFilesFromDir()
    static List<String> unstableFilenamesRegex = [/.*\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}.*/]  // e.g. date strings
    static List<String> plainTextFormatsRegex = [/.*\.(txt|json|tsv)$/]                       // Common plain text formats
}
