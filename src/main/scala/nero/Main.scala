package nero

import scala.util.control.TailCalls._
import scala.util.control.NoStackTrace
import java.io.{PrintStream, OutputStream, InputStream}
import scala.io.StdIn
import java.net.Socket


object REPL {
  case class ReplState(nvim: Neovim, currentMode: Lang)
  case class NeroError(val message: String) extends NoStackTrace with ReplResponse

  def replToString(msg: ReplResponse): String = msg match {
    case NeovimNop => "[NOP ]"
    case NeovimMessage(msg) => s"[OK  ] $msg"
    case NeovimError(msg) => s"[FAIL] $msg"
    case NeroError(msg) => s"[ERR ] $msg"
  }
  val ps: PrintStream = new PrintStream(System.out)
  def out(msg: ReplResponse) = ps.println(replToString(msg))

  def reload(pkg: String): String = s"""
  | for pkg, _ in pairs(package.loaded) do
  |   if pkg:sub(${pkg.size}) == "${pkg}" then
  |     package.loaded[pkg] = nil
  |   end
  | end""".stripMargin('|')


  def handleCommands(state: ReplState, args: String*): TailRec[ReplState] = {
    val cmd :: arguments = args.toList
    val recur: ReplState => TailRec[ReplState] = s => tailcall(repl(s))

    cmd match {
      case x if Set(":q", ":q!", ":quit", ":quit!", ":wq", ":x:") contains x => done(state)
      case ":reload" => {
        val pkg :: _ = arguments
        out(state.nvim.send(Lua, reload(pkg)))
        recur(state)
      }
      case ":set" => {
        val lang :: _ = arguments
        lang match {
          case "lua" => recur(state.copy(currentMode = Lua))
          case "viml" => recur(state.copy(currentMode = VimL))
        }
      }
      case x => {
        out(NeroError(s"'${x}' is not a repl command"))
        recur(state)
      }
    }
  }


  final def repl(state: ReplState): TailRec[ReplState] = {
    val prompt = state.currentMode match {
      case Lua =>  "[Lua ] > "
      case VimL => "[VimL] : "
    }

    StdIn.readLine(prompt) match {
      case cmd if cmd.startsWith(":") => handleCommands(state, cmd.split(" "):_*)
      case cmd => tailcall{
        out(state.nvim.send(state.currentMode, cmd))
        repl(state)
      }
    }
  }

  def run(nvim: Neovim, currentMode: Lang = Lua): ReplState = repl(ReplState(nvim, currentMode)).result

}

object Main {
  def main(args: Array[String]): Unit = {
    val nvim = Neovim.fromSocket(new Socket("127.0.0.1", 12345))

    REPL.run(nvim)

    nvim.close()
  }
}
