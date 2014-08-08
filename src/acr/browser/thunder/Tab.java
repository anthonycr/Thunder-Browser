/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.thunder;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Tab extends LinearLayout {

	private TextView mTitle;
	private ImageView mFavicon;
	private ImageView mClose;

	public Tab(Context context) {
		super(context);
		setOrientation(LinearLayout.HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.tab, this, true);
		mFavicon = (ImageView) getChildAt(0);
		mTitle = (TextView) getChildAt(1);
		setClose((ImageView) getChildAt(2));
		setBackgroundColor(context.getResources().getColor(R.color.gray_medium));
	}

	public void setTitle(String title) {
		mTitle.setText(title);
	}

	public void setFavicon(Bitmap fav) {
		mFavicon.setImageBitmap(fav);
	}

	public ImageView getClose() {
		return mClose;
	}

	public void setClose(ImageView mClose) {
		this.mClose = mClose;
	}

}
