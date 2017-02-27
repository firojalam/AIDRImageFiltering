package hbku.qcri.sc.aidr.imagecrawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hbku.qcri.sc.aidr.imagecrawler.ImageCrawler;

public class DoStat {
	protected static final Logger log = LoggerFactory.getLogger(DoStat.class);
	
	public static void main(String args[]) throws ParseException, IOException {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("f", "folder", true, "Save image to the folder under the name of collection.");
        options.addOption("j", "json_file", true, "Json file of the collection.");
        options.addOption("m", "mode", true, "Run in parallel?");

        CommandLine commandLine = null;
        
        try{
            commandLine = parser.parse(options, args);
        }catch(ParseException ex){
            log.error("Please provide command line options.");
            log.error(ex.getMessage());
            System.exit(0);
        }
        String json_file = commandLine.getOptionValue('j', "sample/150311152434_cyclone_pam-15_20150326_vol-4.json");
        
        FileInputStream fstream = new FileInputStream(json_file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        List<String> urls = new ArrayList<String>();
        List<Tweet> twts = new ArrayList<Tweet>();
        Tweet twt = null;
        String strLine;
        String url = "";
        int count = 0;
        int twt_count = 0;
        
        while ((strLine = br.readLine()) != null) {
        	// throw an exception for bad file here
        	twt_count +=1;
        	try{
        		twt = ImageCrawler.parseDataFeed(strLine); // Get the tweet
        	}
        	catch (Exception e){
        		log.error(json_file + " has a format problem...!");
        	}
            if (twt != null) {
                url = twt.imageURL;
                if (url.trim() != "") {
                    count += 1;
                    if (!urls.contains(url)) {
                        urls.add(url);
                        twts.add(twt);
                    }
                }
            }
        }
        // json_file, num of tweet, num of urls, num of uniq urls
        // running the dedupliation
        // running the filtering 
        
        System.out.println(json_file +"," + twt_count + "," + count + "," + twts.size());
        br.close();
        
    }
}
