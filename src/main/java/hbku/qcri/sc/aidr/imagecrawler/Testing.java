package hbku.qcri.sc.aidr.imagecrawler;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Testing {
	protected static final Logger log = LoggerFactory.getLogger(Testing.class);
	
	public static void main(String args[]) throws IOException{
		BufferedImage img = ImageIO.read(new File("img_1.jpg"));
		log.info(" H: "+ img.getHeight());
		log.info(" W:" +img.getWidth());
		log.info(" Color:" +img.getColorModel());
		
		byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < pixels.length; i++){
			//System.out.println(pixels[i]);
			
		}
		
		}

}
