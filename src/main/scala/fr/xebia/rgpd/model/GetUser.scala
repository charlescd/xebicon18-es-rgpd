package fr.xebia.rgpd.model

import java.util.UUID

sealed trait Query

case class GetUser(id: UUID) extends Query
