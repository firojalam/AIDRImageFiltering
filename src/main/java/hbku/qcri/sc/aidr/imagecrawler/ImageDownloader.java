package hbku.qcri.sc.aidr.imagecrawler;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.cli.ParseException;


public class ImageDownloader {
	protected static final Logger log = LoggerFactory.getLogger(ImageDownloader.class);
	
	public static void main(String args[]) throws IOException, ParseException {
		CommandLineParser parser = new BasicParser() ;
		 
		Options options = new Options();
		options.addOption("f", "folder", true, "Save image to foler");
		options.addOption("u", "url_list", true, "List of uls");
		
		CommandLine commandLine = parser.parse(options, args);
		
		String url_list = commandLine.getOptionValue('u', "list.urls");
		String folder = commandLine.getOptionValue('f', "./crawl-data");
		
		FileInputStream fstream = new FileInputStream(url_list);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		
		String strLine;
		int id = 1;
		String outFile ="";
		while ((strLine = br.readLine()) != null) {
			URL url = new URL(strLine);
	        String imgName = FilenameUtils.getName(url.getPath());
	        String formatName = FilenameUtils.getExtension(url.getPath());
	        //log.info("Image from url: " + imgName + " Image format name: " + formatName);
	        outFile =  folder + "/im_" + id + "_" + imgName;
	        File outputfile = new File(outFile);
	        try{
	        	BufferedImage image =  ImageIO.read(url);
	        	if (image != null) {
		            //log.info("Output file name: " + outFile);
		            ImageIO.write(image, formatName, outputfile);
		        }
	        } catch (IOException e){	
	        }
	        id +=1;
	        if (id%500 ==0){
	        	log.info(" - Done " + id + " urls...!");
	        }
		}
		br.close();			
	}
}
