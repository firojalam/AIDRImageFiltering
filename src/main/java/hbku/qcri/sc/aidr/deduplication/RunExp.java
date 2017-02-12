package hbku.qcri.sc.aidr.deduplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RunExp {
	
	public static ImagePHash h = new ImagePHash(32,8);
	public static Map<String, String> im_hash = new HashMap<String, String>();
	public static int threshold = 10;
	
	public static int upldate_hash(String hash, String im_file){
		Set<String> keys = im_hash.keySet();
		
		String value = "";
		
		if (keys.isEmpty()){
			im_hash.put(hash, im_file); 
		}
		else{
			for(String key : keys){
				//System.out.println(key);
				int x = h.distance(key, hash);
				if(x <=10){
					value = im_hash.get(key) + " " + im_file;
					im_hash.put(hash, value);
					return 1;	
				}
			}
			im_hash.put(hash,im_file);
		}
		return -1;
	}	
	
	
	public static void main(String args[]) throws Exception{
		
		FileInputStream fstream = new FileInputStream("/Users/ndat/Desktop/crawl-data/list.nepal_eq.true");
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;

		//Read File Line By Line
		String im_file = "";
		String hash = "";
		
		while ((strLine = br.readLine()) != null)   {
		  // Print the content on the console
			im_file = "/Users/ndat/Desktop/crawl-data/" + strLine;
			hash = h.getHash(new FileInputStream(new File(im_file)));
			//System.out.println(strLine);
			upldate_hash(hash, strLine);
		}
		//Close the input stream
		br.close();
		Set<String> final_keys = im_hash.keySet();
		
		for(String key : final_keys){
			System.out.println(im_hash.get(key));
		}
	
		
	}

}
