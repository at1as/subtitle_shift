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

object MyApp extends App {

  val targetOffset   = Try(Duration(args(0))).getOrElse({ println("Invalid invocation. Run as sbt run \"-18.1 seconds\" subtitles.srt") ; sys.exit })//-18.1 seconds
  val filePath       = "/Users/jwillems/Repos/srt_shift/sample.srt" 
  val outputFilePath = "/Users/jwillems/Repos/srt_shift/sample.srt-NEW"

  def splitBySeparator[T]( l: List[T], sep: T): List[List[T]] = l.span(_ != sep) match {
    case (hd, _ :: tl) => hd :: splitBySeparator(tl, sep)
    case (hd, _)       => List(hd)
  }

  def durationToTimestamp(d: Duration): String = {
    val mil = "%03d".format(d.toMillis % 1000)
    val sec = "%02d".format((d.toMillis / 1000) % 60)
    val min = "%02d".format((d.toMillis / 1000 / 60) % 60)
    val hr  = ((d.toMillis / 1000 / 60 / 60) % 60).toString.reverse.padTo(2, '0').reverse

    s"$hr:$min:$sec,$mil"
  }

  val fileLines      = File(filePath).lines
  val subtitleGroups = splitBySeparator(fileLines.toList, "")

  val subtitleBlocks = for {
    subGroup          <- subtitleGroups
    index             <- subGroup
                          .headOption
                          .map(_.replaceAll("[^0-9]", "").toInt)
    timeStamp         <- subGroup
                          .drop(1)
                          .headOption
    
    startTime         = timeStamp
                          .split(" --> ")(0)
                          .split("[:,]")
                          .zip(Seq("hours", "minutes", "seconds", "millis"))
                          .map(x => Duration(s"${x._1} ${x._2}"))
                          .map(_.toMillis)
                          .foldLeft(0L)((acc, el) => acc + el)
    
    shiftedStartTime = durationToTimestamp(Duration(s"${startTime + targetOffset.toMillis} millis"))
    
    endTime          = timeStamp
                          .split(" --> ")(1)
                          .split("[:,]")
                          .zip(Seq("hours", "minutes", "seconds", "millis"))
                          .map(x => Duration(s"${x._1} ${x._2}"))
                          .map(_.toMillis)
                          .foldLeft(0L)((acc, el) => acc + el)
    
    shiftedEndTime   = durationToTimestamp(Duration(s"${endTime + targetOffset.toMillis} millis"))
    
    msgs             =  subGroup.drop(2)
  } yield (Subtitle.apply _).tupled((index, s"$shiftedStartTime --> $shiftedEndTime", msgs))

  val shiftedSubtitleBlocks = subtitleBlocks.map(x => s"${x.index}\n${x.timestamp}\n${x.text.mkString("\n")}\n")

  scala.reflect.io.File(outputFilePath).writeAll(shiftedSubtitleBlocks.mkString("\n"))
}

