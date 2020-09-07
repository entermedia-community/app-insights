package data

import java.time.LocalDate;
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
	int startYear = 2015; // TODO: get from somewhere configured?

	// HitTracker all = queryDiscovery(from);
	
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	DiscoverySearcher discovery = mediaarchive.getSearcher("discovery");
	
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
	
	LocalDate currentDate = LocalDate.now();
	// HitTracker all = mediaarchive.query("discovery").match("ibmupdated_at",startYear.toString()).search();
	int currentYear = currentDate.getYear();
	for (int i = startYear; i <= currentYear; i++) {
		log.info("Pulling Year: " + i.toString());
		HitTracker all = mediaarchive.query("discovery").match("ibmupdated_at", i.toString()).search();
		log.info(all.size());
		saveDiscoveryData(all);
	}
		
}


public Data saveToList(String tableName, Object value) {
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

public String findTableName(Data jsonHit) {
	String publicationType = jsonHit.get("publicationType");
	if (publicationType != null) {
		return "insight_product"; 	// product MPL
	} else {
		return "insight_project"; 	// direct projects	
	}
	
	return "insight_domain_poc"; 	// Domain POCs
	return "insight_contract"; 		// Contracts / Project Work Statements
	return "insight_project_mip"; 	// MIP Research Projects
	return "insight_capability"; 	// Capabilities
	return "insight_platform";		// Platforms
}

public HitTracker saveDiscoveryData(HitTracker all) {
	Map toSaveByType = new HashMap();
	
	int recordCounter = 0;
	for (hit in all)
	{
		String tableName = findTableName(hit);
		Searcher searcher = mediaarchive.getSearcher(tableName);
		
		List tosave = toSaveByType.get(tableName);
		if (tosave == null) {
			tosave = new ArrayList();
			toSaveByType.put(tableName, tosave);
		}
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
			else if (!col.equals("trackedtopic")) {
				col = col.substring(3);
			}
			
			Object obj  = null;
			
			if (col == "filename") {
				Map extractedMetadata = hit.getValue("extracted_metadata");
				obj = extractedMetadata.get("filename");
			} else if (col == "trackedtopic") {
				
				Map enrichedText = hit.getValue("enriched_text")
				Collection extractedMetadata = enrichedText.get("concepts");
								
				List<Data> conceptsToSave = new ArrayList();
				for (concept in extractedMetadata) {
					String textConcept = concept.get("text");
					Data topic = saveToList("trackedtopic", textConcept);
					conceptsToSave.add(topic);
				}
				obj = conceptsToSave;
			} else {
				obj = hit.getValue(col);
			}
			if (obj != null ) {
				if ( col.equals("fundingSource")) {
					obj = saveToList("ibmfundingSource",obj);
				} else if (col.equals("level1")) {
					obj = saveToList("ibmlevel1", obj);
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
		recordCounter++;
		if ((recordCounter % 500) == 0) {
			log.info("Records Pulled: " + recordCounter);
		}
	}

	for ( String tableName in toSaveByType.keySet()) {
		Searcher searcher = mediaarchive.getSearcher(tableName);
		List tosave = toSaveByType.get(tableName);
		searcher.saveAllData(tosave, null);
		
		log.info("Final save: " + tosave.size() + " table: " + tableName);
	}
}

init();
log.info("Complete");
//log.info("Found ${duplicates.size()} categorypaths with extra categories");
