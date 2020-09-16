package data

import java.time.*;

import org.entermedia.insights.search.DiscoverySearcher
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.PropertyDetail
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.util.DateStorageUtil
import org.openedit.util.PathUtilities


public String findTableName(Data jsonHit) {
	String sourceType = jsonHit.get("sdl_source_type");

	switch (sourceType) {
		case "PRC": 			return "insight_prc";					// PRC > Future swim lane?
		case "PWS": 			return "insight_contract";				// PWS > Contract Performance Work Statements
		case "MIP Projects": 	return "insight_project_mip"; 			// MIP Projects > MIP Research Projects
		case "MVC": 			return "insight_project_mvc";			// MVC > Direct Projects
		case "MPL": 			return "insight_product_mpl";			// MPL > MITRE Product Library Products
		case "tcas": 			return "insight_capability";			// tcas > Capabilities
		case "platforms": 		return "insight_platform";				// platforms > Platforms		
		default:		 		return null;							// no source or unwanted
	}
}

public String findRealField(String fieldName, Data hit) {
	String sourceType = hit.get("sdl_source_type");
	if (sourceType != null) {
		switch (sourceType) {
			case "PRC":							// PRC > Future swim lane?
				switch (fieldName) {
					case "title": 					return "docName";
					case "text": 					return "text";
				}
			case "PWS":							// PWS > Contract Performance Work Statements
				switch(fieldName) {
					case "title": 					return "title";
					case "text": 					return "text";
				}
			case "MVC": 						// MVC > Direct Projects
				switch(fieldName) {
					case "title": 					return "project_name";
					case "text": 					return "text";				//TBD
				}			
			case "MPL": 			 			// MPL > MITRE Product Library Products
				switch (fieldName) {
					case "title": 					return "title";
					case "text": 					return "text";
				}
			case "platforms": 					// platforms > Platforms
				switch (fieldName) {
					case "title": 					return "title"; // specialCases
					case "text": 					return "text";
				}
		}
	}
}

public String specialCases(String fieldName, Data hit) {
	String sourceType = hit.get("sdl_source_type");
	switch (sourceType) {		
		case "MIP Projects": 				// MIP Projects > MIP Research Projects
			switch(fieldName) {
				case "title": 
				String chargeCode = hit.getValue("chargeCode");
				String longName = hit.getValue("longName")
				return  chargeCode != null ? chargeCode + ' ' : '' + longName != null? longName : '';
			}
		case "tcas": 						//platforms
			switch(fieldName) {
				case "title": 
				String sourceLibrary = hit.getValue("source_library");
				String fileName = PathUtilities.extractFileName(hit.getValue("file_name")); // TODO: remove file.ext
				return sourceLibrary != null ? sourceLibrary + ' ' : '' + fileName != null? fileName : '';
			}
	}
	return null;
}


