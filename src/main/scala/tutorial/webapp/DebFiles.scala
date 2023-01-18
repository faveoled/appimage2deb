package tutorial.webapp

import java.util.Locale
import tutorial.webapp.StringExtensions.substringAfterLast
import tutorial.webapp.StringExtensions.substringAfterFirst
import tutorial.webapp.StringExtensions.substringBeforeFirst
import tutorial.webapp.ExtractionStrategy

object DebFiles {

  def getExecFromDesktop(desktopFileContents: String): Option[String] =
    val i1 =
      desktopFileContents.linesIterator
        .filter(line => line.substringBeforeFirst("=").trim.equalsIgnoreCase("Exec"))
        .map(execLine => execLine.substringAfterFirst("="))
    i1.nextOption()

  def getNameFromDesktop(desktopFileContents: String): Option[String] =
    val i1 =
      desktopFileContents.linesIterator
        .filter(line => line.contains("Name"))
        .map(nameLine => nameLine.replace("Name", "").replace('=', ' ').trim)
    i1.nextOption()

  def processExistingDesktop(desktopFileContents: String, pkgName: String, eStrategy: ExtractionStrategy): String =
    val v1 = 
      desktopFileContents.linesIterator
        .map(line =>
          val eqSignIdx = line.indexOf("=")
          if (eqSignIdx != -1) {
            val beforeSign = line.substringBeforeFirst("=").trim()
            if (beforeSign.equalsIgnoreCase("Exec")) {
              val afterSign = line.substringAfterFirst("=").trim()
              val cmd = buildExecCmd(eStrategy, pkgName, Some(afterSign))
              s"Exec=${cmd}"
            } else if (beforeSign.equalsIgnoreCase("Icon")) {
              s"Icon=${pkgName}"
            } else {
              line
            }
          } else {
            line
          }
        ).mkString("\n")
    v1

  def buildExecCmd(eStrategy: ExtractionStrategy, pkgName: String, existingCommand: Option[String]): String =
    eStrategy match {
      case ExtractionStrategy.Preserve =>
        s"/opt/${pkgName}/${pkgName}.AppImage"
      case ExtractionStrategy.Unpack =>
        if (existingCommand.count(cmd => cmd.contains("AppRun")) == 1) {
          s"/opt/${pkgName}/${existingCommand.get}"
        } else {
          s"/opt/${pkgName}/AppRun"
        }
      }

  def buildDesktopFile(appImageFileName: String, visibleAppName: Option[String], dirNameInOpt: String): String =
    val appImageFileNoExtension =  appImageFileName.substring(0, appImageFileName.lastIndexOf("."))
    Seq(
      "[Desktop Entry]",
      s"Name=${visibleAppName.getOrElse(appImageFileNoExtension)}",
      s"Exec=gnome-terminal -e /opt/${dirNameInOpt}/AppRun --no-sandbox %U",
      "Terminal=true",
      "Type=Application",
    ).mkString("\n")

  def buildControlFile(pkgName: String, installedSize: Int, pkgVersion: String): String =
    Seq(
      s"Package: ${pkgName}",
      s"Version: ${pkgVersion}",
      "Architecture: all",
      "Maintainer: appimage2deb",
      s"Installed-Size: ${installedSize}",
      "Section: Miscellaneous",
      "Priority: optional",
      "Description: Converted from AppImage",
      "  Application archive converted from AppImage",
      "" // without this: end of file during value of field 'Description' (missing final newline)
    ).mkString("\n")
	
	
  def buildPostInst(): String =
    Seq(
      "#!/bin/sh",
      "update-desktop-database",
    ).mkString("\n")

  def buildPostRm(): String =
    buildPostInst()

  def buildExecFile(execCmd: String): String = 
    Seq(
      "#!/bin/sh",
      execCmd + " " +" \"$@\"",
    ).mkString("\n")
}
