
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import scala.collection.mutable
import scala.io.StdIn
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import com.github.tototoshi.csv._
import spray.json.DefaultJsonProtocol.{IntJsonFormat, StringJsonFormat}
import spray.json._
case class Person(index: Int, firstName: String, lastName: String, email: String)
object Main extends App {

  // Below function used to validate the data which inserted through the API request
  def validateData(person: Person): Option[String] = person match {
    case Person(index, _, _, _) if index < 0 =>
      Some("Index should be a positive number.")
    case Person(_, firstName, _, _) if !firstName.matches("[a-zA-Z]+") =>
      Some("First name should contain only alphabets.")
    case Person(_, _, lastName, _) if !lastName.matches("[a-zA-Z]+") =>
      Some("Last name should contain only alphabets.")
    case Person(_, _, _, email) if !email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") =>
      Some("Invalid email format.")
    case _ =>
      None
  }

  // Below code fetch the data from csv file and stored it into list
  val persons: mutable.Buffer[Person] = CSVReader.open(new java.io.File("C:/Users/c22755b/IdeaProjects/Rest_API_Hello/src/main/scala/customers.csv"))
    .toStream
    .map {
      case List(index, firstName, lastName, email) => Person(index.toInt, firstName, lastName, email)
    }.toBuffer

  // Akka HTTP request configuration
  implicit val system: ActorSystem = ActorSystem("validationSystem")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Route for validation
  val validationRoute: Route =
    path("insert") {                   // insert is sub path for POST the request
      post {
        entity(as[String]) { input =>     // received csv data string format so it will split it by using coma(,)
          val fields = input.split(",")
          if (fields.length != 4) {
            complete(StatusCodes.BadRequest -> "Invalid input format. Expected: Index,First Name,Last Name,Email")
          } else {
            val json = input.parseJson      // parse data into jason format so it will be easy to split abd used that data
            try {
              val index = json.asJsObject.fields("index").convertTo[Int]
              val firstName = json.asJsObject.fields("firstName").convertTo[String]
              val lastName = json.asJsObject.fields("lastName").convertTo[String]
              val email = json.asJsObject.fields("email").convertTo[String]

              val person = Person(index, firstName, lastName, email)  // build a one structure using above all params
              validateData(person) match {
                case Some(errorMsg) =>
                  complete(StatusCodes.BadRequest -> errorMsg)    // handle the exception when some error occur
                case None =>
                  persons += person                               // add data in database
                  complete(StatusCodes.OK -> "Inserted Data succesfully!!!")
              }
            } catch {
              case _: NumberFormatException =>                   // handle exception for integer value
                complete(StatusCodes.BadRequest -> "Index should be an integer.")
              case _: Throwable =>                               // handle server errors
                complete(StatusCodes.InternalServerError -> "Internal server error.")
            }
          }
        }
      }
    } ~
      path("allEmployee") {                 // by employeeAll we can get all the records
        get {
          // store in a map then send via request
          complete(StatusCodes.OK -> persons.map(p => s"${p.index},${p.firstName},${p.lastName},${p.email}").mkString("\n"))
        }
      } ~
      path("employee" / IntNumber) { index =>
        get {
          // this get which required integer in path for getting particular record
          persons.find(_.index == index) match {
            case Some(person) =>
              complete(StatusCodes.OK -> s"${person.index},${person.firstName},${person.lastName},${person.email}")
            case None =>
              complete(StatusCodes.NotFound -> "Person not found.")
          }
        } ~
          delete {
            persons.indexWhere(_.index == index) match {
              case -1 =>
                complete(StatusCodes.NotFound -> "Person not found.")
              case idx =>
                persons.remove(idx)      // remove data from list
                complete(StatusCodes.OK -> "Person deleted successfully.")
            }
          } ~
          put {
            // for editing in any data which is existed in database
            entity(as[String]) { input =>
              val fields = input.split(",")
              if (fields.length != 3) {   // just took only 3 length because index is primary so we can't update that
                complete(StatusCodes.BadRequest -> "Invalid input format. Expected: First Name,Last Name,Email")
              } else {
                try {
                  val json = input.parseJson
                  val firstName = json.asJsObject.fields("firstName").convertTo[String]
                  println(firstName)
                  val lastName = json.asJsObject.fields("lastName").convertTo[String]
                  val email = json.asJsObject.fields("email").convertTo[String]
                  val updatedPerson = Person(index, firstName, lastName, email)
                  validateData(updatedPerson) match {
                    case Some(errorMsg) =>
                      complete(StatusCodes.BadRequest -> errorMsg)
                    case None =>
                      persons.indexWhere(_.index == index) match {
                        case -1 =>
                          complete(StatusCodes.NotFound -> "Person not found.")
                        case idx =>
                          persons(idx) = updatedPerson
                          complete(StatusCodes.OK -> "Person updated successfully.")
                      }
                  }
                } catch {
                      // if any internal server 5xx happen then this catch handle it
                  case _: Throwable =>
                    complete(StatusCodes.InternalServerError -> "Internal server error.")
                }
              }
            }
          }
      }

  // used future to handle the connection and for synchronously work
  val bindingFuture: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bindFlow(validationRoute)

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
