package data

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.util.DateStorageUtil

public void init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");

	Date from  = DateStorageUtil.getStorageUtil().substractDaysToDate(new Date(),1000);
	
	//HitTracker all = mediaarchive.query("discovery").after("updated_at",from).search();  //Missing Discovery Table Def. or update_at field
	
	HitTracker all = mediaarchive.query("discovery").all().search();
	
	List tosave = new ArrayList();
	Set fundingSources = new HashSet();
	
	Searcher searcher = mediaarchive.getSearcher("insight_product");
	for (hit in all) 
	{
		Data data = searcher.createNewData();
		
		for (PropertyDetail detail in searcher.getPropertyDetails() )
		{
			String col = detail.getId();
			if( col.equals("id"))
			{
				col = "sdl_id";
			}			
			else {
				col = col.substring(3);
			}
			
			Object obj  = hit.getValue(col);
			if (obj != null ) {
				if ( col.equals("fundingSource"))
				{
						fundingSources.add(obj);
				}
				data.setValue(detail.getId(),obj);
			} 
		}
		
		tosave.add(data);
		
		//data.setV
		if( tosave.size() > 1000)
		{
			log.info("saves " + tosave.size());
			searcher.saveAllData(tosave, null);
			tosave.clear();
		}
	}
	saveList("ibmfundingsource", fundingSources)
	log.info("Final save " + tosave.size());
	searcher.saveAllData(tosave, null);
	
}

public void saveList(String tableName, Set values) {
	
}

log.info("Complete");
init();
//log.info("Found ${duplicates.size()} categorypaths with extra categories");
