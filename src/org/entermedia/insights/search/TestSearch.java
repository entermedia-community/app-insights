package org.entermedia.insights.search;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;

public class TestSearch extends BaseEnterMediaTest
{

	public void testSearch()
	{
		
		Searcher searcher = (Searcher)getFixture().getModuleManager().getBean("discoverySearcher");
		
		SearchQuery search = searcher.createSearchQuery();
		search.addExact("text","create");
		
		HitTracker hits = searcher.search(search);
		assertTrue(hits.size() > 0);
		
	}
	
}
