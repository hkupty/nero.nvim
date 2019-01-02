package nero.controller.backend

import nero.flow._
import Mode._
import java.net.Socket
import scala.util.control.NoStackTrace
import scala.util.{Success, Failure}
import scala.concurrent.{Promise, Await, Future, ExecutionContext}
import scala.concurrent.duration._
import com.ensarsarajcic.neovim.java.corerpc.client._
import com.ensarsarajcic.neovim.java.corerpc.message._


trait NeovimErrorHandler {
  lazy val alwaysSuccessful: PartialFunction[Throwable, Future[ReplResponse]] = {
    case NeovimError(err) => Future.successful(NeovimError(err))
  }
}

class NeovimBackend(client: RPCClient) {
  val timeout = 120.second

  def close(): Unit = {
    client.stop()
  }

  private def handle(p: Promise[ReplResponse])(id: Int, response: ResponseMessage): Unit = (Option(response.getError()), Option(response.getResult()))  match {
      case (None, None) => p.success(NeovimNop)
      case (Some(err), _) => p.failure(NeovimError(err.getMessage()))
      case (None, Some(msg)) => p.success(NeovimMessage(msg.toString))
    }

  def run(msg: RequestMessage.Builder): Future[ReplResponse] = {
    val result: Promise[ReplResponse] = Promise()
    client.send(msg, handle(result))
    result.future
  }

  def sendToNeovim(payload: => Future[ReplResponse]): ReplResponse = Await.result(payload, timeout)
}

object NeovimBackend {
   def fromSocketConnection(conn: TcpSocketRPCConnection): NeovimBackend = {
      val rpcStreamer = RPCClient.getDefaultAsyncInstance()
      rpcStreamer.attach(conn)

      new NeovimBackend(rpcStreamer)
    }
}


class Neovim(socket: Socket) extends Pipe[ReplMessage, ReplResponse] {
  implicit val ec = ExecutionContext.global

  val conn = new TcpSocketRPCConnection(socket)
  val backend = NeovimBackend.fromSocketConnection(conn)

  lazy val luaBackend = new NeovimLua(backend)
  lazy val vimLBackend = new NeovimVimL(backend)

  def close(): Unit = {
    conn.close()
    backend.close()
  }

  def send(msg: ReplMessage): ReplResponse = msg match {
    case ReplMessage(command, Lua) => luaBackend.send(command)
    case ReplMessage(command, VimL) => vimLBackend.send(command)
  }

}
