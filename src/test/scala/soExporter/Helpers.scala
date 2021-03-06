package soExporter

import org.scalatest.FunSuite
import akka.actor.ActorSystem
import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.Await

case class Test(foo:Int, bar:String)

trait CouchSuiteHelpers {
  self:FunSuite =>
    
  import JsonProtocol._
  
  implicit val testFormat = jsonFormat2(Test)
  
  implicit val actorSystem = ActorSystem("MySystem")
  import actorSystem.dispatcher
  implicit val testDuration = Duration("30 seconds")
  def await[A](f:Future[A]) = Await.result(f, testDuration)
  
  
}
