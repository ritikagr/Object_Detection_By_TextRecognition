# Object_Detection_By_TextRecognition

Text Recognition using Google Vision Service :
 
Text Recognition is the process of detecting text in images and video streams and recognizing text contained therein.
 
Google provides two services for visual recognition:

Google Mobile Vision API
Google Cloud Vision API
 
Google Mobile Vision API:
 
After Implementing Text Recognition of Mobile Vision API in android app , it was found that it’s text recognition capability is very less 
compared to Cloud Vision API because this text recognition process was working offline and using mobile device processing power.
 
https://developers.google.com/vision/text-overview
 
So Google Mobile vision API result was not good, we can use Cloud Vision API for better result.
 
Google Cloud Vision API:
	
https://cloud.google.com/vision/
 
Requirements: Internet Permission and Storage Permission
 
After sending bitmap of image in cloud vision service , it will result recognized text and their block position in image.
 
For finding approximate presence of specific object text in our result, We will compare object text with each word in the result of text 
recognition .If matching of a word is above specified Threshold value then we will count it in our result.

If Object text is combination of multiple words then we will find matching count for each word and also for whole object text then we 
will take average of each word’s matching count and object text matching count. This will be our final result.
 
It also results position of text within image, so we can use it for any positioning analysis.
 
Conclusion: We can use this approach for analysis of specific product(Object) presence in a shelf only if there is some text corresponding 
to each object in image 	and image quality is good(It means text is visible properly).
