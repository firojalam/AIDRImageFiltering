package hbku.qcri.sc.aidr.filtering;

import java.util.Random;


import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;
import org.datavec.image.data.ImageWritable;
import org.datavec.image.transform.BaseImageTransform;
import org.bytedeco.javacv.OpenCVFrameConverter;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import org.bytedeco.javacpp.opencv_core.MatOp;




public class MyImageTransform extends BaseImageTransform<Mat> {
	//Cv32suf()
	int R_mean, B_mean, G_mean;
	
	protected MyImageTransform(int R_mean, int G_mean, int B_mean) {
		//super(random);
		this(null, R_mean, G_mean, B_mean);
		this.B_mean = B_mean;
		this.R_mean = R_mean;
		this.G_mean = G_mean;
	}
	
	
    public MyImageTransform(Random random , int r_mean2, int g_mean2, int b_mean2) {
		// TODO Auto-generated constructor stub
    	super(random);
        this.R_mean = r_mean2; 
        this.G_mean = g_mean2; 
        this.B_mean = b_mean2; 
        
        converter = new OpenCVFrameConverter.ToMat();

	}



	public ImageWritable transform(ImageWritable image, Random random) {
		if (image == null) {
            return null;
        }
		
		Mat mat = converter.convert(image.getFrame());
		//resize image to 255x255
		Mat result = new Mat();
        resize(mat, result, new Size(256, 256));
        //crop center image to 227x227 
        int crop = 15;
        int y = Math.min(crop, result.rows() - 1);
        int x = Math.min(crop, result.cols() - 1);
        int h = Math.max(1, result.rows() - crop - y);
        int w = Math.max(1, result.cols() - crop - x);
        result = result.apply(new Rect(x, y, w, h));
	
		//System.out.println(result.rows());
		//System.out.println(result.channels());
		//System.out.println(result.cols());

        //Scalar scar = new Scalar(123,121,3232,0);
        
        int sz[] = {100, 100, 100};
        Pointer p = null;
        MatOp op = new MatOp(p);
        
//    	op.subtract(mat.,mat,mat);
    	
		return new ImageWritable(converter.convert(result));

	}


	


	


	 
}