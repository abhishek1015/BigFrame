package bigframe.bigif;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BigConfConstants {

	// Properties not related to data and query
	public static final String BIGFRAME_CONF_DIR = "bigframe.conf.dir";
	
	// Properties for the home dir of each engine
	public static final String BIGFRAME_HADOOP_HOME = "bigframe.hadoop.home";
	public static final String BIGFRAME_HDFS_ROOTDIR = "bigframe.hdfs.root.dir";
	public static final String BIGFRAME_HADOOP_SLAVE = "bigframe.hadoop.slaves";
	public static final String BIGFRAME_HIVE_HOME = "bigframe.hive.home";
	public static final String BIGFRAME_HIVE_JDBC_SERVER = "bigframe.hive.jdbc.server";
	public static final String BIGFRAME_SHARK_HOME = "bigframe.shark.home";
	public static final String BIGFRAME_SPARK_HOME = "bigframe.spark.home";
	
	// Properties for external scripts
	public static final String BIGFRAME_TPCDS_LOCAL = "bigframe.tpcds.local";
	public static final String BIGFRAME_TPCDS_SCRIPT = "bigframe.tpcds.script";
	public static final String BIGFRAME_GEN_SINGLETBL_SCRIPT = "bigframe.singletblgen.script";


	// BigFrame input format related properties
	public static final String BIGFRAME_APP_DOMAIN = "bigframe.application.domain";
	public static final String APPLICATION_BI = "BI";
	
	public static final String BIGFRAME_DATAVARIETY = "bigframe.datavariety";
	public static final String BIGFRAME_DATAVOLUME = "bigframe.datavolume";

	public static final String BIGFRAME_DATAVOLUME_GRAPH_PROPORTION = "bigframe.datavolume.graph.proportion";
	public static final String BIGFRAME_DATAVOLUME_RELATIONAL_PROPORTION = "bigframe.datavolume.relational.proportion";
	public static final String BIGFRAME_DATAVOLUME_NESTED_PROPORTION = "bigframe.datavolume.nested.proportion";
	public static final String BIGFRAME_DATAVOLUME_TEXT_PROPORTION = "bigframe.datavolume.text.proportion";
	static final String[] DATAVOLUME_PROPORTION = new String[] {
		BIGFRAME_DATAVOLUME_GRAPH_PROPORTION,
		BIGFRAME_DATAVOLUME_RELATIONAL_PROPORTION,
		BIGFRAME_DATAVOLUME_NESTED_PROPORTION,
		BIGFRAME_DATAVOLUME_TEXT_PROPORTION };
	public static final Set<String> BIGFRAME_DATAVOLUME_PORTION_SET = new HashSet<String>(
			Arrays.asList(DATAVOLUME_PROPORTION));

	public static final String BIGFRAME_DATAVELOCITY_RELATIONAL = "bigframe.datavelocity.relational";
	public static final String BIGFRAME_DATAVELOCITY_GRAPH = "bigframe.datavelocity.graph";
	public static final String BIGFRAME_DATAVELOCITY_NESTED = "bigframe.datavelocity.nested";
	public static final String BIGFRAME_DATAVELOCITY_TEXT = "bigframe.datavelocity.text";
	static final String[] DATAVELOCITY = new String[] {
		BIGFRAME_DATAVELOCITY_RELATIONAL, BIGFRAME_DATAVELOCITY_GRAPH,
		BIGFRAME_DATAVELOCITY_NESTED, BIGFRAME_DATAVELOCITY_TEXT };
	public static final Set<String> BIGFRAME_DATAVELOCITY = new HashSet<String>(
			Arrays.asList(DATAVELOCITY));

	public static final String BIGFRAME_DATA_HDFSPATH_RELATIONAL = "bigframe.data.hdfspath.relational";
	public static final String BIGFRAME_DATA_HDFSPATH_GRAPH = "bigframe.data.hdfspath.graph";
	public static final String BIGFRAME_DATA_HDFSPATH_NESTED = "bigframe.data.hdfspath.nested";
	public static final String BIGFRAME_DATA_HDFSPATH_TEXT = "bigframe.data.hdfspath.text";
	static final String[] DATA_HDFSPATH = new String[] {
		BIGFRAME_DATA_HDFSPATH_RELATIONAL, BIGFRAME_DATA_HDFSPATH_GRAPH,
		BIGFRAME_DATA_HDFSPATH_NESTED, BIGFRAME_DATA_HDFSPATH_TEXT };
	public static final Set<String> BIGFRAME_DATA_HDFSPATH = new HashSet<String>(
			Arrays.asList(DATA_HDFSPATH));

	public static final String BIGFRAME_QUERYVARIETY = "bigframe.queryvariety";
	public static final String BIGFRAME_QUERYVELOCITY = "bigframe.queryvelocity";
	public static final String BIGFRAME_QUERYVOLUME = "bigframe.queryvolume";

	public static final String BIGFRAME_QUERYENGINE_RELATIONAL = "bigframe.queryengine.relational";
	public static final String BIGFRAME_QUERYENGINE_GRAPH = "bigframe.queryengine.graph";
	public static final String BIGFRAME_QUERYENGINE_NESTED = "bigframe.queryengine.nested";
	public static final String BIGFRAME_QUERYENGINE_TEXT = "bigframe.queryengine.text";
	
	// Constants for Configuration parameters
	static final String[] DATATYPES = new String[] { "relational", "graph",
		"nested", "text" };
	public static final Set<String> DATAVARIETY = new HashSet<String>(
			Arrays.asList(DATATYPES));

	static final String[] QUERYTYPES = new String[] { "exploratory", "continuous" };
	public static final Set<String> QUERYVELOCITY = new HashSet<String>(
			Arrays.asList(QUERYTYPES));

	public static final Map<String, Integer> DATAVOLUME_MAP;
	static {
		DATAVOLUME_MAP = new HashMap<String, Integer>();
		DATAVOLUME_MAP.put("test", 3);
		DATAVOLUME_MAP.put("tiny", 10);
		DATAVOLUME_MAP.put("small", 100);
		DATAVOLUME_MAP.put("medium", 1000);
		DATAVOLUME_MAP.put("large", 10000);
		DATAVOLUME_MAP.put("extra large", 100000);
	}
}
