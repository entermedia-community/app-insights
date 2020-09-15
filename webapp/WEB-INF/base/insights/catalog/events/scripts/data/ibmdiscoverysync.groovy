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
		case "MPL": 			return "insight_product";  				// MPL > MITRE Product Library Products
		case "tcas": 			return "insight_capability";			// tcas > Capabilities
		case "platforms": 		return "insight_platform";				// platforms > Platforms		
		default:
				log.info("Missing sourcetype " + sourceType);
		 		return "insight_unsourced";				// no source found
	}
}

public String findRealField(String fieldName, Data hit) {
	String sourceType = hit.get("sdl_source_type");
	if (sourceType != null) {
		switch (sourceType) {
			case "PRC":							// PRC > Future swim lane?
				switch (fieldName) {
					case "title": 				return "docName";
					case "text": 				return "text";
					case "projectNumber": 		return "projectNumber";
					case "publicationDate": 	return "publicationDate";
					case "fundingSource": 		return "fundingSource";
					case "originalAuthorName": 	return "originalAuthorName";
					case "copyrightText": 		return "copyrightText";
				}
			case "PWS":							// PWS > Contract Performance Work Statements
				switch(fieldName) {
					case "title": 				return "title";
					case "text": 				return "text";
					case "projectNumber": 		return "projectNumber"; // TBD
					case "publicationDate": 	return "sdl_date";
					case "fundingSource": 		return "fundingSource"; // TBD
					case "originalAuthorName": 	return "originalAuthorName"; TBD
					case "copyrightText": 		return "copyrightText"; TBD
				}
			case "MIP Projects": 				// MIP Projects > MIP Research Projects
				switch(fieldName) {
					case "title": 				return "title"; // specialCases
					case "text": 				return "text";
					case "projectNumber": 		return "projectNumber";
					case "publicationDate": 	return "endDate";
					case "fundingSource": 		return "TBD";
					case "originalAuthorName": 	return "phonebookDisplayName";
					case "copyrightText": 		return "copyrightText";
				}
			
			case "MVC": 						// MVC > Direct Projects
				switch(fieldName) {
					case "title": 				return "project_name";
					case "text": 				return "text";
					case "projectNumber": 		return "project_page_charge_code";
					case "publicationDate": 	return "TBD";
					case "fundingSource": 		return "project_sponsor";
					case "originalAuthorName": 	return "project_leader";
					case "copyrightText": 		return "TBD";
				}
			
			case "MPL": 			 			// MPL > MITRE Product Library Products
				switch (fieldName) {
					case "title": 				return "title";
					case "text": 				return "text";
					case "projectNumber": 		return "projectNumber";
					case "publicationDate": 	return "TBD";
					case "fundingSource": 		return "fundingSource";
					case "originalAuthorName": 	return "originalAuthorName";
					case "copyrightText": 		return "copyrightText";
				}
			case "tcas": 						// tcas > Capabilities
				switch (fieldName) {
					case "title": 				return "title"; //specialCase
					case "text": 				return "text";
					case "projectNumber": 		return "TBD";
					case "publicationDate": 	return "created_at"; // might be created
					case "fundingSource": 		return "TBD";
					case "originalAuthorName": 	return "TBD";     	// (--field_tca_organizationleadername)
					case "copyrightText": 		return "TBD";
				}
			case "platforms": 					// platforms > Platforms
				switch (fieldName) {
					case "title": 				return "title"; // specialCases
					case "text": 				return "text";
					case "projectNumber": 		return "TBD";
					case "publicationDate": 	return "TBD"; 		// might be created
					case "fundingSource": 		return "TBD";
					case "originalAuthorName": 	return "TBD";     	// (--field_tca_organizationleadername)
					case "copyrightText": 		return "TBD";
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
				String fileName = hit.getValue("file_name"); // TODO: remove file.ext
				return sourceLibrary != null ? sourceLibrary + ' ' : '' + fileName != null? fileName : '';
			}
	}
	return null;
}


public void init()
{
	int startYear = 2016; // TODO: get from somewhere configured?
	int addToCurrentYear = 4;

	// HitTracker all = queryDiscovery(from);
	
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	DiscoverySearcher discovery = mediaarchive.getSearcher("discovery");
	
	LocalDate currentDate = LocalDate.now();
	// HitTracker all = mediaarchive.query("discovery").match("ibmupdated_at",startYear.toString()).search();
	int currentYear = currentDate.getYear();
	for (int i = startYear; i <= currentYear + addToCurrentYear; i++) {
		log.info("Pulling Year: " + i.toString());
		for (int j = 1; j <= 12; j++) {
			log.info("Pulling Month: " + j.toString());
			HitTracker all = mediaarchive.query("discovery").match("year", i.toString()).match("month", j.toString())
				.match("count","10000").search();
			if (all != null) {
				log.info(all.size());
				saveDiscoveryData(all);
			} else {
				log.info("Request returned null object");
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
		data.setValue("sourcetype", tableName);
		
		for (PropertyDetail detail in searcher.getPropertyDetails() )
		{
			String col = detail.getId();
			if( col.equals("id")) {
				col = "sdl_id";
			}
			else if (!col.equals("trackedtopics") && !col.equals("keywords")) {
				col = col.substring(3);
			}
			
			Object obj  = null;
			Map enrichedText = hit.getValue("enriched_text");
			if (enrichedText != null) {
				if (col == "filename") {
					Map extractedMetadata = hit.getValue("extracted_metadata");
					if (extractedMetadata != null) {
						obj = extractedMetadata.get("filename");
					}
				} else if (col == "trackedtopics") {
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
				} else if (col == "keywords") {
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
				} else {
					String realField = findRealField(col, hit);
					String specialCase = specialCases(col, hit);
					obj = specialCase != null ? specialCase : hit.getValue(realField);
				}
			}
			
			if (col.equals("sdl_date")) {
//				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
				String dateString = hit.getValue("sdl_date");
//				Instant instant = Instant.parse(dateString);
//				LocalDateTime result = LocalDateTime.ofInstant(instant, ZoneId.of(ZoneOffset.UTC.getId()));
				//log.info(dateString.toString())
				//log.info(detail.getId());
				Date date = DateStorageUtil.getStorageUtil().parseFromStorage(dateString);
				obj = date;
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
