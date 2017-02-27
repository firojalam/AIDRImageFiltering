package hbku.qcri.sc.aidr.imagecrawler;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dat
 * @author Firoj Alam
 */
public class ImageCrawler {

    protected static final Logger log = LoggerFactory.getLogger(ImageCrawler.class);
    
    /**
     * Parse the twitter json
     * @param message
     * @return 
     */
    public static Tweet parseDataFeed(String message) {
        String imageUrl = "";
        String expandedImageUrl = "";
        JSONObject msgJson = new JSONObject(message);
        String tweetID = msgJson.get("id").toString();
        if (tweetID == null) {
            return null;
        }
        JSONObject entities = msgJson.getJSONObject("entities");
        // System.out.println(entities.toString());
        if (entities != null && entities.has("media") && entities.getJSONArray("media") != null
                && entities.getJSONArray("media").length() > 0
                && entities.getJSONArray("media").getJSONObject(0).getString("type") != null
                && entities.getJSONArray("media").getJSONObject(0).getString("type").equals("photo")) {
            imageUrl = entities.getJSONArray("media").getJSONObject(0).getString("media_url");
            expandedImageUrl = entities.getJSONArray("media").getJSONObject(0).getString("expanded_url");
            //System.out.println(imageUrl);
            if (imageUrl == null) {
                return null;
            }
        }

        return new Tweet(tweetID, imageUrl, expandedImageUrl);
    }

