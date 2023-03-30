import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object Multiply {

  def main(args: Array[String]) = {
    val conf = new SparkConf().setAppName("Multiply")
    val sc = new SparkContext(conf)

    conf.set("spark.logConf", "false")
    conf.set("spark.eventLog.enabled", "false")

    val M = sc.textFile(args(0)).map( line => { val a = line.split(",")
                                      (a(0), (a(1), a(2).toDouble)) } )

    val N = sc.textFile(args(1)).map( line => { val a = line.split(",")
                                                (a(0), (a(1), a(2).toDouble)) } )


    val Mult = M.map { case (i,(j,v)) => ( j, ( i, v ) ) }.join( N.map { case (j,(k,w)) => (j,(k,w)) } )
                    .map { case (j,((i,v), (k,w))) => ((i,k), v * w) }
                    .reduceByKey(_+_)

    Mult.sortByKey( true ).coalesce(1, true).saveAsTextFile( args( 2 ) )

    sc.stop()

  }
}

//Command to run:
// ~/spark-3.1.2-bin-hadoop3.2/bin/spark-submit --class Multiply target/cse6331-project4-0.1.jar M-matrix-small.txt N-matrix-small.txt output