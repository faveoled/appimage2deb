package tutorial.webapp

import utest._

object TutorialTest extends TestSuite {

  def tests = Tests {
    test("HelloWorld") {
      assert(1 == 1)
    }

//    test("find offset") {
//      val offset = TutorialApp.findOffset("/home/user/Applications/appimage2deb/Space-Cadet-Pinball-x86-64.AppImage")
//      assert(188392 == offset)
//    }

//    test("make tmp dir") {
//      val tmpDir = TutorialApp.makeTmpDir()
//      assert(true == tmpDir.startsWith("/tmp/appimage2deb-"))
//    }

//    test("make dir") {
//      TutorialApp.makeDir("/tmp/appiamge2deb-newdir")
//      assert(true == true)
//    }

//    test("desktop file") {
//      val file = Node.desktopFileInDir("/tmp/appimage2deb-u3f05H")
//      assert(file.contains("/tmp/appimage2deb-u3f05H/pinball.desktop"))
//    }

    //test("dir icon") {
    //  val file = Node.appIconInDir("/tmp/appimage2deb-u3f05H")
    //  assert(file.contains("/tmp/appimage2deb-u3f05H/pinball.png"))
    //}

    // test("write file and read file") {
    //   Node.writeFile("./src/test/resources/write_test.txt", "123")
    //   val contents = Node.readFile("./src/test/resources/write_test.txt")
    //   assert("123" == contents)
    // }
//

    // test("process existing desktop ") {
    //   Node.readFile("./src/test/resources/input.desktop")
    //   val processed = DebFiles.processExistingDesktop(Node.readFile("./src/test/resources/input.desktop"), "appimage2deb")
    //   val expected= Node.readFile("./src/test/resources/input-out.desktop")
    //   assert(expected == processed)
    // }


    // test("core routine") {
    //   TutorialApp.coreRoutine("/home/user/Applications/appimage2deb/Space-Cadet-Pinball-x86-64.AppImage")
    //   assert(true == true)
    // }

    // test("dir size") {
    //   val dirSize = TutorialApp.dirSize("/home/user/Applications")
    //   assert(615404 == dirSize)
    // }


    // test("files in dir recursive") {
    //   val all = Node.filesInDirRecursive("./src/test/resources/recursive-dirs-test")
    //   assert(3 == all.length)
    //   assert(all(0).contains("/src/test/resources/recursive-dirs-test/16x16/apps/1.png"))
    //   assert(all(1).contains("/src/test/resources/recursive-dirs-test/22x22/apps/2.png"))
    //   assert(all(2).contains("/src/test/resources/recursive-dirs-test/24x24/apps/3.png"))
    // }


    test("parsing test") {
      TutorialApp.main(Array("Notes-x86_64.appimage"))
      assert(true == true)
    }

    // test("core routine") {
    //   TutorialApp.coreRoutine("/home/user/Applications/")
    //   assert(true == true)
    // }
  }
}
