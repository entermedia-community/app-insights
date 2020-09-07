package org.entermedia.insights.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
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
		Collection ids = new ArrayList();
		
		for (Iterator iterator = results.iterator(); iterator.hasNext();)
		{
			Map map = (Map) iterator.next();
			ids.add(map.get("sdl_id"));
		}

		if( !ids.isEmpty() )
		{
			archive.query("module_search").ids(ids).search(inReq);
		}
		
	}
	
}
