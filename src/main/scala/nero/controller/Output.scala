package nero.controller

import java.io.{PrintStream, OutputStream, InputStream}
import nero.flow._


class Output(pipe: Pipe[ReplState, ReplResponse]) extends Pipe[ReplState, ReplResponse] {
  val ps: PrintStream = new PrintStream(System.out)

  def responseStr(resp: ReplResponse): Option[String] = resp match {
    case NeovimMessage(msg) => Some(msg)
    case NeovimError(msg) => Some(s"(fail) ${msg}")
    case ReplResponse.Error(msg) => Some(s"(error) ${msg}")
    case ReplResponse.Message(msg) => Some(s"(debug) ${msg}")
    case _ => None
  }

  def send(state: ReplState): ReplResponse = {
    val resp = pipe.send(state)

    responseStr(resp).foreach(ps.println)

    resp
  }
}
