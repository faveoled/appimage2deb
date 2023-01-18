package tutorial.webapp

import typings.node.processMod.global.process
import typings.node.childProcessMod as node_child_process
import typings.node.utilMod as node_util
import typings.node.fsMod as node_fs
import typings.node.osMod as node_os
import typings.node.pathMod as node_path
import typings.node.bufferMod.global.Buffer
import typings.node.bufferMod.global.BufferEncoding
import typings.node.childProcessMod.ExecSyncOptionsWithStringEncoding
import typings.node.fsMod.MakeDirectoryOptions

import tutorial.webapp.StringExtensions.substringAfterLast
import tutorial.webapp.StringExtensions.substringAfterFirst
import tutorial.webapp.StringExtensions.substringBeforeFirst
import tutorial.webapp.StringExtensions.substringBeforeLast

import scala.util.{Try, Success, Failure}

import org.rogach.scallop._


class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {

  banner("""Usage: appimage2deb [OPTIONS] <path-to-appimage>
           |appimage2deb is a CLI utility for converting AppImage files to deb packages
           |Options:
           |""".stripMargin)

  val pkgName = opt[String](
    name = "pkg-name",
    noshort = true,
    required = false,
    descr = "Specifies output debian package name. Should consist of lowercase symbols and '-'.",
    validate = (_.length >= 2)
  )
  val pkgVersion = opt[String](
    name = "pkg-version",
    noshort = true,
    default = Some("1.0.0"),
    required = false,
    descr = "Specifies output debian package version. Usually looks like three numbers separated by '.'.",
    validate = (_.length > 0)
  )
  val debug = opt[Boolean](
    name = "debug",
    noshort = true,
    descr = "Enables debugging mode if specified. Enables detailed stacktraces for errors."
  )
  val unpack = opt[Boolean](
    name = "unpack",
    noshort = true,
    descr = "Enables AppImage unpacking if specified. AppImage is written to deb package as-is if not specified."
  )
  val path = trailArg[String](
    name = "path-to-appimage",
    required=true,
    validate = (_.length > 0)
  )
  verify()
}

object TutorialApp {

  val rights775 = 509

  def main(unused: Array[String]): Unit =
    val dropCountNode = process.env.get("APPIMAGE2DEB_DROP_COUNT")
    val dropCount = dropCountNode.flatMap(undefOr => undefOr.toOption).getOrElse("2")
    val args = Interop.toScalaArray(process.argv.drop(dropCount.toInt))
    val cliConf = new Conf(args)
    try {
      val appImagePath = node_path.resolve(cliConf.path())
      if (!node_fs.existsSync(appImagePath)) {
        throw Exception(s"File not found: ${appImagePath}")
      }
      val extrStrategy = 
        if (cliConf.unpack()) then ExtractionStrategy.Unpack else ExtractionStrategy.Preserve
      coreRoutine(cliConf.pkgName.toOption, cliConf.pkgVersion.toOption, appImagePath, extrStrategy)
      return ()
    } catch {
      case e: Exception =>
        if (cliConf.debug()) {
          Console.err.println("Execution failed. Error details:")
          e.printStackTrace(Console.err)
        } else {
          Console.err.println(s"Execution failed. Error details: ${e.getMessage()}")
        }
        return ()
    }


  def coreRoutine(pkgNameCmd: Option[String], pkgVersionCmd: Option[String], 
                  appImagePath: String, extrStrategy: ExtractionStrategy): Unit =

    val pkgVersion = pkgVersionCmd.getOrElse("1.0.0")

    val appImageDirPath = appImagePath.substringBeforeLast("/")
    val appImageFileName = appImagePath.substringAfterLast("/")
    val appImageFileNameNoExtension = appImageFileName.substringBeforeLast(".")
    def pkgNameInferred(): String = 
      appImageFileNameNoExtension.replace('.', '-').replace(' ', '-').replace('_', '-').toLowerCase()
    val pkgName = pkgNameCmd.getOrElse(default = pkgNameInferred())

    val unpackDir = Node.makeTmpDir()
    val offset = findOffset(appImagePath)
    extractAppImage(offset, unpackDir, appImagePath)

    val debSrc = Node.makeTmpDir()
    Node.makeDir(node_path.resolve(debSrc, "opt"))
    Node.makeDir(node_path.resolve(debSrc, "usr/share/applications"))

    val iconFilePath = Node.appIconInDir(unpackDir)
    val desktopFilePath = Node.desktopFileInDir(unpackDir)

    val desktopApp = desktopFilePath.isDefined

    val desktopContents = desktopFilePath.map(Node.readFile(_))


    val processedDesktopFile = desktopContents.map(arg =>
      DebFiles.processExistingDesktop(arg, pkgName, extrStrategy)
    )
      
    val execCmd = 
      processedDesktopFile.flatMap(arg =>
        DebFiles.getExecFromDesktop(arg)
      ).getOrElse(DebFiles.buildExecCmd(extrStrategy, pkgName, Option.empty))


    processedDesktopFile.foreach(a =>
      Node.writeFile(node_path.resolve(debSrc, s"usr/share/applications/${pkgName}.desktop"), a)
    )

    Node.makeDir(node_path.resolve(debSrc, "usr/bin"))
    Node.writeFile(node_path.resolve(debSrc, s"usr/bin/${pkgName}"), DebFiles.buildExecFile(execCmd))
    Node.chmod(node_path.resolve(debSrc, s"usr/bin/${pkgName}"), rights775)

    copyToTargetDebDir(unpackDir, debSrc, pkgName, appImagePath, extrStrategy)

