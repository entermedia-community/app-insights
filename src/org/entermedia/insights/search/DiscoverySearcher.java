package org.entermedia.insights.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.BaseData;
import org.openedit.data.BaseSearcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class DiscoverySearcher extends BaseSearcher
{
	private static final Log log = LogFactory.getLog(DiscoverySearcher.class);

	HttpSharedConnection fieldSharedConnection;

	protected String fieldIBMURL="https://api.us-south.discovery.watson.cloud.ibm.com/instances/";
	protected String fieldINSTANCE = "21ab8dc5-7b0f-4e4a-96f7-92b8deb7b0a4";
	protected String fieldIBMENVID="91745818-65e0-4f25-89b7-e17754afdfd7";
	protected String fieldIBMCOLLECTIONID="5563b583-ee7e-4c97-9029-0be597e142d1";

	
	public String getIBMURL()
	{
		return fieldIBMURL;
	}

	public void setIBMURL(String inIBMURL)
	{
		fieldIBMURL = inIBMURL;
	}

	public String getINSTANCE()
	{
		return fieldINSTANCE;
	}

	public void setINSTANCE(String inINSTANCE)
	{
		fieldINSTANCE = inINSTANCE;
	}

	public String getIBMENVID()
	{
		return fieldIBMENVID;
	}

	public void setIBMENVID(String inIBMENVID)
	{
		fieldIBMENVID = inIBMENVID;
	}

	public String getIBMCOLLECTIONID()
	{
		return fieldIBMCOLLECTIONID;
	}

	public void setIBMCOLLECTIONID(String inIBMCOLLECTIONID)
	{
		fieldIBMCOLLECTIONID = inIBMCOLLECTIONID;
	}

	
	public HttpSharedConnection getSharedConnection()
	{
		if (fieldSharedConnection == null)
		{
			fieldSharedConnection = (HttpSharedConnection)getModuleManager().getBean("httpSharedConnection");
		}

		return fieldSharedConnection;
	}

	public void setSharedConnection(HttpSharedConnection inSharedConnection)
	{
		fieldSharedConnection = inSharedConnection;
	}

	@Override
	public void reIndexAll() throws OpenEditException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public SearchQuery createSearchQuery()
	{
		SearchQuery query = new SearchQuery();
		return query;
	}

	@Override
	public HitTracker search(SearchQuery inQuery)
	{

//		JSONObject req = new JSONObject();
//		
//		//req.put("query","enriched_text.sentiment.document.score>0.8");
//		StringBuffer q = new StringBuffer();
//		
//		for (int i = 0; i < inQuery.getTerms().size(); i++)
//		{
//			Term term = inQuery.getTerms().get(i);
//			if( q.length() > 0)
//			{
//				q.append(",");
//			}
//			if( term.getOperation().equals("afterdate"))
//			{
//				String after = term.getValue();
//				Date date = DateStorageUtil.getStorageUtil().parseFromStorage(after);
//				String formated = DateStorageUtil.getStorageUtil().formatDateObj(date, "MM/dd/yyyy");
//				
//				q.append("updated_at:" + formated);
//			}
//			else
//			{
//				String fieldId = term.getDetail().getId();
//				fieldId = fieldId.substring(3);
//				
//				q.append(fieldId + ":" + term.getValue());
//				//req.put("query","text:" + term.getValue());
//			}
//		}
//		
//		req.put("query", q.toString());
		
		String count = "10000";
		String yearSearch = inQuery.getInput("ibmupdated_at");
		String textSearch = inQuery.getInput("description");
		
		 
		
		String queryUrl = "&count=" + count + "&query=updated_at%3A%22"+ yearSearch + "%22";	
		String url = fieldIBMURL + fieldINSTANCE + "/v1/environments/" + fieldIBMENVID + "/collections/" 
		             + fieldIBMCOLLECTIONID + "/query?version=2019-04-30" + queryUrl;
		
		
		// log.info("Searching for : " + req.toJSONString());
		log.info("URL: " +url);
		// log.info("data: " + req);
		
		CloseableHttpResponse resp = getSharedConnection().sharedGet(url); // (url, req);

		if( resp.getStatusLine().getStatusCode() != 200)
		{
			//error
			log.error("Could not get reply " + url + " " + resp.getStatusLine().getReasonPhrase());
			return null;
		}
		
		JSONObject response = getSharedConnection().parseJson(resp);
		
		List results = (List)response.get("results");		
		List datastuff = new ArrayList();
		
		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			Map object = (Map) iterator.next();
			BaseData data  = new BaseData(object);
			datastuff.add(data);
		}
		
		log.info("results: " + datastuff.size());
		
		HitTracker tracker = new ListHitTracker(datastuff);
		
		return tracker;
	}

	@Override
	public String getIndexId()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearIndex()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAll(User inUser)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Data inData, User inUser)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		// TODO Auto-generated method stub

	}

}
