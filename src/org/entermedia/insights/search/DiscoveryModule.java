package org.entermedia.insights.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.elasticsearch.SearchHitData;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;

public class DiscoveryModule extends BaseMediaModule
{

	public void searchByQuery(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String query = inReq.getRequestParameter("description.value");
		
		HitTracker results = archive.query("discovery").match("description",query).search();
		
		//Map byType = new HashMap();
		//TODO: Get them to add a sdl_type or something
		List ids = new ArrayList();
		
		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			Data map = (Data) iterator.next();
			String id = map.get("sdl_id");
			if( id != null)
			{
				ids.add(id);
			}
		}

		if( !ids.isEmpty() )
		{
			//If over 1000 UI should just say "many"
			if( ids.size() > 1000)
			{
				ids = ids.subList(0, 1000);
			}
			HitTracker unsorted = archive.query("modulesearch").ids(ids).hitsPerPage(1000).search(inReq);
			//TODO: Use the list of ids we got to sort the top 4 from each category?
			//Only save up to 4
			
			ArrayList<Data> sorted = new ArrayList(unsorted.getPageOfHits());
			final List finalids = new ArrayList(ids);
			Collections.sort(sorted,  new Comparator<Data>() 
			{ 
			    // Used for sorting in ascending order of 
			    // roll number 
			    public int compare(Data a, Data b) 
			    { 
			        int location1 = finalids.indexOf(a.getId());
			        int location2 = finalids.indexOf(b.getId());
			    	if( location1 == location2)
			    	{
			    		return 0;
			    	}
			    	if( location2 > location1)
			    	{
			    		return -1;
			    	}
			    	return 1;
			    } 
			});
			
			Map bytypes = organizeHits(inReq, sorted.iterator());

		//	String HitsName = inReq.findValue("hitsname");

			
		}
		
	}
	

	public void organizeHits(WebPageRequest inReq) 
	{
		if( inReq.getPageValue("organizedHits") == null )
		{
			String HitsName = inReq.findValue("hitsname");
			HitTracker hits = (HitTracker)inReq.getPageValue(HitsName);
			organizeHits(inReq, hits.getPageOfHits().iterator());
		}
	}
	public Map organizeHits(WebPageRequest inReq, Iterator hits) 
	{
		ArrayList<Data> found = new ArrayList();
		Map bytypes = new HashMap();
		MediaArchive archive = getMediaArchive(inReq);
		
		//TODO: we need to check all the resultset to make sure we got all the types
		for (Iterator iterator = hits; iterator.hasNext();)
		{
			SearchHitData data = (SearchHitData) iterator.next();
			String type = data.getSearchHit().getType();
			
			Collection values = (Collection) bytypes.get(type);
			if( values == null)
			{
				values = new ArrayList();
				bytypes.put(type,values);
				Data module = archive.getCachedData("module", type);
				found.add(module);
			}
			//if(values.size()<4)
			{
				values.add(data);
			}
			
		}
		if (found.size()>0) {
			Collections.sort(found,  new Comparator<Data>() 
			{ 
			    // Used for sorting in ascending order of 
			    // roll number 
			    public int compare(Data a, Data b) 
			    { 
			    	int a1 = Integer.parseInt(a.get("ordering"));
			    	int b1 = Integer.parseInt(b.get("ordering"));
			    	
			        if ( a1 > b1 ) {
			        	return 1;
			        }
			        return -1;
			    } 
			    
			});
		}
		inReq.putPageValue("organizedModules",found);
		inReq.putPageValue("organizedHits",bytypes);
		return bytypes;
	}

	
}
