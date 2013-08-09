/**
 *
 */
package bigframe.spark.relational

import spark.RDD
import spark.SparkContext
import SparkContext._

class MicroQueries(val sc: SparkContext, val tpcds_path: String) {

  def readFile(name: String) = {
      sc.textFile(tpcds_path + "/" + name).map(t => t.split('|'))
  }
  
  def promotionsMappedByItems() = {
	  // Read promotion table, tokenize it, and filter out promotions not in the given list of promo_ids
	  val promotion = readFile(Constants.PromotionTableName)

	  // Create a mapping from item_sk to promotion tuple
	  promotion map { t => (t(4), t.slice(0,5)) }   
  }
  
  def storeSalesPerItem() = {
    // Read sales table and tokenize it
    val sales = readFile(Constants.StoreSalesTableName)

    // Create a mapping from item_sk to sales tuple (sold_date, item_sk, ticket_number, quantity, sales_price)
    sales map { t => (t(2), Array(t(0), t(2), t(9), t(10), t(13))) }    
  }
  
  def catalogSalesPerItem() = {
    // Read sales table and tokenize it
    val sales = readFile(Constants.CatalogSalesTableName)

    // Create a mapping from item_sk to sales tuple (sold_date, item_sk, order_number, quantity, sales_price)
    sales map { t => (t(15), Array(t(0), t(15), t(17), t(18), t(21))) }        
  }
  
  def webSalesPerItem() = {
    // Read sales table and tokenize it
    val sales = readFile(Constants.WebSalesTableName)

    // Create a mapping from item_sk to sales tuple (sold_date, item_sk, order_number, quantity, sales_price)
    sales map { t => (t(3), Array(t(0), t(3), t(17), t(18), t(21))) }    
  }

  /**
   * Joins promotions with supplied sales channel
   * Returns (item_sk, (promotion_id, sales))
   */
  def promoJoinSales(promotions: RDD[(String, Array[String])], sales: RDD[(String, Array[String])]) = {
      // join promotions with sales, filter irrelevant attributes, and filter sales not within promotion dates.
	  promotions.join(sales).mapValues(t => (t._1(1), t._1(2), t._1(3), t._2(0), 
			  ( try{t._2(3).toDouble*t._2(4).toDouble} catch{case e:Exception => 0} )))
			  .filter(t => (t._2._2 <= t._2._4 & t._2._4 <= t._2._3)).mapValues(t => (t._1, t._5))
  }
  
  /**
   * Relational part of the promotion workflow
   * TODO: Write a SQL version
   */
  def salesPerPromotion(promotions: RDD[(String, Array[String])]) = {

    // get all sales
    val store_sales = storeSalesPerItem()
    val catalog_sales = catalogSalesPerItem()
    val web_sales = webSalesPerItem()
    
    // join promotions with sales
    val promo_store_sales = promoJoinSales(promotions, store_sales)
    val promo_catalog_sales = promoJoinSales(promotions, catalog_sales)
    val promo_web_sales = promoJoinSales(promotions, web_sales)
    
    // TODO: Watch out order of operations. Which one is most optimized?
    // TODO: Can we print schedule used by Spark?
    
    // union three join results
    val promo_sales = promo_store_sales union promo_catalog_sales union promo_web_sales
    
    // group items together, sum up the sales
    promo_sales.reduceByKey( (a, b) => (a._1, a._2 + b._2) )
  }
  
  /**
   * Run a relational microbenchmark.
   * TODO: Write SQL for this workflow
   */
  def microBench() = {

	  val promotions = promotionsMappedByItems()
	  
	  val total_sales = salesPerPromotion(promotions)

	  // load item table, tokenize it
	  val item = sc.textFile(tpcds_path+"/"+Constants.ItemTableName).map(t => t.split('|')) 

	  // map item_sk with product name
	  val item_mapped = item map { t => try {(t(0), t(21))} catch { case e:Exception => (t(0), "N/A") } }   

	  // join total_sales with item_mapped
	  val report = total_sales.join(item_mapped).map { t => (t._2._1._1, t._2._2, t._2._1._2) } 

	  // TODO: remove this part
	  println("**************RESULT**************")
//	  val result = report.collect()    
//      println("size: " + result.length)
//	  println("contents: \n" + result)
	  
	  report
  }
  
}