package com.git.wuqf.spark.sql

//import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SparkSession}
import org.elasticsearch.spark.rdd.EsSpark


/**
  * Created by Administrator on 2017/6/20.
  */
object SqlElasticsearch {

  //Logger.getLogger("org").setLevel(Level.DEBUG)

  def main(args: Array[String]): Unit = {
    syslogLevelCount(initSparkSession(initSparkConf()))
  }

  def initSparkConf(): SparkConf = {
    val conf = new SparkConf().setAppName("spark-sql-es").setMaster("spark://10.10.20.189:7077")
      .set("es.index.auto.create", "true")
      .set("es.nodes", "10.10.20.189")
      .set("es.net.http.auth.user", "elastic")
      .set("es.net.http.auth.pass", "elastic")
      .setJars(List("C:\\Users\\Administrator\\.m2\\repository\\org\\elasticsearch\\elasticsearch-spark-20_2.11\\5.4.4\\elasticsearch-spark-20_2.11-5.4.4.jar",
        "D:\\git\\spark-demo\\spark-sql\\target\\spark-sql-1.0-SNAPSHOT.jar"));

    return conf;
  }

  def initSparkSession(conf: SparkConf): SparkSession = {
    val sparkSession = SparkSession.builder()
      .config(conf)
      .getOrCreate()
    return sparkSession;
  }

  def ipCount(sc: SparkSession): Unit = {
    val options = Map("pushdown" -> "true")
    val access = sc.read.format("org.elasticsearch.spark.sql")
      .options(options)
      .load("apache-access-2016/apache-access")
    val ds = access
      .select("clientip")
      .groupBy("clientip")
      .count()
      .rdd.map(row => Map("accessIp" -> row.getString(0), "accessTimes" -> row.getLong(1)))
    EsSpark.saveToEs(ds, "count/access-acess")
  }

  def slowQueryCount(sc: SparkSession): Unit = {
    val options = Map("pushdown" -> "true")
    val access = sc.read.format("org.elasticsearch.spark.sql")
      .options(options)
      .load("logstash-mysqlslowlog-2017/mysqlslowlog")
    val ds = access
      .select("query")
      .groupBy("query")
      .count()
      .rdd.map(row => Map("query" -> row.getString(0), "times" -> row.getLong(1)))
    EsSpark.saveToEs(ds, "count/mysql-slowquery")
  }

  def slowQueryTime(sc: SparkSession): Unit = {
    val options = Map("pushdown" -> "true")
    val access = sc.read.format("org.elasticsearch.spark.sql")
      .options(options)
      .load("logstash-mysqlslowlog-2017/mysqlslowlog")
    val ds = access
      .select("query", "query_time")
      .filter("query_time is not null")
      .groupBy("query")
      .max("query_time")
      .rdd.map(row => Map("query" -> row.getString(0), "query_time" -> row.getFloat(1)))
    EsSpark.saveToEs(ds, "time/mysql-slowquery")
  }

  def httpMethodCount(sc: SparkSession): Unit = {
    val options = Map("pushdown" -> "true")
    val access = sc.read.format("org.elasticsearch.spark.sql")
      .options(options)
      .load("logstash-tomcat_access_realtime-2017/tomcat_access_realtime")
    val ds = access
      .select("verb")
      .groupBy("verb")
      .count()
      .rdd.map(row => Map("verb" -> row.getString(0), "times" -> row.getLong(1)))
    EsSpark.saveToEs(ds, "count/tomcat-method")
  }

  def syslogLevelCount(sc: SparkSession): Unit = {
    val options = Map("pushdown" -> "true")
    val access = sc.read.format("org.elasticsearch.spark.sql")
      .options(options)
      .load("syslog-2017.07.04/syslog")
    val ds = access
      .select("syslog_facility")
      .groupBy("syslog_facility")
      .count()
      .rdd.map(row => Map("syslog_facility" -> row.getString(0), "times" -> row.getLong(1)))
    EsSpark.saveToEs(ds, "count/syslog-level")
  }

  def testSchema(sparkSession: SparkSession): Unit = {
    val peopleRDD = sparkSession.sparkContext.textFile("people.txt")
    val schemaString = "name age"
    val fields = schemaString.split(" ")
      .map(fieldName => StructField(fieldName, StringType, nullable = true))
    val schema = StructType(fields)
    val rowRDD = peopleRDD
      .map(_.split(","))
      .map(attributes => Row(attributes(0), attributes(1).trim))
    val peopleDF = sparkSession.createDataFrame(rowRDD, schema)
    peopleDF.createOrReplaceTempView("people")
    val results = sparkSession.sql("select name from people")
    results.show()
  }
}
