package nero.flow

trait Pipe[Req, Resp] {
  def send(msg: Req): Resp
}
