package hbku.qcri.sc.aidr.filtering;

import java.io.File;
import java.io.IOException;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
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
	public boolean doFilteringImage(String collection_id, String im_file) {
		File file = new File(im_file);
		NativeImageLoader loader = new NativeImageLoader(height, width, channels);
		try {
			INDArray image = loader.asMatrix(file);
			//DataNormalization scaler = new NormalizerStandardize();
			//ImageTransform myTransform =  new MyImageTransform(null, 121,121,122);
			DataNormalization scaler = new ImagePreProcessingScaler(0,1);
	        scaler.transform(image);
	        
	        // Pass through to neural Net
	        //long startTime = System.currentTimeMillis();
	        INDArray output = this.savedNetwork.output(image);
	        //long endTime = System.currentTimeMillis();
	        //System.out.println("A pass through network took: " + (endTime - startTime)/1000 + " seconds"); //measure performance 
	        float p_l1 = output.getFloat(0);
	        float p_l2 = output.getFloat(1);
	        System.out.println(p_l1);
	        System.out.println(p_l2);
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
    	String modelPath = "./gold_models/gold-model-alex-dl4j-ep-7.zip";
    	ImageFilter filter = new ImageFilter(modelPath);
    	System.out.println(filter.doFilteringImage("nepal_eq", "./im_data/POS/ecuador_eq_mild_im_89.jpg"));
    	System.out.println(filter.doFilteringImage("nepal_eq", "./im_data/POS/ecuador_eq_mild_im_89.jpg"));
    	System.out.println(filter.doFilteringImage("nepal_eq", "./im_data/POS/ecuador_eq_mild_im_89.jpg"));
    }
}
