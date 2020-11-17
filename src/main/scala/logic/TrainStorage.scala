package logic

import com.typesafe.config.Config
import logic.Trainer.TrainResult
import org.apache.spark.ml.feature.IndexToString
import org.apache.spark.ml.recommendation.ALSModel

import scala.util.Try

object TrainStorage {

  private def pathFor(bot: String)(implicit config: Config) = s"${config.getString("storage.baseurl")}$bot/"

  def save(bot: String, model: ALSModel, indexToString: IndexToString)(implicit config: Config): Unit = {
    model.write.overwrite.save(pathFor(bot) + "model")
    indexToString.write.overwrite.save(pathFor(bot) + "indexToString")
  }

  def load(bot: String)(implicit config: Config): Try[TrainResult] = Try {
    TrainResult(
      ALSModel.load(pathFor(bot) + "model"),
      IndexToString.load(pathFor(bot) + "indexToString")
    )
  }
}
