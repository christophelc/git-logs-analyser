import ammonite.ops._

// Avant:  java -cp h2*.jar org.h2.tools.Server
import $ivy.`com.h2database:h2:1.4.192`
import java.sql.{DriverManager, Statement}
Class.forName("org.h2.Driver")

println("This program clone github play-json repository and then insert the git logs into a h2 database.")

// use this url and 'sh h2Server.sh' to use h2 in server mode and launch console."
val url_h2tcp =  "jdbc:h2:tcp://localhost/~/gitlog-play-json"

val url_h2local =  "jdbc:h2:~/gitlog-play-json"
val user = "sa"
val pwd = ""
val conn = DriverManager.getConnection(url_h2local, user, pwd)

val url = "https://github.com/playframework/play-json"
implicit var path=cwd
%git('clone, url)

path=cwd/"play-json"
case class Event(hash: String, branch: String, author: String, date: String, time: String, rest: String)

//git log --oneline --decorate --no-merges --pretty=format:'%h %d <%Cred%an%Creset> %ci (%C(yellow)%cr)%Creset %N %C(dim white)[%s]'
def gitHash() : Vector[Event] = {

 val arg0 = "--oneline"
 val arg1 = "--decorate"
 val arg2 = "--no-merges"
 val prettyArg = "--pretty=format:'%h %d <%Cred%an%Creset> %ci (%C(yellow)%cr)%Creset %N %C(dim white)[%s]'"

 val log = %%('git, 'log, arg0, arg1, arg2, prettyArg).out.lines
 val pattern="'([0-9a-z]+)[ ]+([(][^)]+[)])?[ ]?+[<]([^>]+)[> ]+([0-9-]+)[ ]([0-9:]+)(.*)".r
 log.map( event => {
  val pattern(hash, branch, author, date, time, rest) = event
  Event(hash, branch, author, date, time, rest)
 })
}

case class Diff(add: Integer, sub: Integer, filename: String, ext: String)
case class Commit(hash: String, diff: Vector[Diff])

//git show --pretty=oneline --find-renames --find-copies -b -w --numstat 058741b
def gitShow(hash: String) : Commit = {

 val arg0 = "--pretty=oneline"
 val arg1 = "--find-renames"
 val arg2 = "--find-copies"
 val arg3 = "-b"
 val arg4 = "-w"
 val arg5 = "--numstat"

 val log = %%('git, 'show, arg0, arg1, arg2, arg3, arg4, arg5, hash).out.lines
 val pattern0 = "[0-9a-z]+ (?:[(][^)]+[)])?[ ]*(.*)".r
 println(log(0))
 val pattern0(comments) = log(0)

 val pattern = "([0-9\\-]+)[\\h]+([0-9\\-]+)[\\h]+(.*)".r
 val diffs = log.drop(1).map(l => {
   val pattern(add, sub, filename) = l
   val nAdd = (if (add.equals("-")) 0 else add.toInt)
   val nSub = (if (sub.equals("-")) 0 else sub.toInt)
   Diff(nAdd, nSub, filename, filename.reverse.split("\\.")(0).reverse)
 })
 Commit(hash, diffs)
}

println("now connect to the h2 console")

val events = gitHash()
events.groupBy(_.author).map {
  case (author, listOfEvents) => {
//    author -> listOfEvents.  // TODO
  }
  case _ =>
}



def insertData(events: Vector[Event], stmt: Statement): Unit = {

  for (ev <- events) {
    val sqlInsertEvent =
      s"""
          |insert into events(hash, branch, author, date, time) 
          | values('${ev.hash}', '${ev.branch}', '${ev.author}', '${ev.date}', '${ev.time}')
      """
          .stripMargin
    stmt.executeUpdate(sqlInsertEvent)

    val commit = gitShow(ev.hash)
    for (diff <- commit.diff) {
      val filename = diff.filename.replace("'", "''")
      val sqlInsertDiff =
        s"""
          |insert into diffs(hash, add, sub, filename, ext)
          | values('${commit.hash}', ${diff.add}, ${diff.sub}, '${filename}', '${diff.ext}')
        """
          .stripMargin
      stmt.executeUpdate(sqlInsertDiff)
    }
  }
}

val stmt = conn.createStatement()
stmt.executeUpdate("drop table if exists events")
stmt.executeUpdate("drop table if exists diffs")
val sqlCreateTableEvents =
          """
               |create table if not exists events(id BIGINT auto_increment, 
               |hash VARCHAR(255),
               |branch VARCHAR(255),
               |author VARCHAR(255),
               |date VARCHAR(10),
               |time VARCHAR(8),
               |PRIMARY KEY(id))
          """
          .stripMargin
stmt.executeUpdate(sqlCreateTableEvents)

val sqlCreateTableDiff =
          """
               |create table if not exists diffs(id BIGINT auto_increment,
               |hash VARCHAR(255),
               |add INTEGER not null,
               |sub INTEGER not null,
               |filename VARCHAR(255) not null,
               |ext VARCHAR(255),
               |PRIMARY KEY(id))
          """
          .stripMargin
stmt.executeUpdate(sqlCreateTableDiff)
insertData(gitHash(), stmt)
stmt.close()
conn.close()
