package edu.bigframe.datagen.relational;

import java.util.List;
import java.util.Set;

import javax.xml.soap.Text;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.bigframe.datagen.DatagenConf;
import edu.bigframe.datagen.nested.PromotedProduct;

public class CollectTPCDSstatHadoop extends CollectTPCDSstat {

	
	class GetPromotedProdsMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
		
	}

	
	@Override
	public List<PromotedProduct> getPromotedProds() {
		// TODO Auto-generated method stub
		
		
		
		return null;
	}

/*
	@Override
	public void IntialCustTwitterAcc(String hdfs_path, DatagenConf conf) {

	}
*/
	@Override
	public long [] getNonCustTwitterAcc(long[] customer_twitterAcc, int num_twitter_user) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public long[] getCustTwitterAcc(float tpcds_targetGB, float graph_targetGB) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public long getNumOfCustomer(int targetGB) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public long getNumOfItem(int targetGB) {
		// TODO Auto-generated method stub
		return 0;
	}

}
