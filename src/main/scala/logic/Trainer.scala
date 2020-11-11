package logic

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.audienceproject.spark.dynamodb.implicits.DynamoDBDataFrameReader
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.recommendation.{ALS, ALSModel}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col

object Trainer {

  trait Command

  final case class Train(bot: String) extends Command

  private final case class Rating(uid: Int, query: String, queryId: Double, rating: Float = 1)

  def apply(recommender: ActorRef[Recommender.Command])(implicit spark: SparkSession): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case Train(bot) =>
      val model = train(bot)
      recommender ! Recommender.SetModel(bot, model)
      Behaviors.same
  }

  private def train(bot: String)(implicit spark: SparkSession): ALSModel = {
    import spark.implicits._

    val als = new ALS()
      .setMaxIter(5)
      .setRegParam(0.01)
      .setUserCol("uid")
      .setItemCol("queryId")
      .setRatingCol("rating")

    val df = spark.read
      .option("bytesPerRCU", 4000000)
      .dynamodb("analyticsTable-prod")
      .select("uid", "query")
      .filter(col("type") === "search")
      .filter(col("bot") === bot)
      .filter(col("resultsCount") > 0)
      .cache()

    val indexer = new StringIndexer()
      .setInputCol("query")
      .setOutputCol("queryId")
      .fit(df)

    val indexed = indexer
      .transform(df)
      .map(row => Rating(row.getAs[Number](0).intValue(), row.getString(1), row.getDouble(2)))

    val model = als.fit(indexed)

    model.write.overwrite().save("file:/G:/tmp/model")

    model
  }
}