package nero

import scala.util.control.TailCalls._
import java.io.{PrintStream, OutputStream, InputStream}
import scala.io.StdIn
import java.net.Socket


trait REPL {
  case class ReplState(nvim: Neovim, currentMode: Lang)

  def handleCommands(state: ReplState, args: String*): TailRec[ReplState] = {
    val cmd :: arguments = args.toList
    val recur: ReplState => TailRec[ReplState] = s => tailcall(repl(s))

    cmd match {
      case x if Set(":q", ":q!", ":quit", ":quit!", ":wq", ":x:") contains x => done(state)
      case ":set" => {
        val lang :: _ = arguments
        lang match {
          case "lua" => recur(state.copy(currentMode = Lua))
          case "viml" => recur(state.copy(currentMode = VimL))
        }
      }
      case x => {
        ps.println(s"[Err ]   '$x' is not a repl command")
        recur(state)
      }
    }
  }

  val ps: PrintStream = new PrintStream(System.out)

  final def repl(state: ReplState): TailRec[ReplState] = {
    val prompt = state.currentMode match {
      case Lua =>  "[Lua ] > "
      case VimL => "[VimL] : "
    }

    StdIn.readLine(prompt) match {
      case cmd if cmd.startsWith(":") => handleCommands(state, cmd.split(" "):_*)
      case cmd => tailcall{
        val resp = state.nvim.send(state.currentMode, cmd)
        ps.println(resp)
        repl(state)
      }
    }
  }

}

object Main extends REPL {
  def main(args: Array[String]): Unit = {
    val nvim = Neovim.fromSocket(new Socket("127.0.0.1", 12345))

    repl(ReplState(nvim, Lua)).result

    nvim.close()
  }
}
