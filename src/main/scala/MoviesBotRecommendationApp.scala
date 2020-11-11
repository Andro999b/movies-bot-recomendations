import akka.actor.typed.ActorSystem
import logic.Trainer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession


object MoviesBotRecommendationApp extends App {

  val sparkConfig = new SparkConf()
  sparkConfig.setMaster("local[*]")
//  sparkConfig.set("spark.ui.enabled", "false")

  implicit val spark: SparkSession = SparkSession.builder()
    .appName("local")
    .config(sparkConfig)
    .getOrCreate()
}
