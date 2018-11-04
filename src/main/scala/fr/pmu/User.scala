package fr.pmu

case class NewUser(name: String)
case class Transaction(id: String, amount: Int)

case class UserCreated(`type`: String, id: String, name: String, amount: Int)
case class AmountUpdated(`type`: String, id: String, amount: Int)

case class User(id: String, name: String, amount: Int)
case class Status(uptime: String)