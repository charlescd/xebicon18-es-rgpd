package fr.xebia.rgpd.model

import java.util.UUID

sealed trait Command

case class CreateUser(name: String) extends Command

case class UpdateAmount(id: UUID, amount: Int) extends Command

case class DeleteUser(id: UUID) extends Command