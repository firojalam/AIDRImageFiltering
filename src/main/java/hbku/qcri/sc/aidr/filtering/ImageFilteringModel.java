package hbku.qcri.sc.aidr.filtering;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.datavec.api.io.filters.BalancedPathFilter;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.datavec.image.transform.ImageTransform;
import org.datavec.image.transform.ResizeImageTransform;
import org.datavec.image.transform.ScaleImageTransform;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.distribution.Distribution;
import org.deeplearning4j.nn.conf.distribution.GaussianDistribution;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Random;

/**
 * Filtering Model using AlexNet
 *
 */

public class ImageFilteringModel {
    protected static final Logger log = LoggerFactory.getLogger(ImageFilteringModel.class);
    protected static int height = 227;
    protected static int width = 227;
    protected static int channels = 3;
    protected static int numExamples = 4000;
    protected static int numLabels = 2;
    protected static int batchSize = 64;

    protected static long seed = 113;
    protected static Random rng = new Random(seed);
    protected static int listenerFreq = 1;
    protected static int iterations = 2;
    protected static int epochs = 30;
    protected static double splitTrainTest = 0.75;
    protected static int nCores = 4;
    protected static boolean save = true;
    
    protected static double l_rate = 1e-2;
    protected static String modelType = "AlexNet"; // LeNet, AlexNet or Custom but you need to fill it out

    
	public void run(String[] args) throws Exception {
		//Nd4j.set
		
		//Parsing parameters
		CommandLineParser parser = new BasicParser();
		Options options = new Options();
		options.addOption("d", "im_data", true, "dataset folder");
		options.addOption("n", "num_examples", true, "Number of examples");
		options.addOption("b", "batch_size", true, "batch_size");
		options.addOption("i", "iterations", true, "interation");
		options.addOption("l", "l_rate", true, "learing rate");
		options.addOption("s", "saved_model", true, "saved_models");

		CommandLine commandLine = parser.parse(options, args);
		
		String dataDir = commandLine.getOptionValue('d', "z_data");
		String savedDir = commandLine.getOptionValue('s', "saved_models");
		
		numExamples = Integer.parseInt(commandLine.getOptionValue('n', "1000"));
		batchSize  = Integer.parseInt(commandLine.getOptionValue('b', "20"));
		iterations = Integer.parseInt(commandLine.getOptionValue('i', "2"));
		l_rate = Double.parseDouble(commandLine.getOptionValue('l', "1e-3"));
		
		
		log.info("----------------------------------------");
		log.info("Pamateters:");
		log.info("Dataset folder: " + dataDir);
		log.info("Saved model folder: " + savedDir);
		log.info("Num of Examples: " + numExamples);
		log.info("Batch size: " + batchSize);
		log.info("Iterations: " + iterations);
		log.info("Learning rate: " + l_rate);
		log.info("Image size: " + height);
		log.info("Normalization: 0 - 1") ;
		log.info("----------------------------------------");
        log.info("Load data....");
        
        /**
         * Data Setup -> organize and limit data file paths:
         *  - mainPath = path to image files
         *  - fileSplit = define basic dataset split with limits on format
         *  - pathFilter = define additional file load filter to limit size and balance batch content
         **/
        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
        File mainPath = new File(System.getProperty("user.dir"), dataDir);
        FileSplit fileSplit = new FileSplit(mainPath, NativeImageLoader.ALLOWED_FORMATS, rng);
        
        //BalancedPathFilter pathFilter = new BalancedPathFilter(rng, labelMaker, numExamples, numLabels, numExamples/2);
        BalancedPathFilter pathFilter = new BalancedPathFilter(rng, labelMaker, numExamples/2);
        
        //- inputSplit = define train and test split
        InputSplit[] inputSplit = fileSplit.sample(pathFilter, 75, 25);
        
        InputSplit trainData = inputSplit[0];
        InputSplit testData = inputSplit[1];
        
        log.info("Num of train: " + trainData.length());
        log.info("Num of test: " + testData.length());
        
        //using the image scale normalization 0 -255        
        DataNormalization scaler = new ImagePreProcessingScaler(0,255);
        
        
        log.info("Build model...."); 
        //Using Alexnet
        MultiLayerNetwork network = alexnetModel();
        network.init();
        network.setListeners(new ScoreIterationListener(listenerFreq));
        
        //For monitoring
        //UIServer uiServer = UIServer.getInstance();
        ///StatsStorage statsStorage = new InMemoryStatsStorage();             //Alternative: new FileStatsStorage(File) - see UIStorageExample
        //network.setListeners(new StatsListener(statsStorage, listenerFreq));
        //uiServer.attach(statsStorage);
        
        /**
         * Data Setup -> define how to load data into net:
         *  - recordReader = the reader that loads and converts image data pass in inputSplit to initialize
         *  - dataIter = a generator that only loads one batch at a time into memory to save memory
         *  - trainIter = uses MultipleEpochsIterator to ensure model runs through the data for all epochs
         **/
        ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
        DataSetIterator trainIter;
        
        log.info("Train model....");
        // Train with or without transformations
        recordReader.initialize(trainData, null);
        trainIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels);
        scaler.fit(trainIter);
        trainIter.setPreProcessor(scaler);
        
