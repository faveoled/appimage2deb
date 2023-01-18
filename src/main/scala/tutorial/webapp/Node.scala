package tutorial.webapp

import typings.node.childProcessMod as node_child_process
import typings.node.utilMod as node_util
import typings.node.fsMod as node_fs
import typings.node.osMod as node_os
import typings.node.pathMod as node_path
import typings.node.bufferMod.global.BufferEncoding

object Node {

  def readFile(filePath: String): String =
    val opts = BufferEncoding.utf8
    val read = node_fs.readFileSync(filePath, opts)
    read

  def writeFile(filePath: String, contents: String): Unit =
    node_fs.writeFileSync(filePath, contents)

  def rename(oldPath: String, newPath: String): Unit = 
    node_fs.renameSync(oldPath, newPath)

  def cp(filePath1: String, filePath2: String): Unit =
    node_fs.copyFileSync(filePath1, filePath2)

  def makeDir(dirPath: String): Unit =
    try {
      if (!node_fs.existsSync(dirPath)) {
        val opts = node_fs.MakeDirectoryOptions()
        opts.recursive  = true
        node_fs.mkdirSync(dirPath, opts)
      }
    } catch {
      case e: Exception =>
        throw Exception(s"Couldn't create a new directory: ${dirPath}", e)
    }

  def makeTmpDir(): String =
    try {
      val tmpDir = node_fs.mkdtempSync(node_path.join(node_os.tmpdir(), "appimage2deb-"));
      tmpDir
    } catch {
      case e: Exception =>
        throw Exception(s"Couldn't create a new temporary directory", e)
    }

  def chmod(filePath: String, mode: Int): Unit =
    try {
      node_fs.chmodSync(filePath, mode.toDouble);
    } catch {
      case e: Exception =>
        throw Exception(s"Couldn't chmod ${filePath} on ${filePath}")
    }

  def filesInDir(dirPath: String): Array[String] =
    val res = node_fs.readdirSync(dirPath)
    Interop.toScalaArray(res)

  def filesInDirRecursive(dirPath: String): List[String] =
    val filesInDirectory = node_fs.readdirSync(dirPath);
    val a1 = 
      filesInDirectory
        .flatMap(file =>
          val nextPath = node_path.resolve(dirPath, file);
          val stats = node_fs.statSync(nextPath).getOrElse(scala.sys.error(s"Couldn't read directory ${nextPath}"))
          if (stats.isDirectory()) {
              filesInDirRecursive(nextPath);
          } else {
              List(nextPath);
          }
        )
        .toList
    a1

  // private def filesInDirRecursiveAux(directory: String, collected: ArrayBuffer[String]): Unit =
  //   val filesInDirectory = node_fs.readdirSync(directory);
  //   filesInDirectory.foreach(file =>
  //     val absolute = node_path.join(directory, file);
  //     if (node_fs.statSync(absolute).isDirectory()) {
  //         filesInDirRecursiveAux(absolute, collected);
  //     } else {
  //         collected.push(absolute);
  //     }
  //   )

  def desktopFileInDir(dirPath: String): Option[String] =
    filesInDir(dirPath)
      .filter(path => path.endsWith(".desktop"))
      .map(node_path.resolve(dirPath, _))
      .headOption

  def appIconInDir(dirPath: String): Option[String] =
    try {
      val linkVal = node_fs.readlinkSync(node_path.resolve(dirPath, ".DirIcon"))
      Some(node_path.resolve(dirPath, linkVal))
    } catch {
      case e: Exception =>
        filesInDir(dirPath)
          .filter(path => path.endsWith(".png") || path.endsWith(".svg"))
          .map(node_path.resolve(dirPath, _))
          .headOption
    }

}
