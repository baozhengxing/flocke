package org.purang.net.flocke.server

import org.http4s._
import org.http4s.blaze._
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.blaze.websocket.WebSocketSupport
import org.http4s.blaze.channel.nio2.NIO2ServerChannelFactory
import org.http4s.blaze.channel.SocketConnection

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.nio.channels.AsynchronousSocketChannel
import org.http4s.{HttpService, Status, Request}
import org.http4s.middleware.GZip
import org.purang.net.flocke.Flocke

class FlockeServer(addr: InetSocketAddress) extends StrictLogging {

  private val pool = Executors.newCachedThreadPool()

  implicit val flocke  = Flocke()
  val base64 = new FlockeTextBase64Route().service
  val plain = new FlockeTextPlainRoute().service

  val service: HttpService =  { case req: Request =>
    val uri = req.requestUri.path
    if (uri.endsWith("html")) {
      logger.info(s"${req.remoteAddr.getOrElse("null")} -> ${req.requestMethod}: ${req.requestUri.path}")
    }

    base64 orElse plain applyOrElse (req, {_: Request => Status.NotFound(req)})
  }

  private val factory = new NIO2ServerChannelFactory(f) {
    override protected def acceptConnection(channel: AsynchronousSocketChannel): Boolean = {
      logger.info(s"New connection: ${channel.getRemoteAddress}")
      super.acceptConnection(channel)
    }
  }

  def f(conn: SocketConnection) = {
    val s = new Http1Stage(service, Some(conn))(pool) with WebSocketSupport
    LeafBuilder(s)
  }

  def run(): Unit = factory.bind(addr).run()
}

object FlockeServer extends StrictLogging {
  val ip = Option(System.getenv("HOST")).getOrElse("0.0.0.0")
  val port = (Option(System.getenv("PORT")) orElse
              Option(System.getenv("HTTP_PORT")))
          .map(_.toInt)
          .getOrElse(8080)

  logger.info(s"Starting Http4s-blaze example on '$ip:$port'")
  println(s"Starting Http4s-blaze example on '$ip:$port'")

  def main(args: Array[String]): Unit = new FlockeServer(new InetSocketAddress(ip, port)).run()
}

