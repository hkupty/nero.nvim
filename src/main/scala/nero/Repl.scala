package nero

import java.net.Socket
import scala.util.control.TailCalls._
import nero.flow.{ReplState, Mode}
import nero.controller.{CommandParser, ControlParser, Output, Input, State}
import nero.controller.backend.Neovim

/* layers
 * ------
 *
 *
 *  Input
 *  -----
 *  State Management
 *  -----
 *  Output
 *  -----
 *  REPL controller
 *  -----
 *  command parser
 *  -------
 *  backend
 *  -------
 *  
 */

class Repl {

  val backend = new Neovim(new Socket("127.0.0.1", 12345))
  val control = new ControlParser(backend)
  val command = new CommandParser(control)
  val out = new Output(command)
  val state = new State(out)
  val input = new Input(state)

  final def repl(state: ReplState): TailRec[ReplState] = {
    val newState = input.send(state)

    if (newState.mode == Mode.Closed) {
      backend.close()

      done(newState)
    } else {
      tailcall(repl(newState))
    }
  }

  def run(): ReplState = repl(ReplState.empty).result
}
