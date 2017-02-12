package hbku.qcri.sc.aidr.deduplication;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

public class ImageDeduplication_py {
	
	protected static final Logger log = LoggerFactory.getLogger(ImageDeduplication_py.class);
	
	public static String py_path = "";
	public static String py_code = "";
	
	public static int hash_length = 64;
	public static int threshold = 10;
	
	public String jedis_server = "";
	public Jedis myJedis;
	
	public ImageDeduplication_py(String py_pathx, String py_codex, String jedis_serverx) {
		py_code = py_codex;
		py_path = py_pathx;
		jedis_server = jedis_serverx;
		myJedis = new Jedis(jedis_server);
	}
	
	public static String getPHash(String im_file){
		String line = py_path + " " + py_code+ " "  + im_file;
		//System.out.println(line);
		CommandLine commandLine = CommandLine.parse(line);
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(new File("./pHash/"));
		//System.out.println(executor.getWorkingDirectory());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		
		executor.setStreamHandler(streamHandler);
		try {
			executor.execute(commandLine);
		} catch (ExecuteException e) {
			log.error("Cannot run the python code..!");
			log.error(e.getMessage());
		} catch (IOException e) {
			log.error("Errors of python path, python code implementation..!");
			log.error(e.getMessage());
		}	
		return outputStream.toString().trim();
	}
	
	//convert hexa to binary with full 0000
	public static String hexToBin(String s) {
		String hash = new BigInteger(s, 16).toString(2);
		int h_len = hash.length();
		if (h_len < hash_length){
			String prex = "";
			for (long i = 0; i < hash_length - h_len; i++) {
			    prex = prex.concat("0");
			}	
			hash = prex + hash; 
		}	
		return hash;
	}
	
	public static int getHammingDistance(String hexa_s1, String hexa_s2) {
		// check preconditions
		String s1 = hexToBin(hexa_s1);
		String s2 = hexToBin(hexa_s2);
		
		if (s1 == null || s2 == null || s1.length() != s2.length()) {
			throw new IllegalArgumentException();
		}
		// compute hamming distance
		int distance = 0;
		for (int i = 0; i < s1.length(); i++) {
			if (s1.charAt(i) != s2.charAt(i)) {
				distance++;
			}
		}
		return distance;
	}
	
	
	//do search the hash, return whether 
	public int searchAndUpdate(String collection_id, String new_hash ){
		List<String> hash_list = myJedis.lrange(collection_id, 0, -1);
		int n = hash_list.size();
		if(n>0){
			String curr_hash;
			int dis;
			for (int i=0; i<n; i++){  //need to optimize a bit // now the max computation is O(n)
				curr_hash = hash_list.get(i);
				dis = getHammingDistance(curr_hash, new_hash);
				if(dis <= threshold){
					return i;
				}	
			}
		}
		//update the hash here if there is no duplication
		myJedis.lpush(collection_id, new_hash);
		return -1;
	}
	
	// filter upcoming image whether it has a duplicate or not.
	public boolean filerImage(String collection_id, String im_file) {
		String new_hash = getPHash(im_file);
		int h_idx = searchAndUpdate(collection_id, new_hash);
		if(h_idx > -1){
			return false;
		}
		return true;	
	}
	
	public static void main(String[] args) throws ExecuteException, IOException {
			
		//the key is collection ID
		//the value is the list of hash 
		/*
		String py_path = "/Library/Frameworks/Python.framework/Versions/2.7/bin/python";
		String py_code = "compute_pHash.py"; 
		String jedis_server = "localhost";
		
		ImageDeduplication imDedup = new ImageDeduplication(py_path,py_code,jedis_server);
		
		System.out.println(imDedup.filerImage("collection1", "../im_data/POS/ecuador_eq_mild_im_89.jpg"));		
		System.out.println(imDedup.filerImage("collection1","../im_data/POS/ecuador_eq_mild_im_89.jpg"));		
		System.out.println(imDedup.filerImage("collection2","../im_data/NEG/ecuador_eq_none_im_218.jpg"));
		System.out.println(imDedup.filerImage("collection2","../im_data/POS/ecuador_eq_mild_im_89.jpg"));
		System.out.println(imDedup.filerImage("collection2","../im_data/POS/ecuador_eq_mild_im_89.jpg"));
		System.out.println(imDedup.filerImage("collection3","../im_data/POS/ecuador_eq_mild_im_89.jpg"));
		
		//System.out.println(myJedis.del));
		*/
		
		
	}

}
