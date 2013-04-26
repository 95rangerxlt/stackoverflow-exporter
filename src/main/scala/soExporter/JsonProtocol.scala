package soExporter

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

object JsonProtocol extends DefaultJsonProtocol {
  implicit val nullFormat:JsonFormat[Null] = new JsonFormat[Null] {
    override def read(js:JsValue) = if (js == JsNull) null else throw new Exception("null expected")
    override def write(n:Null) = JsNull
  }
  case object Empty
  
  implicit val emptyFormat = new RootJsonFormat[Empty.type] {
    def read(js:JsValue) = Empty
    def write(e:Empty.type) = new JsObject(Map())
  }
  
  case class Question(
		title:String, 
		up_vote_count:Int,
		down_vote_count:Int,
		view_count:Int,
		favorite_count:Int) {
  	
  	def score = (up_vote_count - down_vote_count + favorite_count + (view_count / 250.0))
  }
  implicit val questionFormat = jsonFormat5(Question)		

  case class QuestionsResponse(
  		page:Int,
  		pagesize:Int,
  		total:Int,
      questions:Seq[Question]		
  )
  implicit val questionsResponseFormat = jsonFormat4(QuestionsResponse)
  
}


