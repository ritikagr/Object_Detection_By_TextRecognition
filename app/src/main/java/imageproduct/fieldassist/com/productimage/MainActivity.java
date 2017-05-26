package imageproduct.fieldassist.com.productimage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MAIN_ACTIVITY";
    private String API_TAG = "TEXT_API";
    private ImageView mIvPickedImage;
    private TextView mTvDetectedText;
    private TextView mTvObjectTitle;
    private EditText mEtObjectNames;
    private Button mBAnalyze;
    private TableLayout mTlObjects;
    private Uri uri = null;
    private int CHOOSE_IMAGE_REQUEST = 1;
    private int STORAGE_PERMISSION_CODE = 100;

    private static final String CLOUD_VISION_API_KEY = "AIzaSyAU34RtXNDz7WaX6fFhSmzxsmCLsIB_Z8g";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private Collection<Collection<String>> mSavedResult = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean permissionCheck = PermissionUtils.requestPermission(this, STORAGE_PERMISSION_CODE,
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE});

        mIvPickedImage = (ImageView) findViewById(R.id.picked_image);
        mTvDetectedText = (TextView) findViewById(R.id.processing);
        mTvObjectTitle = (TextView) findViewById(R.id.object_title);
        mEtObjectNames = (EditText) findViewById(R.id.object_name);
        mBAnalyze = (Button) findViewById(R.id.analyze_image);
        mTlObjects = (TableLayout) findViewById(R.id.objectCountTable);

    }

    public void ChooseImage(View view)
    {
        boolean permissionCheck = PermissionUtils.requestPermission(this, STORAGE_PERMISSION_CODE,
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE});

        if(permissionCheck) {
            Intent chooseImageIntent = new Intent();
            chooseImageIntent.setType("image/*");
            chooseImageIntent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(chooseImageIntent, "Select Image"), CHOOSE_IMAGE_REQUEST);
        }
        else
        {
            Toast.makeText(this, "Please Enable Read Storage Permission", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CHOOSE_IMAGE_REQUEST && resultCode == RESULT_OK)
        {
            uri = data.getData();
            Log.i(TAG, uri.toString());
            try {
                //Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                mSavedResult = null;

                Bitmap bitmap = decodeBitmapUri(this, uri);

                mIvPickedImage.setVisibility(View.VISIBLE);
                mIvPickedImage.setImageBitmap(bitmap);

                mTvDetectedText.setVisibility(View.GONE);

                mTvObjectTitle.setVisibility(View.VISIBLE);
                mEtObjectNames.setVisibility(View.VISIBLE);
                mEtObjectNames.setText("");

                mTlObjects.setVisibility(View.GONE);

                mBAnalyze.setVisibility(View.VISIBLE);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String imageFilePath = getPathFromUri(uri);
            Log.i(TAG, "Image Path:" + imageFilePath);
        }
    }

    private String getPathFromUri(Uri uri) {
        if (Build.VERSION.SDK_INT < 11)
            return RealPathUtil.getRealPathFromURI_BelowAPI11(this, uri);
        else if (Build.VERSION.SDK_INT < 19)
            return RealPathUtil.getRealPathFromURI_API11to18(this, uri);
        else
            return RealPathUtil.getRealPathFromURI_API19(this, uri);
    }

    public void AnalyzeImage(View view)
    {
        mTlObjects.setVisibility(View.GONE);
        if(uri!=null) {
            try {
                Bitmap bitmap = decodeBitmapUri(getApplicationContext(), uri);

                List<Bitmap> bitmapList = new ArrayList<Bitmap>();
                bitmapList.add(bitmap);

                Bitmap temp = rotateImage(bitmap, 180);
                bitmapList.add(temp);

                callCloudVision(bitmapList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            Log.i(TAG, "Please choose a valid image");
        }
    }

    private void callCloudVision(final List<Bitmap> bitmaps) throws IOException {

        mTvDetectedText.setVisibility(View.VISIBLE);
        mTvDetectedText.setText(R.string.loading);

        new AsyncTask<Object, Void, Collection<Collection<String>>>() {
            @Override
            protected Collection<Collection<String>> doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                    String signature = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, signature);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();


                    BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();

                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>()
                    {
                        {
                            //adding annotateImageRequest for four orientation of a image
                            for(Bitmap bitmap : bitmaps)
                            {
                                AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                                // Add the image
                                Image base64EncodedImage = new Image();
                                // Convert the bitmap to a JPEG
                                // Just in case it's a format that Android understands but Cloud Vision
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                                // Base64 encode the JPEG
                                base64EncodedImage.encodeContent(imageBytes);
                                annotateImageRequest.setImage(base64EncodedImage);

                                // add the features we want
                                annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                                    Feature textDetection = new Feature();
                                    textDetection.setType("TEXT_DETECTION");
                                    //textDetection.setMaxResults(10);
                                    add(textDetection);
                                }});

                                // Add the list of one thing to the request
                                add(annotateImageRequest);
                                Log.i(API_TAG, "added");
                            }
                        }
                    });

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);

                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.i(API_TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    List<AnnotateImageResponse> responses = response.getResponses();
                    return convertResponsesToString(responses);

                } catch (GoogleJsonResponseException e) {
                    Log.i(API_TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.i(API_TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                //return "Cloud Vision API request failed. Check logs for details.";
                return null;
            }

            protected void onPostExecute(Collection<Collection<String>> results) {

                if(results == null)
                {
                    mTvDetectedText.setText("Failed, Try AGain");
                }
                else
                {
                    mTvDetectedText.setText("");
                    mTvDetectedText.setVisibility(View.GONE);

                    int count = mTlObjects.getChildCount();

                    mTlObjects.removeViewsInLayout(1,count-1);
                    mTlObjects.setVisibility(View.VISIBLE);
                    HashMap<String,Integer> objectCounts = count(results);

                    Iterator iterator = objectCounts.keySet().iterator();

                    while (iterator.hasNext())
                    {
                        String key = (String) iterator.next();
                        String value = String.valueOf(objectCounts.get(key));

                        Log.i(TAG, key + " : " + value);

                        TableRow tr = new TableRow(MainActivity.this);
                        TextView tvLeft = new TextView(MainActivity.this);
                        setTextViewAttribute(tvLeft, key);
                        tvLeft.setPadding(5,5,5,5);

                        TextView tvRight = new TextView(MainActivity.this);
                        setTextViewAttribute(tvRight, value);
                        tvRight.setGravity(5);
                        tvRight.setPadding(5,5,40,5);

                        tr.addView(tvLeft);
                        tr.addView(tvRight);

                        mTlObjects.addView(tr);
                    }
                }
            }
        }.execute();
    }

    public void setTextViewAttribute(TextView view, String key)
    {
        view.setText(key);
        view.setAllCaps(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.setTextAppearance(android.R.style.TextAppearance_Medium);
        }
        else
        {
            view.setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Medium);
        }
    }

    private HashMap<String, Integer> count(Collection<Collection<String>> results)
    {
        HashMap<String,Integer> avgCountList = new HashMap<>();

        String objectNames = mEtObjectNames.getText().toString().toLowerCase().trim();
        String[] objectList = objectNames.split(",");

        for(String object: objectList) {
            int itr = 0;
            int tCount = 0;
            for (Collection<String> result : results) {
                int count = matchCount(object.trim(), result);

                if (count != 0) {
                    tCount += count;
                    itr++;
                }
            }

            int avgCount = 0;
            if(itr!=0)
            avgCount = tCount / itr;
            avgCountList.put(object, avgCount);
        }

        return avgCountList;
    }


    private Bitmap rotateImage(Bitmap bitmap, int angle)
    {
        Matrix matrix = new Matrix();

        matrix.postRotate(angle);

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        return rotatedBitmap;
    }

    private Collection<Collection<String>> convertResponsesToString(List<AnnotateImageResponse> responses) {

        Collection<Collection<String>> result = new ArrayList<>();
        for(AnnotateImageResponse response: responses)
        {
            List<EntityAnnotation> texts = response.getTextAnnotations();

            int i=1;
            int n = texts.size();
            Collection<String> detectedWords = new ArrayList<>();
            if (texts != null) {
                for (i=1;i<n;i++) {
                    EntityAnnotation text = texts.get(i);
                    detectedWords.add(text.getDescription().trim().toLowerCase());

                    Log.i(API_TAG, text.getDescription() + " " + text.getBoundingPoly().toString());
                }
                Log.i(API_TAG, "..................");
                result.add(detectedWords);
            } else {

            }
        }

        mSavedResult = result;

        return result;
    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 1000;
        int targetH = 1000;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }

    public int matchCount(String pattern,Collection<String> text)
    {
        String[] pat_words = pattern.split(" ");
        int count = 0;
        int itr=0;

        //for individual words of pattern
        if(pat_words.length>1)
        for(int i=0;i<pat_words.length;i++)
        {
            List<ExtractedResult> matchedResult = FuzzySearch.extractAll(pat_words[i], text, 90);

            if(matchedResult.size()>0) {
                count += matchedResult.size();
                itr++;
            }
            Log.i(TAG, String.valueOf(matchedResult.size()));
        }

        //for whole pattern
        List<ExtractedResult> matchedResult = FuzzySearch.extractAll(pattern, text, 70);
        Log.i(TAG, String.valueOf(matchedResult.size()));
        count += matchedResult.size();
        itr++;

        //taking average
        if(itr!=0)
        count = count/itr;
        return count;
    }
}
