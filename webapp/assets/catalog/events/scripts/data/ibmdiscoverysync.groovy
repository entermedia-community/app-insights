package categories

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
	
	HitTracker all = mediaarchive.query("discovery").after("updated_at",from).search();
	
	List tosave = new ArrayList();
	
	Searcher searcher = mediaarchive.getSearcher("insight_product");
	for (hit in all) 
	{
		Data data = searcher.createNewData();
		
		for (PropertyDetail detail in searcher.getPropertyDetails() )
		{
			Object obj  = hit.getValue(detail.getId().substring(3));
			data.setValue(detail.getId(),obj); 
		}
		
		tosave.add(data);
		
		//data.setV
		if( tosave.size() > 1000)
		{
			searcher.saveAllData(tosave, null);
			tosave.clear();
		}
	}
	searcher.saveAllData(tosave, null);
	
}

log.info("Complete");
init();
//log.info("Found ${duplicates.size()} categorypaths with extra categories");
