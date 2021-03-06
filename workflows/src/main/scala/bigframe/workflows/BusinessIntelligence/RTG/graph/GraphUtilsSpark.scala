/**
 * 
 */
package bigframe.workflows.BusinessIntelligence.RTG.graph

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._
import org.apache.spark.HashPartitioner
/**
 * @author mayuresh
 *
 */
case class GraphUtilsSpark() {

   /**
   * Calculates similarity between two users for every product
   * sim_p(i, j) = 1 - |norm_countsByUser_p(i) - norm_countsByUser_p(j)|
   * Arguments: Seq[(product, counts)] for each user
   * Output: Map(product -> similarity score)
   */
  private def simScore(seq1: Seq[(String, Int)], seq2: Seq[(String, Int)]) = {
    val vector1 = Map(seq1 map {t => t._1 -> t._2}: _*)
    val vector2 = Map(seq2 map {t => t._1 -> t._2}: _*)
    val magnitude1 = vector1.map(_._2).sum.toDouble
    val magnitude2 = vector2.map(_._2).sum.toDouble
    
    (vector1.keySet ++ vector2.keySet) map {t => t -> 
    (1.0 - math.abs(
        vector1.getOrElse(t, 0).toDouble / magnitude1 - 
        vector2.getOrElse(t, 0).toDouble / magnitude2))} toSeq
  }
  
  /**
   * Computes following quantity as a factor in transition probability of 
   * going from user i to user j: 
   * |tweets(j)| / sum_{k in friends i} |tweets(k)|
   */
  private def fraction(j: (Int, Int), friends: Seq[(Int, Int)]): Double
  = {
    val num = j._2 toDouble
    var den = 0.0
    friends foreach (k => (den += k._2))
    println("Num for user: " + j + ": " + num); 
    println("Den for user: " + j + ": " + den); 
    if (den == 0.0) 0.0 else num/den
  }
  
   /**
   * Calculates similarity between every pair of friends for each product
   * Arguments: 
   * 1. (user_id -> Seq[(product, counts)]) map
   * 2. Original graph i.e. (follower -> friend) map
   * Output: Map((user1, user2) -> Map(product, similarity score))
   */
  def similarity(countsByUser: RDD[(Int, Seq[(String, Int)])], 
      graph: RDD[(Int, Int)]) = {
    val firstJoin = countsByUser join graph map {
      t => t._2._2 -> (t._1, t._2._1)}
    firstJoin join countsByUser map {
      t => (t._2._1._1, t._1) -> simScore(t._2._1._2, t._2._2)}
//    countsByUser cartesian countsByUser map {t => (
//        (t._1._1, t._2._1) -> simScore(t._1._2, t._2._2))}
  }
  
  /**
   * Computes influence of a friend in terms of fraction of number of tweets
   * Argument: friends RDD which lists all friends of a user along with 
   * their tweet counts
   */
  def ratios(friends: RDD[(Int, Seq[(Int, Int)])]) = {
    friends flatMap {(t: (Int, Seq[(Int, Int)])) => (t._2 map (
        s => (t._1, s._1) -> fraction(s, t._2)))}
  }
  
  /**
   * Filters the 'friend' relationships in which the participants have not 
   * tweeted.
   * Arguments: 
   * 1. friends: relationships read from graph data
   * 2. tweetsByUser: number of tweets by each user tweeting about any product
   * of interest
   * Returns: Filtered friends along with number of tweets by friend which is 
   * required in computing influence the friend has on the follower
   */
  def filterFriends(friends: RDD[(Int, Int)], tweetsByUser: RDD[(Int, Int)]) 
  = {
    val filteredFollower = tweetsByUser.join(friends) map {t => t._2._2 -> t._1}
    tweetsByUser.join(filteredFollower) map {t => t._2._2 -> (t._1, t._2._1)}
  }
  
  private def scale(similarity: Seq[(String, Double)], ratio: Double) = {
    similarity map {t => t._1 -> (ratio * t._2)}
  }
  
  /**
   * Computes transition probability by scaling similarity scores of user pairs 
   * by ratio computed by ratio(i, j). 
   * Arguments:
   * 1. ratios: @see ratios
   * 2. similarity: @see similarity
   */
  def transitionProbabilities(ratios:RDD[((Int, Int), Double)], 
      similarity: RDD[((Int, Int), Seq[(String, Double)])]) = {
    ratios join similarity map (
        t => t._1 -> scale(t._2._2, t._2._1))
  }
  
  /**
   * Computes teleport probabilities for each user and product as a fraction of 
   * number of mentions of the product by the user and the total number of 
   * mentions of the product.
   * Arguments:
   * 1. countsByUser: Number of tweets by each user for each product
   * 2. countsPerProduct: Number of tweets for each product
   */
  def teleportProbabilities(countsByUser: RDD[(Int, Seq[(String, Int)])], 
      countsPerProduct: org.apache.spark.broadcast.Broadcast[Map[String, Int]]) = {
    countsByUser map {t => t._1 -> (t._2 map {
      s => s._1 -> s._2.toDouble / countsPerProduct.value.getOrElse(s._1, 1)})
    }
  }

  /**
   * A hack to store collected probabilities to be used in iterative computation
   */
  var tranProb: scala.collection.immutable.Map[(Int, Int),Seq[(String, Double)]] = null 
  var teleProb: scala.collection.immutable.Map[Int ,Seq[(String, Double)]] = null
  
  def collectProbabilities(transition: RDD[((Int, Int),Seq[(String, Double)])], 
      teleport: RDD[(Int ,Seq[(String, Double)])]) = {
    tranProb = transition.collect.toMap 
    teleProb = teleport.collect.toMap
  }
  
  def createVertices( 
      transition: RDD[((Int, Int), Seq[(String, Double)])], 
      teleport: RDD[(Int, Seq[(String, Double)])]) = {
//    val initialranks = friends leftOuterJoin teleport mapValues { 
//      case (v, Some(w)) => w 
//      case (v, None) => List()
//    }
//    friends join initialranks map {t => t._1 -> new TRVertex(t._1, t._2._2, 
//        t._2._1 map {_._1} toList, true)}
    
    val friends = (transition map {t => t._1._1 -> (t._1._2, t._2)}) groupByKey(new HashPartitioner(transition.partitions.length))
    
    friends join (teleport, partitioner=friends.partitioner.get) map {t => t._1 -> new TRVertex(t._1, t._2._2, 
            t._2._1 map {_._1} toList, t._2._1, t._2._2, true)}
  }
  
  /**
   * Function to run on each vertex.
   * Arguments:
   * 1. Vertex object
   * 2. All incoming messages
   * 3. Superstep number
   */
  def compute (numIter: Int, gamma: Double):
      (TRVertex, Option[Iterable[TRMessage]], Int) => 
        (TRVertex, Array[TRMessage]) = (self: TRVertex, 
            msgs: Option[Iterable[TRMessage]], superstep: Int) => {

//              val newRanks = 
//                if(msgs == null || msgs.isEmpty)
//                  self.ranks
//                else {
//                  val trans = Map(transition.filter(
//                      t => t._1 == self.id).map(_._2): _*)
//                  val sharedRanks = Map(msgs.getOrElse(List()).map {
//                    m => m.sourceId -> m.rankShare}.asInstanceOf[
//                    Seq[(Int, Seq[(String, Double)])]]: _*)
//                  val joinKeys = trans.keySet.intersect(sharedRanks.keySet)
//                  
//                  val first = Map((joinKeys map {t => t -> transFactor(
//                      trans(t), sharedRanks(t), gamma)}).asInstanceOf[
//                        Seq[(Int, Seq[(String, Double)])]]: _*).map {_._2}.
//                        reduce {                  
//                        (a, b) => transReducer(a, b)
//                      }
//                  
//                  val tele = teleport.filter(
//                      t => t._1 == self.id).map(_._2).first
//                  
//                  teleFactor(first, tele, gamma)
//                }
//              
//              println("At vertex: " + self.id)
//              println("New ranks: " + newRanks)

     val newRanks = 
        if(msgs == null || msgs.isEmpty)
          self.ranks
        else {
          val transitions = msgs getOrElse(List()) flatMap {m => 
//            tranProb.getOrElse((m.sourceId, m.targetId), List())} groupBy (
            Map(self.transInProb: _*).getOrElse(m.sourceId, List())} groupBy (
                _._1) map { case (k,v) => k -> v.map(_._2).sum};
          val teleportations = self.teleProb.toMap//teleProb.getOrElse(self.id, List()) toMap;
          self.ranks map {r => r._1 -> (
              gamma * transitions.getOrElse(r._1, 0.0) * r._2 + 
              (1-gamma) * teleportations.getOrElse(r._1, 0.0))}
        }
      
              val halt = superstep >= numIter
              val msgsOut =
              if (!halt)
                self.outEdges.map(e => new TRMessage(e, self.id, newRanks))
              else
                List()

              (new TRVertex(self.id, newRanks, self.outEdges, self.transInProb, self.teleProb, !halt), 
                  msgsOut toArray)
  }
  
  private def transFactor(seq1: Seq[(String, Double)], 
      seq2: Seq[(String, Double)], 
      gamma: Double) = {
    val vector1 = Map(seq1 map {t => t._1 -> t._2}: _*)
    val vector2 = Map(seq2 map {t => t._1 -> t._2}: _*)
    
    (vector1.keySet ++ vector2.keySet) map {t => t -> 
    (vector1.getOrElse(t, 0.0) * vector2.getOrElse(t, 0.0) * gamma)} toSeq
  }
  
  private def transReducer(seq1: Seq[(String, Double)], 
      seq2: Seq[(String, Double)]) = {
    val vector1 = Map(seq1 map {t => t._1 -> t._2}: _*)
    val vector2 = Map(seq2 map {t => t._1 -> t._2}: _*)
    
    (vector1.keySet ++ vector2.keySet) map {t => t -> 
    (vector1.getOrElse(t, 0.0) + vector2.getOrElse(t, 0.0))} toSeq
  }
  
  private def teleFactor(seq1: Seq[(String, Double)], 
      seq2: Seq[(String, Double)], 
      gamma: Double) = {
    val vector1 = Map(seq1 map {t => t._1 -> t._2}: _*)
    val vector2 = Map(seq2 map {t => t._1 -> t._2}: _*)
    
    (vector1.keySet ++ vector2.keySet) map {t => t -> 
    (vector1.getOrElse(t, 0.0) + vector2.getOrElse(t, 0.0) * (1 - gamma))} toSeq
  }
  
  /**
   * One iteration over twitter rank in standalone implementation
   * TODO: test for correctness
   */
  def iterateRank(ranks: RDD[(Int, Seq[(String, Double)])], 
      trans: RDD[(Int, (Int, Seq[(String, Double)]))], 
      teleport: RDD[(Int, Seq[(String, Double)])], 
      gamma: Double) = {    
    // transition probability times previous rank weighted by gamma
    val term1 = trans.join(ranks, partitioner=trans.partitioner.get) mapValues {
      t => transFactor(t._1._2, t._2, gamma)
    } reduceByKey {
      (a, b) => transReducer(a, b)
    }
    
    // teleport probability weighted by (1-gamma)
    term1 rightOuterJoin teleport mapValues {
      case(Some(v), w) => teleFactor(v, w, gamma)
      case(None, w) => w.map {s => s._1 -> s._2 * (1-gamma)}
    }
  }
    
  /**
   * Find item_sk in the given sequence and return the corresponding rank,
   * 0 by default
   */
  def influence(ranks: Seq[(String, Double)], item_sk: String) = {
	  var influence = 0.0;
	  ranks map {r => if(r._1 == item_sk) influence = r._2; r};
	  influence
  }

}
