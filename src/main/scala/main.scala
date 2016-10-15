import cats._
import cats.instances.all._
import cats.data.{Xor, XorT}
// import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}

case class User(id: Long, name: String) {
  def update(newName: String): User = copy(name = newName)
}

object UserRepo {
  import Error._

  val users = List(
    User(1L, "Angus"),
    User(2L, "David"),
    User(3L, "James"),
    User(4L, "Ming"),
    User(5L, "Victor")
  )

  def find(id: Long)(implicit ec: ExecutionContext): XorT[Future, Error, User] = {
    println(s"find user #$id")
    users.find(u => u.id == id) match {
      case Some(user) => XorT.right(Future { user })
      case None => XorT.left(Future { UserNotFoundError(id) })
    }
  }

  def find1(id: Long)(implicit ec: ExecutionContext): Future[Option[User]] = Future {
    println(s"find user #$id")
    users.find(u => u.id == id)
  }

  def update(user: User)(implicit ec: ExecutionContext): Future[Unit] = Future {
    println(s"User #${user.id.toString} is updated")
  }
}

sealed trait Error {
  def code: Int
  def message: String
}
object Error {
  final case class UnknownError() extends Error {
    override def code: Int = 0
    override def message: String = s"Unknown error"
  }
  final case class UserNotFoundError(userId: Long) extends Error {
    override def code: Int = 1
    override def message: String = s"User not found - #$userId"
  }
  final case class ProjectNotFoundError(projectId: Long) extends Error {
    override def code: Int = 2
    override def message: String = s"Project not found - #$projectId"
  }
}

object ErrorT {
  def apply[A](optInstanceF: Future[Option[A]], error: Error = Error.UnknownError())(implicit ec: ExecutionContext): XorT[Future, Error, A] = XorT(optInstanceF.map(Xor.fromOption(_, error)))
  def pure[A](instance: A)(implicit ec: ExecutionContext): XorT[Future, Error, A] = XorT.fromXor[Future](Xor.right[Error, A](instance))
  def fromOption[A](optInstance: Option[A], error: Error = Error.UnknownError())(implicit ec: ExecutionContext): XorT[Future, Error, A] = XorT.fromXor[Future](Xor.fromOption(optInstance, error))
  def liftF[A](instanceF: Future[A])(implicit ec: ExecutionContext): XorT[Future, Error, A] = XorT(instanceF.map(Xor.right[Error, A]))
}

object Main extends App {
  import Error._
  implicit val ec = ExecutionContext.global

//  pureXorT
  def pureXorT = {
    (
      for {
        u1 <- UserRepo.find(1L)
        u9 <- UserRepo.find(9L)
        u2 <- UserRepo.find(2L)
      } yield List(u1, u2)
    ).value.map {
      case Xor.Right(users) => users.foreach(user => println(s"Form result: ${user.name}"))
      case Xor.Left(error) => println(error.message)
    }
  }

  dealWithFutureOption
  def dealWithFutureOption = {
    val u1Id = 1L
    (
      for {
        u1 <- ErrorT(UserRepo.find1(u1Id), UserNotFoundError(u1Id))
        newU1 <- ErrorT.pure(u1.update("Angus Tse"))
        _ <- ErrorT.liftF(UserRepo.update(newU1))
      } yield (u1, newU1)
    ).value.map {
      case Xor.Right((oldUser, newUser)) =>
        println(s"${oldUser.name} is changed to ${newUser.name}")
      case Xor.Left(error) =>
        println(error.message)
    }
  }
}

