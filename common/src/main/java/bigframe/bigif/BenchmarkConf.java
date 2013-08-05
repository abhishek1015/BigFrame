package bigframe.bigif;


/**
 * Class for recording the BigFrame input specification.
 * 
 * @author andy
 * 
 */
public class BenchmarkConf {

	private DatagenConf datagenConf;
	private QuerygenConf querygenConf;


	public BenchmarkConf() {
		datagenConf = new DatagenConf();
		querygenConf = new QuerygenConf();

	}

	public BenchmarkConf(DatagenConf dc, QuerygenConf qc) {
		datagenConf = dc;
		querygenConf = qc;
	}

	public DatagenConf getDatagenConf() {
		return datagenConf;
	}

	public QuerygenConf getQuerygenConf() {
		return querygenConf;
	}

	public void setDatagenConf(DatagenConf dc) {
		datagenConf = dc;
	}

	public void setQuerygenConf(QuerygenConf qc) {
		querygenConf = qc;
	}

	public void printConf() {
		System.out.println("Benchmark specification:");
		System.out.println("-------------------------------------");
		datagenConf.printConf();
		System.out.println("-------------------------------------");
		querygenConf.printConf();
	}
}
