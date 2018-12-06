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
      def run(success: Object => String, failure: RPCError => String): RequestMessage.Builder => String = { request =>
        val result: Promise[String] = Promise()

        def handle(id: Int, response: ResponseMessage) ={
          result.success(
            Option(response.getError())
              .fold(success(response.getResult()))(failure)
            )
        }

        client.send(request, handle)
        Await.result(result.future, timeout)
      }

      val request: RequestMessage.Builder = lang match {
        case Lua => new RequestMessage.Builder("nvim_execute_lua")
          .addArgument(s"return ${cmd}")
          .addArgument(args.toArray)

        case VimL => new RequestMessage.Builder("nvim_command")
          .addArgument(s"execute '${cmd}'")
      }

      val success: Object => String = obj => Option(obj).fold("[OK ]")(v => s"[OK  ]   ${v.toString}")
      val defaultFailure: RPCError => String = err => {
        System.out.println(err)
        s"[Err ]   ${err.getMessage()}"
      }

      val failure: RPCError => String = lang match {
        case Lua => { err =>

          val newMsg: RequestMessage.Builder = new RequestMessage.Builder("nvim_execute_lua")
            .addArgument(cmd)
            .addArgument(args.toArray)

            // run(success, defaultFailure)(newMsg)
            ""


        }
        case _ => defaultFailure
      }

      run(success, failure)(request)
    }

    def close(): Unit = {
      conn.close()
      client.stop()
    }
  }

}
