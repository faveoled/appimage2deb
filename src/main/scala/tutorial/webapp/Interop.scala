package tutorial.webapp

import scala.scalajs.js

object Interop {

  def toScalaArray(input: js.typedarray.Uint8Array): Array[Byte] =
    input.view.map(_.toByte).toArray

  def toScalaArray(input: js.Array[Double]): Array[Byte] =
    input.view.map(_.toByte).toArray

  def toScalaArray(input: js.Array[String]): Array[String] =
    input.view.toArray

}
