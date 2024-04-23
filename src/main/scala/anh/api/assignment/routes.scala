package anh.api.assignment

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import DefaultJsonProtocol._
import scala.collection.mutable

class routes(persons: mutable.Buffer[Person]) {
  def validationRoute: Route =
    path("customers") {
      get {
        complete(StatusCodes.OK -> persons.map(p => s"${p.index},${p.firstName},${p.lastName},${p.email}").mkString("\n"))
      }
    } ~
    path("customer") {
      post {
        entity(as[String]) { input =>
          val fields = input.split(",")
          if (fields.length != 4) {
            complete(StatusCodes.BadRequest -> "Invalid input format. Expected: Index,First Name,Last Name,Email")
          }
          else {
            val json = input.parseJson
            try {
              val index = json.asJsObject.fields("index").convertTo[Int]
              val firstName = json.asJsObject.fields("firstName").convertTo[String]
              val lastName = json.asJsObject.fields("lastName").convertTo[String]
              val email = json.asJsObject.fields("email").convertTo[String]
              val person = Person(index, firstName, lastName, email)
              validateCustomer.validateData(person) match {
                case Some(errorMsg) =>
                  complete(StatusCodes.BadRequest -> errorMsg)
                case None =>
                  persons += person
                  complete(StatusCodes.OK -> "Inserted Data succesfully!!!")
              }
            }
            catch {
              case _: NumberFormatException =>
                complete(StatusCodes.BadRequest -> "Index should be an integer.")
              case _: Throwable =>
                complete(StatusCodes.InternalServerError -> "Internal server error.")
            }
          }
        }
      }
    } ~
      path("customer" / IntNumber) { index =>
        get {
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
                persons.remove(idx)
                complete(StatusCodes.OK -> "Person deleted successfully.")
            }
          } ~
          put {
            entity(as[String]) { input =>
              val fields = input.split(",")
              if (fields.length != 3) {
                complete(StatusCodes.BadRequest -> "Invalid input format. Expected: First Name,Last Name,Email")
              } else {
                try {
                  val json = input.parseJson
                  val firstName = json.asJsObject.fields("firstName").convertTo[String]
                  val lastName = json.asJsObject.fields("lastName").convertTo[String]
                  val email = json.asJsObject.fields("email").convertTo[String]
                  val updatedPerson = Person(index, firstName, lastName, email)
                  validateCustomer.validateData(updatedPerson) match {
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
                  case _: Throwable =>
                    complete(StatusCodes.InternalServerError -> "Internal server error.")
                }
              }
            }
          }
      }
}

