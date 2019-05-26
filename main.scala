// import cats._
import cats.data._
import cats.implicits._
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.tools.nsc.io.File
import scala.util.Try

/*
    SRT Subtitle Format
      Repeating blocks of:
   
    <INDEX>       2
    <TIMESTAMP>   00:01:52,945 --> 00:01:57,443
    <TEXT LINE>   To our makers
    <TEXT LINE>   Who are no longer with us
    <EMPTY LINE>

    Usage:
      sbt run "-18.1 seconds" subtitles.srt
*/

final case class Subtitle(index: Int, timestamp: String, text: List[String])
final case class Timestamp(startTime: String, endTime: String) {
  override def toString = s"$startTime --> $endTime"
}

object MyApp extends App {

  val usage = """
    |> sbt
    |> run \"-18.1 seconds\" /path/to/subtitles.srt""".stripMargin

  val targetOffset   = Try(Duration(args(0))).getOrElse({ println("Invalid invocation. Run as $usage") ; sys.exit })
  val filePath       = Try(args(1)).getOrElse({ println("Invalid invocation. Run as $usage") ; sys.exit })
  val outputFilePath = s"${filePath}-NEW"

  val fileLines      = File(filePath).lines
  val subtitleGroups = splitBySeparator(fileLines.toList, "")


  def splitBySeparator[T](l: List[T], sep: T): List[List[T]] = l.span(_ != sep) match {
    case (hd, _ :: tl) => hd :: splitBySeparator(tl, sep)
    case (hd, _)       => List(hd)
  }

  def millisecondsToTimestamp(d: Long): String = {
    val mil = "%03d".format(d % 1000)
    val sec = "%02d".format((d / 1000) % 60)
    val min = "%02d".format((d / 1000 / 60) % 60)
    val hr  = ((d / 1000 / 60 / 60) % 60).toString.reverse.padTo(2, '0').reverse
    
    s"$hr:$min:$sec,$mil"
  }

  def extractTimestampsAsMillis(timestamp: String): List[Long] =
    timestamp
      .split(" --> ")
      .map(time =>
        time    
          .split("[:,]")
          .zip(Seq("hours", "minutes", "seconds", "millis"))
          .map(x => Duration(s"${x._1} ${x._2}"))
          .map(_.toMillis)
          .foldLeft(0L)((acc, el) => acc + el)
      )
      .toList


  val subtitleBlocks = for {
    subGroup        <- subtitleGroups
    index           <- subGroup
                          .headOption
                          .map(_.replaceAll("[^0-9]", "").toInt)
    timeStamp       <- subGroup
                          .drop(1)
                          .headOption
  
    ts               = extractTimestampsAsMillis(timeStamp)
    shiftedStartTime = millisecondsToTimestamp(ts(0) + targetOffset.toMillis)
    shiftedEndTime   = millisecondsToTimestamp(ts(1) + targetOffset.toMillis)
    newTimestamp     = Timestamp(shiftedStartTime, shiftedEndTime)

    msgs             =  subGroup.drop(2)
  } yield (Subtitle.apply _).tupled((index, newTimestamp.toString, msgs))

  val shiftedSubtitleBlocks = subtitleBlocks
                                .map(x => s"${x.index}\n${x.timestamp}\n${x.text.mkString("\n")}\n")

  scala.reflect.io.File(outputFilePath).writeAll(shiftedSubtitleBlocks.mkString("\n"))
}

