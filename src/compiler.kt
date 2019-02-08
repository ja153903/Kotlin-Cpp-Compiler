import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

private fun containsQuestionNumber(filename: String, questionNum: Int): Boolean {
    return filename.matches(Regex(".*(q|Q|question|QUESTION|quest)$questionNum.*"))
}

private fun isCppFile(filename: String): Boolean {
    return filename.matches(Regex(".*[.]c.*"))
}

@Throws(IOException::class)
private fun addToFile(line: String, pathToFile: Path) {
    try {
        if (line != "#include \"pch.h\"") {
            val formattedLine = "$line\n"
            when (Files.exists(pathToFile)) {
                true -> Files.write(pathToFile, formattedLine.toByteArray(), StandardOpenOption.APPEND)
                false -> Files.write(pathToFile, formattedLine.toByteArray(), StandardOpenOption.CREATE)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

@Throws(IOException::class)
private fun handlePCH(dir: String, filename: String) {
    val copyFile = "copy_$filename"
    val pathToCopyFile = Paths.get(dir, copyFile)
    val pathToFile = Paths.get(dir, filename)

    val linesRead = Files.readAllLines(pathToFile, StandardCharsets.ISO_8859_1)
    linesRead.forEach { line -> addToFile(line, pathToCopyFile) }

    val pathToCopyFileObj = File(pathToCopyFile.toString())
    val pathToFileObj = File(pathToFile.toString())

    when (pathToCopyFileObj.renameTo(pathToFileObj)) {
        true -> println("renaming $filename succeeded")
        false -> println("renaming $filename failed")
    }
}

@Throws(IOException::class)
private fun getStudentSubmissions(studentPaths: List<String>, questionNum: Int): Map<String, String> {
    val studentSubmissions = mutableMapOf<String, String>()

    for (path in studentPaths) {
        val cppFiles = File(path).listFiles()
        for (file in cppFiles) {
            val filename = file.name
            if (containsQuestionNumber(filename, questionNum) && isCppFile(filename)) {
                if (!filename.startsWith("._")) {
                    handlePCH(path, filename)
                    studentSubmissions[path] = filename
                }
            }
        }
    }

    return studentSubmissions.toMap()
}

@Throws(IOException::class)
private fun compile(studentSubmission: Map<String, String>, questionNum: Int, compiler: String) {
    for (absolutePath in studentSubmission.keys) {
        val filename = studentSubmission[absolutePath]
        val pathToFile = "$absolutePath$filename"
        val commandPath = listOf(compiler,
            "-o", "$absolutePath${filename?.substring(0, 3)}_$questionNum",
            pathToFile)

        ProcessBuilder(commandPath).start()
    }
}

@Throws(IOException::class)
fun runPipeline(homeDirectory: String, questionNum: Int, compiler: String = "clang++") {
    val homedir = File(homeDirectory)
    val studentPaths = ArrayList<String>()

    for (file in homedir.listFiles()) {
        if (!file.name.startsWith("._")) {
            studentPaths.add("${file.absolutePath}/Submission attachment(s)/")
        }
    }

    val studentSubmissions = getStudentSubmissions(studentPaths, questionNum)
    compile(studentSubmissions, questionNum, compiler)
}

fun main() {
    val homeDirectory = "/Volumes/Samsung_T5/nyu/cs-bridge/winter-extended-2019/homework #3/"
    for (i in 1..6) {
        runPipeline(homeDirectory, i)
    }
}