package bigframe.util.dataloader.vertica;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import com.vertica.hadoop.VerticaOutputFormat;
import com.vertica.hadoop.VerticaRecord;

import bigframe.bigif.WorkflowInputFormat;
//import bigframe.util.TableNotFoundException;
import bigframe.util.dataloader.vertica.VerticaTpcdsLoader.Map;
import bigframe.util.parser.JsonParser;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A data loader that can load tweet data from different sources into Vertica.
 *  
 * @author andy
 *
 */
public class VerticaTweetLoader extends VerticaDataLoader {

	
	public VerticaTweetLoader(WorkflowInputFormat workflowIF) {
		super(workflowIF);
		// TODO Auto-generated constructor stub
	}

	public static class Map extends
	Mapper<LongWritable, Text, Text, VerticaRecord> {
	
		VerticaRecord record = null;
		
		String tableName;
		
		SimpleDateFormat twitterDateFormat;
	
		@Override
		public void setup(Context context) throws IOException, InterruptedException {
			super.setup(context);
			try {
				record = new VerticaRecord(context.getConfiguration());
			} catch (Exception e) {
				throw new IOException(e);
			}
			
			twitterDateFormat = new SimpleDateFormat(
					"EEE MMM dd HH:mm:ss ZZZZZ yyyy");
			
			tableName = context.getConfiguration().get(MAPRED_VERTICA_TABLE_NAME);
		}
	
		@Override
	    public void map(LongWritable key, Text value, Context context)
	        throws IOException, InterruptedException {
	
			if (record == null) {
				throw new IOException("No output record found");
			}
			
			JSONObject tweet_json = JsonParser.parseJsonFromString(value.toString());
			
			if(tableName.equals("tweet")) {
				
				String tweet_id = (String) tweet_json.get("id");
				String text = (String) tweet_json.get("text");
				String create_at = (String) tweet_json.get("created_at");
				
				try {
					Timestamp create_date = new Timestamp(twitterDateFormat.parse(create_at).getTime());

				
					JSONObject user_json = (JSONObject) tweet_json.get("user");
					Long user_id = (Long) user_json.get("id");
				
					record.set("created_at", create_date);
	
					record.setFromString("id", tweet_id);
					record.setFromString("user_id", user_id.toString());
					record.setFromString("text", text);
				
					context.write(new Text(tableName), record);
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
					
				
			}
			else if(tableName.equals("entities")) {
				String tweet_id = (String) tweet_json.get("id");
				
				try {


				
					JSONObject entities_json = (JSONObject) tweet_json.get("entities");
					JSONArray hashtags = (JSONArray) entities_json.get("hashtags");
					
					if (hashtags.isEmpty()){
						record.setFromString("tweet_id", tweet_id);
						record.setFromString("hashtag", "");
						context.write(new Text(tableName), record);
					}
					//String hashtags_str = hashtags.toString().substring(1,hashtags.toString().length()-1);
					else {
						for(Object tag : hashtags) {
							String tag_str = (String) tag;
							
							record.setFromString("tweet_id", tweet_id);
							record.setFromString("hashtag", tag_str);
							context.write(new Text(tableName), record);
						}				
					}
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			
			
		}
	}

	
	@Override
	public boolean load(Path srcHdfsPath, String table) {
		Configuration mapred_config = new Configuration();
		
		mapred_config.addResource(new Path(workIF.getHadoopHome()
				+ "/conf/core-site.xml"));
		mapred_config.addResource(new Path(workIF.getHadoopHome()
				+ "/conf/mapred-site.xml"));
		
		mapred_config.set(MAPRED_VERTICA_DATABASE, workIF.getVerticaDatabase());
		mapred_config.set(MAPRED_VERTICA_USERNAME, workIF.getVerticaUserName());
		mapred_config.set(MAPRED_VERTICA_PASSWORD, workIF.getVerticaPassword());
		mapred_config.set(MAPRED_VERTICA_HOSTNAMES, workIF.getVerticaHostNames());
		mapred_config.set(MAPRED_VERTICA_PORT, workIF.getVerticaPort().toString());
		
		
		try {
			
			mapred_config.set(MAPRED_VERTICA_TABLE_NAME, table);
			
			Job job = new Job(mapred_config);
		    
			FileInputFormat.setInputPaths(job, srcHdfsPath);
	
		    job.setJobName("Load data to Table " + table);
		    
		    job.setOutputKeyClass(Text.class);
		    job.setOutputValueClass(VerticaRecord.class);
		    
		    job.setOutputFormatClass(VerticaOutputFormat.class);
		    
		    job.setJarByClass(VerticaTpcdsLoader.class);
		    job.setMapperClass(Map.class);
		    //job.setReducerClass(Reduce.class);
		    
		    job.setNumReduceTasks(0);
		 
		    if(table.equals("entities"))
			    VerticaOutputFormat.setOutput(job, "entities", true, "tweet_id int", "urls varchar(100)",
			    		"hashtag varchar(50)", "user_mentions varchar(200)");
		   
		    else if(table.equals("user"))
			    VerticaOutputFormat.setOutput(job, "user", true, "profile_sidebar_border_color char(20)", 
			    		"name varchar(70)", "profile_sidebar_fill_color char(20)", "profile_background_tile char(20)",
			    		"profile_image_url varchar(70)", "location varchar(20)", "created_at  TIMESTAMP WITH TIMEZONE",
			    		"id_str char(20)", "follow_request_sent varchar(20)", "profile_link_color char(20)", 
			    		"favourites_count int",  "url varchar(70)", "contributors_enabled BOOLEAN", "utc_offset varchar(20)",
			    		"id int", "profile_use_background_image varchar(70)", "listed_count int", "protected BOOLEAN",
			    		"lang char(20)", "profile_text_color char(20)", "followers_count int", "time_zone char(20)",
			    		"verified boolean", "geo_enabled boolean", "profile_background_color varchar(70)",
			    		"notifications boolean", "description varchar(200)", "friends_count int", "profile_background_image_url varchar(120)",
			    		"statuses_count int", "screen_name char(20)", "following boolean", "show_all_inline_media boolean");
		    
		    else if(table.equals("tweet")) 
		    	VerticaOutputFormat.setOutput(job, "tweet", true, "coordinates char(20)", "created_at TIMESTAMP WITH TIMEZONE",
		    			"favorited boolean", "truncated boolean", "id_str char(20)", "in_reply_to_user_id_str char(20)",
		    			"text varchar(200)", "contributors char(20)", "id int", "retweet_count int", "in_reply_to_status_id_str char(20)",
		    			"geo char(20)", "retweeted boolean", "in_reply_to_user_id int", "user_id int", "in_reply_to_screen_name char(20)",
		    			"source char(20)", "place char(20)", "in_reply_to_status_id int");
		    
		    else {
		    	//throw new TableNotFoundException("Table " + table + " doesn't exist!");
		    	System.out.println("Table " + table + " doesn't exist!");
		    	return false;
		    }
		
		
			return job.waitForCompletion(true);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public boolean load() {
		// TODO Auto-generated method stub
		return false;
	}

}