    val iconsDirInUnpacked = node_path.resolve(unpackDir, "usr/share/icons")
    val iconsDirDeb = s"${debSrc}/usr/share/icons"
    if (node_fs.existsSync(iconsDirInUnpacked)) {
      runProcess(s"cp -a \"${iconsDirInUnpacked}\" \"${iconsDirDeb}\"", "Copying icons")
      val icons = Node.filesInDirRecursive(iconsDirDeb)
      icons.foreach(iconPath =>
        val containingDir = iconPath.substringBeforeLast("/")
        val oldFileName = iconPath.substringAfterLast("/")
        val extension = oldFileName.substringAfterLast(".")
        val newFileName = s"${pkgName}.${extension}"
        Node.rename(iconPath, containingDir + "/" + newFileName)
      )
    }

    iconFilePath.foreach(iconPath =>
      val extension = iconPath.substringAfterLast(".")
      val dirPart = 
        if (iconPath.substringAfterLast(".").equalsIgnoreCase("svg")) "scalable" else "128x128"
      Node.makeDir(node_path.resolve(debSrc, s"usr/share/icons/hicolor/${dirPart}/apps"))
      Node.cp(iconPath, node_path.resolve(debSrc, s"usr/share/icons/hicolor/${dirPart}/apps/${pkgName}.${extension}"))
    )

    val installedSize = dirSize(debSrc)
    val controlFile = DebFiles.buildControlFile(pkgName, installedSize, pkgVersion)

    val debianControlDir = node_path.resolve(debSrc, "DEBIAN")
    Node.makeDir(debianControlDir)
    Node.writeFile(node_path.resolve(debianControlDir, "control"), controlFile)
    Node.writeFile(node_path.resolve(debianControlDir, "postinst"), DebFiles.buildPostInst())
    Node.writeFile(node_path.resolve(debianControlDir, "postrm"), DebFiles.buildPostRm())

    Node.chmod(node_path.resolve(debianControlDir, "postinst"), rights775)
    Node.chmod(node_path.resolve(debianControlDir, "postrm"), rights775)

    runProcess(s"fakeroot dpkg-deb -b \"${debSrc}\" \"${appImageDirPath}/${pkgName}.deb\"", "Building deb package")

    println(s"Result package has been written to ${appImageDirPath}/${pkgName}.deb")

  def dirSize(dirPath: String): Int =
    val size = 
      runProcess(s"du -s ${dirPath}", "Calculating package size")
      .linesIterator
      .nextOption()
      .flatMap(firstNumInString)
    if (size.isEmpty) {
      println("WARNING: couldn't find the installed size of the deb archive")
      10000
    } else {
      size.get
    }

  def copyToTargetDebDir(mountDir: String, debSrc: String, pkgName: String, 
                         appImagePath: String,
                         extrStrategy: ExtractionStrategy): Unit =
    var err = Try[Unit]
    extrStrategy match {
      case ExtractionStrategy.Preserve =>
        Node.makeDir(s"${debSrc}/opt/${pkgName}")
        Node.cp(appImagePath, s"${debSrc}/opt/${pkgName}/${pkgName}.AppImage")
        Try(Node.chmod(s"${debSrc}/opt/${pkgName}/${pkgName}.AppImage", rights775)) match {
          case Failure(e) =>
            println(e)
          case Success(value) => 
            ()
        }

      case ExtractionStrategy.Unpack =>
        runProcess(s"cp -a \"${mountDir}\" \"${debSrc}/opt/${pkgName}\"", "Unpacking application archive")
        Try(Node.chmod(s"${debSrc}/opt/${pkgName}/AppRun", rights775)) match {
          case Failure(e) =>
            println(e)
          case Success(value) => 
            ()
        }
    }

  def runProcess(command: String, details: String = ""): String =
    val msg = if (details == "") then s"Running command `${command}`" else s"${details}"
    println(s"${msg}...")
    val options = ExecSyncOptionsWithStringEncoding(BufferEncoding.utf8)
    val output = node_child_process.execSync(command, options)
    println(s"Done: ${details}")
    output


  def findOffset(filePath: String): Int =
    val output = runProcess(s"readelf -h ${filePath}", "Reading source AppImage")
    val outputKeyVals =
      output.linesIterator
      .filter(line => line.contains(":"))
      .map(line =>
        val colonIdx = line.indexOf(':')
        (line.substring(0, colonIdx).trim, line.substring(colonIdx + 1).trim))
      .toMap
    val v1: String = outputKeyVals.getOrElse("Start of section headers",
                                     scala.sys.error("Start of section headers not found"))
    val v2: String = outputKeyVals.getOrElse("Size of section headers",
                                     scala.sys.error("Size of section headers not found"))
    val v3: String = outputKeyVals.getOrElse("Number of section headers",
                                     scala.sys.error("Number of section headers not found"))
    val resultNums =
      Seq(v1, v2, v3)
      .map(str => firstNumInString(str).getOrElse(scala.sys.error(s"Couldn't get int in: ${str}")))
    resultNums.head + (resultNums(1) * resultNums(2))

  def firstNumInString(input: String): Option[Int] =
    val numPattern = """\d+(\.\d*)?|\.\d+""".r
    numPattern.findFirstIn(input).flatMap(_.toIntOption)


  def extractAppImage(offset: Int, destDirPath: String, appImagePath: String): Unit =
    runProcess(s"unsquashfs -force -offset ${offset} -dest ${destDirPath} ${appImagePath}", "Extracting source AppImage")

}
