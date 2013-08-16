/**
 *
 */
package bigframe.queries

import spark.SparkContext
import SparkContext._

/**
 * Implement this interface such that a query can be run on spark.
 * 
 * @author andy
 *
 */
trait SparkRunnable {
	
	/**
	 * Run the query on spark
	 */
	def run(sc: SparkContext): Unit
}