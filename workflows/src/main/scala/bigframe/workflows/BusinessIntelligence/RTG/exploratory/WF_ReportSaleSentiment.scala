package bigframe.workflows.BusinessIntelligence.RTG.exploratory

import java.util.concurrent.{Executors, ExecutorService}
import java.util.concurrent.Future
import java.sql.Connection
import java.sql.SQLException

import bigframe.workflows.Query
import bigframe.workflows.runnable.HadoopRunnable
import bigframe.workflows.runnable.VerticaRunnable
import bigframe.workflows.runnable.HiveRunnable
import bigframe.workflows.runnable.SharkRunnable
import bigframe.workflows.BaseTablePath
import bigframe.workflows.BusinessIntelligence.relational.exploratory.PromotedProdHadoop
import bigframe.workflows.BusinessIntelligence.relational.exploratory.ReportSalesHadoop
import bigframe.workflows.BusinessIntelligence.text.exploratory.FilterTweetHadoop

import bigframe.workflows.BusinessIntelligence.text.exploratory.SenAnalyzeHadoop
import bigframe.workflows.BusinessIntelligence.text.exploratory.SenAnalyzeConstant;

import org.apache.hadoop.conf.Configuration

import scala.collection.JavaConversions._

class WF_ReportSaleSentiment(basePath: BaseTablePath, num_iter: Int) extends Query 
					with HadoopRunnable with VerticaRunnable with HiveRunnable with SharkRunnable{

	def printDescription(): Unit = {}

	override def runHadoop(mapred_config: Configuration): java.lang.Boolean = {	
		var promotedSKs = Set[java.lang.Integer]()
//		val promotedSKs = Set(new java.lang.Integer(1), new java.lang.Integer(2), new java.lang.Integer(3))
		
		for(i <- 1 to 300)
			promotedSKs += new java.lang.Integer(i)
		
		val iteration = num_iter
		
		val job_getPromotedProd = new PromotedProdHadoop(basePath.relational_path, promotedSKs, mapred_config)
		val job_reportSales = new ReportSalesHadoop(basePath.relational_path, promotedSKs, mapred_config )
		val job_filterTweets = new FilterTweetHadoop(basePath, mapred_config);
		val job_senAnalyze = new SenAnalyzeHadoop(SenAnalyzeConstant.FILTERED_TWEETS_PATH, mapred_config)
		val job_tweetByUser = new TweetByUserHadoop(mapred_config)
		val job_tweetByProd = new TweetByProductHadoop(mapred_config)
		val job_sumFriendTweet = new SumFriendsTweetsHadoop(basePath.graph_path, mapred_config)
		val job_userPairProb = new UserPairProbHadoop(mapred_config)
		val job_rankSufferVec = new RandSufferVectorHadoop(mapred_config)
		val job_initialRank = new ComputeInitialRankHadoop(mapred_config)
		val job_simBetweenUser = new SimUserByProdHadoop(basePath.graph_path, mapred_config)
		val job_transitMatrix = new TransitMatrixHadoop(basePath.graph_path, mapred_config)
		val job_twitterRank = new TwitterRankImpl(iteration, mapred_config)
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
			
		pool.shutdown()
		return false
	}

	
		/*
	 * Prepeare the basic tables before run the Hive query
	 */
	override def prepareHiveTables(connection: Connection): Unit = {
		
		val itemHDFSPath = basePath.relational_path + "/item"
		val web_salesHDFSPath = basePath.relational_path + "/web_sales"
		val catalog_salesHDFSPath = basePath.relational_path + "/catalog_sales"
		val store_salesHDFSPath = basePath.relational_path + "/store_sales"
		val promotionHDFSPath = basePath.relational_path + "/promotion"
		val customerHDFSPath =  basePath.relational_path + "/customer"
		val date_dimHDFSPath = basePath.relational_path + "/date_dim"
		
		val twitter_grapgHDFSPath = basePath.graph_path
		val tweets_HDFSPath = basePath.nested_path
		
		val stmt = connection.createStatement();
		
		
		val add_JsonSerde = "ADD JAR "
		
		
		val dropWebSales = "DROP TABLE web_sales"
		val dropStoreSales = "DROP TABLE store_sales"  
		val dropCatalogSales = "DROP TABLE catalog_sales"
		val dropItem = "DROP TABLE item"
		val dropPromot = "DROP TABLE promotion"
		val dropCust = "DROP TABLE customer"
		val dropDateDim = "DROP TABLE date_dim"
		val dropGraph = "DROP TABLE twitter_graph"
		val dropTweets = "DROP TABLE tweets"

		
		stmt.execute(dropWebSales)
		stmt.execute(dropCatalogSales)
		stmt.execute(dropStoreSales)
		stmt.execute(dropItem)
		stmt.execute(dropPromot)
		stmt.execute(dropCust)
		stmt.execute(dropDateDim)
		stmt.execute(dropGraph)
		stmt.execute(dropTweets)
		
		val createWebSales = "create external table web_sales" + 
					"(" + 
					    "ws_sold_date_sk           int," + 
					    "ws_sold_time_sk           int," + 
					    "ws_ship_date_sk           int," + 
					    "ws_item_sk                int," + 
					    "ws_bill_customer_sk       int," + 
					    "ws_bill_cdemo_sk          int," + 
					    "ws_bill_hdemo_sk          int," + 
					    "ws_bill_addr_sk           int," + 
					    "ws_ship_customer_sk       int," + 
					    "ws_ship_cdemo_sk          int," + 
					    "ws_ship_hdemo_sk          int," + 
					    "ws_ship_addr_sk           int," + 
					    "ws_web_page_sk            int," + 
					    "ws_web_site_sk            int," + 
					    "ws_ship_mode_sk           int," + 
					    "ws_warehouse_sk           int," + 
					    "ws_promo_sk               int," + 
					    "ws_order_number           int," + 
					    "ws_quantity               int," + 
					    "ws_wholesale_cost         float," + 
					    "ws_list_price             float," + 
					    "ws_sales_price            float," + 
					    "ws_ext_discount_amt       float," + 
					    "ws_ext_sales_price        float," + 
					    "ws_ext_wholesale_cost     float," + 
					    "ws_ext_list_price         float," + 
					    "ws_ext_tax                float," + 
					    "ws_coupon_amt             float," + 
					    "ws_ext_ship_cost          float," + 
					    "ws_net_paid               float," + 
					    "ws_net_paid_inc_tax       float," + 
					    "ws_net_paid_inc_ship      float," + 
					    "ws_net_paid_inc_ship_tax  float," + 
					    "ws_net_profit             float" +                 
					")" +
					"row format delimited fields terminated by \'|\' " + "\n" +
					"location " + "\'" + web_salesHDFSPath + "\'"
		
		val createCatalogSales = "create external table catalog_sales" +
					"(" +
					    "cs_sold_date_sk           int                       ," +
					    "cs_sold_time_sk           int                       ," +
					    "cs_ship_date_sk           int                       ," +
					    "cs_bill_customer_sk       int                       ," +
					    "cs_bill_cdemo_sk          int                       ," +
					    "cs_bill_hdemo_sk          int                       ," +
					    "cs_bill_addr_sk           int                       ," +
					    "cs_ship_customer_sk       int                       ," +
					    "cs_ship_cdemo_sk          int                       ," +
					    "cs_ship_hdemo_sk          int                       ," +
					    "cs_ship_addr_sk           int                       ," +
					    "cs_call_center_sk         int                       ," +
					    "cs_catalog_page_sk        int                       ," +
					    "cs_ship_mode_sk           int                       ," +
					    "cs_warehouse_sk           int                       ," +
					    "cs_item_sk                int               		," +
					    "cs_promo_sk               int                       ," +
					    "cs_order_number           int               		," +
					    "cs_quantity               int                       ," +
					    "cs_wholesale_cost         float                  ," +
					    "cs_list_price             float                  ," +
					    "cs_sales_price            float                  ," +
					    "cs_ext_discount_amt       float                  ," +
					    "cs_ext_sales_price        float                  ," +
					    "cs_ext_wholesale_cost     float                  ," +
					    "cs_ext_list_price         float                  ," +
					    "cs_ext_tax                float                  ," +
					    "cs_coupon_amt             float                  ," +
					    "cs_ext_ship_cost          float                  ," +
					    "cs_net_paid               float                  ," +
					    "cs_net_paid_inc_tax       float                  ," +
					    "cs_net_paid_inc_ship      float                  ," +
					    "cs_net_paid_inc_ship_tax  float                  ," +
					    "cs_net_profit             float                  " +
					")" +
					"row format delimited fields terminated by \'|\' " + "\n" + 
					"location " + "\'" + catalog_salesHDFSPath + "\'"
		
					
		val createStoreSales = "create external table store_sales" +
					"(" +
					    "ss_sold_date_sk           int                       ," +
					    "ss_sold_time_sk           int                       ," +
					    "ss_item_sk                int               		," +
					    "ss_customer_sk            int                       ," +
					    "ss_cdemo_sk               int                       ," +
					    "ss_hdemo_sk               int                       ," +
					    "ss_addr_sk                int                       ," +
					    "ss_store_sk               int                       ," +
					    "ss_promo_sk               int                       ," +
					    "ss_ticket_number          int               		," +
					    "ss_quantity               int                       ," +
					    "ss_wholesale_cost         float                  ," +
					    "ss_list_price             float                  ," +
					    "ss_sales_price            float                  ," +
					    "ss_ext_discount_amt       float                  ," +
					    "ss_ext_sales_price        float                  ," +
					    "ss_ext_wholesale_cost     float                  ," +
					    "ss_ext_list_price         float                  ," +
					    "ss_ext_tax                float                  ," +
					    "ss_coupon_amt             float                  ," +
					    "ss_net_paid               float                  ," +
					    "ss_net_paid_inc_tax       float                  ," +
					    "ss_net_profit             float                  " +
					")" +
					"row format delimited fields terminated by \'|\' " + "\n" +
					"location" + "\'" + store_salesHDFSPath + "\'"

					
		val createItem = "create external table item" + 
					"(" +
					    "i_item_sk                 int," + 
					    "i_item_id                 string," + 
					    "i_rec_start_date          timestamp," + 
					    "i_rec_end_date            timestamp," + 
					    "i_item_desc               string," + 
					    "i_current_price           float," + 
					    "i_wholesale_cost          float," +
					    "i_brand_id                int," +
					    "i_brand                   string," +
					    "i_claws_id                int," +
					    "i_class                   string," +
					    "i_category_id             int," +
					    "i_category                string," +
					    "i_manufact_id             int," +
					    "i_manufact                string," +
					    "i_size                    string," +
					    "i_formulation             string," +
					    "i_color                   string," +
					    "i_units                   string," +
					    "i_container               string," +
					    "i_manager_id              int," +
					    "i_product_name            string" +
					")"  +
					"row format delimited fields terminated by \'|\' " + "\n" +
					"location " + "\'" + itemHDFSPath + "\'"
		
		val createCust = "create external table customer" + 
				"(" +
					    "c_customer_sk             int," +
					    "c_customer_id             string," +
					    "c_current_cdemo_sk        int," +
					    "c_current_hdemo_sk        int," +
					    "c_current_addr_sk         int," +
					    "c_first_shipto_date_sk    int," +
					    "c_first_sales_date_sk     int," +
					    "c_salutation              string," +
					    "c_first_name              string," +
					    "c_last_name               string," +
					    "c_preferred_cust_flag     string," +
					    "c_birth_day               int," +
					    "c_birth_month             int," +
					    "c_birth_year              int," +
					    "c_birth_country           string," +
					    "c_login                   string," +
					    "c_email_address           string," +
					    "c_last_review_date_sk        string" +
					")" + 
					"row format delimited fields terminated by \'|\' " + "\n" +
					"location " + "\'" + customerHDFSPath + "\'"
		
		val createPromot = "create external table promotion" + 
					"(" +
					    "p_promo_sk                int," +
					    "p_promo_id                string," +
					    "p_start_date_sk           int," +
					    "p_end_date_sk             int," +
					    "p_item_sk                 int," +
					    "p_cost                    float," +
					    "p_response_target         int," +
					    "p_promo_name              string," +
					    "p_channel_dmail           string," +
					    "p_channel_email           string," +
					    "p_channel_catalog         string," +
					    "p_channel_tv              string," +
					    "p_channel_radio           string," +
					    "p_channel_press           string," +
					    "p_channel_event           string," +
					    "p_channel_demo            string," +
					    "p_channel_details         string," +
					    "p_purpose                 string," +
					    "p_discount_active         string " +
					")" + 
					"row format delimited fields terminated by \'|\' " + "\n" +
					"location " + "\'" + promotionHDFSPath + "\'"
					
		val createDateDim = "create external table date_dim" +
				"	(" +
				"		d_date_sk 			int," +
				"		d_date_id 			string," +
				"		d_date 				string," +
				"		d_month_seq 		int," +
				"		d_week_seq 			int," +
		    	"		d_quarter_seq 		int," +
		    	"		d_year 				int," +
		    	"		d_dow 				int," +
		    	"		d_moy 				int," +
		    	"		d_dom 				int," +
		    	"		d_qoy 				int," +
		    	"		d_fy_year 			int," + 
		    	"		d_fy_quarter_seq 	int," +
		    	"		d_fy_week_seq 		int," +
		    	"		d_day_name 			string," + 
		    	"		d_quarter_name 		string," +
		    	"		d_holiday 			string," + 
		    	"		d_weekend 			string," + 
		    	"		d_following_holiday string," +
		    	"		d_first_dom 		int," + 
		    	"		d_last_dom 			int," + 
		    	"		d_same_day_ly 		int," + 
		    	"		d_same_day_lq 		int," + 
		    	"		d_current_day 		string," + 
		    	"		d_current_week 		string," +
		    	"		d_current_month 	string," + 
		    	"		d_current_quarter 	string," +
		    	"		d_current_year 		string"  +
		    	"	)" +
				"	row format delimited fields terminated by \'|\' " +
				"	location " + "\'" + date_dimHDFSPath + "\'"
					
		val createGraph = "create external table twitter_graph" +
				"	(" +
				"		friend_id	int," +
				"		follower_id	int" +
				"	)" +
				"	row format delimited fields terminated by \'|\'" +
				"	location \'" + twitter_grapgHDFSPath +"\'"
				
		/**
		 * TO DO, create json format table
		 */
		val createTweets = "create external table tweets" +
				"	(" +
				"		contributors string," +
				"		coordinates string," +
				"		created_at string," +
				"		entities struct<hashtags:array<string>, " +
				"						urls:array<struct<expanded_url:string, " +
				"											indices:array<int>, " +
				"											url:string>>, " +
				"						user_mentions:array<struct<id:string," +
				"													id_str:string, " +
				"													indices:array<int>, " +
				"													name:string, " +
				"													screen_name:string>>>," +
				"		favorited string," +
				"		geo string," +
				"		id int," +
				"		id_str string," +
				"		in_reply_to_screen_name string," +
				"		in_reply_to_status_id string," +
				"		in_reply_to_status_id_str string," +
				"		in_reply_to_user_id string," +
				"		in_reply_to_user_id_str string," +
				"		place string," +
				"		retweet_count string," +
				"		retweeted string," +
				"		source string," +
				"		text string," +
				"		truncated string," +
				"		user struct<contributors_enabled:string, " +
				"					created_at:string, " +
				"					description:string, " +
				"					favourites_count:string, " +
				"					follow_request_sent:string, " +
				"					followers_count:string, " +
				"					following:string, " +
				"					friends_count:string, " +
				"					geo_enabled:string, " +
				"					id:int, " +
				"					id_str:string, " +
				"					lang:string, " +
				"					listed_count:string, " +
				"					`location`:string, " +
				"					name:string, " +
				"					notifications:string, " +
				"					profile_background_color:string, " +
				"					profile_background_image_url:string, " +
				"					profile_background_tile:string," +
				"					profile_image_url:string," +
				" 					profile_link_color:string, " +
				"					profile_sidebar_border_color:string, " +
				"					profile_sidebar_fill_color:string, " +
				"					profile_text_color:string, " +
				"					profile_use_background_image:string, " +
				"					protected:string, screen_name:string, " +
				"					show_all_inline_media:string, " +
				"					statuses_count:string, " +
				"					time_zone:string, " +
				"					url:string, " +
				"					utc_offset:string, " +
				"					verified:string>" +
				"	)" +
				"	ROW FORMAT SERDE \'org.openx.data.jsonserde.JsonSerDe\'" +
				"	location \'" + tweets_HDFSPath +"\'"
					
		stmt.execute(createWebSales)
		stmt.execute(createCatalogSales)
		stmt.execute(createStoreSales)
		stmt.execute(createItem)
		stmt.execute(createCust)
		stmt.execute(createPromot)
		stmt.execute(createDateDim)
		stmt.execute(createGraph)
		stmt.execute(createTweets)
		
		stmt.execute("create temporary function sentiment as \'bigframe.workflows.util.SenExtractorHive\'")
		stmt.execute("create temporary function isWithinDate as \'bigframe.workflows.util.WithinDateHive\'")
		
	}
	
	def cleanUpHiveImpl1(connection: Connection): Unit = {
		
		val stmt = connection.createStatement()	
		
		val list_drop = Seq("DROP TABLE IF EXISTS promotionSelected",
							"DROP VIEW IF EXISTS promotedProduct",
							"DROP VIEW IF EXISTS RptSalesByProdCmpn",
							"DROP TABLE IF EXISTS relevantTweet",
							"DROP VIEW IF EXISTS senAnalyse",
							"DROP TABLE IF EXISTS tweetByUser",
							"DROP TABLE IF EXISTS tweetByProd",
							"DROP VIEW IF EXISTS sumFriendTweets",
							"DROP TABLE IF EXISTS mentionProb",
							"DROP VIEW IF EXISTS simUserByProd",
							"DROP TABLE IF EXISTS transitMatrix",
							"DROP TABLE IF EXISTS randSuffVec",
							"DROP TABLE IF EXISTS initialRank")
		
		list_drop.foreach(stmt.execute(_))
									
		for(iteration <- 1 to num_iter) {
							
			val drop_twitterRank = "DROP TABLE IF EXISTS twitterRank"+iteration

			stmt.execute(drop_twitterRank)
				
		}
	}
	
	override def cleanUpHive(connection: Connection): Unit = {
		//cleanUpHiveImpl1(connection)
	}
	
	
	def runHiveImpl1(connection: Connection) : Boolean = {
			
		try {
			val stmt = connection.createStatement();			
						
			val promotSKs = "1"
			val lower = 1
			val upper = 300
			
			val drop_promotionSelected = "DROP TABLE IF EXISTS promotionSelected"
			val create_promotionSelected = "CREATE TABLE promotionSelected (promo_id string, item_sk int," +
					"start_date_sk int, end_date_sk int)"
			
			val query_promotionSelected = "INSERT INTO TABLE promotionSelected" +
					"	SELECT p_promo_id, p_item_sk, p_start_date_sk, p_end_date_sk " +
					"	FROM promotion " +
					"	WHERE " + lower + " <= p_promo_sk AND p_promo_sk <= " + upper +
					"	AND p_item_sk IS NOT NULL AND p_start_date_sk IS NOT NULL AND p_end_date_sk IS NOT NULL"
			stmt.execute(drop_promotionSelected)
			stmt.execute(create_promotionSelected)
			stmt.execute(query_promotionSelected)

			
			val drop_promotedProduct = "DROP VIEW IF EXISTS promotedProduct"
			val create_promotedProduct = "CREATE VIEW promotedProduct (item_sk, product_name, start_date_sk, end_date_sk) AS" +
					"	SELECT i_item_sk, i_product_name, start_date_sk, end_date_sk " +
					"	FROM item JOIN promotionSelected " +
					"	ON item.i_item_sk = promotionSelected.item_sk" +
					"	WHERE i_product_name IS NOT NULL"
			stmt.execute(drop_promotedProduct)
			stmt.execute(create_promotedProduct)	
							
			
			val drop_RptSalesByProdCmpn = "DROP VIEW IF EXISTS RptSalesByProdCmpn"
			val create_RptSalesByProdCmpn = "CREATE VIEW RptSalesByProdCmpn (promo_id, item_sk, totalsales) AS" +
					"	SELECT promotionSelected.promo_id, promotionSelected.item_sk, sum(price*quantity) as totalsales " +
					"	FROM" + 
					"		(SELECT ws_sold_date_sk as sold_date_sk, ws_item_sk as item_sk, ws_sales_price as price, ws_quantity as quantity " +
					"		FROM web_sales " + 
					"		UNION ALL" + 
					"		SELECT ss_sold_date_sk as sold_date_sk, ss_item_sk as item_sk, ss_sales_price as price, ss_quantity as quantity "  +
					"		FROM store_sales" + 
					"		UNION ALL" + 
					"		SELECT cs_sold_date_sk as sold_date_sk, cs_item_sk as item_sk, cs_sales_price as price, cs_quantity as quantity" +
					"		FROM catalog_sales) sales"  +
					"		JOIN promotionSelected "  +
					"		ON sales.item_sk = promotionSelected.item_sk" +
					"	WHERE " + 
					"		promotionSelected.start_date_sk <= sold_date_sk " +
					"		AND" + 
					"		sold_date_sk <= promotionSelected.end_date_sk" +
					"	GROUP BY" + 
					"		promotionSelected.promo_id, promotionSelected.item_sk "
			stmt.execute(drop_RptSalesByProdCmpn)			
			stmt.execute(create_RptSalesByProdCmpn)
			
			val drop_relevantTweet = "DROP TABLE IF EXISTS relevantTweet"
			val create_relevantTweet = "CREATE TABLE relevantTweet" +
					"	(item_sk int, user_id int, text string)"
			
			val query_relevantTweet	= "	INSERT INTO TABLE relevantTweet" +
					"		SELECT item_sk, user_id, text" +
					"		FROM " +
					"			(SELECT user.id as user_id, text, created_at, entities.hashtags[0] as hashtag" +
					"			FROM tweets" +
					"			WHERE entities.hashtags[0] IS NOT NULL) t1 " +
					"		JOIN " +	
					"			(SELECT item_sk, product_name, start_date, d_date as end_date" +
					"			FROM " +
					"				(SELECT item_sk, product_name, d_date as start_date, end_date_sk" +
					"				FROM promotedProduct JOIN date_dim" +
					"				ON promotedProduct.start_date_sk = date_dim.d_date_sk) t2 " +
					"			JOIN date_dim " +
					"			ON t2.end_date_sk = date_dim.d_date_sk) t3" +
					"		ON t1.hashtag = t3.product_name" +
					"		WHERE isWithinDate(created_at, start_date, end_date)"
			
			stmt.execute(drop_relevantTweet)
			stmt.execute(create_relevantTweet)
			stmt.execute(query_relevantTweet)
			

			val drop_senAnalyse = "DROP VIEW IF EXISTS senAnalyse"
			val create_senAnalyse = "CREATE VIEW senAnalyse" +
					"	(item_sk, user_id, sentiment_score) AS" +
					"	SELECT item_sk, user_id, sum(sentiment(text)) as sum_score" +
					"	FROM relevantTweet" +
					"	GROUP BY" +
					"	item_sk, user_id"
			
			stmt.execute(drop_senAnalyse)
			stmt.execute(create_senAnalyse)
			
			
			val drop_tweetByUser = "DROP TABLE IF EXISTS tweetByUser"
			val create_tweetByUser = "CREATE TABLE tweetByUser (user_id int, num_tweets int)"
				
			val query_tweetByUser =	"INSERT INTO TABLE tweetByUser" +
					"	SELECT user_id, count(*)" +
					"	FROM relevantTweet" +
					"	GROUP BY" +
					"	user_id"
			
			stmt.execute(drop_tweetByUser)
			stmt.execute(create_tweetByUser)
			stmt.execute(query_tweetByUser)
					
			val drop_tweetByProd = "DROP TABLE IF EXISTS tweetByProd"
			val create_tweetByProd = "CREATE TABLE tweetByProd (item_sk int, num_tweets int)"
				
			val	query_tweetByProd =	"INSERT INTO TABLE tweetByProd" +
					"	SELECT item_sk, count(*)" +
					"	FROM relevantTweet" +
					"	GROUP BY" +
					"	item_sk"
																																																																																																																		
			stmt.execute(drop_tweetByProd)
			stmt.execute(create_tweetByProd)
			stmt.execute(query_tweetByProd)

							
			val drop_sumFriendTweets = "DROP VIEW IF EXISTS sumFriendTweets"
			val create_sumFriendTweets = "CREATE VIEW sumFriendTweets (follower_id, num_friend_tweets) AS" +
					"	SELECT user_id, " +
					"		CASE WHEN num_friend_tweets > 0 THEN num_friend_tweets" +
					"			 ELSE 0L" +
					"		END" +
					"	FROM" +
					"		(SELECT user_id, sum(friend_tweets) as num_friend_tweets" +
					"		FROM tweetByUser LEFT OUTER JOIN" +
					"			(SELECT follower_id, friend_id, num_tweets as friend_tweets" +
					"			FROM tweetByUser JOIN twitter_graph" +
					"	 		ON tweetByUser.user_id = twitter_graph.friend_id) f" +
					"		ON tweetByUser.user_id = f.follower_id" +
					"		GROUP BY " +
					"		user_id) result"
				
			stmt.execute(drop_sumFriendTweets)
			stmt.execute(create_sumFriendTweets)
			
			
			val drop_mentionProb = "DROP TABLE IF EXISTS mentionProb"
			val create_mentionProb = "CREATE TABLE mentionProb (item_sk int, user_id int, prob float)"
				
			val query_mentionProb =	"INSERT INTO TABLE mentionProb" +
					"	SELECT item_sk, tweetByUser.user_id, r.num_tweets/tweetByUser.num_tweets" +
					"	FROM tweetByUser JOIN " +
					"		(SELECT item_sk, user_id, count(*) as num_tweets" +
					"		FROM relevantTweet" +
					"		GROUP BY" +
					"		item_sk, user_id) r" +
					"	ON tweetByUser.user_id = r.user_id"
				
			stmt.execute(drop_mentionProb)
			stmt.execute(create_mentionProb)

			
			val drop_simUserByProd = "DROP VIEW IF EXISTS simUserByProd"
			val create_simUserByProd = "CREATE VIEW simUserByProd " +
					"	(item_sk, follower_id, friend_id, similarity) AS" +
					"	SELECT * FROM" +
					"	(SELECT item_sk, user_id as follower_id, user_id as friend_id, COALESCE(0) as similarity" +
					"	FROM mentionProb" +
					"	UNION ALL" +
					"	SELECT f.item_sk, follower_id, friend_id, (1 - ABS(follower_prob - prob)) as similarity" +
					"	FROM " +
					"		(SELECT item_sk, follower_id, friend_id, prob as follower_prob" +
					"		FROM mentionProb JOIN twitter_graph " +
					"		ON mentionProb.user_id = twitter_graph.follower_id) f" +
					"	JOIN mentionProb " +
					"	ON	f.friend_id = mentionProb.user_id) c"
		

			
			stmt.execute(drop_simUserByProd)
			stmt.execute(create_simUserByProd)
					
					

			val drop_transitMatrix = "DROP TABLE IF EXISTS transitMatrix"
			val create_transitMatrix = "CREATE TABLE transitMatrix (item_sk int, follower_id int, friend_id int, transit_prob float)"
				
			val query_transitMatrix = "INSERT INTO TABLE transitMatrix" +
					"	SELECT item_sk, follower_id, friend_id, " +
					"		CASE WHEN follower_id != friend_id THEN num_tweets/num_friend_tweets*similarity" +
					"			 ELSE 0.0" +
					"		END" +
					"	FROM" +
					"		(SELECT item_sk, t1.follower_id, friend_id, similarity, num_friend_tweets" +
					"		FROM simUserByProd t1 JOIN sumFriendTweets t2" +
					"		ON t1.follower_id = t2.follower_id) t3" +
					"	JOIN tweetByUser" +
					"	ON t3.friend_id = tweetByUser.user_id"
				
			stmt.execute(drop_transitMatrix)
			stmt.execute(create_transitMatrix)
			stmt.execute(query_transitMatrix)
			
			
			val drop_randSufferVec = "DROP TABLE IF EXISTS randSuffVec"
			val create_randSuffVec = "CREATE TABLE randSuffVec (item_sk int, user_id int, prob float)"
				
			val query_randSuffVec =	"INSERT INTO TABLE randSuffVec	" +
					"	SELECT t1.item_sk, user_id, t1.num_tweets/t2.num_tweets" +
					"	FROM" +
					"		(SELECT item_sk, user_id, count(*) as num_tweets" +
					"		FROM relevantTweet" +
					"		GROUP BY" +
					"		item_sk, user_id) t1" +
					"	JOIN tweetByProd t2" +
					"	ON t1.item_sk = t2.item_sk"
				
			stmt.execute(drop_randSufferVec)
			stmt.execute(create_randSuffVec)
			stmt.execute(query_randSuffVec)
			
			val drop_initalRank = "DROP TABLE IF EXISTS initialRank"
			val create_initialRank = "CREATE TABLE initialRank (item_sk int, user_id int, rank_score float)"
				
			val query_initialRank =	"INSERT INTO TABLE initialRank" +
					"	SELECT t2.item_sk, user_id, 1.0/num_users as rank_score" +
					"	FROM " +
					"		(SELECT item_sk, count(*) as num_users" +
					"		FROM" +
					"			(SELECT item_sk, user_id" +
					"			FROM relevantTweet" +
					"			GROUP BY" +
					"			item_sk, user_id) t1" +
					"		GROUP BY" +
					"		item_sk) t2 JOIN mentionProb" +
					"	ON t2.item_sk = mentionProb.item_sk"
				
			stmt.execute(drop_initalRank)
			stmt.execute(create_initialRank)
			stmt.execute(query_initialRank)
			
					
			val alpha = 0.85
			for(iteration <- 1 to num_iter) {
						
				val twitterRank_previous = if(iteration == 1) "initialRank" else "twitterRank"+(iteration-1)	
				val drop_twitterRank = "DROP TABLE IF EXISTS twitterRank"+iteration
				val create_twitterRank = "CREATE TABLE twitterRank"+iteration+" (item_sk int, user_id int, rank_score float)"
				
				val query_twitterRank =	"INSERT INTO TABLE twitterRank"+iteration +
						"	SELECT t4.item_sk, t4.user_id, " +
						"		CASE WHEN sum_follower_score > 0 THEN " + alpha + " * sum_follower_score + " + (1-alpha) +" * prob" +
						"			 ELSE " + (1-alpha) +" * prob" +
						"		END" +
						"	FROM" +
						"		(SELECT t1.item_sk, follower_id, sum(transit_prob * rank_score) as sum_follower_score" +
						"		FROM transitMatrix t1 JOIN " + twitterRank_previous +" t2" +
						"		ON t1.friend_id = t2.user_id AND t1.item_sk = t2.item_sk " +
						"		WHERE t1.follower_id != t2.user_id" +
						"		GROUP BY " +
						"		t1.item_sk, follower_id) t3" +
						"	RIGHT OUTER JOIN randSUffVec t4" +
						"	ON t3.item_sk = t4.item_sk AND t3.follower_id = t4.user_id"
			
				stmt.execute(drop_twitterRank)
				stmt.execute(create_twitterRank)
				stmt.execute(query_twitterRank)
				
			}
			
			val drop_RptSAProdCmpn = "DROP TABLE IF EXISTS RptSAProdCmpn"
			val create_RptSAProdCmpn = "CREATE TABLE RptSAProdCmpn (promo_id string, item_sk int, totalsales float , total_sentiment float)"
					
			stmt.execute(drop_RptSAProdCmpn)
			stmt.execute(create_RptSAProdCmpn)
			
			val twitterRank = "twitterRank" + num_iter
			
			val query_RptSAProdCmpn = "INSERT INTO TABLE RptSAProdCmpn" +
					"	SELECT promo_id, t.item_sk, totalsales, total_sentiment" +
					"	FROM " +
					"		(SELECT senAnalyse.item_sk, sum(rank_score * sentiment_score) as total_sentiment" +
					"		FROM senAnalyse JOIN " + twitterRank +
					"		ON senAnalyse.item_sk = " + twitterRank + ".item_sk " +
					"		WHERE senAnalyse.user_id = " + twitterRank +".user_id" +
					"		GROUP BY" +
					"		senAnalyse.item_sk) t" +
					"	JOIN RptSalesByProdCmpn " +
					"	ON t.item_sk = RptSalesByProdCmpn.item_sk"				
			
			if (stmt.execute(query_RptSAProdCmpn)) {
				stmt.close();
				return true
			}
			else{ 
				stmt.close();
				return false
			}

		} catch {
			case sqle :
				SQLException => sqle.printStackTrace()
			case e :
				Exception => e.printStackTrace()
		} 
		
		return false
	}
	
	
	/**
	 * Run the benchmark query
	 */
	override def runHive(connection: Connection): Boolean = {
		
			return runHiveImpl1(connection)

	}
	
	/**
	 * Prepare the base tables before actually run the shark query
	 */
	override def prepareSharkTables(connection: Connection): Unit = {}
	
	override def cleanUpShark(connection: Connection): Unit = {}
	
	def runSharkImpl1(connection: Connection): Boolean = {
		
		return runHiveImpl1(connection)
	}
	
	override def runShark(connection: Connection): Boolean = {
		return runSharkImpl1(connection)
	}
	
	

	
	
	override def prepareVerticaTables(connection: Connection): Unit = {}
	

	
	def cleanUpVertica1(connection: Connection): Unit = {
		val stmt = connection.createStatement()	
		
		val list_drop = Seq("DROP TABLE IF EXISTS promotionSelected",
							"DROP VIEW IF EXISTS promotedProduct",
							"DROP VIEW IF EXISTS RptSalesByProdCmpn",
							"DROP TABLE IF EXISTS relevantTweet",
							"DROP VIEW IF EXISTS senAnalyse",
							"DROP TABLE IF EXISTS tweetByUser",
							"DROP TABLE IF EXISTS tweetByProd",
							"DROP VIEW IF EXISTS sumFriendTweets",
							"DROP TABLE IF EXISTS mentionProb",
							"DROP VIEW IF EXISTS simUserByProd",
							"DROP TABLE IF EXISTS transitMatrix",
							"DROP TABLE IF EXISTS randSuffVec",
							"DROP TABLE IF EXISTS initialRank")
		
		list_drop.foreach(stmt.execute(_))
									
		for(iteration <- 1 to num_iter) {
							
			val drop_twitterRank = "DROP TABLE IF EXISTS twitterRank"+iteration

			stmt.execute(drop_twitterRank)
				
		}
	}
	
	override def cleanUpVertica(connection: Connection): Unit = {
		//cleanUpVertica1(connection)		
	}
	
	
	
	/**
	 * This implementation requires more memory since it doesn't materialize all intermediate tables.  
	 * 
	 */
	def verticaImpl1(connection: Connection): Boolean = {
		try {
			val stmt = connection.createStatement();
						
			val promotSKs = "1"
			val lower = 1
			val upper = 300
			
			val drop_promotionSelected = "DROP TABLE IF EXISTS promotionSelected"
			val create_promotionSelected = "CREATE TABLE promotionSelected (promo_id char(16), item_sk int," +
					"start_date_sk int, end_date_sk int)"
			stmt.execute(drop_promotionSelected)
			stmt.execute(create_promotionSelected)

			val query_promotionSelected = "INSERT INTO promotionSelected " + 
					"	SELECT p_promo_id, p_item_sk, p_start_date_sk, p_end_date_sk " +
					"	FROM promotion " +
					"	WHERE " + lower + " <= p_promo_sk AND p_promo_sk <= " + upper +
					"	AND p_item_sk IS NOT NULL AND p_start_date_sk IS NOT NULL AND p_end_date_sk IS NOT NULL"
			

			stmt.executeUpdate(query_promotionSelected)

			
			val drop_promotedProduct = "DROP VIEW IF EXISTS promotedProduct"
			val create_promotedProduct = "CREATE VIEW promotedProduct (item_sk, product_name, start_date_sk, end_date_sk) AS" +
					"	SELECT i_item_sk, i_product_name, start_date_sk, end_date_sk" +
					"	FROM item JOIN promotionSelected " +
					"	ON i_item_sk = item_sk AND i_product_name IS NOT NULL"
			stmt.execute(drop_promotedProduct)
			stmt.execute(create_promotedProduct)	
							
			
			val drop_RptSalesByProdCmpn = "DROP VIEW IF EXISTS RptSalesByProdCmpn"
			val create_RptSalesByProdCmpn = "CREATE VIEW RptSalesByProdCmpn " +
					"	(promo_id, item_sk, totalsales) AS" +
					"		SELECT p.promo_id, p.item_sk, sum(price*quantity) as totalsales " +
					"		FROM" + 
					"			(SELECT ws_sold_date_sk as sold_date_sk, ws_item_sk as item_sk, ws_sales_price as price, ws_quantity as quantity " +
					"			FROM web_sales " +
					"			WHERE ws_sales_price IS NOT NULL AND ws_quantity IS NOT NULL" + 
					"			UNION ALL " + 
					"			SELECT ss_sold_date_sk as sold_date_sk, ss_item_sk as item_sk, ss_sales_price as price, ss_quantity as quantity "  +
					"			FROM store_sales" + 
					"			WHERE ss_sales_price IS NOT NULL AND ss_quantity IS NOT NULL" + 
					"			UNION ALL" + 
					"			SELECT cs_sold_date_sk as sold_date_sk, cs_item_sk as item_sk, cs_sales_price as price, cs_quantity as quantity" +
					"			FROM catalog_sales"  +
					"			WHERE cs_sales_price IS NOT NULL AND cs_quantity IS NOT NULL ) sales" + 	
					"			JOIN promotionSelected p "  +
					"			ON sales.item_sk = p.item_sk" +
					"		WHERE " + 
					"			p.start_date_sk <= sold_date_sk " +
					"			AND" + 
					"			sold_date_sk <= p.end_date_sk" +
					"		GROUP BY" + 
					"			p.promo_id, p.item_sk "
			stmt.execute(drop_RptSalesByProdCmpn)
			stmt.execute(create_RptSalesByProdCmpn)
						
			
			val drop_relevantTweet = "DROP TABLE IF EXISTS relevantTweet"
			val create_relevantTweet = "CREATE TABLE relevantTweet" +
					"	(item_sk int, user_id int, text varchar(200) )"
			
			stmt.execute(drop_relevantTweet)
			stmt.execute(create_relevantTweet)
			
			val query_relevantTweet = "INSERT INTO relevantTweet" +
					"		SELECT item_sk, user_id, text" +
					"		FROM " +
					"			(SELECT user_id, hashtag, created_at, text" +
					"			FROM tweet JOIN entities" +
					"			ON tweet_id = id) t1 ," +
					"			(SELECT item_sk, product_name, start_date, d_date as end_date" +
					"			FROM " +
					"				(SELECT item_sk, product_name, d_date as start_date, end_date_sk" +
					"				FROM promotedProduct, date_dim" +
					"				WHERE start_date_sk = d_date_sk) t2 " +
					"			JOIN date_dim " +
					"			ON end_date_sk = d_date_sk) t3" +
					"		WHERE product_name = hashtag AND created_at > start_date AND created_at < end_date"

					
			stmt.executeUpdate(query_relevantTweet)
			

			val drop_senAnalyse = "DROP VIEW IF EXISTS senAnalyse"
			val create_senAnalyse = "CREATE VIEW senAnalyse" +
					"	(item_sk, user_id, sentiment_score) AS" +
					"	SELECT item_sk, user_id, sum(sentiment(text)) as sum_score" +
					"	FROM relevantTweet" +
					"	GROUP BY" +
					"	item_sk, user_id"
			
			stmt.execute(drop_senAnalyse)
			stmt.execute(create_senAnalyse)
			
			
			val drop_tweetByUser = "DROP TABLE IF EXISTS tweetByUser"
			val create_tweetByUser = "CREATE TABLE tweetByUser (user_id int, num_tweets int)"
				
			val query_tweetByUser =	"INSERT INTO tweetByUser" +
					"	SELECT user_id, count(*)" +
					"	FROM relevantTweet" +
					"	GROUP BY" +
					"	user_id"
			
			stmt.execute(drop_tweetByUser)
			stmt.execute(create_tweetByUser)
			stmt.executeUpdate(query_tweetByUser)
					
			val drop_tweetByProd = "DROP TABLE IF EXISTS tweetByProd"
			val create_tweetByProd = "CREATE TABLE tweetByProd (item_sk int, num_tweets int)"
				
			val query_tweetByProd =	"INSERT INTO tweetByProd" +
					"	SELECT item_sk, count(*)" +
					"	FROM relevantTweet" +
					"	GROUP BY" +
					"	item_sk"
																																																																																																																		
			stmt.execute(drop_tweetByProd)
			stmt.execute(create_tweetByProd)
			stmt.executeUpdate(query_tweetByProd)

							
			val drop_sumFriendTweets = "DROP VIEW IF EXISTS sumFriendTweets"
			val create_sumFriendTweets = "CREATE VIEW sumFriendTweets (follower_id, num_friend_tweets) AS" +
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
				
			stmt.execute(drop_sumFriendTweets)
			stmt.execute(create_sumFriendTweets)
			
			
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

			
			val drop_simUserByProd = "DROP VIEW IF EXISTS simUserByProd"
			val create_simUserByProd = "CREATE VIEW simUserByProd " +
					"	(item_sk, follower_id, friend_id, similarity) AS" +
					"	SELECT f.item_sk, follower_id, friend_id, (1 - ABS(follower_prob - prob)) as similarity" +
					"	FROM " +
					"		(SELECT item_sk, follower_id, friend_id, prob as follower_prob" +
					"		FROM mentionProb JOIN twitter_graph " +
					"		ON user_id = follower_id) f" +
					"	JOIN mentionProb " +
					"	ON	friend_id = user_id" +
					"	UNION ALL" +
					"	SELECT item_sk, user_id, user_id, 0" +
					"	FROM mentionProb"
			
			stmt.execute(drop_simUserByProd)
			stmt.execute(create_simUserByProd)
					
					
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
					"	JOIN mentionProb" +
					"	ON t1.item_sk = mentionProb.item_sk"
			stmt.executeUpdate(query_initialRank)

					
			val alpha = 0.85
			for(iteration <- 1 to num_iter) {
						
				val twitterRank_previous = if(iteration == 1) "initialRank" else "twitterRank"+(iteration-1)	
				val drop_twitterRank = "DROP VIEW IF EXISTS twitterRank"+iteration
				val create_twitterRank = "CREATE VIEW twitterRank"+iteration+" (item_sk, user_id, rank_score) AS" +
						"	SELECT t4.item_sk, t4.user_id, " +
						"		CASE WHEN sum_follower_score > 0 THEN " + alpha + " * sum_follower_score + " + (1-alpha) +" * prob" +
						"			 ELSE " + (1-alpha) +" * prob" +
						"		END" +
						"	FROM" +
						"		(SELECT t1.item_sk, follower_id, sum(transit_prob * rank_score) as sum_follower_score" +
						"		FROM transitMatrix t1, " + twitterRank_previous +" t2" +
						"		WHERE t1.friend_id = t2.user_id AND t1.item_sk = t2.item_sk " +
						"			AND t1.follower_id != t2.user_id" +
						"		GROUP BY " +
						"		t1.item_sk, follower_id) t3" +
						"	RIGHT JOIN randSUffVec t4" +
						"	ON t3.item_sk = t4.item_sk AND t3.follower_id = t4.user_id"
			
				stmt.execute(drop_twitterRank)
				stmt.execute(create_twitterRank)
				
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
			
			if (stmt.executeUpdate(query_RptSAProdCmpn) > 0) {
				stmt.close();
				return true
			}
			else{ 
				stmt.close();
				return false
			}

		} catch {
			case sqle :
				SQLException => sqle.printStackTrace()
			case e :
				Exception => e.printStackTrace()
		}
		
		return false
	}
	
	override def runVertica(connection: Connection): Boolean = {
		return verticaImpl1(connection)
	
	}
		
}