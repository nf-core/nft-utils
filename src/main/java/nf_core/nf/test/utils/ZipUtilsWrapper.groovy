package nf_core.nf.test.utils

class ZipUtilsWrapper {
    static void unzip(String zipFilePath, String destDirectory) {
        ZipUtils.unzip(zipFilePath, destDirectory)
    }

    static void zip(String sourceDirPath, String zipFilePath) {
        ZipUtils.zip(sourceDirPath, zipFilePath)
    }
}
