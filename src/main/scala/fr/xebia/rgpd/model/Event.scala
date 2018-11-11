package fr.xebia.rgpd.model

import java.util.UUID

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

sealed trait Event

object Event {
  implicit val decoder: Decoder[Event] = (c: HCursor) =>
    c.downField("type").as[String].flatMap {
      case "user-created" => c.as[UserCreated]
      case "amount-updated" => c.as[AmountUpdated]
      case "user-deleted" => c.as[UserDeleted]
    }

  implicit val encoder: Encoder[Event] = {
    case u: UserCreated => Json.obj("type" -> "user-created".asJson).deepMerge(u.asJson)
    case a: AmountUpdated => Json.obj("type" -> "amount-updated".asJson).deepMerge(a.asJson)
    case u: UserDeleted => Json.obj("type" -> "user-deleted".asJson).deepMerge(u.asJson)
  }

}

case class UserCreated(id: UUID, name: String, amount: Int) extends Event

case class AmountUpdated(id: UUID, amount: Int) extends Event

case class UserDeleted(id: UUID) extends Event