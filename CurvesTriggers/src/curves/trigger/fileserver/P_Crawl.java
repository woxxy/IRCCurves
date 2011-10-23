package curves.trigger.fileserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import org.json.*;

import curves.main.Bot;
import curves.trigger.IPeriodicHandler;

public class P_Crawl implements IPeriodicHandler {

	int skip = 0;
	Logger log = Logger.getLogger(P_Crawl.class);

	HashMap<String, String> filelist;
	String[] extensions;
	String root;
	long latestDate = 0;
	String latestFile;

	public boolean isReady(Bot bot, Hashtable<String, Object> storage) {
		skip = (skip + 1) % 60;
		return skip == 1;
	}

	public void process(Bot bot, Hashtable<String, Object> storage) {
		filelist = new HashMap<String, String>();
		traverse();
		storage.put("file list", filelist);
		storage.put("latest file", latestFile);
		log.info(filelist.size() + " file(s) found");
		log.info("The newest file is " + latestFile);
	}

	boolean checkExtension(String filename) {
		for (String extension : extensions) {
			if (filename.endsWith(extension))
				return true;
		}
		return false;
	}
	
	public void traverse() {
		traverse(1);
	}

	public void traverse(int page) {
		try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec("php /var/www/slide/index.php api reader chapters orderby desc_created per_page 100 page " + page);

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line = input.readLine();
            JSONObject obj = new JSONObject(line);
            
            if(obj.has("error"))
            {
            	return;
            }
            
            JSONArray chapters_arr = obj.getJSONArray("chapters");
            
            for(int i = 0; i < chapters_arr.length(); i++)
            {
            	String search_line = "";
            	String request_line = "";
            	JSONObject chapter_obj = chapters_arr.getJSONObject(i).getJSONObject("chapter");
            	JSONObject comic_obj = chapters_arr.getJSONObject(i).getJSONObject("comic");
            	JSONArray teams_arr = chapters_arr.getJSONObject(i).getJSONArray("teams");
            	search_line = "Series: " + comic_obj.getString("name") + " - ";
            	if(chapter_obj.getInt("volume") > 0)
            		search_line += "Volume " + chapter_obj.getInt("subchapter") + ", ";
            	search_line += "Chapter " + chapter_obj.getInt("chapter");
            	if(chapter_obj.getInt("subchapter") > 0)
            		search_line += "." + chapter_obj.getInt("subchapter");
            	if(chapter_obj.getString("name").length() > 0)
            		search_line += " : " + chapter_obj.getString("name");
            	
            	search_line += " by ";
            	for(int j = 0; j < teams_arr.length(); j++)
            	{
            		search_line += teams_arr.getJSONObject(j).getString("name");
            		if(j < teams_arr.length() - 1)
            			search_line += ", ";
            	}
            	
            	request_line = chapter_obj.getString("download_href");
            	request_line = request_line.replaceFirst("http://localhost/", "");
            	request_line = request_line.replace("/", " ");
            	filelist.put(search_line, request_line);
            	log.debug(search_line);
            	if(i == 0 && page == 1)
            	{
            		latestFile = search_line;
            	}
            	
            	if(i == 99)
            	{
            		traverse(++page);
            	}
            }
            

        } catch(Exception e) {
            log.error(e.toString());
        }
	}
}
