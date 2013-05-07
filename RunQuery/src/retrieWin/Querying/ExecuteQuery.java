package retrieWin.Querying;

import java.util.*;

import retrieWin.Indexer.ThriftReader;
import retrieWin.Indexer.TrecTextDocument;

import lemurproject.indri.QueryEnvironment;
import lemurproject.indri.QueryRequest;
import lemurproject.indri.QueryResult;
import lemurproject.indri.QueryResults;

public class ExecuteQuery {
	
	final String INDEX_LOCATION;
	QueryEnvironment env = new QueryEnvironment();
	public ExecuteQuery(String IndexLocation) {
		this.INDEX_LOCATION = IndexLocation;
		try {
			env.addIndex(INDEX_LOCATION);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public List<String> queryIndex(String query, int resultsRequested) {
		List<String> results = new ArrayList<String>();
		try {
			QueryRequest queryRequest = new QueryRequest();
			queryRequest.query = query;
			queryRequest.resultsRequested = resultsRequested;
			QueryResults queryResults = env.runQuery(queryRequest);
			for(QueryResult result : queryResults.results) {
				results.add(result.documentName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}
	
	public List<String> queryIndex(String query) {
		return queryIndex(query, Integer.MAX_VALUE);
	}

	public List<TrecTextDocument> executeQuery(String query, int numResults, String workingDirectory) {
    	List<String> queryResults = queryIndex(query, numResults);
    	Map<String, Set<String>> fileMap = new HashMap<String, Set<String>>();
    	Map<String, Set<String>> streamIDMap = new HashMap<String, Set<String>>();
    	
    	for(String result:queryResults) {    	
	    	String[] a = result.split("_");
			String streamID = a[0];
			String localfilename = a[1];
			String[] b = localfilename.split("/");
			String filename = b[b.length-1];
			
			String timeStamp = a[2];
			String timeTokens[] = timeStamp.split("T");
			String folder = timeTokens[0] + "-" + timeTokens[1].substring(0,2);
			
			if(!fileMap.keySet().contains(folder)) {
				fileMap.put(folder, new HashSet<String>());
			}
			Set<String> set = fileMap.get(folder);
			set.add(filename);
			fileMap.put(folder, set);
			
			if(!streamIDMap.keySet().contains(filename)) {
				streamIDMap.put(filename, new HashSet<String>());
			}
			set = streamIDMap.get(filename);
			set.add(streamID);
			streamIDMap.put(filename, set);
    	}
    	
    	System.out.println("Retreiving documents...");
    	
    	List<TrecTextDocument> result = new ArrayList<TrecTextDocument>();
    	for(String folder:fileMap.keySet()) {
    		for(String file:fileMap.get(folder)) {
				result.addAll(ThriftReader.GetFilteredFiles(folder, file, workingDirectory, streamIDMap.get(file)));
    		}
    	}
    	return result;
	}
}