package logic

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.audienceproject.spark.dynamodb.implicits.DynamoDBDataFrameReader
import com.typesafe.config.Config
import org.apache.spark.ml.feature.{IndexToString, StringIndexer}
import org.apache.spark.ml.recommendation.{ALS, ALSModel}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.col

object Trainer {

  trait Command

  final case class Train(bots: List[String]) extends Command
  final case class TrainResult(model: ALSModel, indexToString: IndexToString)

  private final case class Rating(uid: Int, query: String, queryId: Double, rating: Float = 1)

  def apply(recommender: ActorRef[Recommender.Command])(implicit spark: SparkSession, config: Config): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case Train(bots) =>
      train(bots).foreach { res =>
        recommender ! Recommender.SetModel(res._1,res._2)
      }
      Behaviors.same
  }

  private def train(bots: List[String])(implicit spark: SparkSession, config: Config): List[(String, TrainResult)] = {
    val als = new ALS()
      .setUserCol("uid")
      .setItemCol("queryId")
      .setRatingCol("rating")

    val df = spark.read
      .option("bytesPerRCU", 4000000)
      .dynamodb("analyticsTable-prod")
      .select("uid", "query")
      .filter(col("resultsCount") > 0)
      .filter(col("type") === "search")
      .cache()

    bots
      .map { bot =>
        (bot, trainBot(als, bot, df))
      }
  }

  def trainBot(als: ALS, bot: String, df: DataFrame)(implicit spark: SparkSession, config: Config): TrainResult = {
    import spark.implicits._

    val botEvents = df
      .filter(col("bot") === bot)
      .cache()

    val indexer = new StringIndexer()
      .setInputCol("query")
      .setOutputCol("queryId")
      .fit(botEvents)

    val indexed = indexer
      .transform(botEvents)
      .map(row => Rating(row.getAs[Number](0).intValue(), row.getString(1), row.getDouble(2)))

    val model = als.fit(indexed)

    val indexToString = new IndexToString()
      .setOutputCol("query")
      .setInputCol("queryId")
      .setLabels(indexer.labelsArray(0))

    TrainStorage.save(bot, model, indexToString)

    TrainResult(model, indexToString)
  }
}
