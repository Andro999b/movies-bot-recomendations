package logic

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.apache.spark.ml.recommendation.ALSModel
import org.apache.spark.sql.SparkSession

object Recommender {
  trait Command
  case class SetModel(bot: String, model: ALSModel) extends Command
  case class Recommend(bot: String, uid: Int, num: Int = 10) extends Command

  def apply()(implicit spark: SparkSession): Behavior[Command] = recommender(Map())

  private def recommender(models: Map[String, ALSModel])(implicit spark: SparkSession): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case SetModel(bot, model) => recommender(models + (bot -> model))
    case Recommend(bot, uid, num) =>
      import spark.implicits._
//      for {
//        model <- models.get(bot)
//        df <- model.recommendForUserSubset(Seq(uid).toDS(), num)
//        items <- df.collectAsList()
//      }
      models
        .get(bot)
        .map(_.recommendForUserSubset(Seq(uid).toDS(), 10))
        .map(_.collectAsList())
      Behaviors.same
  }
}
