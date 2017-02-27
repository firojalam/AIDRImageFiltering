package hbku.qcri.sc.aidr.imagecrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.TreeTraverser;
import com.google.common.io.Files;

import hbku.qcri.sc.aidr.deduplication.ImagePHash;
import hbku.qcri.sc.aidr.filtering.ImageFilter;

public class DoStat02 {
	protected static final Logger log = LoggerFactory.getLogger(DoStat02.class);

	public static ImagePHash hasher = new ImagePHash(32, 8);
	public static List<String> im_hash = new LinkedList<String>();
	public static int threshold = 10;
	
	public static boolean checkHash(String im) throws FileNotFoundException, Exception {
		int n = im_hash.size();
		String hash = hasher.getHash(new FileInputStream(new File(im)));
		String tmp;
		if (n != 0) {
			for (int i = 0; i < n; i++) {
				tmp = im_hash.get(i);
				if (hasher.distance(hash, tmp) <= threshold) {
					return true;
				}
			}
		} else {
			im_hash.add(hash);
		}
		return false;

	}

	public static ArrayList<String> collectFilesInDir(File dir) {
		HashSet<String> imTypes = new HashSet<String>(Arrays.asList("jpg", "png", "jpeg"));
		ArrayList<String> list = new ArrayList<String>();
		String ext =""; 
		String path = "";
		
		TreeTraverser<File> traverser = Files.fileTreeTraverser();
		FluentIterable<File> filesInPostOrder = traverser.preOrderTraversal(dir);
		
		for (File f : filesInPostOrder){
			path = f.getPath();
			ext = FilenameUtils.getExtension(path);
			if (imTypes.contains(ext) == true){
				list.add(path);
			}
		}
		
		return list;
	}

	public static void main(String args[]) throws Exception {
		CommandLineParser parser = new BasicParser();
		Options options = new Options();
		options.addOption("f", "image folde", true, "Folder of images.");
		options.addOption("z", "z_model", true, "X model.");
		options.addOption("r", "r_model", true, "Relevancy model.");
		options.addOption("s", "saved_stat", true, "File to save results.");

		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);
		} catch (ParseException ex) {
			log.error("Please provide command line options.");
			log.error(ex.getMessage());
			System.exit(0);
		}

		String folder = commandLine.getOptionValue('f', "sample/images");
		String r_model_path = commandLine.getOptionValue('r', "./gold_models/alex-dl4j-ep-7.zip");
		String z_model_path = commandLine.getOptionValue('z', "./gold_models/alex-dl4j-ep-7.zip");
		String saved_file = commandLine.getOptionValue('s', "./stat.csv");
		
		log.info("------------------------------------------");
		log.info("Image folder: " + folder);
		log.info("R model : " + r_model_path);
		log.info("R model : " + z_model_path);
		log.info("Saved file : " + saved_file); 

		ArrayList<String> imList = collectFilesInDir(new File(folder));
		// loading r and z model
		ImageFilter r_model = new ImageFilter(r_model_path);
		ImageFilter z_model = new ImageFilter(z_model_path);

		String stat2CSV = "";
		boolean h_check = false;
		boolean r_check = false;
		boolean z_check = false;

		int count = 0;
		for (String im : imList) {
			// do the fillterting and depulication here
			try {
				//log.info(im);
				h_check = checkHash(im);
				r_check = r_model.doClassify("demo", im);
				z_check = z_model.doClassify("demo", im);
				stat2CSV += im + "," + h_check + "," + r_check + "," + z_check + "\n";
			} catch (Exception e) {
				log.error("not an image file");
			}
			
			count +=1;
			if (count % 1000 ==0){
				log.info(" - processed: " + count + " images");
			}
		}

		// write string to file
		FileUtils.writeStringToFile(new File(saved_file), stat2CSV);

	}
}
