import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import org.openedit.hittracker.Term

public void init()
{
	HitTracker hits = context.getPageValue("hits");
	
	if( hits == null)
	{
		return null;  //should never happen
	}

	SearchQuery query = hits.getSearchQuery();
	Collection terms = query.getTerms();

	JSONObject json = new JSONObject();
	
	JSONArray array = new JSONArray();
	json.put("fields",array);
		log.info("Has main: "  + hits.getSearchQuery().hasMainInput());
	if( hits.getSearchQuery().hasMainInput())
	{
		String desc = hits.getSearchQuery().getMainInput();
		JSONObject field = new JSONObject();
		field.put("name", "description");
		field.put("operation", "freeform");
		field.put("value", desc);
		array.add(field);
	}
	for( Term term in terms)
	{
		if( term.isUserFilter() 
			&& term.getDetail().getId() != "id")
		{
			JSONObject field = new JSONObject();
			field.put("name", term.getDetail().getId());
			field.put("operation", term.getOperation());
			field.put("value", term.getValue());
			array.add(field);
		}
	}	
	context.putPageValue("jsonsearch",json.toString());
}

init();



