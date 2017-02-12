package hbku.qcri.sc.aidr.deduplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

public class ImageDeduplicator {
	
	protected static final Logger log = LoggerFactory.getLogger(ImageDeduplicator.class);
	
	public static int hash_length = 64;
	public static int threshold = 10;
	public String jedis_server = "";
	public Jedis myJedis;
	
	public static ImagePHash im_pHash; 
	
	public ImageDeduplicator(String jedis_serverx) {
		im_pHash = new ImagePHash(32,8);
		jedis_server = jedis_serverx;
		myJedis = new Jedis(jedis_server);
	}
	
	//where I can get the hexa hash
	public String getPHash(InputStream input_im){
		try {
			return im_pHash.getHash(input_im);
		} catch (Exception e) {
			log.error("Cannot load the BuffedImage...!");
			log.error(e.getMessage());
		}
		return null;
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
				//System.out.println(curr_hash);
				dis = im_pHash.distance(curr_hash, new_hash);
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
	public boolean filerImage(String collection_id, InputStream input_im) {
		String new_hash = getPHash(input_im);
		//need to convert to hex
		int h_idx = searchAndUpdate(collection_id, new_hash);
		if(h_idx > -1){
			return true; //true mean: we will remove it, is has duplication 
		}
		return false;	// false mean: no duplicate, keep the image
	}
	
	public static void main(String[] args) throws ExecuteException, IOException {
			
		//the key is collection ID
		//the value is the list of hash 
		String jedis_server = "localhost";
		
		ImageDeduplicator imDedup = new ImageDeduplicator(jedis_server);

		InputStream file_1 = new FileInputStream(new File("./im_data/POS/ecuador_eq_mild_im_89.jpg"));
		InputStream file_2 = new FileInputStream(new File("./im_data/NEG/ecuador_eq_none_im_218.jpg"));
		InputStream file_3 = new FileInputStream(new File("./im_data/NEG/ecuador_eq_none_im_218.jpg"));

		
		System.out.println(imDedup.filerImage("collection7", file_1)) ;
		System.out.println(imDedup.filerImage("collection7", file_1)) ;
		System.out.println(imDedup.filerImage("collection7", file_3)) ;
//		System.out.println(imDedup.filerImage("collection2", file_4)) ;
//		System.out.println(imDedup.filerImage("collection1", file_5)) ;
//		System.out.println(imDedup.filerImage("collection3", file_1)) ;
//		System.out.println(imDedup.filerImage("collection3", file_2)) ;
		
	//	Jedis myJedis= new Jedis(jedis_server);
	//	System.out.println(myJedis.del(myJedis.keys("collectio*")));
		
	}

}