public void init() {
	int startYear = 2020; // TODO: get from somewhere configured?
	int addToCurrentYear = 1;

	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");	
	DiscoverySearcher discovery = mediaarchive.getSearcher("discovery");
	
	LocalDate currentDate = LocalDate.now();
	// HitTracker all = mediaarchive.query("discovery").match("ibmupdated_at",startYear.toString()).search();
	int currentYear = currentDate.getYear();
	for (int i = startYear; i <= currentYear + addToCurrentYear; i++) {
		log.info("Pulling Year: " + i.toString());
		for (int j = 1; j <= 12; j++) {
			HitTracker all = mediaarchive.query("discovery").match("year", i.toString()).match("month", j.toString())
				.match("count","10000").search();
			if (all != null) {
				log.info(all.size());
				saveDiscoveryData(all, j);
			}
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

public Collection SaveAllValues(Collection entities, String filterType, String colName) {
	Collection toSave = new ArrayList();
	for (entity in entities) {		
		String entityType = entity.get("type");
		if (entityType.equals(filterType)) {
			Map disambiguation = entity.get("disambiguation");		
			if (disambiguation != null) {
				String label = disambiguation != null ?  disambiguation.get("name") : entity.get("name");
				Data data = saveToList(colName, label);
				toSave.add(data);
			}	
		}
	}
	return toSave;
}

public HitTracker saveDiscoveryData(HitTracker all, int month) {
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
			if( col.equals("id")) {
				col = "sdl_id";
			}
			else if (col.startsWith("ibm")) {
				col = col.substring(3);
			}
			
			Object obj  = null;
			
			if (col.equals("filename")) {
				Map extractedMetadata = hit.getValue("extracted_metadata");
				if (extractedMetadata != null) {
					obj = extractedMetadata.get("filename");
				}
			}
						
			Map enrichedText = hit.getValue("enriched_text");
			if (enrichedText != null) {
				switch (col) {
					case "trackedtopics":
						Collection concepts = enrichedText.get("concepts");
						if (concepts != null) {
							List<Data> conceptsToSave = new ArrayList();
							for (concept in concepts) {
								String textConcept = concept.get("text");
								Data topic = saveToList("trackedtopics", textConcept);
								conceptsToSave.add(topic);
							}
							obj = conceptsToSave;
						}
						break;						
					case "keywords":
						obj = "";
						if (enrichedText != null) {
							Collection entities = enrichedText.get("entities");
							if (entities != null) {
								for (entity in entities) {
									Map disambiguation = entity.get("disambiguation");
									if (disambiguation != null) {
										String conceptName = disambiguation != null ?  disambiguation.get("name") : entity.get("name");
										if (conceptName != null) {
											obj += conceptName + "|"
										}
									}
								}
							}
						}
						break;
					default:
						String realField = findRealField(col, hit);
						String specialCase = specialCases(col, hit);
						if (realField != null) {
							obj = specialCase != null ? specialCase : hit.getValue(realField);
						} else {
							obj = null;
						}
						break;
							
				}
				Collection entities = enrichedText.get("entities");
				if (entities != null) {
					switch (col) {
						case "entitycompany": 			obj = SaveAllValues(entities, "Company", detail.getId()); break;
						case "entitypeople": 			obj = SaveAllValues(entities, "People", detail.getId()); break;
						case "entityorganization": 		obj = SaveAllValues(entities, "Organization", detail.getId()); break;
						case "entityfacility": 			obj = SaveAllValues(entities, "Facility", detail.getId()); break;
						case "entitygeographicfeature": obj = SaveAllValues(entities, "GeographicFeature", detail.getId()); break;
						case "entityhealthcondition": 	obj = SaveAllValues(entities, "HealthCondition", detail.getId()); break;
						case "entitylocation":			obj = SaveAllValues(entities, "Location", detail.getId()); break;
						case "entityperson":			obj = SaveAllValues(entities, "Person", detail.getId()); break;
						case "entityprintmedia": 		obj = SaveAllValues(entities, "PrintMedia", detail.getId()); break;
						case "entityquantity": 			obj = SaveAllValues(entities, "Quantity", detail.getId()); break;
						case "entitysport": 			obj = SaveAllValues(entities, "Sport", detail.getId()); break;
						case "entitybroadcaster": 		obj = SaveAllValues(entities, "Broadcaster", detail.getId()); break;
						case "entitycrime": 			obj = SaveAllValues(entities, "Crime", detail.getId()); break;
						case "entitydrug": 				obj = SaveAllValues(entities, "Drug", detail.getId()); break;
						case "entityemailaddress": 		obj = SaveAllValues(entities, "EmailAddress", detail.getId()); break;
						case "entityhashtag": 			obj = SaveAllValues(entities, "Hashtag", detail.getId()); break;
						case "entityipaddress": 		obj = SaveAllValues(entities, "IPAddress", detail.getId()); break;
						case "entityjobtitle": 			obj = SaveAllValues(entities, "JobTitle", detail.getId()); break;
					}
				}
			}			

			
			if (col.equals("sdl_date")) {
				String dateString = hit.getValue("sdl_date");
				Date date = DateStorageUtil.getStorageUtil().parseFromStorage(dateString);
				obj = date;
			} else if (col.equals("sdl_source_type")) {
				obj = tableName;
			}
			
			if (obj != null ) {
				if ( col.equals("fundingSource")) {
					obj = saveToList("ibmfundingSource",obj);
				} else if (col.equals("level1")) {
					obj = saveToList("ibmlevel1", obj);
				}
				//log.info("saving " + detail.getId() + " + " + obj);
				data.setValue(detail.getId(),obj);
			}

		}
		
		tosave.add(data);
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
		
		log.info("Month: " + month + ", Saved: " + tosave.size() + " records, Table: " + tableName);
	}
}

init();
log.info("Complete");
