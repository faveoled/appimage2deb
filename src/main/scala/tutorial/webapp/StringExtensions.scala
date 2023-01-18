package tutorial.webapp

object StringExtensions {
  extension (str: String) {

    def substringAfterFirst(part: String) =
      str.indexOf(part) match { 
        case -1 => ""; 
        case i => str.substring(i + part.length)
      }

    def substringAfterLast(part: String) =
      str.lastIndexOf(part) match { 
        case -1 => ""; 
        case i => str.substring(i + part.length)
      }

    def substringBeforeFirst(part: String) =
      str.indexOf(part) match { 
        case -1 => ""; 
        case i => str.substring(0, i)
      }

    def substringBeforeLast(part: String) =
      str.lastIndexOf(part) match { 
        case -1 => ""; 
        case i => str.substring(0, i)
      }

  }
}
