package soExporter

import akka.actor._
import spray.can.client.HttpClient
import spray.client.HttpConduit
import HttpConduit._
import spray.http._
import HttpMethods._
import spray.httpx.encoding.{Gzip, Deflate}
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling.Unmarshaller
import spray.json._
import spray.util._
import java.util.UUID
import akka.event.Logging
import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import scala.concurrent.Future
import spray.io.IOExtension

case class Config(
    actorSystem:ActorSystem,
    hostName:String,
    port:Int,
    userPass:Option[(String,String)] = None,
    https:Boolean = false
)

class Pipelines(config:Config) {
  import config._
  
  val as = config.actorSystem
  import as.dispatcher
	
  private val conduit = {
    val ioBridge = IOExtension(actorSystem).ioBridge()
    val httpClient = actorSystem.actorOf(Props(new HttpClient(ioBridge)))
    actorSystem.actorOf(Props(new HttpConduit(httpClient, hostName, port, https)))
  }
  private val log = Logging(actorSystem, conduit)
  
  private val logRequest: HttpRequest => HttpRequest = r => {
    log.info(r.toString + "\n")
    r
  }
  
  private val logResponse: HttpResponse => HttpResponse = r => {
    log.info(r.toString + "\n")
    r
  }  
  def pipeline[A:Unmarshaller]: HttpRequest => Future[A] = pipeline[A](None)
  
  def pipeline[A:Unmarshaller](etag:Option[String]): HttpRequest => Future[A] = {
    def unmarshalEither[A:Unmarshaller]: HttpResponse => A = {
      hr => (hr match {
        case HttpResponse(status, _, _, _) if status.isSuccess => {
          unmarshal[A](implicitly[Unmarshaller[A]])(hr)
        }
        case HttpResponse(errorStatus, _, _, _) => {
          log.error(hr.toString)
          throw new Exception(hr.toString)
        }
      })
    }
    val p: HttpRequest => Future[HttpResponse] = {
      (addHeader("accept", "application/json") ~>
      //addHeader("Accept-Encoding", "") ~>
      (userPass match {
        case Some((u,p)) => addCredentials(BasicHttpCredentials(u, p))
        case None => (x:HttpRequest) => x
      }) ~>
      logRequest ~>
      sendReceive(conduit)) ~>
      decode(Gzip)
    }
    p.andThen(resp => resp.map(unmarshalEither[A]))
  }
}
