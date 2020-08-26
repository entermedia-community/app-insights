package data

import org.apache.commons.codec.binary.Base64
import org.entermedia.insights.search.DiscoverySearcher
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.util.DateStorageUtil
import org.openedit.util.PathUtilities

public void init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");

	Date from  = DateStorageUtil.getStorageUtil().substractDaysToDate(new Date(),1000);
	
	//HitTracker all = mediaarchive.query("discovery").after("updated_at",from).search();  //Missing Discovery Table Def. or update_at field
	
	DiscoverySearcher discovery = mediaarchive.getSearcher("discovery");
	
	
	//Init discovery
	
	discovery.getSharedConnection().clearSharedHeaders();
	def secretkey = mediaarchive.getCatalogSettingValue("discovery_secretkey");//"8tU2gwnnX8CtvwFfJ8q0VogskHGvHpxM3h3M2P6q-5YG"
	
	String enc = "apikey" + ":" + secretkey;
	byte[] encodedBytes = Base64.encodeBase64(enc.getBytes());
	String authString = new String(encodedBytes);
	discovery.getSharedConnection().addSharedHeader("Accept", "application/json");
	discovery.getSharedConnection().addSharedHeader("Content-type", "application/json");
	discovery.getSharedConnection().addSharedHeader("Authorization", "Basic " + authString);
	def url = mediaarchive.getCatalogSettingValue("discovery_url");//"https://api.us-south.discovery.watson.cloud.ibm.com/instances/"
	discovery.setIBMURL(url);
	def instance = mediaarchive.getCatalogSettingValue("discovery_instance");//21ab8dc5-7b0f-4e4a-96f7-92b8deb7b0a4"
	discovery.setINSTANCE(instance);
	def envid = mediaarchive.getCatalogSettingValue("discovery_envid");//"91745818-65e0-4f25-89b7-e17754afdfd7"
	discovery.setIBMENVID(envid);
	def collectionid = mediaarchive.getCatalogSettingValue("discovery_collectionid");//"5563b583-ee7e-4c97-9029-0be597e142d1"
	discovery.setIBMCOLLECTIONID(collectionid);

	
	HitTracker all = mediaarchive.query("discovery").all().search();
	
	List tosave = new ArrayList();
	
	
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
			else if(col.equals("keywords")) {
				col = "declaredTags";
			}			
			else {
				col = col.substring(3);
			}
			
			Object obj  = null;
			if (col == "filename") {
				Map extractedMetadata = hit.getValue("extracted_metadata");				
				obj = extractedMetadata.get("filename");
			} else {
				obj = hit.getValue(col);
			}
			if (obj != null ) {
				if ( col.equals("fundingSource")) {
					obj = saveToList("ibmfundingSource",obj)
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
	// saveToList("ibmfundingsource", fundingSource)
	log.info("Final save " + tosave.size());
	searcher.saveAllData(tosave, null);	
	
}

public Object saveToList(String tableName, Object value) {
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	String id = PathUtilities.extractId(value.toString());
	Data data = mediaarchive.getCachedData(tableName, id);
	if (data == null) {
		data = mediaarchive.getSearcher(tableName).createNewData();
		data.setId(id);
		data.setName(value.toString());
		mediaarchive.saveData(tableName, data);
	}
	return data;	
}

log.info("Complete");
init();
//log.info("Found ${duplicates.size()} categorypaths with extra categories");
