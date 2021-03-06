package com.cloudsense.icqa;

import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DetailedFeedbackFragment extends Fragment {

	public static OnUserInputChangedListener mUserInputListener;
	
	private static Context appContext;
	private static EditText editText;
	private static SpannableStringBuilder ssb;
	private static final int CHOICE_BUTTON_NO = 11;
	private static final String SERVER_URL = "http://130.233.124.173:9000/xmlPost";
	
	public static String[] buttonAdjectives = new String[CHOICE_BUTTON_NO];
	
	private Button[] buttonArray;
	private Button feedbackSubmit;
	private ArrayList<String> chosen; // Array for holding the choices

	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (container == null)
			return null;
		return inflater.inflate(R.layout.detailed_feedback, container, false);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	} // end onCreate

	public void onStart() {
		super.onStart();
		appContext = getActivity();
		feedbackSubmit = (Button) getActivity().findViewById(
				R.id.feedback_button);
		editText = (EditText) getActivity().findViewById(
				R.id.feedback_edit_text);

		buttonArray = new Button[CHOICE_BUTTON_NO];

		buttonArray[0] = (Button) getActivity().findViewById(R.id.button1);
		buttonArray[1] = (Button) getActivity().findViewById(R.id.button2);
		buttonArray[2] = (Button) getActivity().findViewById(R.id.button3);
		buttonArray[3] = (Button) getActivity().findViewById(R.id.button4);
		buttonArray[4] = (Button) getActivity().findViewById(R.id.button5);
		buttonArray[5] = (Button) getActivity().findViewById(R.id.button6);
		buttonArray[6] = (Button) getActivity().findViewById(R.id.button7);
		buttonArray[7] = (Button) getActivity().findViewById(R.id.button8);
		buttonArray[8] = (Button) getActivity().findViewById(R.id.button9);
		buttonArray[9] = (Button) getActivity().findViewById(R.id.button10);
		buttonArray[10] = (Button) getActivity().findViewById(R.id.button11);

		for (int i = 0; i < buttonArray.length; i++)
			buttonAdjectives[i] = buttonArray[i].getText().toString();

		chosen = new ArrayList<String>();

		for (final Button btn : buttonArray) {
			btn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					chosen.add((String) btn.getText());
					
					ssb = createTextTokenizer(btn.getText().toString());
					editText.append(ssb);

					Log.d("DETAIL_FEEDBACK", editText.getText().toString());

					
					btn.setEnabled(false);
				}
			});
		}

		editText.setMovementMethod(LinkMovementMethod.getInstance());
		editText.addTextChangedListener(new TextWatcher() {

			// If all the text is deleted
			// enable the buttons again
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (s.toString().length() == 0) {
					for (Button btn : buttonArray)
						btn.setEnabled(true);
					chosen.clear();
				}
				Selection.setSelection(editText.getText(), editText.getText().length()); // set cursor at the end
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) { /* empty */
			}

			@Override
			public void afterTextChanged(Editable s) { /* empty */
			}
		});

		feedbackSubmit.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				chosen = new ArrayList<String>(Arrays.asList(editText.getText().toString().split(",")));
				Log.v("UPON_SUBMIT", chosen.toString());
				
				// ==================================================
				String[] val = String.valueOf(editText.getText()).split(" ");
				Log.v("EDIT_TEXT_ON_SUBMIT", TextUtils.join(":", val));
				// ==================================================

				new AsyncHttpPost().execute(SERVER_URL);
				Intent intent = getActivity().getIntent();
				intent.setClass(getActivity(), MainActivity.class);
				startActivity(intent);
			}
		});
	} // onStart

	// Don't change the screen orientation
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getActivity().setRequestedOrientation(
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	/**
	 * This method writes the climate report of the user into a well-formed XML.
	 */
	private String writeXml(List<String> report) {
		Bundle extras = getActivity().getIntent().getExtras();
		String user_id = null;
		if (extras != null) {
			user_id = extras.getString(LoginUsingFacebook.FACEBOOK_USER_ID);
		}

		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", "user-report");
			serializer.attribute("", "user-id", user_id);
			for (String c : chosen) {
				serializer.startTag("", "value");
				serializer.text(c);
				serializer.endTag("", "value");
			}
			serializer.endTag("", "user-report");
			serializer.endDocument();
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	// Upload XML on AsyncTask
	public class AsyncHttpPost extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			byte[] result = null;
			String str = "";

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(params[0]); // url

			try {
				// String name = "Beriyo";
				// StringEntity entity = new StringEntity("name="+name,
				// "UTF-8");
				// httppost.setEntity(entity);
				// httppost.addHeader("Content-Type",
				// "application/x-www-form-urlencoded"); // This MIME type is
				// VERY IMPORTANT for forms. Spent few hours not knowing it.

				String xml = writeXml(chosen);
				StringEntity entity = new StringEntity(xml, "UTF-8");
				httpPost.setEntity(entity);
				httpPost.addHeader("Accept", "application/xml");
				httpPost.addHeader("Content-Type", "application/xml");
				HttpResponse response = httpclient.execute(httpPost);
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpURLConnection.HTTP_OK) {
					result = EntityUtils.toByteArray(response.getEntity());
					str = new String(result, "UTF-8");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return str;
		}

		@Override
		protected void onPostExecute(String result) {
			// Toast.makeText( getActivity, result, Toast.LENGTH_LONG).show();
			Log.v("DetailedFeedbackFragment - AsyncPost", result);
		}
	}

	// =================================================================
	// Experimental UI
	// Creating bubbles a.k.a Text tokenizer for the user feedback UI
	// The clickableSpan event handler doesn't work for now.
	// =================================================================

	/**
	 * Returns a textView with the passed String argument
	 * 
	 * @param text
	 * @return
	 */
	public static TextView createTextView(String text) {
		TextView tv = new TextView(appContext);
		tv.setText(text);
		tv.setTextSize(20);
		tv.setBackgroundResource(R.drawable.bubble);
		// tv.setCompoundDrawablesWithIntrinsicBounds(0, 0,
		// android.R.drawable.ic_delete, 0);
		return tv;
	}

	/**
	 * Converts a TextView into Bitmap
	 * 
	 * @param view
	 * @return
	 */
	public static Object convertViewToDrawable(View view) {
		int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		view.measure(spec, spec);
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(),
				view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		c.translate(-view.getScrollX(), -view.getScrollY());
		view.draw(c);
		view.setDrawingCacheEnabled(true);
		Bitmap cacheBmp = view.getDrawingCache();
		Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
		view.destroyDrawingCache();

		return new BitmapDrawable(appContext.getResources(), viewBmp);
	}

	/**
	 * It puts together a TextView and a bitmap and returns a bunch of bitmaps
	 * as a SpannableStringBuilder object which can be added to an EditText by
	 * simply using setText.
	 * 
	 * @param args
	 * @return
	 */
	public static SpannableStringBuilder createTextTokenizer(String... args) {
		SpannableStringBuilder ssb = new SpannableStringBuilder();

		for (final String msg : args) {
			TextView tv = createTextView(msg);
			BitmapDrawable bd = (BitmapDrawable) convertViewToDrawable(tv);
			bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());
			ssb.append(msg + ",");

			ImageSpan imageSpan = new ImageSpan(bd);

			int start = ssb.length() - (msg.length() + 1);
			int end = ssb.length() - 1;

			ssb.setSpan(imageSpan, start, end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			ClickableSpan clickSpan = new ClickableSpan() {

				@Override
				public void onClick(View view) {
					mUserInputListener.onUserInputSelected(editText, msg);
				}
			};

			ssb.setSpan(clickSpan, start, end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		}
		return ssb;
	}

	public interface OnUserInputChangedListener {
		public void onUserInputSelected(View view, String item);
	}

	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mUserInputListener = (OnUserInputChangedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.getClass().toString()
					+ " should implement OnUserInputChangedListener");
		}
	}

} // end Class

