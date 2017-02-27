package hbku.qcri.sc.aidr.filtering;

import java.io.File;
import java.io.IOException;

import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.transform.ImageTransform;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageFilter {
	protected static final Logger log = LoggerFactory.getLogger(ImageFilter.class);
	
	MultiLayerNetwork savedNetwork = null;
	String modelPath ;
	
	protected static int height = 227;
    protected static int width = 227;
    protected static int channels = 3;
    
    //Loading the pre-trained model
	public ImageFilter(String modelPath) {
		super();
		this.modelPath = modelPath;
		//loading the model 
		try {
			this.savedNetwork = ModelSerializer.restoreMultiLayerNetwork(modelPath);
		} catch (IOException e) {
			log.error("Cannot loading the pre-trained filter model...!");
			log.error(e.getMessage());
		}
	}
	
	//doing binary classification for one image
	public boolean doClassify(String collection_id, String im_file) {
		File file = new File(im_file);
		NativeImageLoader loader = new NativeImageLoader(height, width, channels);
		try {
			INDArray image = loader.asMatrix(file);
			
			//using the image scale normalization 0 -255
			DataNormalization scaler = new ImagePreProcessingScaler(0,255);
	        scaler.transform(image);
	        
	        // Pass through to neural Net
	        //long startTime = System.currentTimeMillis();
	        INDArray output = this.savedNetwork.output(image);
	        //long endTime = System.currentTimeMillis();
	        //System.out.println("A pass through network took: " + (endTime - startTime)/1000 + " seconds"); //measure performance 
	        float p_l1 = output.getFloat(0);
	        float p_l2 = output.getFloat(1);
	        //System.out.println(p_l1);
	        //System.out.println(p_l2);
	        if(p_l1 > p_l2){ // the classified label is NEG
	        	return false;
	        }
			
		} catch (IOException e) {
			log.error("Cannot loading the image...!");
			log.error(e.getMessage());
		}
		
		return true;
	}
	
    public static void main(String args[]){
    	String modelPath = "./gold_models/alex-dl4j-ep-7.zip";
    	ImageFilter filter = new ImageFilter(modelPath);
    	System.out.println(filter.doClassify("nepal_eq", "./test_img/581045846810169344.jpg"));
    	System.out.println(filter.doClassify("nepal_eq", "./test_img/581046110665318400.png"));
    	System.out.println(filter.doClassify("nepal_eq", "./test_img/581046449544130560.jpg"));
    }
}
