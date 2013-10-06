/**
 *
 */
package bigframe.spark.graph

/**
 * @author mayuresh
 *
 */
import org.apache.spark.bagel._
import org.apache.spark.bagel.Bagel._

@serializable class TRVertex (
  val id: Int, 
  val ranks: Seq[(String, Double)], 
  val outEdges: Seq[Int],
  val active: Boolean ) extends Vertex

@serializable class TRMessage (
  val targetId: Int,
  val sourceId: Int ) extends Message[Int]

/** Default combiner that simply appends messages together (i.e. performs no aggregation) */
class TRCombiner extends Combiner[TRMessage, Seq[TRMessage]] 
with Serializable {
  def createCombiner(msg: TRMessage): Seq[TRMessage] =
    List(msg)
  def mergeMsg(combiner: Seq[TRMessage], msg: TRMessage): Seq[TRMessage] =
    combiner :+ msg
  def mergeCombiners(a: Seq[TRMessage], b: Seq[TRMessage]): Seq[TRMessage] =
    a ++ b
}