package api

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import logic.Recommender
import logic.Recommender.RecommendResponse
import logic.Trainer

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object RecommendApi {
  def apply(recommender: ActorRef[Recommender.Command], trainer: ActorRef[Trainer.Command])(implicit system: ActorSystem[_]) : Route = {
    import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
    import akka.actor.typed.scaladsl.AskPattern.Askable

    implicit val timeout: Timeout = 10.seconds

    concat(
      pathPrefix("recommends") {
        path(IntNumber) { id =>
          parameters("num".withDefault(10), "bot") { (num, bot) =>
            get {
              val f = recommender.ask(Recommender.Recommend(bot, id, num, _))
              onComplete(f) {
                case Success(value) => complete(value.recommendations.toString)
                case Failure(exception) => complete(StatusCodes.InternalServerError, exception.getMessage)
              }
            }
          }
        }
      },
      pathPrefix("train") {
        post {
          parameter("bot") { bot =>
            trainer ! Trainer.Train(bot)
            complete("train started")
          }
        }
      }
    )
  }
}
