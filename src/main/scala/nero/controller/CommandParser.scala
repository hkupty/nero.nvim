package nero.controller

import nero.flow.{Pipe, ReplMessage, ReplState, ReplResponse, Mode}

class  CommandParser(pipe: Pipe[ReplState, ReplResponse]) extends Pipe[ReplState, ReplResponse] {

  def send(state: ReplState): ReplResponse = {
    val ln = state.buffer.last.split(" ")
    val command = ln.head
    val args = ln.tail

    command match {
      case ":set" => ReplResponse.ChangeMode(Mode.fromString(args(0)))
      case ":q" => ReplResponse.Close
      // TODO Make debug disappear from buffer and proceed
      case ":debug" => ReplResponse.Message(state.toString)
      case typo if typo.startsWith(":") => ReplResponse.Error(s"'$typo' is not a nero command")
      case _ => pipe.send(state)
    }
  }
}
