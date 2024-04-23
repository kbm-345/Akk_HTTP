package anh.api.assignment

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.github.tototoshi.csv.CSVReader
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}

object Main extends App {
  // Below code fetch the data from csv file and stored it into list
  val persons: mutable.Buffer[Person] = CSVReader.open(new java.io.File("C:/Users/c22755b/IdeaProjects/Rest_API_Hello/src/main/scala/resources/customers.csv"))
    .toStream
    .map {
      case List(index, firstName, lastName, email) => Person(index.toInt, firstName, lastName, email)
    }.toBuffer

  // Akka HTTP request configuration
  implicit val system: ActorSystem = ActorSystem("validationSystem")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Route for validation
  val apiRoute = new routes(persons)
  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bindFlow(apiRoute.validationRoute)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()

  // after getting result terminate the system
  bindingFuture.flatMap(_.unbind()).onComplete {
    case Success(_) =>
      system.terminate()
    case Failure(e) =>
      println(s"Failed to unbind and terminate: ${e.getMessage}")
      system.terminate()
  }
}
