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
	
	
	public HttpSharedConnection getSharedConnection()
	{
		if (fieldSharedConnection == null)
		{
			fieldSharedConnection = (HttpSharedConnection)getModuleManager().getBean("httpSharedConnection");

			//TODO: get key
			String enc = "apikey" + ":" + "8tU2gwnnX8CtvwFfJ8q0VogskHGvHpxM3h3M2P6q-5YG";
			byte[] encodedBytes = Base64.encodeBase64(enc.getBytes());
			String authString = new String(encodedBytes);
			fieldSharedConnection.addSharedHeader("Accept", "application/json");
			fieldSharedConnection.addSharedHeader("Content-type", "application/json");
			fieldSharedConnection.addSharedHeader("Authorization", "Basic " + authString);

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

		JSONObject req = new JSONObject();
		
		//req.put("query","enriched_text.sentiment.document.score>0.8");
		StringBuffer q = new StringBuffer();
		
		for (int i = 0; i < inQuery.getTerms().size(); i++)
		{
			Term term = inQuery.getTerms().get(i);
			if( q.length() > 0)
			{
				q.append(",");
			}
			if( term.getOperation().equals("afterdate"))
			{
				String after = term.getValue();
				Date date = DateStorageUtil.getStorageUtil().parseFromStorage(after);
				String formated = DateStorageUtil.getStorageUtil().formatDateObj(date, "MM/dd/yyyy");
				
				q.append("updated_at:" + formated);
			}
			else
			{
				q.append("text:" + term.getValue());
				//req.put("query","text:" + term.getValue());
			}
		}
		
		req.put("query", q.toString());
		req.put("count", 10000);
		String IBMURL="https://api.us-south.discovery.watson.cloud.ibm.com/instances/21ab8dc5-7b0f-4e4a-96f7-92b8deb7b0a4";
		String IBMENVID="91745818-65e0-4f25-89b7-e17754afdfd7";
		//String IBMCONFIGURATIONID="b6e319ba-9dcd-4e01-b8cb-6caa56d6db1b";
		String IBMCOLLECTIONID="5563b583-ee7e-4c97-9029-0be597e142d1";
			
		String url = IBMURL + "/v1/environments/" + IBMENVID + "/collections/" + IBMCOLLECTIONID + "/query?version=2019-04-30";
		
		
		log.info("Searching for : " + req.toJSONString());
		log.info("URL: " +url);
		log.info("data: " + req);
		
		CloseableHttpResponse resp = getSharedConnection().sharedPostWithJson(url, req);

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
