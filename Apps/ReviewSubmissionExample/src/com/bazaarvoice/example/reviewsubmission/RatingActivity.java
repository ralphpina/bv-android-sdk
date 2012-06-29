package com.bazaarvoice.example.reviewsubmission;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bazaarvoice.OnBazaarResponse;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

public class RatingActivity extends Activity {
	
	protected static final String TAG = "Rating Activity";
	private Bitmap displayImage;
	private ImageView thumbImage;
	private RatingBar ratingBar;
	private EditText titleField;
	private EditText nicknameField;
	private EditText reviewField;
	private Button submitButton;
	private ProgressDialog progressDialog;
	private boolean photoUploaded = false;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.rating);
        
        Intent myIntent = getIntent();
    	byte[] byteArray = myIntent.getByteArrayExtra("capturedImage");
    	if (byteArray == null){
    		displayImage = null;
    	}
    	else if(displayImage == null){
    		displayImage = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    	}
    	
    	Uri imageUri = Uri.parse(myIntent.getStringExtra("imageUri"));
		uploadPhoto(imageUri);
        
        initializeViews();
	}

	private void uploadPhoto(Uri imageUri) {
		BazaarFunctions.uploadPhoto(new File(imageUri.getPath()), new OnImageUploadComplete(){

			@Override
			public void onFinish() {
				/*
				 * If the user has clicked "Submit" before the photo finishes uploading
				 * we must now start submitting the review.
				 */
				photoUploaded = true;
				if(progressDialog.isShowing()){
					runOnUiThread(new Runnable(){

						@Override
						public void run() {
							submitReview();
						}
						
					});
				}
			}
			
		});
	}

	private void initializeViews() {
		thumbImage = (ImageView) findViewById(R.id.thumbImage);
		thumbImage.setImageBitmap(displayImage);
		
		progressDialog = new ProgressDialog(this);
		
		ratingBar = (RatingBar) findViewById(R.id.ratingBar);
		
		titleField = (EditText) findViewById(R.id.titleField);
		nicknameField = (EditText) findViewById(R.id.nicknameField);
		reviewField = (EditText) findViewById(R.id.reviewField);
		submitButton = (Button) findViewById(R.id.submitButton);
		submitButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				submitReview();
			}
			
		});
	}

	protected void submitReview() {
		if(ratingBar.getRating() == 0){
			Toast.makeText(getBaseContext(), "You must give a rating between 1 and 5.", Toast.LENGTH_SHORT).show();
		}
		else if(titleField.getText().toString().equals("")){
			Toast.makeText(getBaseContext(), "You must enter a title.", Toast.LENGTH_SHORT).show();
		}
		else if(nicknameField.getText().toString().equals("")){
			Toast.makeText(getBaseContext(), "You must enter a nickname.", Toast.LENGTH_SHORT).show();
		}
		else if(reviewField.getText().toString().equals("")){
			Toast.makeText(getBaseContext(), "You must enter a review.", Toast.LENGTH_SHORT).show();
		}
		else if(photoUploaded){
			final BazaarReview review = new BazaarReview();
			review.setTitle(titleField.getText().toString());
			review.setReviewText(reviewField.getText().toString());
			review.setNickname(nicknameField.getText().toString());
			review.setRating((int) ratingBar.getRating());
			
			BazaarFunctions.previewReview(MainActivity.productId, review, new OnBazaarResponse(){

				@Override
				public void onException(String message, Throwable exception) {
					Log.e(TAG, "Error = " + message + "\n" + Log.getStackTraceString(exception));
				}

				@Override
				public void onResponse(JSONObject json) {
					Log.i(TAG, "Response = \n" + json);
					
					try {
						if(json.getBoolean("HasErrors")){
							displayErrorMessage(json);
							progressDialog.dismiss();
						}
						else{
							Intent intent = new Intent(getBaseContext(), RatingPreviewActivity.class);
							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							displayImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
							byte[] byteArray = stream.toByteArray();
							intent.putExtra("displayImage", byteArray);
							intent.putExtra("reviewTitle", review.getTitle());
							intent.putExtra("reviewText", review.getReviewText());
							intent.putExtra("reviewNickname", review.getNickname());
							intent.putExtra("reviewRating", review.getRating());
							progressDialog.dismiss();
							startActivity(intent);
						}
					} catch (JSONException exception) {
						Log.e(TAG, Log.getStackTraceString(exception));
					}

				}
				
			});
			progressDialog.setMessage("Submitting Review...");
			progressDialog.show();
		}
		else{
			progressDialog.setMessage("Uploading Photo...");
			progressDialog.show();
		}
	}

	/*
	 * Grabs the first field error and displays it in a toast.
	 * If no form errors occurred, displays a general error.
	 */
	protected void displayErrorMessage(final JSONObject json) {
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				try{
					JSONObject formErrors = json.getJSONObject("FormErrors");
					JSONArray errorNames = formErrors.getJSONArray("FieldErrorsOrder");
					JSONObject fieldErrors = formErrors.getJSONObject("FieldErrors");
					if(!errorNames.optString(0).equals("")){
						String name = errorNames.getString(0);
						JSONObject error = fieldErrors.getJSONObject(name);
						String message = error.getString("Message");
						Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
					}
					else{
						Toast.makeText(getBaseContext(), "An error has occurred", Toast.LENGTH_LONG).show();
					}
				} catch (JSONException exception){
					Log.e(TAG, Log.getStackTraceString(exception));
				}
			}
			
		});
	}

}
