# git-logs-analyser
Clone a github repository and then exports logs into a h2 database with the following information per commit: hash, branch, author, date, time, lines added, deleted, filename, filename extension.

## Summary
This program clones the play-json repository from github and use git command to inspect all commits done: it exports logs in a h2 database for further analysis.

## Prerequis
use Ammonite

## Run the program
amm analyse-git.sc

## Analysis

### h2 console
You need to change the h2 url to use h2 in server mode instead of embedded mode. You can then examine the logs with sql queries through the h2 console.

### Sql Examples

#### query 1
select D.hash, D.add, D.sub,D.filename,e.branch, e.author, e.date,e.time from diffs D inner join events E on D.hash = E.hash limit 5;

#### query 2
select sum(T.add - T.sub) as total, T.author, T.ext from (select D.hash, D.add, D.sub,D.filename,D.ext, e.branch, e.author, e.date,e.time from diffs D inner join events E on D.hash = E.hash) T group by T.author, 
T.ext;

#### query 3
select sum(T.add - T.sub) as total, T.author, T.ext from (select D.hash, D.add, D.sub,D.filename,D.ext, e.branch, e.author, e.date,e.time from diffs D inner join events E on D.hash = E.hash) T where ext='java' gr
oup by T.author, T.ext order by total desc;
~                           
