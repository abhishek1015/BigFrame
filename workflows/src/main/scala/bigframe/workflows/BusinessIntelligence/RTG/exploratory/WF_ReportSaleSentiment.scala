package bigframe.workflows.BusinessIntelligence.RTG.exploratory

import java.util.concurrent.{Executors, ExecutorService}
import java.util.concurrent.Future
import java.sql.Connection
import java.sql.SQLException

import bigframe.workflows.Query
import bigframe.workflows.runnable.HadoopRunnable
import bigframe.workflows.runnable.VerticaRunnable
import bigframe.workflows.BaseTablePath
import bigframe.workflows.BusinessIntelligence.relational.exploratory.PromotedProdHadoop
import bigframe.workflows.BusinessIntelligence.relational.exploratory.ReportSalesHadoop
import bigframe.workflows.BusinessIntelligence.text.exploratory.FilterTweetHadoop

import bigframe.workflows.BusinessIntelligence.text.exploratory.SenAnalyzeHadoop
import bigframe.workflows.BusinessIntelligence.text.exploratory.SenAnalyzeConstant;

import org.apache.hadoop.conf.Configuration

import scala.collection.JavaConversions._

class WF_ReportSaleSentiment(basePath: BaseTablePath, num_iter: Int) extends Query with HadoopRunnable with VerticaRunnable{

	def printDescription(): Unit = {}

	override def runHadoop(mapred_config: Configuration): java.lang.Boolean = {	
		val promotedSKs = Set(new java.lang.Integer(1), new java.lang.Integer(2), new java.lang.Integer(3))
		val iteration = num_iter
		
		val job_getPromotedProd = new PromotedProdHadoop(basePath.relational_path, promotedSKs, mapred_config)
		val job_reportSales = new ReportSalesHadoop(basePath.relational_path, promotedSKs, mapred_config )
		val job_filterTweets = new FilterTweetHadoop(basePath.nested_path, mapred_config);
		val job_senAnalyze = new SenAnalyzeHadoop(SenAnalyzeConstant.FILTERED_TWEETS_PATH, mapred_config)
		val job_tweetByUser = new TweetByUserHadoop(mapred_config)
		val job_tweetByProd = new TweetByProductHadoop(mapred_config)
		val job_sumFriendTweet = new SumFriendsTweetsHadoop(basePath.graph_path, mapred_config)
		val job_userPairProb = new UserPairProbHadoop(mapred_config)
		val job_rankSufferVec = new RandSufferVectorHadoop(mapred_config)
		val job_initialRank = new ComputeInitialRankHadoop(mapred_config)
		val job_simBetweenUser = new SimUserByProdHadoop(basePath.graph_path, mapred_config)
		val job_transitMatrix = new TransitMatrixHadoop(basePath.graph_path, mapred_config)
		val job_splitByProd = new SplitByProdHadoopImpl2(mapred_config)
		val job_twitterRank = new TwitterRankImpl2(iteration, mapred_config)
		val job_joinSenAndInflu = new JoinSenAndInfluHadoop(mapred_config)
		val job_groupSenByProd = new GroupSenByProdHadoop(mapred_config)
		val job_joinSaleAnsSen = new JoinSaleAndSenHadoop(mapred_config)

		val pool: ExecutorService = Executors.newFixedThreadPool(4)
				
		
		val future_getPromotedProd = pool.submit(job_getPromotedProd)
		val future_reportSale = pool.submit(job_reportSales)
		
		
		if(future_getPromotedProd.get()) {
			val future_filterTweets = pool.submit(job_filterTweets)
			
			if(future_filterTweets.get()) {
				val future_senAnalyze = pool.submit(job_senAnalyze)
				val future_tweetByUser = pool.submit(job_tweetByUser)
				val future_tweetByProd = pool.submit(job_tweetByProd)
				
				if(future_tweetByUser.get() && future_tweetByProd.get()) {
					val future_sumFriendTweet = pool.submit(job_sumFriendTweet)
					val future_userPairProb = pool.submit(job_userPairProb)
					val future_rankSufferVec = pool.submit(job_rankSufferVec)
					val future_initialRank = pool.submit(job_initialRank)
					
					if(future_sumFriendTweet.get() && future_userPairProb.get()
							&& future_rankSufferVec.get() && future_initialRank.get()) {
						val future_simBetweenUser = pool.submit(job_simBetweenUser)
						
						if(future_simBetweenUser.get()) {
							val future_transitMatrix = pool.submit(job_transitMatrix)
							
							if(future_transitMatrix.get()) {
								val future_splitByProd = pool.submit(job_splitByProd)
								
								if(future_splitByProd.get()) {
									val future_twitterRank = pool.submit(job_twitterRank)
									
									if(future_twitterRank.get() && future_senAnalyze.get()) {
										val future_joinSenAndInflu = pool.submit(job_joinSenAndInflu)
										
										if(future_joinSenAndInflu.get()) {
											val future_groupSenByProd = pool.submit(job_groupSenByProd)
											
											if(future_groupSenByProd.get()) {
												val future_joinSaleAnsSen = pool.submit(job_joinSaleAnsSen)
												
												if(future_joinSaleAnsSen.get()) {
													pool.shutdown()
													return true
												}														
											}
										}					
									}						
								}
							}
						}
					}
				}			
			}
		}
			
		pool.shutdown()
		return false
	}

