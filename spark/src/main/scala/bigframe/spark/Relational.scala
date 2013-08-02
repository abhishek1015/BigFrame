package bigframe.spark

import spark.SparkContext
import SparkContext._

/*
 Class to run relational queries.
*/
class Relational {

   private var sc:SparkContext = _

   /*
    Argument:
     Spark context
   */
   def init(sparkContext: SparkContext) {
       sc = sparkContext
   }

   /*
    Argument:
     TPCDS data path -- "hdfs://<ip/name>:9000/<path_to_tpcds>"

   */
   def process(path: String) = {

// Read promotion table, tokenize it, and filter out promotions not in the given list of promo_ids
val promotion = sc.textFile(path+"/"+Constants.PromotionTableName).map(t => t.split('|'))

// Create a mapping from item_sk to promotion tuple
val promotion_mapped = promotion map { t => (t(4), t.slice(0,5)) }   

// Read sales table and tokenize it
val sales = sc.textFile(path+"/"+Constants.SalesTableName).map(t => t.split('|'))

// Create a mapping from item_sk to sales tuple
val sales_mapped = sales map { t => (t(3), Array(t(0), t(3), t(17), t(18), t(21))) }

// join promotion_mapped with sales_mapped, filter irrelevant attributes, and filter sales not within promotion dates.
val promo_sales = promotion_mapped.join(sales_mapped).mapValues(t => (t._1(1), t._1(2), t._1(3), t._2(0), ( try{t._2(3).toDouble*t._2(4).toDouble} catch{case e:Exception => 0} ))).filter(t => (t._2._2 <= t._2._4 & t._2._4 <= t._2._3)).mapValues(t => (t._1, t._5))


// group items together, sum up the sales
val total_sales = promo_sales.reduceByKey( (a, b) => (a._1, a._2 + b._2) )

// load item table, tokenize it
val item = sc.textFile(path+"/"+Constants.ItemTableName).map(t => t.split('|')) 

// map item_sk with product name
val item_mapped = item map { t => try {(t(0), t(21))} catch { case e:Exception => (t(0), "N/A") } }   

// join total_sales with item_mapped
val report = total_sales.join(item_mapped).map { t => (t._2._1._1, t._2._2, t._2._1._2) } 

// write the result, doing it locally now!
report.saveAsTextFile("output")

   }

}
