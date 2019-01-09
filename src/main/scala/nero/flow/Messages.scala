package nero.flow

import scala.util.control.NoStackTrace

sealed trait Mode

object Mode {
  case object Closed extends Mode
  case class Pending(mode: Mode) extends Mode
  case object Lua extends Mode
  case object VimL extends Mode

  def fromString(str: String): Mode = str match {
    case "lua" => Lua
    case "viml" => VimL
  }
}

case class ReplMessage(command: String, language: Mode)
case class ReplState(buffer: Vector[String], history: Vector[String], mode: Mode) {
  def withMode(newMode: Mode): ReplState = this.copy(mode = newMode)
  def append(msg: String): ReplState = this.copy(buffer = buffer :+ msg)
  def cleanBuffer(): ReplState = this.copy(buffer = Vector[String]())
}

object ReplState {
  def empty: ReplState = ReplState(Vector(), Vector(), Mode.Lua)
}

trait ResponseOwner
case object NeroResponseOwner extends ResponseOwner

trait ReplResponse {
  def owner: ResponseOwner
}

trait NeroReplResponse extends ReplResponse {
  def owner: ResponseOwner = NeroResponseOwner
}

object ReplResponse {
  case object Pending extends NeroReplResponse
  case class Message(message: String) extends NeroReplResponse
  case class Error(message: String) extends NoStackTrace with NeroReplResponse
  case class ChangeMode(lang: Mode) extends NeroReplResponse
  case object Close extends NeroReplResponse
}
