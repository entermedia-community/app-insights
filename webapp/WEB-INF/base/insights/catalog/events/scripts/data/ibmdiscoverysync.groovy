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
	int startYear = 2020; // TODO: get from somewhere configured?

	// HitTracker all = queryDiscovery(from);
	
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	DiscoverySearcher discovery = mediaarchive.getSearcher("discovery");
	
	LocalDate currentDate = LocalDate.now();
	// HitTracker all = mediaarchive.query("discovery").match("ibmupdated_at",startYear.toString()).search();
	int currentYear = currentDate.getYear();
	for (int i = startYear; i <= currentYear; i++) {
		log.info("Pulling Year: " + i.toString());
		HitTracker all = mediaarchive.query("discovery").match("ibmupdated_at", i.toString()).search();
		if (all != null) {
			log.info(all.size());
			saveDiscoveryData(all);
		} else { 
			log.info("Request returned null object");
		}
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
	String level7 = jsonHit.get("level7");
	String level6 = jsonHit.get("level6");
	String level5 = jsonHit.get("level5");
	String level4 = jsonHit.get("level4");
	String level3 = jsonHit.get("level3");
	String docName= jsonHit.get("docName");
	String releaseStatement = jsonHit.get("releaseStatement");
	if (level7 != null) {
		return "insight_product"; 	// product MPL
	} else if (level6 != null) {
		return "insight_project"; 	// direct projects	
	} else if (level5 != null) {	
		return "insight_domain_poc"; 	// Domain POCs
	} else if (level4 != null)
		return "insight_contract"; 		// Contracts / Project Work Statements
	else if (level3 != null)
		return "insight_project_mip"; 	// MIP Research Projects
	else if (publicationType != null)
		return  "insight_capability"; 	// Capabilities
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
			else if (!col.equals("trackedtopics") && !col.equals("keywords")) {
				col = col.substring(3);
			}
			
			Object obj  = null;
			Map enrichedText = hit.getValue("enriched_text");
			if (col == "filename") {
				Map extractedMetadata = hit.getValue("extracted_metadata");
				obj = extractedMetadata.get("filename");
			} else if (col == "trackedtopics") {
				Collection concepts = enrichedText.get("concepts");								
				List<Data> conceptsToSave = new ArrayList();
				for (concept in concepts) {
					String textConcept = concept.get("text");
					Data topic = saveToList("trackedtopics", textConcept);
					conceptsToSave.add(topic);
				}
				obj = conceptsToSave;
			} else if (col == "keywords") {
				obj = "";				
				Collection entities = enrichedText.get("entities");				
				for (entity in entities) {
					Map disambiguation = entity.get("disambiguation");
					String conceptName = disambiguation != null ?  disambiguation.get("name") : entity.get("name");
					if (conceptName != null) {
						obj += conceptName + "|"
					}
				}
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
