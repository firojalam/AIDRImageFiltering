from PIL import Image
import imagehash
import sys,os

hash = imagehash.phash(Image.open(sys.argv[1]))
print(hash)

