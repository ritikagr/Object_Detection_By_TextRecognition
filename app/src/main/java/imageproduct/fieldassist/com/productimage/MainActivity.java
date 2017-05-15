package imageproduct.fieldassist.com.productimage;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.FileNotFoundException;
import java.io.IOException;

import static android.R.attr.value;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MAIN_ACTIVITY";
    private String API_TAG = "TEXT_API";
    private ImageView mIvPickedImage;
    private Uri uri = null;
    private int CHOOSE_IMAGE_REQUEST = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIvPickedImage = (ImageView) findViewById(R.id.picked_image);
    }

    public void ChooseImage(View view)
    {
        Intent chooseImageIntent = new Intent();
        chooseImageIntent.setType("image/*");
        chooseImageIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(chooseImageIntent, "Select Image"),CHOOSE_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CHOOSE_IMAGE_REQUEST && resultCode == RESULT_OK)
        {
            uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                mIvPickedImage.setVisibility(View.VISIBLE);
                mIvPickedImage.setImageBitmap(bitmap);
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
        if(uri!=null) {
            try {
                Bitmap bitmap = decodeBitmapUri(this, uri);

                int i=0;
                while(i < 4)
                {
                    TextRecognizer trDetector = new TextRecognizer.Builder(getApplicationContext()).build();
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90*i);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    if(trDetector.isOperational() && rotatedBitmap != null)
                    {
                        Frame frame = new Frame.Builder().setBitmap(rotatedBitmap).build();
                        SparseArray<TextBlock> textBlocks = trDetector.detect(frame);

                        String blocks = "";

                        for (int index=0; index<textBlocks.size();index++)
                        {
                            TextBlock textBlock = textBlocks.valueAt(index);
                            blocks += (textBlock.getValue() + " ");
                        }

                        if(textBlocks.size() == 0)
                        {
                            Log.i(API_TAG, "Scan Failed: Nothing Found");
                        }
                        else
                        {
                            Log.i(API_TAG, "Detected Text: " + blocks);
                        }
                    }
                    else
                    {
                        Log.i(API_TAG, "Couldn't Setup the detector");
                    }

                    trDetector.release();
                    i++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            Log.i(TAG, "Please choose a valid image");
        }
    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
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
}
