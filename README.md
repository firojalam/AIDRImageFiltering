# AIDRImageFiltering



# Image Crawler Module
Crawl images from json file extracted from twitter.

## How to run:
```bash
cd AIDRImageFiltering/
java -classpath /target/image-filtering-0.0.1-SNAPSHOT.jar hbku.qcri.sc.aidr.imagecrawler.ImageCrawler -j sample/150311152434_cyclone_pam-15_20150326_vol-4.json -f sample/images -m p
```
Options to run:
* -j Json file of the collection
* -f Save image to the folder under the name of collection
* -m Run in parallel, value is p for parallel running option, it will check the number of processor the machine has and will utilizes 75% of it.


## How to train a binary classification model (AlexNet)
- Copy trainning images to NEG and POS folder
```bash
cd AIDRImageFiltering/
java -classpath /target/image-filtering-0.0.1-SNAPSHOT.jar hbku.qcri.sc.aidr.filtering.ImageFilteringModel -d toTrain/ -n 100 -b 60 -i 2 -s savedModel
```

Options to run:
* -d dataset folder
* -n total number of training images 
* -b batch_size
* -i interation
* -l learing rate
* -s save trained model to a folder 
