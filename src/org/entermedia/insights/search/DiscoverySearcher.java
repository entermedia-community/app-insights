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
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.BaseData;
import org.openedit.data.BaseSearcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.users.User;

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
		String count = inQuery.getInput("count") == null ? "5000" : inQuery.getInput("count");
		String yearSearch = inQuery.getInput("year");
		int monthSearch = Integer.parseInt(inQuery.getInput("month"));
		String textSearch = inQuery.getInput("description");
		
		String queryUrl = null;
		 
		if (yearSearch != null && !yearSearch.isEmpty()) {
			DateTime endDate = (new DateTime(Integer.parseInt(yearSearch), monthSearch, 1, 0,0)).plusMonths(1);						
			queryUrl = "&count=" + count + "&query=%5Bsdl_date%3E%3D" + yearSearch + "-" + String.format("%02d", monthSearch) + "-01T00%3A00%3A00.000Z,sdl_date%3C" 
					   +  endDate.getYear() + '-' + String.format("%02d", endDate.getMonthOfYear()) + "-01T00%3A00%3A00.000Z%5D";
		} else {
			queryUrl = "&count=" + count + "&return=sdl_id&query=" + textSearch;
		}
		String url = fieldIBMURL + fieldINSTANCE + "/v1/environments/" + fieldIBMENVID + "/collections/" 
		             + fieldIBMCOLLECTIONID + "/query?version=2019-04-30" + queryUrl;
		
		log.info("URL: " +url);
		
		CloseableHttpResponse resp = getSharedConnection().sharedGet(url); // (url, req);

		if( resp.getStatusLine().getStatusCode() != 200)
		{
			//error
			getSharedConnection().release(resp);
			log.error("Could not get reply " + url + " " + resp.getStatusLine().getReasonPhrase());
			return null;
		}
		
		JSONObject response = getSharedConnection().parseJson(resp);
		
		List results = (List)response.get("results");
		// String totalMatching = (String)response.get("matching_results");		
		// log.info("Total Matching in Query: " + totalMatching);
		
		List datastuff = new ArrayList();
		log.info("results-json: " + results.size());
		
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
	public boolean initialize()
	{
		MediaArchive mediaarchive = (MediaArchive)getModuleManager().getBean(getCatalogId(), "mediaArchive");
		getSharedConnection().clearSharedHeaders();
		String secretkey = mediaarchive.getCatalogSettingValue("discovery_secretkey");//"8tU2gwnnX8CtvwFfJ8q0VogskHGvHpxM3h3M2P6q-5YG"
		
		String enc = "apikey" + ":" + secretkey;
		byte[] encodedBytes = Base64.encodeBase64(enc.getBytes());
		String authString = new String(encodedBytes);
		getSharedConnection().addSharedHeader("Accept", "application/json");
		getSharedConnection().addSharedHeader("Content-type", "application/json");
		getSharedConnection().addSharedHeader("Authorization", "Basic " + authString);
		String url = mediaarchive.getCatalogSettingValue("discovery_url");//"https://api.us-south.watson.cloud.ibm.com/instances/"
		setIBMURL(url);
		String instance = mediaarchive.getCatalogSettingValue("discovery_instance");//21ab8dc5-7b0f-4e4a-96f7-92b8deb7b0a4"
		setINSTANCE(instance);
		String envid = mediaarchive.getCatalogSettingValue("discovery_envid");//"91745818-65e0-4f25-89b7-e17754afdfd7"
		setIBMENVID(envid);
		String collectionid = mediaarchive.getCatalogSettingValue("discovery_collectionid");//"5563b583-ee7e-4c97-9029-0be597e142d1"
		setIBMCOLLECTIONID(collectionid);
		
		return super.initialize();
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
