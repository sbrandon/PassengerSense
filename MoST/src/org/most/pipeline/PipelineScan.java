package org.most.pipeline;

public class PipelineScan {
		//Table Name
		public static final String TBL_SCAN = "SCAN";
		//Table Fields
		public static final String FLD_TIMESTAMP = "timestamp";
		public static final String FLD_GROUND_TRUTH = "groundtruth";
		public static final String FLD_BUS_ROUTE = "busroute";
		//Create String
		public static final String CREATE_SCAN_TABLE = String.format("_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL", FLD_TIMESTAMP, FLD_GROUND_TRUTH, FLD_BUS_ROUTE);
}
