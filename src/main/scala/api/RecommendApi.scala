package api

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.Config
import logic.{Recommender, Trainer}

import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

object RecommendApi {
  def apply(recommender: ActorRef[Recommender.Command], trainer: ActorRef[Trainer.Command])
           (implicit system: ActorSystem[_], config: Config) : Route = {
    import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}

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
          parameter("bot".optional) { bot =>
            val bots: List[String] = bot
              .map(List(_))
              .getOrElse(config.getStringList("bots.names").asScala.toList)

            trainer ! Trainer.Train(bots)
            complete("train started")
          }
        }
      }
    )
  }
}
