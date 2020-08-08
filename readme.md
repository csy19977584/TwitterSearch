#### How to Deploy the project?

We have two scripts, "runlucene.sh" is for creating index and "runweb.sh" is for our website.

To run "runlucene.sh", please type the following command lines in terminal:

**chmod +x ./runlucene.sh**

**./runlucene.sh**

After run the script, you'll find a file named "lucene-spatial-all" in the geoLucene_jar folder like this:

![image-20200611152151692](/Users/apple/Library/Application Support/typora-user-images/image-20200611152151692.png)

It shows that you have successfully created index on the testours.json. "testours.json" is a small part of our real json file and contains 10 rows.



To run "runweb.sh", please type in the following command lines in terminal:

**chmod +x ./runweb.sh**

**./runweb.sh**

And then visit "localhost:8080"



#### Link of our project demo video

https://drive.google.com/file/d/1YolzQMSmgku1hAOnD7CMdettfhbbUXKM/view?usp=sharing



#### Other Instructions

Source code for lucene is in "geoLucene" folder and source code for web is in "websearch_jar/src"

A picture to make it clear:

![IMG_0105](/Users/apple/Downloads/IMG_0105.jpg)# TwitterSearch
