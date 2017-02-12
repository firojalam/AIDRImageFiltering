package hbku.qcri.sc.aidr.imagecrawler;

public class Tweet {

    public String tweetID = "";
    public String imageURL = "";
    public String expanedImageURL = "";

    public Tweet(String tweetID, String imageURL, String expanedImageURL) {
        //super();
        this.tweetID = tweetID;
        this.imageURL = imageURL;
        this.expanedImageURL = expanedImageURL;
    }

    public String getID() {
        return tweetID;
    }

    public void set_id(String tweetID) {
        this.tweetID = tweetID;
    }

    public String getURL() {
        return imageURL;
    }

    public void set_url(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getTweetID() {
        return tweetID;
    }

    public void setTweetID(String tweetID) {
        this.tweetID = tweetID;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

}
