package bigframe.workflows.BusinessIntelligence.graph.exploratory

import org.apache.hadoop.conf.Configuration
import bigframe.workflows.HadoopRunnable

class WF_PageRank(val graph_path: String) extends HadoopRunnable {

	def run(mapred_config: Configuration): java.lang.Boolean = { false }

}