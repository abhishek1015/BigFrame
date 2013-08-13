package bigframe.qgen.engineDriver;

import bigframe.bigif.WorkflowInputFormat;



public abstract class Workflow {
    protected WorkflowInputFormat workIF;
	
	public Workflow(WorkflowInputFormat workIF) {
		this.workIF = workIF;
	}
	
	public abstract int numOfQueries();
	
	public abstract void init();
	
	public abstract void run();
	
	public abstract void cleanup();
}
