package logic

import java.nio.file.Path

import logic.Trainer.TrainResult
import org.apache.spark.ml.feature.{IndexToString, StringIndexer}
import org.apache.spark.ml.recommendation.ALSModel

import scala.util.Try

object TrainStorage {

  private def pathFor(bot: String) = s"file:/G:/ml/$bot/"

  def save(bot: String, model: ALSModel, indexToString: IndexToString): Unit = {
    model.write.overwrite.save(pathFor(bot) + "model")
    indexToString.write.overwrite.save(pathFor(bot) + "indexToString")
  }

  def load(bot: String): Try[TrainResult] = Try {
    TrainResult(
      ALSModel.load(pathFor(bot) + "model"),
      IndexToString.load(pathFor(bot) + "indexToString")
    )
  }
}