    /**
     * Parse the url and make a unique list
     * @param json_file
     * @return
     * @throws IOException 
     */
    public static List<Tweet> getUniqUrls(String json_file) throws IOException {
        FileInputStream fstream = new FileInputStream(json_file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        List<String> urls = new ArrayList<String>();
        List<Tweet> twts = new ArrayList<Tweet>();
        Tweet twt = null;
        String strLine;
        String url = "";
        int count = 0;
        while ((strLine = br.readLine()) != null) {
        	// throw an exception for bad file here
        	try{
        		twt = parseDataFeed(strLine); // Get the tweet
        	}
        	catch (Exception e){
        		log.error(json_file + " has a format problem...!");
        	}
            if (twt != null) {
                url = twt.imageURL;
                if (url.trim() != "") {
                    count += 1;
                    if (!urls.contains(url)) {
                        urls.add(url);
                        twts.add(twt);
                    }
                }
            }
        }
        log.info("Number of image URLs: " + count);
        br.close();
        return twts;
    }

    /**
     * Check the number of core in the system
     *
     * @return number of threads selected to run (75%)
     */
    private int getNumberOfThreads() {
        int selectedThreads = 1;
        try {
            int threads = Runtime.getRuntime().availableProcessors();
            if (threads > 1) {
                selectedThreads = (threads * 75) / 100;
            } else {
                selectedThreads = threads;
            }
            System.out.println("Number of core " + threads + " selected to run " + selectedThreads);
        } catch (Exception ex) {
            System.err.println("Problem in reading number of processors.");
        }
        return selectedThreads;
    }

    /**
     * Downloads image in parallel
     * @param tweetList
     * @param outDir
     */
    public void downloadImageParallel(List<Tweet> tweetList, String outDir) {
        int NTHREADS = this.getNumberOfThreads();
        ExecutorService executor = Executors.newFixedThreadPool(NTHREADS);
        try {
            for (int i = 0; i < tweetList.size(); i++) {
                try {
                    String urlStr = tweetList.get(i).getURL();
                    String tweetID = tweetList.get(i).getID();
                    final URL url = new URL(urlStr);
                    final String formatName = FilenameUtils.getExtension(url.getPath());
                    String outFile = outDir + "/" + tweetID + "." + formatName;
                    final File outputfile = new File(outFile);
                    //log.info(urlStr);
                    Runnable worker = new Runnable() {
                        public void run() {
                            try {
                                downloadImage(url, outputfile, formatName);
                            } catch (Exception ex) {
                                log.error("Problem in the executing threads");
                            }
                        }//end run                        
                    };
                    executor.execute(worker);
                } catch (Exception ex) {
                    log.error("Error in parallel processing of individual json file.");
                }
                if (i % 1000 == 0) {
                    log.info(" -Done " + i + " urls...!");
                }

            }
            executor.shutdown();
            while (!executor.isTerminated()) {
                // waiting for the threads to be finished
            }
            log.info("Finished all threads.");
        } catch (Exception ex) {
            log.error("Error in parallel processing.");
        }

    }

    /**
     * 
     * @param url
     * @param outFile
     * @param formatName
     */
    public void downloadImage2(URL url, String outFile, String formatName) {
        try {
            BufferedImage image = ImageIO.read(url.openStream());
            File outputfile = new File(outFile);
            if (image != null) {
                ImageIO.write(image, formatName, outputfile);
            }

        } catch (IOException e) {
        }
    }

    /**
     * Download image and save image to a directory
     * @param url
     * @param outFile
     * @param formatName
     */
    public void downloadImage(URL url, File outFile, String formatName) {
        URLConnection con = null;
        InputStream in = null;
        try {
            //String webadd = "urls go here try the two you have had probelms with and success";
            //URL url = new URL(webadd);
            con = url.openConnection();
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            in = con.getInputStream();
            BufferedImage image = ImageIO.read(in);
            if (image != null) {
                ImageIO.write(image, formatName, outFile);
                //log.info("Image downloaded.");
            } else {
                //log.info("Image could not load." + url.getFile());
//                System.out.println(tweet.imageURL);
//                System.out.println(tweet.expanedImageURL);
            }
        } catch (IOException ex) {
            //log.error("Error in downloading image.");
            //log.error(ex.getMessage());
//            System.out.println(tweet.imageURL);
//            System.out.println(tweet.expanedImageURL);

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    // handle close failure
                    //log.error("Error in downloading image.");
                }
            }
            if (con != null) {
                con = null;
            }
        }
    }

    /**
     * Single thread image download
     * @param twts
     * @param folder 
     */
    public void downloadImagesSingleThread(List<Tweet> twts, String folder) {
        String outFile = "";
        String imgName = "";
        String formatName = "";
        int n = twts.size();
        for (int i = 0; i < n; i++) {
            try {
                URL url = new URL(twts.get(i).getURL());
                imgName = twts.get(i).getID();
                formatName = FilenameUtils.getExtension(url.getPath());
                outFile = folder + "/" + imgName + "." + formatName;
                File outputfile = new File(outFile);
                this.downloadImage(url, outputfile, formatName);
            } catch (MalformedURLException ex) {
                log.error("Error in downloading image..");
                log.error(ex.getMessage());
            }
            if (i % 1000 == 0) {
                log.info(" -Done " + i + " urls...!");
            }

        }
    }

    /**
     * 
     * @param args
     * @throws ParseException
     * @throws IOException 
     */
    public static void main(String args[]) throws ParseException, IOException {
        CommandLineParser parser = new BasicParser();
        Options options = new Options();
        options.addOption("f", "folder", true, "Save image to the folder under the name of collection.");
        options.addOption("j", "json_file", true, "Json file of the collection.");
        options.addOption("m", "mode", true, "Run in parallel?");

        CommandLine commandLine = null;
        
        try{
            commandLine = parser.parse(options, args);
        }catch(ParseException ex){
            log.error("Please provide command line options.");
            log.error(ex.getMessage());
            System.exit(0);
        }
        String json_file = commandLine.getOptionValue('j', "sample/150311152434_cyclone_pam-15_20150326_vol-4.json");
        String folder = commandLine.getOptionValue('f', "sample/images");
        String modeRun = commandLine.getOptionValue('m', "p");

        File f = new File(folder);
        try {
            if (!f.exists()) {
                f.mkdirs();
            }
        } catch (Exception ex) {
            log.error("Error in creating directory.");
            log.error(ex.getMessage());
        }
        log.info("-------------------------------------------------");
        List<Tweet> twitterList = getUniqUrls(json_file);
        int n = twitterList.size();
        log.info("Number of uniq URLs: " + n);
        log.info("Starting to crawl images...!");
        ImageCrawler imageCrawler = new ImageCrawler();
        if (modeRun.equalsIgnoreCase("p")) {
            imageCrawler.downloadImageParallel(twitterList, folder);
        } else {
            imageCrawler.downloadImagesSingleThread(twitterList, folder);
        }
        log.info("Fnished...!");
    }

}
