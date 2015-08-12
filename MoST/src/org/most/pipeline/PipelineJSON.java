package org.most.pipeline;

public class PipelineJSON {
	
	//Table Name
	public static final String TBL_JSON = "JSON_LOG";
	//Table Fields
	public static final String FLD_JSON = "json";
	//Create String
	public static final String CREATE_JSON_TABLE = String.format("%s TEXT NOT NULL", FLD_JSON);
}
