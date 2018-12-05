package nero
import java.net.Socket
import scala.util.control.NoStackTrace
import scala.concurrent.{Promise,Await}
import scala.concurrent.duration._
import com.ensarsarajcic.neovim.java.corerpc.client._
import com.ensarsarajcic.neovim.java.corerpc.message._

sealed trait Lang
case object Lua extends Lang
case object VimL extends Lang

case class NeovimError(message: String) extends NoStackTrace

trait Neovim {
  def send(lang: Lang, cmd: String, args: String*): String
  def close(): Unit
}


object Neovim {
  val timeout = 10.second

  def fromSocket(socket: Socket): Neovim = new Neovim {
    val conn = new TcpSocketRPCConnection(socket)

    val client = {
      val rpcStreamer = RPCClient.getDefaultAsyncInstance()
      rpcStreamer.attach(conn)
      rpcStreamer
    }

    def send(lang: Lang, cmd: String, args: String*): String = {
      val result: Promise[String] = Promise()
      val request = lang match {
        case Lua => new RequestMessage.Builder("nvim_execute_lua")
          .addArgument(cmd)
          .addArgument(args.toArray)
          //.addArgument("luaeval")
          //.addArgument(Seq(cmd).toArray)

        case VimL => new RequestMessage.Builder("nvim_command")
          .addArgument(s"execute '${cmd}'")
      }

      client.send(request, (id, response) => {

        Option(response.getError()).fold{
          result
            .success(Option(response.getResult())
            .map(x => s"[OK  ]   ${x.toString}")
            .getOrElse("[OK  ]"))
          }{err =>
            //TODO: Deal with errors on printing
            //result.failure(NeovimError(err.message))
            result.success(s"[Err ]   ${err.getMessage()}")
          }

      })

      Await.result(result.future, timeout)
    }

    def close(): Unit = {
      conn.close()
      client.stop()
    }
  }

}