	override def prepareVerticaTables(connection: Connection): Unit = {}
	
	override def runVertica(connection: Connection): Boolean = {
		
		try {
			val stmt = connection.createStatement();
						
			val drop_promotionSelected = "DROP TABLE IF EXISTS promotionSelected"
			val create_promotionSelected = "CREATE TABLE promotionSelected (promo_id char(16), item_sk int," +
					"start_date_sk int, end_date_sk int)"
			stmt.execute(drop_promotionSelected)
			stmt.execute(create_promotionSelected)
			
			val promotSKs = "1"
			val lower = 1
			val upper = 100
			val query_promotionSelected = "INSERT INTO promotionSelected " + 
					"	SELECT p_promo_id, p_item_sk, p_start_date_sk, p_end_date_sk " +
					"	FROM promotion " +
//					"	WHERE p_promo_sk in (" + promotSKs + ")" 
					"	WHERE " + lower + " < p_promo_sk AND p_promo_sk < " + upper 
			

			stmt.executeUpdate(query_promotionSelected)
			
			val drop_promotedProduct = "DROP TABLE IF EXISTS promotedProduct"
			val create_promotedProduct = "CREATE TABLE promotedProduct (item_sk int, product_name char(50))" 
			stmt.execute(drop_promotedProduct)
			stmt.execute(create_promotedProduct)	
				
			val query_pomotedProduct = "INSERT INTO promotedProduct " +
					"	SELECT i_item_sk, i_product_name " +
					"	FROM item JOIN promotionSelected " +
					"	ON i_item_sk = item_sk "
						
			stmt.executeUpdate(query_pomotedProduct)
			
			
			val drop_RptSalesByProdCmpn = "DROP TABLE IF EXISTS RptSalesByProdCmpn"
			val create_RptSalesByProdCmpn = "CREATE TABLE RptSalesByProdCmpn " +
					"	(promo_id char(16), item_sk int, totalsales float)"
			stmt.execute(drop_RptSalesByProdCmpn)
			stmt.execute(create_RptSalesByProdCmpn)
						
			val query_RptSalesByProdCmpn = "INSERT INTO RptSalesByProdCmpn " +
					"		SELECT p.promo_id, p.item_sk, sum(price*quantity) as totalsales " +
					"		FROM" + 
					"			(SELECT ws_sold_date_sk as sold_date_sk, ws_item_sk as item_sk, ws_sales_price as price, ws_quantity as quantity " +
					"			FROM web_sales " + 
					"			UNION ALL " + 
					"			SELECT ss_sold_date_sk as sold_date_sk, ss_item_sk as item_sk, ss_sales_price as price, ss_quantity as quantity "  +
					"			FROM store_sales" + 
					"			UNION ALL" + 
					"			SELECT cs_sold_date_sk as sold_date_sk, cs_item_sk as item_sk, cs_sales_price as price, cs_quantity as quantity" +
					"			FROM catalog_sales) sales"  +
					"			JOIN promotionSelected p "  +
					"			ON sales.item_sk = p.item_sk" +
					"		WHERE " + 
					"			p.start_date_sk <= sold_date_sk " +
					"			AND" + 
					"			sold_date_sk <= p.end_date_sk" +
					"		GROUP BY" + 
					"			p.promo_id, p.item_sk "
							
			stmt.executeUpdate(query_RptSalesByProdCmpn)
			
			val drop_relevantTweet = "DROP TABLE IF EXISTS relevantTweet"
			val create_relevantTweet = "CREATE TABLE relevantTweet" +
					"	(item_sk int, user_id int, text varchar(200) )"
			
			stmt.execute(drop_relevantTweet)
			stmt.execute(create_relevantTweet)
			
			val query_relevantTweet = "INSERT INTO relevantTweet" +
					"	SELECT item_sk, user_id, text" +
					"	FROM " +
					"		(SELECT item_sk, tweet_id" +
					"		FROM promotedProduct " +
					"		JOIN entities" +
					"		ON product_name = hashtag) t" +
					"	JOIN tweet " +
					"	ON tweet_id = id" 
					
			stmt.executeUpdate(query_relevantTweet)

			val drop_senAnalyse = "DROP TABLE IF EXISTS senAnalyse"
			val create_senAnalyse = "CREATE TABLE senAnalyse" +
					"	(item_sk int, user_id int, sentiment_score int)"
			
			stmt.execute(drop_senAnalyse)
			stmt.execute(create_senAnalyse)
			
			val query_senAnalyse = "INSERT INTO senAnalyse" +
					"	SELECT item_sk, user_id, sum(sentiment(text)) as sum_score" +
					"	FROM relevantTweet" +
					"	GROUP BY" +
					"	item_sk, user_id"
			stmt.executeUpdate(query_senAnalyse)
			
			val drop_tweetByUser = "DROP TABLE IF EXISTS tweetByUser"
			val create_tweetByUser = "CREATE TABLE tweetByUser (user_id int, num_tweets int)"
			
			stmt.execute(drop_tweetByUser)
			stmt.execute(create_tweetByUser)
			
			val query_tweetByUser = "INSERT INTO tweetByUser" +
					"	SELECT user_id, count(*)" +
					"	FROM relevantTweet" +
					"	GROUP BY" +
					"	user_id"
			stmt.executeUpdate(query_tweetByUser)
					
			val drop_tweetByProd = "DROP TABLE IF EXISTS tweetByProd"
			val create_tweetByProd = "CREATE TABLE tweetByProd (item_sk int, num_tweets int)"
																																																																																																																		
			stmt.execute(drop_tweetByProd)
			stmt.execute(create_tweetByProd)
			
			val query_tweetByProd = "INSERT INTO tweetByProd" +
					"	SELECT item_sk, count(*)" +
					"	FROM relevantTweet" +
					"	GROUP BY" +
					"	item_sk"
			stmt.executeUpdate(query_tweetByProd)
			

			
								
			val drop_sumFriendTweets = "DROP TABLE IF EXISTS sumFriendTweets"
			val create_sumFriendTweets = "CREATE TABLE sumFriendTweets (follower_id int, num_friend_tweets int)"
				
			stmt.execute(drop_sumFriendTweets)
			stmt.execute(create_sumFriendTweets)
			
			val query_sumFriendTweets = "INSERT INTO sumFriendTweets" +
					"	SELECT user_id, " +
					"		CASE WHEN num_friend_tweets > 0 THEN num_friend_tweets" +
					"			 ELSE 0" +
					"		END" +
					"	FROM" +
					"		(SELECT user_id, sum(friend_tweets) as num_friend_tweets" +
					"		FROM tweetByUser LEFT OUTER JOIN" +
					"			(SELECT follower_id, friend_id, num_tweets as friend_tweets" +
					"			FROM tweetByUser JOIN twitter_graph" +
					"	 		ON user_id = friend_id) f" +
					"		ON user_id = follower_id" +
					"		GROUP BY " +
					"		user_id) result"
			
			stmt.executeUpdate(query_sumFriendTweets)
			
			val drop_mentionProb = "DROP TABLE IF EXISTS mentionProb"
			val create_mentionProb = "CREATE TABLE mentionProb (item_sk int, user_id int, prob float)"
				
			stmt.execute(drop_mentionProb)
			stmt.execute(create_mentionProb)
			
			val query_mentionProb = "INSERT INTO mentionProb" +
					"	SELECT item_sk, t.user_id, r.num_tweets/t.num_tweets" +
					"	FROM tweetByUser as t JOIN " +
					"		(SELECT item_sk, user_id, count(*) as num_tweets" +
					"		FROM relevantTweet" +
					"		GROUP BY" +
					"		item_sk, user_id) r" +
					"	ON t.user_id = r.user_id"
			
			stmt.executeUpdate(query_mentionProb)
			
			val drop_simUserByProd = "DROP TABLE IF EXISTS simUserByProd"
			val create_simUserByProd = "CREATE TABLE simUserByProd " +
					"	(item_sk int, follower_id int, friend_id int, similarity float)"
			
			stmt.execute(drop_simUserByProd)
			stmt.execute(create_simUserByProd)
			
			val query_simUserByProd = "INSERT INTO simUserByProd" +
					"	SELECT f.item_sk, follower_id, friend_id, 1 - ABS(follower_prob - prob)" +
					"	FROM " +
					"		(SELECT item_sk, follower_id, friend_id, prob as follower_prob" +
					"		FROM mentionProb JOIN twitter_graph " +
					"		ON user_id = follower_id) f" +
					"	JOIN mentionProb " +
					"	ON	friend_id = user_id" +
					"	UNION ALL" +
					"	SELECT item_sk, user_id, user_id, 0" +
					"	FROM mentionProb"
					
			stmt.executeUpdate(query_simUserByProd)
					
					
			val drop_transitMatrix = "DROP TABLE IF EXISTS transitMatrix"
			val create_transitMatrix = "CREATE TABLE transitMatrix (item_sk int, follower_id int, friend_id int, transit_prob float)"
				
			stmt.execute(drop_transitMatrix)
			stmt.execute(create_transitMatrix)
			
			val query_transitMatrix = "INSERT INTO transitMatrix" +
					"	SELECT item_sk, follower_id, friend_id, " +
					"		CASE WHEN follower_id != friend_id THEN num_tweets/num_friend_tweets*similarity" +
					"			 ELSE 0" +
					"		END" +
					"	FROM" +
					"		(SELECT item_sk, t1.follower_id, friend_id, similarity, num_friend_tweets" +
					"		FROM simUserByProd t1 JOIN sumFriendTweets t2" +
					"		ON t1.follower_id = t2.follower_id) t3" +
					"	JOIN tweetByUser" +
					"	ON friend_id = user_id"
					
					
			stmt.executeUpdate(query_transitMatrix)
			
			
			val drop_randSufferVec = "DROP TABLE IF EXISTS randSuffVec"
			val create_randSuffVec = "CREATE TABLE randSUffVec (item_sk int, user_id int, prob float)"
				
			stmt.execute(drop_randSufferVec)
			stmt.execute(create_randSuffVec)
			
			val query_randSuffVec = "INSERT INTO randSuffVec" +
					"	SELECT t1.item_sk, user_id, t1.num_tweets/t2.num_tweets" +
					"	FROM" +
					"		(SELECT item_sk, user_id, count(*) as num_tweets" +
					"		FROM relevantTweet" +
					"		GROUP BY" +
					"		item_sk, user_id) t1" +
					"	JOIN tweetByProd t2" +
					"	ON t1.item_sk = t2.item_sk"
			
			stmt.executeUpdate(query_randSuffVec)
			
			val drop_initalRank = "DROP TABLE IF EXISTS initialRank"
			val create_initialRank = "CREATE TABLE initialRank (item_sk int, user_id int, rank_score float)"
				
			stmt.execute(drop_initalRank)
			stmt.execute(create_initialRank)
			
			val query_initialRank = "INSERT INTO initialRank" +
					"	SELECT t1.item_sk, user_id, 1/num_users" +
					"	FROM " +
					"		(SELECT item_sk, COUNT(DISTINCT user_id) as num_users" +
					"		FROM relevantTweet" +
					"		GROUP BY" +
					"		item_sk) t1 " +
					"	JOIN" +
					"		(SELECT DISTINCT *" +
					"		FROM" +
					"			(SELECT item_sk, user_id" +
					"			FROM relevantTweet) t2" +
					"		) t3" +
					"	ON t1.item_sk = t3.item_sk"
			stmt.executeUpdate(query_initialRank)
					
			val alpha = 0.85
			for(iteration <- 1 to num_iter) {
						
				val drop_twitterRank = "DROP TABLE IF EXISTS twitterRank"+iteration
				val create_twitterRank = "CREATE TABLE twitterRank"+iteration+" (item_sk int, user_id int, rank_score float)"
			
				stmt.execute(drop_twitterRank)
				stmt.execute(create_twitterRank)
					
				val twitterRank_last = if(iteration == 1) "initialRank" else "twitterRank"+(iteration-1)				

				val query_twitterRank = "INSERT INTO twitterRank"+iteration +
						"	SELECT t4.item_sk, t4.user_id, " +
						"		CASE WHEN sum_follower_score > 0 THEN " + alpha + " * sum_follower_score + " + (1-alpha) +" * prob" +
						"			 ELSE " + (1-alpha) +" * prob" +
						"		END" +
						"	FROM" +
						"		(SELECT t1.item_sk, follower_id, sum(transit_prob * rank_score) as sum_follower_score" +
						"		FROM transitMatrix t1, " + twitterRank_last +" t2" +
						"		WHERE t1.friend_id = t2.user_id AND t1.item_sk = t2.item_sk " +
						"			AND t1.follower_id != t2.user_id" +
						"		GROUP BY " +
						"		t1.item_sk, follower_id) t3" +
						"	RIGHT JOIN randSUffVec t4" +
						"	ON t3.item_sk = t4.item_sk AND t3.follower_id = t4.user_id"
			
				stmt.executeUpdate(query_twitterRank)
				
			}
			
			val drop_RptSAProdCmpn = "DROP TABLE IF EXISTS RptSAProdCmpn"
			val create_RptSAProdCmpn = "CREATE TABLE RptSAProdCmpn (promo_id char(16), item_sk int, totalsales float , total_sentiment float)"
					
			stmt.execute(drop_RptSAProdCmpn)
			stmt.execute(create_RptSAProdCmpn)
			
			val query_RptSAProdCmpn = "INSERT INTO RptSAProdCmpn" +
					"	SELECT promo_id, t4.item_sk, totalsales, total_sentiment" +
					"	FROM " +
					"		(SELECT t1.item_sk, sum(rank_score * sentiment_score) as total_sentiment" +
					"		FROM senAnalyse t1, twitterRank" + num_iter + " t2" +
					"		WHERE t1.item_sk = t2.item_sk AND t1.user_id = t2.user_id" +
					"		GROUP BY" +
					"		t1.item_sk) t3" +
					"	JOIN RptSalesByProdCmpn t4 " +
					"	ON t3.item_sk = t4.item_sk"				
			
			if (stmt.executeUpdate(query_RptSAProdCmpn) > 0) return true else return false

		} catch {
			case sqle :
				SQLException => sqle.printStackTrace()
			case e :
				Exception => e.printStackTrace()
		}
		
		return false
	}
		
}