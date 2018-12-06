package nero
import java.net.Socket
import scala.util.control.NoStackTrace
import scala.util.{Success, Failure}
import scala.concurrent.{Promise, Await, Future, ExecutionContext}
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
  implicit val ec = ExecutionContext.global
  val timeout = 120.second

  def fromSocket(socket: Socket): Neovim = new Neovim {
    val conn = new TcpSocketRPCConnection(socket)

    val client = {
      val rpcStreamer = RPCClient.getDefaultAsyncInstance()
      rpcStreamer.attach(conn)
      rpcStreamer
    }

    def send(lang: Lang, cmd: String, args: String*): String = {
      type MarkSuccess[A] = (Promise[A], Object) => Unit
      type MarkFailure[A] = (Promise[A], RPCError) => Unit

      def run(success: MarkSuccess[String], failure: MarkFailure[String]): RequestMessage.Builder => Future[String] = { request =>
        val result: Promise[String] = Promise()

        def handle(id: Int, response: ResponseMessage) ={
            Option(response.getError()).fold(success(result, response.getResult()))(f => failure(result, f))
        }

        client.send(request, handle)

        result.future
      }

      val request: RequestMessage.Builder = lang match {
        case Lua => new RequestMessage.Builder("nvim_execute_lua")
          .addArgument(s"return ${cmd}")
          .addArgument(args.toArray)

        case VimL => new RequestMessage.Builder("nvim_command")
          .addArgument(s"execute '${cmd}'")
      }

      val success: MarkSuccess[String] = (p, obj) => p.success(
        Option(obj)
          .fold("[OK  ]")(v => s"[OK  ]   ${v.toString}")
      )

      val failure: MarkFailure[String] = lang match {
        case Lua => { (p, err) => p.failure(NeovimError(err.getMessage())) }
        case _ => { (p, err) => p.success(s"[Err ]   ${err.getMessage()}") }
      }

     val resolve: PartialFunction[Throwable, Future[String]] = lang match{
       case Lua => {
         case err => {
          val newMsg: RequestMessage.Builder = new RequestMessage.Builder("nvim_execute_lua")
            .addArgument(cmd)
            .addArgument(args.toArray)

            run(success, failure)(newMsg)
         }
       }
       case _ => {
         case err => Future.successful(s"[Err ]   ${err.getMessage()}")
       }
     }

      Await.result(
        run(success, failure)(request) recoverWith resolve,
        timeout
      )
    }

    def close(): Unit = {
      conn.close()
      client.stop()
    }
  }

}
