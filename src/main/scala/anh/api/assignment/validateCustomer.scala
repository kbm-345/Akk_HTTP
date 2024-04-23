package anh.api.assignment

case class Person(index: Int, firstName: String, lastName: String, email: String)
object validateCustomer {
  val patternMatching = "[a-zA-Z]+"
  def validateData(person: Person): Option[String] = person match {
    case Person(index, _, _, _) if index < 0 =>
      Some("Index should be a positive number.")
    case Person(_, firstName, _, _) if !firstName.matches(patternMatching) =>
      Some("First name should contain only alphabets.")
    case Person(_, _, lastName, _) if !lastName.matches(patternMatching) =>
      Some("Last name should contain only alphabets.")
    case Person(_, _, _, email) if !email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") =>
      Some("Invalid email format.")
    case _ =>
      None
  }
}
