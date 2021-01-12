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
import org.openedit.data.PropertyDetail;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.profile.UserProfile;

public class DiscoveryModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(DiscoveryModule.class);

	public void searchByQuery(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String query = inReq.getRequestParameter("description.value");
		
		QueryBuilder dq = archive.query("discovery").match("description",query);
		dq.getQuery().setValue("count",1000);
		HitTracker results  = dq.search();
		
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

		if( ids.isEmpty() )
		{
			log.info("Discovery returned no results for " + query);
			QueryBuilder q = archive.query("modulesearch").freeform("description",query).named("modulehits").hitsPerPage(1000);
			HitTracker unsorted = q.search(inReq);
			return;
		}
			//If over 1000 UI should just say "many" maxClase is 1024
			if( ids.size() > 1000)
			{
				ids = ids.subList(0, 1000);
			}
			inReq.setRequestParameter("clearfilters","true");
			QueryBuilder q = archive.query("modulesearch").ids(ids).named("modulehits").hitsPerPage(2000);
			HitTracker unsorted = q.search(inReq);
			unsorted.getSearchQuery().setValue("description",query);

			//inReq.getUs
//Discovery might return more than we do			
//			String key = "modulesearch" + archive.getCatalogId() + "userFilters";
//			UserFilters filters = (UserFilters) inReq.getSessionValue(key);
//			if( filters != null)
//			{
//				//filters.clearOptions("modulesearch", query);
//				unsorted.setUserFilterValues(null);
//			}
			
			//TODO: Use the list of ids we got to sort the top 4 from each category?
			//Only save up to 4

			//Do the same description search locally and for the see more page
			
//			ArrayList<Data> sorted = new ArrayList(unsorted.getPageOfHits());
//			final List finalids = new ArrayList(ids);
//			Collections.sort(sorted,  new Comparator<Data>() 
//			{ 
//			    // Used for sorting in ascending order of 
//			    // roll number 
//			    public int compare(Data a, Data b) 
//			    { 
//			        int location1 = finalids.indexOf(a.getId());
//			        int location2 = finalids.indexOf(b.getId());
//			    	if( location1 == location2)
//			    	{
//			    		return 0;
//			    	}
//			    	if( location2 > location1)
//			    	{
//			    		return -1;
//			    	}
//			    	return 1;
//			    } 
//			});
			
//			log.info("unsorted Size: " + unsorted.size());
//			log.info("sorted Size: " + sorted.size());
		//	organizeHits(inReq, unsorted, sorted);

		//	String HitsName = inReq.findValue("hitsname");
		
	}
	
	public void organizeHits(WebPageRequest inReq) 
	{
		String HitsName = inReq.findValue("hitsname");
		HitTracker hits = (HitTracker)inReq.getPageValue(HitsName);
		if( hits != null)
		{
			Collection pageOfHits = hits.getPageOfHits();
			
			organizeHits(inReq, hits, pageOfHits);
		}
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
				MediaArchive archive = getMediaArchive(inReq);

//				String key = "modulesearch" + archive.getCatalogId() + "userFilters";
//				UserFilters filtervalues = (UserFilters)inReq.getSessionValue(key);
//				if( filtervalues != null)
//				{
//
//				}
				FilterNode node = hits.findFilterValue("ibmsdl_source_type");

				
				// log.info(hits.getHitsPerPage());
				//Find counts
				String smaxsize = inReq.findValue("maxcols");
				int targetsize = smaxsize == null? 8 : Integer.parseInt(smaxsize);
				
				Map<String,Collection> bytypes = organizeHits(inReq, pageOfHits.iterator(),targetsize);
				
				ArrayList foundmodules = new ArrayList();
				//See if we have enough from one page. If not then run searches to get some results
				if( node != null)
				{
					for (Iterator iterator = node.getChildren().iterator(); iterator.hasNext();)
					{
						FilterNode filter = (FilterNode) iterator.next();
						String sourcetype = filter.getId();
						int total  = filter.getCount();
						Collection sthits = bytypes.get(sourcetype);
						int maxpossible = Math.min(total,targetsize);
						if( sthits == null || sthits.size() < maxpossible)
						{
//							if( !hits.getSearchQuery().isEmpty())
//							{
								//Only makes sense when someone searched for text. Otherwise we get all values from *
//								String input = hits.getSearchQuery().getMainInput();
//								if( input != null)
//								{
									Collection moredata = loadMoreResults(archive,hits.getSearchQuery(),sourcetype, maxpossible);
									//TODO: Compine results, avoid dups
									bytypes.put(sourcetype,moredata);
//								}
//							}
						}
						if( sthits != null && !sthits.isEmpty())
						{
							Data module = archive.getCachedData("module", sourcetype);
							foundmodules.add(module);
						}
					}
				}

				sortModules(foundmodules);
				log.info("organized Modules: " + foundmodules);
				
				if (foundmodules.size() == 0) {
					log.info("####---####--###--### ISSUE HERE ####---####--###--###");
				}
				
				inReq.putPageValue("organizedModules",foundmodules);
				
			}
		}
	}

	protected void sortModules(ArrayList foundmodules)
	{
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
	}
	private Collection loadMoreResults(MediaArchive archive, SearchQuery inSearchQuery, String inSourcetype, int maxsize)
	{
		//search for more
		Searcher searcher = archive.getSearcher(inSourcetype);
		SearchQuery q = inSearchQuery.copy();
		q.setHitsPerPage(maxsize);
		for (Iterator iterator = q.getTerms().iterator(); iterator.hasNext();)
		{
			Term term = (Term) iterator.next();
			PropertyDetail old = term.getDetail();
			
			term.setDetail( searcher.getDetail(old.getId()) );
		}
		HitTracker more = searcher.search(q);
		return more.getPageOfHits();
	}


	public Map organizeHits(WebPageRequest inReq, Iterator hits, int maxsize) 
	{
		Map bytypes = new HashMap();
		MediaArchive archive = getMediaArchive(inReq);
		
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

	public void showFavorites(WebPageRequest inReq) 
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		//get the user profile and do a module search
		UserProfile profile = inReq.getUserProfile();
		if( profile == null)
		{
			return;
		}
		
		ArrayList<Data> foundmodules = new ArrayList();
		
		Collection<Data> modulestocheck = listSearchModules(archive);

		Collection uids = new ArrayList();
		for (Iterator iterator = modulestocheck.iterator(); iterator.hasNext();)
		{
			Data module = (Data) iterator.next();
			String searchtype = module.getId();
			Collection ids = profile.getValues("favorites_" + searchtype);
			if( ids != null)
			{
				for (Iterator iterator2 = ids.iterator(); iterator2.hasNext();)
				{
					String id = (String) iterator2.next();
					uids.add(searchtype + "_" + id);
				}
			}
		}
		if( !uids.isEmpty())
		{
			Searcher searcher = archive.getSearcher("modulesearch");
			
			SearchQuery query = searcher.addStandardSearchTerms(inReq);
			if( query == null)
			{
				query = searcher.createSearchQuery();
			}
			query.setName("modulehits");
			query.addOrsGroup("uid",uids);
			query.setHitsPerPage(1000);
			HitTracker hits = searcher.cachedSearch(inReq, query);
			if( hits != null)
			{
				//organizeHits(inReq, hits, hits.getPageOfHits());
				log.info("Found " + hits.size() + " on " + hits.getHitsName());
			}
		}

	}
	protected Collection<Data> listSearchModules(MediaArchive archive)
	{
		Collection<Data> modules = getSearcherManager().getList(archive.getCatalogId(), "module");
		Collection searchmodules = new ArrayList();
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String show = data.get("showonsearch");
			if( !"modulesearch".equals(data.getId() ) && Boolean.parseBoolean(show)) //Permission check?
			{
				searchmodules.add(data);
			}
		}
		return searchmodules;
	}

	
}
