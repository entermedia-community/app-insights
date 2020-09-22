package org.entermedia.insights.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.elasticsearch.SearchHitData;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class DiscoveryModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(DiscoveryModule.class);

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

			//Do the same description search locally and for the see more page
			
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
			
//			log.info("unsorted Size: " + unsorted.size());
//			log.info("sorted Size: " + sorted.size());
			organizeHits(inReq, unsorted, sorted);

		//	String HitsName = inReq.findValue("hitsname");

			
		}
		
	}
	
	public void organizeHits(WebPageRequest inReq) {
		String HitsName = inReq.findValue("hitsname");
		HitTracker hits = (HitTracker)inReq.getPageValue(HitsName);
		Collection pageOfHits = hits.getPageOfHits();
		
		organizeHits(inReq, hits, pageOfHits);
	}
	

	public void organizeHits(WebPageRequest inReq,HitTracker hits, Collection pageOfHits) 
	{
		//if( inReq.getPageValue("organizedHits") == null )
		{
			// String HitsName = inReq.findValue("hitsname");
			//  HitTracker hits = (HitTracker)inReq.getPageValue(HitsName);
			// Collection pageOfHits = hits.getPageOfHits();
			if( hits != null)
			{
				// log.info(hits.getHitsPerPage());
				//Find counts
				String smaxsize = inReq.findValue("maxcols");
				int targetsize = smaxsize == null? 7:Integer.parseInt(smaxsize);
				
				Map<String,Collection> bytypes = organizeHits(inReq, pageOfHits.iterator(),targetsize);
				
				ArrayList foundmodules = new ArrayList();
				//See if we have enough from one page. If not then run searches to get some results
				FilterNode node  = hits.getUserFilterValue("ibmsdl_source_type");
				if( node != null)
				{
					MediaArchive archive = getMediaArchive(hits.getCatalogId());
					for (Iterator iterator = node.getChildren().iterator(); iterator.hasNext();)
					{
						FilterNode filter = (FilterNode) iterator.next();
						String sourcetype = filter.getId();
						int total  = filter.getCount();
						Collection sthits = bytypes.get(sourcetype);
						int maxpossible = Math.min(total,targetsize);
						if( sthits == null || sthits.size() < maxpossible)
						{
							sthits = loadMoreResults(archive,hits.getSearchQuery(),sourcetype, maxpossible);
							bytypes.put(sourcetype,sthits);
						}
						Data module = archive.getCachedData("module", sourcetype);
						foundmodules.add(module);
					}
				}

				if (!foundmodules.isEmpty()) {
					Collections.sort(foundmodules,  new Comparator<Data>() 
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
				log.info("organized Modules: " + foundmodules);
				
				if (foundmodules.size() == 0) {
					log.info("####---####--###--### ISSUE HERE ####---####--###--###");
				}
				
				inReq.putPageValue("organizedModules",foundmodules);
				
			}
		}
	}
	private Collection loadMoreResults(MediaArchive archive, SearchQuery inSearchQuery, String inSourcetype, int maxsize)
	{
		//search for more
		Searcher searcher = archive.getSearcher(inSourcetype);
		SearchQuery q = searcher.createSearchQuery();
		q.addChildQuery(inSearchQuery);
		q.setHitsPerPage(maxsize);
		HitTracker more = searcher.search(q);
		return more.getPageOfHits();
	}


	public Map organizeHits(WebPageRequest inReq, Iterator hits, int maxsize) 
	{
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
			}
			if(values.size()<maxsize)
			{
				values.add(data);
			}
			
		}
//		log.info("put un page: " + bytypes);
//		log.info("size: " + bytypes.size());
		inReq.putPageValue("organizedHits",bytypes);
		return bytypes;
	}

	
}
