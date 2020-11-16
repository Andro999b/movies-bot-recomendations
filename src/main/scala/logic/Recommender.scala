package logic

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, explode}

object Recommender {

  trait Command

  case class SetModel(bot: String, trainResult: Trainer.TrainResult) extends Command

  case class Recommend(bot: String, uid: Int, num: Int = 10, replyTo: ActorRef[RecommendResponse]) extends Command

  case class RecommendResponse(recommendations: Any)

  def apply()(implicit spark: SparkSession): Behavior[Command] = recommender(Map())

  private def recommender(models: Map[String, Trainer.TrainResult])(implicit spark: SparkSession): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case SetModel(bot, trainResult) => recommender(models + (bot -> trainResult))
    case Recommend(bot, uid, num, replyTo) =>
      import spark.implicits._

      val recommendations = models.get(bot)
        .map { trainResult =>
          val df = trainResult.model
            .recommendForUserSubset(Seq(uid).toDF("uid"), num)
            .select(col("uid"), explode(col("recommendations")).alias("recommendation"))
            .select(col("uid"), col("recommendation.*"))

          trainResult.indexToString
            .transform(df)
            .select(col("query"), col("rating"))
            .map(_.json)
            .collectAsList()
        }
        .getOrElse(List.empty)

      print(recommendations)

      replyTo ! RecommendResponse(recommendations)

      Behaviors.same
  }
}
