import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import api.{RecommendApi, Server}
import com.typesafe.config.{Config, ConfigFactory}
import logic.{Recommender, TrainStorage, Trainer}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import scala.util.{Failure, Success}

object Root {
  trait Command

  def apply()(implicit spark: SparkSession, config: Config): Behavior[Command] = Behaviors.setup { ctx  =>
    implicit val system: ActorSystem[_] = ctx.system

    val recommender = ctx.spawn(Recommender(), "recommender")
    val trainer = ctx.spawn(Trainer(recommender), "trainer")

    Seq("anime", "films").foreach { bot =>
      TrainStorage.load(bot) match {
        case Failure(ex) => ctx.log.warn(s"Fail load bot $bot model: ${ex.getMessage}")
        case Success(value) => recommender ! Recommender.SetModel(bot, value)
      }
    }

    Server(RecommendApi(recommender, trainer), "localhost", 9000)

    Behaviors.same
  }
}

object MoviesBotRecommendationApp extends App {

  val sparkConfig = new SparkConf()
  sparkConfig.setMaster("local[*]")
//  sparkConfig.set("spark.ui.enabled", "false")

  implicit val spark: SparkSession = SparkSession.builder()
    .appName("local")
    .config(sparkConfig)
    .getOrCreate()

  implicit val config: Config = ConfigFactory.load()

  ActorSystem.create(Root(), "root", config)
}
