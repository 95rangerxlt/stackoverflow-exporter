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
import spray.io._
import spray.json._
import spray.util._
import java.util.UUID
import akka.event.Logging
import java.net.URLEncoder.{encode => urlEncode}
import JsonProtocol._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait UriBuilder {
  protected[this] def sep = "/"
  protected[this] def encode(s:String) = urlEncode(s, "UTF-8") 
  protected[this] def path(parts:String*) = sep + parts.map(encode).mkString(sep)  
  protected[this] def searchUri(tag:String, page:Int):String = { 
  	"/1.1/search?tagged=" + encode(tag) + "&pagesize=100&page=" + page
  }
}


class SoExporter(config:Config) extends UriBuilder {
  private val as = config.actorSystem
  import as.dispatcher
  
  private val pipelines = new Pipelines(config)
  private lazy val pipeline = pipelines.pipeline[QuestionsResponse]
  
  def questionsTagged(tag:String):Future[Seq[Question]] = {
    val qrsf = Future.sequence((0 to 20) map (page => {
    	pipeline(Get(searchUri(tag, page))).map(Some(_)).recover{case _ => {println("ERROR on page" + page); None}}
    }))
    qrsf.map(_.flatten).map(qrs => qrs.flatMap(qr => qr.questions))
  }
  
}

object Couch {
	
  def main(args:Array[String]) {
  	val as = ActorSystem("mysystem")
  	import as.dispatcher
  
  	val config = Config(as, "api.stackoverflow.com", 80)
  	val soe = new SoExporter(config)
  	val output = soe.questionsTagged("playframework-2.0").map(qs => {
                val questions = qs.toSet.toList.sortBy((q:Question) => -q.score)
  		println("questions: " + questions.size)
  		questions.foreach(q => {
  			println(q.title.replaceAll("\\|", "/") + "|" + 
  					q.score)
  		})
  	})
    Await.result(output, Duration("10 minutes"))
    as.shutdown()
  }

}
