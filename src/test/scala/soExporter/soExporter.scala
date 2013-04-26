package soExporter

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.actor.ActorSystem
import java.util.UUID
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.httpx.marshalling.Marshaller
import com.typesafe.config.ConfigFactory
import scala.concurrent.Await

@RunWith(classOf[JUnitRunner])
class CouchSuite extends FunSuite with CouchSuiteHelpers {
  import JsonProtocol._
  import actorSystem.dispatcher
    
  test("ssl enabled") {
    val conf = ConfigFactory.load()
    val sslEnabled = conf.getBoolean("spray.can.client.ssl-encryption")
    assert(sslEnabled, "ssl not enabled in config")
  }
  
  
}