        //Evaluation data
        ImageRecordReader testRecordReader = new ImageRecordReader(height, width, channels, labelMaker);
        DataSetIterator testIter;
        testRecordReader.initialize(testData,null);
        testIter = new RecordReaderDataSetIterator(testRecordReader, batchSize, 1, numLabels);
        scaler.fit(testIter);
        testIter.setPreProcessor(scaler);
        
        String loc2save;
        for( int i=1; i<epochs + 1; i++ ) {
        	network.fit(trainIter);
            log.info("*** Completed epoch {} ***", i);
            log.info("Evaluate model on dev set....");
            Evaluation eval = new Evaluation(2);
            while(testIter.hasNext()){
                DataSet ds = testIter.next();
                INDArray output = network.output(ds.getFeatureMatrix(), false);
                eval.eval(ds.getLabels(), output);
            }
            log.info("--- Dev Acc: " + eval.accuracy());
            log.info("--- Dev Pre: " + eval.precision());
            log.info("--- Dev Rec: " + eval.recall());
            log.info("--- Dev F1: " + eval.f1());
            log.info("-------------------------------");
            loc2save = savedDir + "/alex-dl4j-ep-" + i + ".zip";
            
            //Updater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this if you want to train your network more in the future
            ModelSerializer.writeModel(network, loc2save, true);
            //log.info(eval.stats());
            testIter.reset();
        }
             
        log.info("****************Training Done..!********************");
                
    }

    private ConvolutionLayer convInit(String name, int in, int out, int[] kernel, int[] stride, int[] pad, double bias) {
        return new ConvolutionLayer.Builder(kernel, stride, pad).name(name).nIn(in).nOut(out).biasInit(bias).build();
    }

    private ConvolutionLayer conv3x3(String name, int out, double bias) {
        return new ConvolutionLayer.Builder(new int[]{3,3}, new int[] {1,1}, new int[] {1,1}).name(name).nOut(out).biasInit(bias).build();
    }

    private ConvolutionLayer conv5x5(String name, int out, int[] stride, int[] pad, double bias) {
        return new ConvolutionLayer.Builder(new int[]{5,5}, stride, pad).name(name).nOut(out).biasInit(bias).build();
    }

    private SubsamplingLayer maxPool(String name,  int[] kernel) {
        return new SubsamplingLayer.Builder(kernel, new int[]{2,2}).name(name).build();
    }

    private DenseLayer fullyConnected(String name, int out, double bias, double dropOut, Distribution dist) {
        return new DenseLayer.Builder().name(name).nOut(out).biasInit(bias).dropOut(dropOut).dist(dist).build();
    }

    
    public MultiLayerNetwork alexnetModel() {
    	/**
         * AlexNet model interpretation based on the original paper ImageNet Classification with Deep Convolutional Neural Networks
         * and the imagenetExample code referenced.
         * http://papers.nips.cc/paper/4824-imagenet-classification-with-deep-convolutional-neural-networks.pdf
         **/

        double nonZeroBias = 1;
        double dropOut = 0.5;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .weightInit(WeightInit.DISTRIBUTION)
            .dist(new NormalDistribution(0.0, 0.01))
            .activation(Activation.RELU)
            .updater(Updater.SGD)
            .iterations(iterations)
            .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer) // normalize to prevent vanishing or exploding gradients
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .learningRate(l_rate)
            .biasLearningRate(1e-2*2)
            .learningRateDecayPolicy(LearningRatePolicy.Step)
            .lrPolicyDecayRate(0.1)
            .lrPolicySteps(100000)
            .regularization(true)
            .l2(5 * 1e-4)
            .momentum(0.9)
            .miniBatch(false)
            .list()
            .layer(0, convInit("cnn1", channels, 96, new int[]{11, 11}, new int[]{4, 4}, new int[]{3, 3}, 0))
            .layer(1, new LocalResponseNormalization.Builder().name("lrn1").build())
            .layer(2, maxPool("maxpool1", new int[]{3,3}))
            .layer(3, conv5x5("cnn2", 256, new int[] {1,1}, new int[] {2,2}, nonZeroBias))
            .layer(4, new LocalResponseNormalization.Builder().name("lrn2").build())
            .layer(5, maxPool("maxpool2", new int[]{3,3}))
            .layer(6,conv3x3("cnn3", 384, 0))
            .layer(7,conv3x3("cnn4", 384, nonZeroBias))
            .layer(8,conv3x3("cnn5", 256, nonZeroBias))
            .layer(9, maxPool("maxpool3", new int[]{3,3}))
            .layer(10, fullyConnected("ffn1", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
            .layer(11, fullyConnected("ffn2", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
            .layer(12, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .name("output")
                .nOut(numLabels)
                .activation(Activation.SOFTMAX)
                .build())
            .backprop(true)
            .pretrain(false)
            .setInputType(InputType.convolutional(height, width, channels))
            .build();

        return new MultiLayerNetwork(conf);

    }
    
    public static void main(String[] args)  {
    	 
    	System.out.println(System.getProperty("user.dir"));
        try {
			new ImageFilteringModel().run(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
    }

}
