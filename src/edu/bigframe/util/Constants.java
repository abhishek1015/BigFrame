package edu.bigframe.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Constants {
	
	//required configuration
	public static String BIGFRAME_CONF_DIR = "bigframe.conf.dir";
	public static String BIGFRAME_HADOOP_HOME = "bigframe.hadoop.home";
	public static String BIGFRAME_HADOOP_SLAVE = "bigframe.hadoop.slaves";
	public static String BIGFRAME_TPCDS_LOCAL = "bigframe.tpcds.local";
	public static String BIGFRAME_TPCDS_SCRIPT = "bigframe.tpcds.script";
	public static String BIGFRAME_GEN_PROMTTBL_SCRIPT = "bigframe.promttblgen.script";
	
	
	// Constants for Configuration parameters
	static final String[] DATATYPES = new String[] { "relational", "graph", "nested", "text" };
	public static final Set<String> DATAVARIETY = new HashSet<String>(Arrays.asList(DATATYPES));


	static final String[] QUERYTYPES = new String[] { "historical", "repeated_batch", "continuous" };
	public static final Set<String> QUERYVELOCITY = new HashSet<String>(Arrays.asList(QUERYTYPES));
	
	public static final String BIGFRAME_DATAVARIETY = "bigframe.datavariety";
	public static final String BIGFRAME_DATAVOLUME = "bigframe.datavolume";
	
	public static final String BIGFRAME_DATAVOLUME_GRAPH_PROPORTION = "bigframe.datavolume.graph.proportion";
	public static final String BIGFRAME_DATAVOLUME_RELATIONAL_PROPORTION = "bigframe.datavolume.relational.proportion";
	public static final String BIGFRAME_DATAVOLUME_NESTED_PROPORTION = "bigframe.datavolume.nested.proportion";
	public static final String BIGFRAME_DATAVOLUME_TEXT_PROPORTION = "bigframe.datavolume.text.proportion";
	static final String[] DATAVOLUME_PROPORTION = new String[] { BIGFRAME_DATAVOLUME_GRAPH_PROPORTION, BIGFRAME_DATAVOLUME_RELATIONAL_PROPORTION, 
		BIGFRAME_DATAVOLUME_NESTED_PROPORTION,BIGFRAME_DATAVOLUME_TEXT_PROPORTION };
	public static final Set<String> BIGFRAME_DATAVOLUME_PORTION_SET = new HashSet<String>(Arrays.asList(DATAVOLUME_PROPORTION));
	
	
	public static final String BIGFRAME_DATAVELOCITY_RELATIONAL = "bigframe.datavelocity.relational";
	public static final String BIGFRAME_DATAVELOCITY_GRAPH = "bigframe.datavelocity.graph";
	public static final String BIGFRAME_DATAVELOCITY_NESTED = "bigframe.datavelocity.nested";
	public static final String BIGFRAME_DATAVELOCITY_TEXT = "bigframe.datavelocity.text";
	static final String[] DATAVELOCITY = new String[] { BIGFRAME_DATAVELOCITY_RELATIONAL, BIGFRAME_DATAVELOCITY_GRAPH, 
		BIGFRAME_DATAVELOCITY_NESTED,BIGFRAME_DATAVELOCITY_TEXT };
	public static final Set<String> BIGFRAME_DATAVELOCITY = new HashSet<String>(Arrays.asList(DATAVELOCITY));
	
	public static final String BIGFRAME_DATA_HDFSPATH_RELATIONAL = "bigframe.data.hdfspath.relational";
	public static final String BIGFRAME_DATA_HDFSPATH_GRAPH = "bigframe.data.hdfspath.graph";
	public static final String BIGFRAME_DATA_HDFSPATH_NESTED = "bigframe.data.hdfspath.nested";
	public static final String BIGFRAME_DATA_HDFSPATH_TEXT = "bigframe.data.hdfspath.text";
	static final String[] DATA_HDFSPATH = new String[] { BIGFRAME_DATA_HDFSPATH_RELATIONAL, BIGFRAME_DATA_HDFSPATH_GRAPH, 
		BIGFRAME_DATA_HDFSPATH_NESTED,BIGFRAME_DATA_HDFSPATH_TEXT };
	public static final Set<String> BIGFRAME_DATA_HDFSPATH = new HashSet<String>(Arrays.asList(DATA_HDFSPATH));
	
	public static final String BIGFRAME_QUERYVARIETY = "bigframe.queryvariety";
	public static final String BIGFRAME_QUERYVELOCITY = "bigframe.queryvelocity";
	public static final String BIGFRAME_QUERYVOLUME = "bigframe.queryvolume";
	

	 public static final Map<String, Integer> DATAVOLUME_MAP;
	    static
	    {
	    	DATAVOLUME_MAP = new HashMap<String, Integer>();
	    	DATAVOLUME_MAP.put("tiny", 10);
	    	DATAVOLUME_MAP.put("small", 100);
	    	DATAVOLUME_MAP.put("medium", 1000);
	    	DATAVOLUME_MAP.put("large", 100000);
	    	DATAVOLUME_MAP.put("extra large", 1000000);
	    }
	
    
    public static String TPCDS_BEGINDATE = "1900-01-01";
    public static String TPCDS_ENDDATE = "2100-01-01";
    
    public static String TWEET_BEGINDATE = "1997-01-01";
    public static String TWEET_ENDDATE = "1998-01-01";
   
    
    
   
}
