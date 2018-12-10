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

trait ReplResponse {
  val message: String
}

case class NeovimError(val message: String) extends NoStackTrace with ReplResponse
case class NeovimMessage(val message: String) extends ReplResponse
case object NeovimNop extends ReplResponse {
  val message: String = ""
}

trait Neovim {
  def send(lang: Lang, cmd: String, args: String*): ReplResponse
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

    def send(lang: Lang, cmd: String, args: String*): ReplResponse = {
      type MarkSuccess[A] = (Promise[A], Object) => Unit
      type MarkFailure[A] = (Promise[A], RPCError) => Unit

      def run(success: MarkSuccess[ReplResponse], failure: MarkFailure[ReplResponse]): RequestMessage.Builder => Future[ReplResponse] = { request =>
        val result: Promise[ReplResponse] = Promise()

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

      val success: MarkSuccess[ReplResponse] = (p, obj) => p.success(Option(obj)
        .fold[ReplResponse](NeovimNop)(v => NeovimMessage(v.toString))
      )

      val handleFailure: MarkFailure[ReplResponse] = (p, err) => p.success(NeovimError(err.getMessage())) 

      val failure: MarkFailure[ReplResponse] = lang match {
        case Lua => { (p, err) => p.failure(NeovimError(err.getMessage())) }
        case _ => handleFailure
      }

     val resolve: PartialFunction[Throwable, Future[ReplResponse]] = 
       lang match{
       case Lua => {
         case err => {
          val newMsg: RequestMessage.Builder = new RequestMessage.Builder("nvim_execute_lua")
            .addArgument(cmd)
            .addArgument(args.toArray)

            run(success, handleFailure)(newMsg)
         }
       }
       case _ => {
         case err => Future.successful(NeovimError(err.getMessage()))
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
