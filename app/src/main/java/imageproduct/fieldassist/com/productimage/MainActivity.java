package imageproduct.fieldassist.com.productimage;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;

import static android.R.attr.value;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MAIN_ACTIVITY";
    private Button mBtChooseImage;
    private ImageView mIvPickedImage;
    private int CHOOSE_IMAGE_REQUEST = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtChooseImage = (Button) findViewById(R.id.choose_image);
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
            Uri uri = data.getData();

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
}
