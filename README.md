# SRT Shift

Simple script to shift SRT timestamps by a specified duration

### Usage

```
$ sbt
> run "-18.1 seconds" sample_subtitles.srt
>
[info] Running MyApp -18.1 seconds sample_subtitles.srt
New subtitle written to: sample_subtitles.srt-NEW
```

First argument to run will accept any format `scala.concurrent.duration.Duration` understands, while the second argument is the path to the file.

The new subtitle will retain the original filename appended with `-NEW`.

### Environment

* Scala 2.12.1
* SBT 1.2.8
* Java 8

