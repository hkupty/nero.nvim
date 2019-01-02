package nero.controller

import scala.io.StdIn
import java.io.{PrintStream, OutputStream, InputStream}
import nero.flow._


class Input(pipe: Pipe[ReplState, ReplState]) extends Pipe[ReplState, ReplState] {
  def promptStr(mode: Mode): String = mode match {
    case Mode.Lua => " > "
    case Mode.VimL =>" : "
    case Mode.Pending(Mode.Lua) => ">> "
    case Mode.Pending(Mode.VimL) => " | "
    case _ => ""
  }

  def send(state: ReplState): ReplState = {
    val msg = StdIn.readLine(promptStr(state.mode))
    pipe.send(state.append(msg))
  }
}
