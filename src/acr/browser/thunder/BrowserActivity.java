/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.thunder;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.Browser;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebViewDatabase;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.AutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

public class BrowserActivity extends Activity implements BrowserController {

	private Activity mActivity;
	private Context mContext;
	private List<LightningView> mWebViewList = new ArrayList<LightningView>();
	private List<TextView> mTabList = new ArrayList<TextView>();
	private List<Integer> mIdList = new ArrayList<Integer>();
	private LinearLayout mNewTab;
	private ImageView mBack;
	private ImageView mForward;
	private ImageView mOptions;
	private LinearLayout mTabLayout;
	private FrameLayout mBrowserFrame;
	private AutoCompleteTextView mSearch;
	private ProgressBar mProgressBar;
	private LightningView mCurrentView = null;
	private int mIdGenerator = 0;
	private String mSearchText;
	private PopupMenu mMenu;
	private MenuInflater mMenuInflater;
	private List<HistoryItem> mBookmarkList;
	private boolean mSystemBrowser = false;
	private DatabaseHandler mHistoryHandler;
	private SQLiteDatabase mHistoryDatabase;
	private SharedPreferences mPreferences;
	private Editor mEditPrefs;
	private ValueCallback<Uri> mUploadMessage;
	private ClickHandler mClickHandler;
	private View mCustomView;
	private int mOriginalOrientation;
	private FullscreenHolder mFullscreenContainer;
	private CustomViewCallback mCustomViewCallback;
	private final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT);
	private final int API = android.os.Build.VERSION.SDK_INT;
	private Bitmap mDefaultVideoPoster;
	private View mVideoProgressView;
	private CookieManager mCookieManager;
	private boolean mFullScreen;
	private Drawable mDeleteIcon;
	private Drawable mRefreshIcon;
	private Drawable mIcon;
	private boolean mIsNewIntent = false;
	private String mHomepage;
	private Animation mAddTab;
	private Animation mRemoveTab;
	private RelativeLayout mParentBackground;
	private RelativeLayout mUrlBar;
	private Animation mSlideUpAnimation;
	private Animation mSlideDownAnimation;
	private HorizontalScrollView mTabScrollView;
	private VideoView mVideoView;
	private static SearchAdapter mSearchAdapter;
	private boolean viewIsAnimating = false;
	private boolean isIncognito = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser_activity);
		mActivity = this;
		mContext = this;
		mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
		mEditPrefs = mPreferences.edit();
		initializePreferences();
		initialize();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void updateUrl(String url) {
		if (url == null)
			return;
		url = url.replaceFirst(Constants.HTTP, "");
		if (url.startsWith(Constants.FILE)) {
			url = "";
		}
		mSearch.setText(url);
	}

	@Override
	public void updateProgress(int n) {
		mProgressBar.setProgress(n);
		if (n >= 100) {
			mProgressBar.setVisibility(View.INVISIBLE);
			setIsFinishedLoading();
		} else {
			mProgressBar.setVisibility(View.VISIBLE);
			setIsLoading();
		}

	}

	@Override
	public void updateHistory(final String title, final String url) {

	}

	public void addItemToHistory(final String title, final String url) {
		Runnable update = new Runnable() {
			@Override
			public void run() {
				if (isSystemBrowserAvailable()) {
					try {
						Browser.updateVisitedHistory(getContentResolver(), url,
								true);
					} catch (NullPointerException ignored) {
					}
				}
				try {
					StringBuilder sb = new StringBuilder("url" + " = ");
					DatabaseUtils.appendEscapedSQLString(sb, url);

					if (mHistoryHandler == null) {
						mHistoryHandler = new DatabaseHandler(mContext);
						mHistoryDatabase = mHistoryHandler
								.getReadableDatabase();
					} else if (!mHistoryHandler.isOpen()) {
						mHistoryHandler = new DatabaseHandler(mContext);
						mHistoryDatabase = mHistoryHandler
								.getReadableDatabase();
					} else if (mHistoryDatabase == null) {
						mHistoryDatabase = mHistoryHandler
								.getReadableDatabase();
					} else if (!mHistoryDatabase.isOpen()) {
						mHistoryDatabase = mHistoryHandler
								.getReadableDatabase();
					}
					Cursor cursor = mHistoryDatabase.query(
							DatabaseHandler.TABLE_HISTORY, new String[] { "id",
									"url", "title" }, sb.toString(), null,
							null, null, null);
					if (!cursor.moveToFirst()) {
						mHistoryHandler.addHistoryItem(new HistoryItem(url,
								title));
					} else {
						mHistoryHandler.delete(url);
						mHistoryHandler.addHistoryItem(new HistoryItem(url,
								title));
					}
					cursor.close();
					cursor = null;
				} catch (IllegalStateException e) {
					Log.e(Constants.TAG,
							"IllegalStateException in updateHistory");
				} catch (NullPointerException e) {
					Log.e(Constants.TAG,
							"NullPointerException in updateHistory");
				} catch (SQLiteException e) {
					Log.e(Constants.TAG, "SQLiteException in updateHistory");
				}
			}
		};
		if (url != null) {
			if (!url.startsWith(Constants.FILE)) {
				new Thread(update).start();

			}
		}
	}

	public void closeActivity() {
		finish();
	}

	@Override
	public void openFileChooser(ValueCallback<Uri> uploadMsg) {
		mUploadMessage = uploadMsg;
		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);
		i.setType("*/*");
		startActivityForResult(Intent.createChooser(i, "File Chooser"), 1);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == 1) {
			if (null == mUploadMessage)
				return;
			Uri result = intent == null || resultCode != RESULT_OK ? null
					: intent.getData();
			mUploadMessage.onReceiveValue(result);
			mUploadMessage = null;

		}
	}

	@Override
	public void update() {
		// superfluous method from Drawer UI
	}

	@Override
	public void onLongPress() {
		if (mClickHandler == null) {
			mClickHandler = new ClickHandler(mContext);
		}
		Message click = mClickHandler.obtainMessage();
		if (click != null) {
			click.setTarget(mClickHandler);
		}
		mCurrentView.getWebView().requestFocusNodeHref(click);
	}

	@Override
	public void onShowCustomView(View view, int requestedOrientation,
			CustomViewCallback callback) {
		if (view == null) {
			return;
		}
		if (mCustomView != null && callback != null) {
			callback.onCustomViewHidden();
			return;
		}
		view.setKeepScreenOn(true);
		mOriginalOrientation = getRequestedOrientation();
		FrameLayout decor = (FrameLayout) getWindow().getDecorView();
		mFullscreenContainer = new FullscreenHolder(this);
		mCustomView = view;
		mFullscreenContainer.addView(mCustomView, COVER_SCREEN_PARAMS);
		decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
		setFullscreen(true);
		mCurrentView.setVisibility(View.GONE);
		if (view instanceof FrameLayout) {
			if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
				mVideoView = (VideoView) ((FrameLayout) view).getFocusedChild();
				mVideoView.setOnErrorListener(new VideoCompletionListener());
				mVideoView
						.setOnCompletionListener(new VideoCompletionListener());
			}
		}
		mCustomViewCallback = callback;
	}

	@Override
	public void onHideCustomView() {
		if (mCustomView == null || mCustomViewCallback == null
				|| mCurrentView == null)
			return;
		Log.i(Constants.TAG, "onHideCustomView");
		mCurrentView.setVisibility(View.VISIBLE);
		mCustomView.setKeepScreenOn(false);
		setFullscreen(mPreferences.getBoolean(
				PreferenceConstants.HIDE_STATUS_BAR, false));
		FrameLayout decor = (FrameLayout) getWindow().getDecorView();
		if (decor != null) {
			decor.removeView(mFullscreenContainer);
		}

		if (API < 19) {
			try {
				mCustomViewCallback.onCustomViewHidden();
			} catch (Throwable ignored) {

			}
		}
		mFullscreenContainer = null;
		mCustomView = null;
		if (mVideoView != null) {
			mVideoView.setOnErrorListener(null);
			mVideoView.setOnCompletionListener(null);
			mVideoView = null;
		}
		setRequestedOrientation(mOriginalOrientation);
	}

	private class VideoCompletionListener implements
			MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			return false;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			onHideCustomView();
		}

	}

	@Override
	public Bitmap getDefaultVideoPoster() {
		if (mDefaultVideoPoster == null) {
			mDefaultVideoPoster = BitmapFactory.decodeResource(getResources(),
					android.R.drawable.ic_media_play);
		}
		return mDefaultVideoPoster;
	}

	@Override
	public View getVideoLoadingProgressView() {
		if (mVideoProgressView == null) {
			LayoutInflater inflater = LayoutInflater.from(this);
			mVideoProgressView = inflater.inflate(
					R.layout.video_loading_progress, null);
		}
		return mVideoProgressView;
	}

	@Override
	public void onCreateWindow(boolean isUserGesture, Message resultMsg) {
		if (resultMsg == null) {
			return;
		}
		newTab(true, "");
		WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
		transport.setWebView(mCurrentView.getWebView());
		resultMsg.sendToTarget();
	}

	@Override
	public Activity getActivity() {
		return mActivity;
	}

	@Override
	public void hideActionBar() {
		if (mFullScreen) {
			if (mUrlBar.isShown()) {
				mUrlBar.bringToFront();
				mUrlBar.startAnimation(mSlideUpAnimation);
			}
		}
	}

	@Override
	public void showActionBar() {
		if (mFullScreen) {
			if (!mUrlBar.isShown()) {
				mUrlBar.bringToFront();
				mUrlBar.startAnimation(mSlideDownAnimation);
			}
		}
	}

	@Override
	public void longClickPage(final String url) {
		HitTestResult result = null;
		if (mCurrentView.getWebView() != null) {
			result = mCurrentView.getWebView().getHitTestResult();
		}
		if (mCurrentView.getUrl().equals(
				Constants.FILE + mContext.getCacheDir() + "/bookmarks.html")) {

			if (url != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
				builder.setTitle(mContext.getResources().getString(
						R.string.action_bookmarks));
				builder.setMessage(
						getResources().getString(R.string.dialog_bookmark))
						.setCancelable(true)
						.setPositiveButton(
								getResources().getString(
										R.string.action_new_tab),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										newTab(false, url);
									}
								})
						.setNegativeButton(
								getResources()
										.getString(R.string.action_delete),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										deleteBookmark(url);
									}
								})
						.setNeutralButton(
								getResources().getString(R.string.action_edit),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										editBookmark(url);
									}
								});
				AlertDialog alert = builder.create();
				alert.show();
			}
		} else if (url != null) {
			if (result != null) {
				if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
						|| result.getType() == HitTestResult.IMAGE_TYPE) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								newTab(false, url);
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								mCurrentView.loadUrl(url);
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								if (API > 8) {
									Utils.downloadFile(mActivity, url,
											mCurrentView.getUserAgent(),
											"attachment", false);
								}
								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mActivity); // dialog
					builder.setTitle(url.replace(Constants.HTTP, ""))
							.setMessage(
									getResources().getString(
											R.string.dialog_image))
							.setPositiveButton(
									getResources().getString(
											R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(
									getResources().getString(
											R.string.action_open),
									dialogClickListener)
							.setNeutralButton(
									getResources().getString(
											R.string.action_download),
									dialogClickListener).show();

				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								newTab(false, url);
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								mCurrentView.loadUrl(url);
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("label",
										url);
								clipboard.setPrimaryClip(clip);

								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mActivity); // dialog
					builder.setTitle(url)
							.setMessage(
									getResources().getString(
											R.string.dialog_link))
							.setPositiveButton(
									getResources().getString(
											R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(
									getResources().getString(
											R.string.action_open),
									dialogClickListener)
							.setNeutralButton(
									getResources().getString(
											R.string.action_copy),
									dialogClickListener).show();
				}
			} else {
				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE: {
							newTab(false, url);
							break;
						}
						case DialogInterface.BUTTON_NEGATIVE: {
							mCurrentView.loadUrl(url);
							break;
						}
						case DialogInterface.BUTTON_NEUTRAL: {
							ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
							ClipData clip = ClipData.newPlainText("label", url);
							clipboard.setPrimaryClip(clip);

							break;
						}
						}
					}
				};

				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
				builder.setTitle(url)
						.setMessage(
								getResources().getString(R.string.dialog_link))
						.setPositiveButton(
								getResources().getString(
										R.string.action_new_tab),
								dialogClickListener)
						.setNegativeButton(
								getResources().getString(R.string.action_open),
								dialogClickListener)
						.setNeutralButton(
								getResources().getString(R.string.action_copy),
								dialogClickListener).show();
			}
		} else if (result != null) {
			if (result.getExtra() != null) {
				final String newUrl = result.getExtra();
				if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
						|| result.getType() == HitTestResult.IMAGE_TYPE) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								newTab(false, newUrl);
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								mCurrentView.loadUrl(newUrl);
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								if (API > 8) {
									Utils.downloadFile(mActivity, newUrl,
											mCurrentView.getUserAgent(),
											"attachment", false);
								}
								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mActivity); // dialog
					builder.setTitle(newUrl.replace(Constants.HTTP, ""))
							.setMessage(
									getResources().getString(
											R.string.dialog_image))
							.setPositiveButton(
									getResources().getString(
											R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(
									getResources().getString(
											R.string.action_open),
									dialogClickListener)
							.setNeutralButton(
									getResources().getString(
											R.string.action_download),
									dialogClickListener).show();

				} else {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case DialogInterface.BUTTON_POSITIVE: {
								newTab(false, newUrl);
								break;
							}
							case DialogInterface.BUTTON_NEGATIVE: {
								mCurrentView.loadUrl(newUrl);
								break;
							}
							case DialogInterface.BUTTON_NEUTRAL: {
								ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("label",
										newUrl);
								clipboard.setPrimaryClip(clip);

								break;
							}
							}
						}
					};

					AlertDialog.Builder builder = new AlertDialog.Builder(
							mActivity); // dialog
					builder.setTitle(newUrl)
							.setMessage(
									getResources().getString(
											R.string.dialog_link))
							.setPositiveButton(
									getResources().getString(
											R.string.action_new_tab),
									dialogClickListener)
							.setNegativeButton(
									getResources().getString(
											R.string.action_open),
									dialogClickListener)
							.setNeutralButton(
									getResources().getString(
											R.string.action_copy),
									dialogClickListener).show();
				}

			}

		}

	}

	public void deleteBookmark(String url) {
		File book = new File(getFilesDir(), "bookmarks");
		File bookUrl = new File(getFilesDir(), "bookurl");
		try {
			BufferedWriter bookWriter = new BufferedWriter(new FileWriter(book));
			BufferedWriter urlWriter = new BufferedWriter(new FileWriter(
					bookUrl));
			Iterator<HistoryItem> iter = mBookmarkList.iterator();
			HistoryItem item;
			int num = 0;
			int deleteIndex = -1;
			while (iter.hasNext()) {
				item = iter.next();
				if (!item.getUrl().equalsIgnoreCase(url)) {
					bookWriter.write(item.getTitle());
					urlWriter.write(item.getUrl());
					bookWriter.newLine();
					urlWriter.newLine();
				} else {
					deleteIndex = num;
				}
				num++;
			}
			if (deleteIndex != -1) {
				mBookmarkList.remove(deleteIndex);
			}
			bookWriter.close();
			urlWriter.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		mSearchAdapter.refreshBookmarks();
		openBookmarkPage(mCurrentView.getWebView());
	}

	public synchronized void editBookmark(String url) {
		int id = -1;
		int size = mBookmarkList.size();
		for (int n = 0; n < size; n++) {
			if (mBookmarkList.get(n).getUrl().equals(url)) {
				id = n;
				break;
			}
		}
		if (id == -1) {
			return;
		}
		final int loc = id;
		final AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
		picker.setTitle(getResources().getString(R.string.title_edit_bookmark));
		final EditText getTitle = new EditText(mContext);
		getTitle.setHint(getResources().getString(R.string.hint_title));
		getTitle.setText(mBookmarkList.get(id).getTitle());
		getTitle.setSingleLine();
		final EditText getUrl = new EditText(mContext);
		getUrl.setHint(getResources().getString(R.string.hint_url));
		getUrl.setText(mBookmarkList.get(id).getUrl());
		getUrl.setSingleLine();
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.addView(getTitle);
		layout.addView(getUrl);
		picker.setView(layout);
		picker.setPositiveButton(getResources().getString(R.string.action_ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						mBookmarkList.get(loc).setTitle(
								getTitle.getText().toString());
						mBookmarkList.get(loc).setUrl(
								getUrl.getText().toString());
						File book = new File(getFilesDir(), "bookmarks");
						File bookUrl = new File(getFilesDir(), "bookurl");
						try {
							BufferedWriter bookWriter = new BufferedWriter(
									new FileWriter(book));
							BufferedWriter urlWriter = new BufferedWriter(
									new FileWriter(bookUrl));
							Iterator<HistoryItem> iter = mBookmarkList
									.iterator();
							HistoryItem item;
							while (iter.hasNext()) {
								item = iter.next();

								bookWriter.write(item.getTitle());
								urlWriter.write(item.getUrl());
								bookWriter.newLine();
								urlWriter.newLine();

							}

							bookWriter.close();
							urlWriter.close();
						} catch (FileNotFoundException e) {
						} catch (IOException e) {
						}
						openBookmarkPage(mCurrentView.getWebView());
					}
				});
		picker.show();

	}

	@Override
	public void openBookmarkPage(WebView view) {
		String bookmarkHtml = BookmarkPageVariables.Heading;
		mBookmarkList = getBookmarks();
		Collections.sort(mBookmarkList, new SortIgnoreCase());
		Iterator<HistoryItem> iter = mBookmarkList.iterator();
		HistoryItem helper;
		while (iter.hasNext()) {
			helper = iter.next();
			bookmarkHtml += (BookmarkPageVariables.Part1 + helper.getUrl()
					+ BookmarkPageVariables.Part2 + helper.getUrl()
					+ BookmarkPageVariables.Part3 + helper.getTitle() + BookmarkPageVariables.Part4);
		}
		bookmarkHtml += BookmarkPageVariables.End;
		File bookmarkWebPage = new File(mContext.getCacheDir(),
				"bookmarks.html");
		try {
			FileWriter bookWriter = new FileWriter(bookmarkWebPage, false);
			bookWriter.write(bookmarkHtml);
			bookWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		view.loadUrl(Constants.FILE + bookmarkWebPage);
	}

	private List<HistoryItem> getBookmarks() {
		List<HistoryItem> bookmarks = new ArrayList<HistoryItem>();
		File bookUrl = new File(getApplicationContext().getFilesDir(),
				"bookurl");
		File book = new File(getApplicationContext().getFilesDir(), "bookmarks");
		try {
			BufferedReader readUrl = new BufferedReader(new FileReader(bookUrl));
			BufferedReader readBook = new BufferedReader(new FileReader(book));
			String u, t;
			while ((u = readUrl.readLine()) != null
					&& (t = readBook.readLine()) != null) {
				HistoryItem map = new HistoryItem(u, t);
				bookmarks.add(map);
			}
			readBook.close();
			readUrl.close();
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		}
		return bookmarks;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_ENTER) {
			if (mSearch.hasFocus()) {
				searchTheWeb(mSearch.getText().toString());
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	public void clearHistory() {
		this.deleteDatabase(DatabaseHandler.DATABASE_NAME);
		WebViewDatabase m = WebViewDatabase.getInstance(this);
		m.clearFormData();
		m.clearHttpAuthUsernamePassword();
		if (API < 18) {
			m.clearUsernamePassword();
			WebIconDatabase.getInstance().removeAllIcons();
		}
		if (mSystemBrowser) {
			try {
				Browser.clearHistory(getContentResolver());
			} catch (NullPointerException ignored) {
			}
		}
		SettingsController.setClearHistory(true);
		Utils.trimCache(this);
	}

	public void clearCookies() {
		CookieManager c = CookieManager.getInstance();
		CookieSyncManager.createInstance(this);
		c.removeAllCookie();
	}

	@Override
	public synchronized void onBackPressed() {
		showActionBar();
		if (mCurrentView.canGoBack()) {
			if (!mCurrentView.isShown()) {
				onHideCustomView();
			} else {
				mCurrentView.goBack();
			}
		} else {
			if (!mCurrentView.isDestroyed())
				deleteTab(mCurrentView.getId());
		}
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mPreferences.getBoolean(PreferenceConstants.CLEAR_CACHE_EXIT,
					false) && mCurrentView != null) {
				mCurrentView.clearCache(true);
				Log.i(Constants.TAG, "Cache Cleared");

			}
			if (mPreferences.getBoolean(PreferenceConstants.CLEAR_HISTORY_EXIT,
					false) && !isIncognito()) {
				clearHistory();
				Log.i(Constants.TAG, "History Cleared");

			}
			if (mPreferences.getBoolean(PreferenceConstants.CLEAR_COOKIES_EXIT,
					false) && !isIncognito()) {
				clearCookies();
				Log.i(Constants.TAG, "Cookies Cleared");

			}
			mCurrentView = null;
			for (int n = 0; n < mWebViewList.size(); n++) {
				if (mWebViewList.get(n) != null)
					mWebViewList.get(n).onDestroy();
			}
			mWebViewList.clear();
			finish();
		}
		return true;
	}

	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		super.onLowMemory();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	public void handleNewIntent(Intent intent) {
		if (mCurrentView == null) {
			initialize();
		}

		String url = null;
		if (intent != null) {
			url = intent.getDataString();
		}
		int num = 0;
		if (intent != null && intent.getExtras() != null)
			num = intent.getExtras().getInt(getPackageName() + ".Origin");
		if (num == 1) {
			mCurrentView.loadUrl(url);
		} else if (url != null) {
			if (url.startsWith(Constants.FILE)) {
				Utils.showToast(this,
						getResources()
								.getString(R.string.message_blocked_local));
				url = null;
			}
			newTab(true, url);
			mIsNewIntent = true;
		}
		super.onNewIntent(intent);
	}

	@Override
	protected void onDestroy() {
		if (mHistoryDatabase != null) {
			if (mHistoryDatabase.isOpen())
				mHistoryDatabase.close();
		}
		if (mHistoryHandler != null) {
			if (mHistoryHandler.isOpen())
				mHistoryHandler.close();
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mCurrentView != null) {
			mCurrentView.pauseTimers();
			mCurrentView.onPause();
		}
		if (mHistoryDatabase != null) {
			if (mHistoryDatabase.isOpen())
				mHistoryDatabase.close();
		}
		if (mHistoryHandler != null) {
			if (mHistoryHandler.isOpen())
				mHistoryHandler.close();
		}

	}

	public void saveOpenTabs() {
		if (mPreferences
				.getBoolean(PreferenceConstants.RESTORE_LOST_TABS, true)) {
			String s = "";
			for (int n = 0; n < mWebViewList.size(); n++) {
				if (mWebViewList.get(n).getUrl() != null) {
					if (!mWebViewList.get(n).getUrl().equals(mHomepage)
							&& !mWebViewList.get(n).getUrl()
									.startsWith(Constants.FILE)) {
						s = s + mWebViewList.get(n).getUrl()
								+ "|$|SEPARATOR|$|";
					}
				}
			}
			mEditPrefs.putString(PreferenceConstants.URL_MEMORY, s);
			mEditPrefs.commit();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (SettingsController.getClearHistory()) {
		}
		if (mSearchAdapter != null) {
			mSearchAdapter.refreshPreferences();
			mSearchAdapter.refreshBookmarks();
		}
		if (mCurrentView != null) {
			mCurrentView.resumeTimers();
			mCurrentView.onResume();

			if (mHistoryHandler == null) {
				mHistoryHandler = new DatabaseHandler(this);
			} else if (!mHistoryHandler.isOpen()) {
				mHistoryHandler = new DatabaseHandler(this);
			}
			mHistoryDatabase = mHistoryHandler.getReadableDatabase();
			mBookmarkList = getBookmarks();
		} else {
			initialize();
		}
		initializePreferences();
		if (mFullScreen && mParentBackground != null) {
			if (mParentBackground.indexOfChild(mUrlBar) != -1) {
				mParentBackground.removeView(mUrlBar);
				mBrowserFrame.addView(mUrlBar);
				mUrlBar.bringToFront();
			}
		} else if (mParentBackground != null) {
			if (mBrowserFrame.indexOfChild(mUrlBar) != -1) {
				mBrowserFrame.removeView(mUrlBar);
				mParentBackground.addView(mUrlBar);
				mUrlBar.bringToFront();
			}
		}
		if (mWebViewList != null) {
			for (int n = 0; n < mWebViewList.size(); n++) {
				if (mWebViewList.get(n) != null) {
					mWebViewList.get(n).initializePreferences(this);
				} else {
					mWebViewList.remove(n);
				}
			}
		} else {
			initialize();
		}
	}

	public boolean isSystemBrowserAvailable() {
		return mSystemBrowser;
	}

	public boolean getSystemBrowser() {
		Cursor c = null;
		String[] columns = new String[] { "url", "title" };
		boolean browserFlag = false;
		try {

			Uri bookmarks = Browser.BOOKMARKS_URI;
			c = getContentResolver()
					.query(bookmarks, columns, null, null, null);
		} catch (SQLiteException ignored) {
		} catch (IllegalStateException ignored) {
		} catch (NullPointerException ignored) {
		}

		if (c != null) {
			Log.i("Browser", "System Browser Available");
			browserFlag = true;
		} else {
			Log.e("Browser", "System Browser Unavailable");
			browserFlag = false;
		}
		if (c != null) {
			c.close();
			c = null;
		}
		mEditPrefs.putBoolean(PreferenceConstants.SYSTEM_BROWSER_PRESENT,
				browserFlag);
		mEditPrefs.commit();
		return browserFlag;
	}

	private synchronized void initialize() {
		mClickHandler = new ClickHandler(this);
		mParentBackground = (RelativeLayout) findViewById(R.id.background);
		mUrlBar = (RelativeLayout) findViewById(R.id.urlBar);
		mNewTab = (LinearLayout) findViewById(R.id.action_new_tab);
		mBack = (ImageView) findViewById(R.id.back);
		mForward = (ImageView) findViewById(R.id.forward);
		mOptions = (ImageView) findViewById(R.id.options);
		mTabLayout = (LinearLayout) findViewById(R.id.TabLayout);
		mBrowserFrame = (FrameLayout) findViewById(R.id.holder);
		mTabScrollView = (HorizontalScrollView) findViewById(R.id.tabScroll);
		mSearch = (AutoCompleteTextView) findViewById(R.id.enterUrl);
		mDeleteIcon = getResources().getDrawable(R.drawable.ic_action_delete);
		mDeleteIcon.setBounds(0, 0, Utils.convertToDensityPixels(mContext, 24),
				Utils.convertToDensityPixels(mContext, 24));
		mRefreshIcon = getResources().getDrawable(R.drawable.ic_action_refresh);
		mRefreshIcon.setBounds(0, 0,
				Utils.convertToDensityPixels(mContext, 24),
				Utils.convertToDensityPixels(mContext, 24));
		mIcon = mRefreshIcon;
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

		if (mWebViewList != null) {
			mWebViewList.clear();
		} else {
			mWebViewList = new ArrayList<LightningView>();
		}
		if (mTabList != null) {
			mTabList.clear();
		} else {
			mTabList = new ArrayList<TextView>();
		}

		if (mIdList != null) {
			mIdList.clear();
		} else {
			mIdList = new ArrayList<Integer>();
		}
		mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE,
				Constants.HOMEPAGE);
		mSystemBrowser = getSystemBrowser();

		mMenu = new PopupMenu(mContext, mOptions);
		mMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
				case R.id.action_new_tab:
					newTab(true, null);
					return true;
				case R.id.action_incognito:
					startActivity(new Intent(mContext, IncognitoActivity.class));
					return true;
				case R.id.action_share:
					if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
						Intent shareIntent = new Intent(
								android.content.Intent.ACTION_SEND);
						shareIntent.setType("text/plain");
						shareIntent.putExtra(
								android.content.Intent.EXTRA_SUBJECT,
								mCurrentView.getTitle());
						String shareMessage = mCurrentView.getUrl();
						shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
								shareMessage);
						startActivity(Intent.createChooser(
								shareIntent,
								getResources().getString(
										R.string.dialog_title_share)));
					}
					return true;
				case R.id.action_bookmarks:
					openBookmarkPage(mCurrentView.getWebView());
					return true;
				case R.id.action_copy:
					if (mCurrentView != null) {
						if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
							ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
							ClipData clip = ClipData.newPlainText("label",
									mCurrentView.getUrl().toString());
							clipboard.setPrimaryClip(clip);
							Utils.showToast(mContext, mContext.getResources()
									.getString(R.string.message_link_copied));

						}
					}
					return true;
				case R.id.action_settings:
					startActivity(new Intent(mContext, SettingsActivity.class));
					return true;
				case R.id.action_history:
					openHistory();
					return true;
				case R.id.action_add_bookmark:
					if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
						addBookmark(mContext, mCurrentView.getTitle(),
								mCurrentView.getUrl());
					}
					return true;
				case R.id.action_find:
					findInPage();
					return true;
				default:
					return true;
				}
			}

		});
		mMenuInflater = mMenu.getMenuInflater();
		mMenuInflater.inflate(getMenu(), mMenu.getMenu());
		OnTouchListener drag = null;
		if (API > 18) {
			drag = mMenu.getDragToOpenListener();
		}
		if (drag != null) {
			mOptions.setOnTouchListener(drag);
		}
		mOptions.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mMenu.show();
			}

		});

		mBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mCurrentView.canGoBack()) {
					mCurrentView.goBack();
				} else {
					deleteTab(mCurrentView.getId());
				}
			}

		});

		mForward.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mCurrentView.canGoForward()) {
					mCurrentView.goForward();
				}
			}

		});

		mNewTab.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				newTab(true, null);
			}

		});

		mSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus && mCurrentView != null) {
					if (mCurrentView != null) {
						if (mCurrentView.getProgress() < 100) {
							setIsLoading();
						} else {
							setIsFinishedLoading();
						}
					}
					updateUrl(mCurrentView.getUrl());
				} else if (hasFocus) {
				}
			}
		});

		mSearch.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View arg0, int arg1, KeyEvent arg2) {

				switch (arg1) {
				case KeyEvent.KEYCODE_ENTER:
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
					searchTheWeb(mSearch.getText().toString());
					if (mCurrentView != null) {
						mCurrentView.requestFocus();
					}
					return true;
				default:
					break;
				}
				return false;
			}

		});

		mSearch.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mSearch.getCompoundDrawables()[2] != null) {
					boolean tappedX = event.getX() > (mSearch.getWidth()
							- mSearch.getPaddingRight() - mIcon
							.getIntrinsicWidth());
					if (tappedX) {
						if (event.getAction() == MotionEvent.ACTION_UP) {
							refreshOrStop();
						}
						return true;
					}
				}
				return false;
			}

		});

		mSearch.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView arg0, int actionId,
					KeyEvent arg2) {
				// hide the keyboard and search the web when the enter key
				// button is pressed
				if (actionId == EditorInfo.IME_ACTION_GO
						|| actionId == EditorInfo.IME_ACTION_DONE
						|| actionId == EditorInfo.IME_ACTION_NEXT
						|| actionId == EditorInfo.IME_ACTION_SEND
						|| actionId == EditorInfo.IME_ACTION_SEARCH
						|| (arg2.getAction() == KeyEvent.KEYCODE_ENTER)) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
					searchTheWeb(mSearch.getText().toString());
					if (mCurrentView != null) {
						mCurrentView.requestFocus();
					}
					return true;
				}
				return false;
			}

		});

		mAddTab = AnimationUtils.loadAnimation(mContext, R.anim.up);
		mRemoveTab = AnimationUtils.loadAnimation(mContext, R.anim.down);
		mSlideDownAnimation = AnimationUtils.loadAnimation(mContext,
				R.anim.slide_down);
		mSlideUpAnimation = AnimationUtils.loadAnimation(mContext,
				R.anim.slide_up);

		mSlideDownAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationStart(Animation animation) {
				mUrlBar.setVisibility(View.VISIBLE);
			}

		});

		mSlideUpAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {
				mUrlBar.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationStart(Animation animation) {

			}

		});

		if (mHistoryHandler == null) {
			mHistoryHandler = new DatabaseHandler(this);
		} else if (!mHistoryHandler.isOpen()) {
			mHistoryHandler = new DatabaseHandler(this);
		}
		mHistoryDatabase = mHistoryHandler.getReadableDatabase();

		if (API < 19) {
			WebIconDatabase.getInstance().open(
					getDir("icons", MODE_PRIVATE).getPath());
		}

		initializeSearchSuggestions(mSearch);
		initializeTabs();

		boolean useProxy = mPreferences.getBoolean(
				PreferenceConstants.USE_PROXY, false);

		// if (useProxy)
		// initializeTor();
		// else
		checkForTor();
	}

	public int getMenu() {
		return R.menu.main;
	}

	/*
	 * If Orbot/Tor is installed, prompt the user if they want to enable
	 * proxying for this session
	 */
	public boolean checkForTor() {
		boolean useProxy = mPreferences.getBoolean(
				PreferenceConstants.USE_PROXY, false);

		OrbotHelper oh = new OrbotHelper(this);
		if (oh.isOrbotInstalled()
				&& !mPreferences.getBoolean(
						PreferenceConstants.INITIAL_CHECK_FOR_TOR, false)) {
			mEditPrefs.putBoolean(PreferenceConstants.INITIAL_CHECK_FOR_TOR,
					true);
			mEditPrefs.apply();
			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						mPreferences
								.edit()
								.putBoolean(PreferenceConstants.USE_PROXY, true)
								.apply();

						initializeTor();
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						mPreferences
								.edit()
								.putBoolean(PreferenceConstants.USE_PROXY,
										false).apply();
						break;
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.use_tor_prompt)
					.setPositiveButton(R.string.yes, dialogClickListener)
					.setNegativeButton(R.string.no, dialogClickListener).show();

			return true;
		} else if (oh.isOrbotInstalled() & useProxy == true) {
			initializeTor();
			return true;
		} else {
			mEditPrefs.putBoolean(PreferenceConstants.USE_PROXY, false);
			mEditPrefs.apply();
			return false;
		}
	}

	/*
	 * Initialize WebKit Proxying for Tor
	 */
	public void initializeTor() {

		OrbotHelper oh = new OrbotHelper(this);
		if (!oh.isOrbotRunning())
			oh.requestOrbotStart(this);

		WebkitProxy wkp = new WebkitProxy();
		try {
			String host = mPreferences.getString(
					PreferenceConstants.USE_PROXY_HOST, "localhost");
			int port = mPreferences.getInt(PreferenceConstants.USE_PROXY_PORT,
					8118);
			wkp.setProxy("acr.browser.thunder.BrowserApp",
					getApplicationContext(), host, port);
		} catch (Exception e) {
			Log.d(Constants.TAG, "error enabling web proxying", e);
		}

	}

	private synchronized void initializeSearchSuggestions(
			final AutoCompleteTextView getUrl) {

		getUrl.setThreshold(1);
		getUrl.setDropDownWidth(-1);
		getUrl.setDropDownAnchor(R.id.back);
		getUrl.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				try {
					String url;
					url = ((TextView) arg1.findViewById(R.id.url)).getText()
							.toString();
					if (url.startsWith(mContext.getString(R.string.suggestion))) {
						url = ((TextView) arg1.findViewById(R.id.title))
								.getText().toString();
					} else {
						getUrl.setText(url);
					}
					searchTheWeb(url);
					url = null;
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
					if (mCurrentView != null) {
						mCurrentView.requestFocus();
					}
				} catch (NullPointerException e) {
					Log.e("Browser Error: ",
							"NullPointerException on item click");
				}
			}

		});

		getUrl.setSelectAllOnFocus(true);
		mSearchAdapter = new SearchAdapter(mContext, isIncognito());
		getUrl.setAdapter(mSearchAdapter);
	}

	public synchronized void initializeTabs() {

	}

	public void restoreOrNewTab() {
		String url = null;
		if (getIntent() != null) {
			url = getIntent().getDataString();
			if (url != null) {
				if (url.startsWith(Constants.FILE)) {
					Utils.showToast(
							this,
							getResources().getString(
									R.string.message_blocked_local));
					url = null;
				}
			}
		}
		if (mPreferences
				.getBoolean(PreferenceConstants.RESTORE_LOST_TABS, true)) {
			String mem = mPreferences.getString(PreferenceConstants.URL_MEMORY,
					"");
			mEditPrefs.putString(PreferenceConstants.URL_MEMORY, "");
			String[] array = getArray(mem);
			int count = 0;
			for (int n = 0; n < array.length; n++) {
				if (array[n].length() > 0) {
					newTab(true, array[n]);
					count++;
				}
			}
			if (url != null) {
				newTab(true, url);
			} else if (count == 0) {
				newTab(true, null);
			}
		} else {
			newTab(true, url);
		}
	}

	public static String[] getArray(String input) {
		return input.split("\\|\\$\\|SEPARATOR\\|\\$\\|");
	}

	public synchronized void initializePreferences() {
		if (mPreferences == null) {
			mPreferences = getSharedPreferences(
					PreferenceConstants.PREFERENCES, 0);
		}
		mFullScreen = mPreferences.getBoolean(PreferenceConstants.FULL_SCREEN,
				false);
		if (mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}

		switch (mPreferences.getInt(PreferenceConstants.SEARCH, 1)) {
		case 0:
			mSearchText = mPreferences.getString(
					PreferenceConstants.SEARCH_URL, Constants.GOOGLE_SEARCH);
			if (!mSearchText.startsWith(Constants.HTTP)
					&& !mSearchText.startsWith(Constants.HTTPS)) {
				mSearchText = Constants.GOOGLE_SEARCH;
			}
			break;
		case 1:
			mSearchText = Constants.GOOGLE_SEARCH;
			break;
		case 2:
			mSearchText = Constants.ANDROID_SEARCH;
			break;
		case 3:
			mSearchText = Constants.BING_SEARCH;
			break;
		case 4:
			mSearchText = Constants.YAHOO_SEARCH;
			break;
		case 5:
			mSearchText = Constants.STARTPAGE_SEARCH;
			break;
		case 6:
			mSearchText = Constants.STARTPAGE_MOBILE_SEARCH;
			break;
		case 7:
			mSearchText = Constants.DUCK_SEARCH;
			break;
		case 8:
			mSearchText = Constants.DUCK_LITE_SEARCH;
			break;
		case 9:
			mSearchText = Constants.BAIDU_SEARCH;
			break;
		case 10:
			mSearchText = Constants.YANDEX_SEARCH;
			break;
		}

		updateCookiePreference();
		if (mPreferences.getBoolean(PreferenceConstants.USE_PROXY, false)) {
			initializeTor();
		} else {
			try {
				WebkitProxy.resetProxy("acr.browser.thunder.BrowserApp",
						getApplicationContext());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void updateCookiePreference() {

	}

	@Override
	public void closeEmptyTab() {
		if (mCurrentView != null
				&& mCurrentView.getWebView().copyBackForwardList().getSize() == 0) {
			closeCurrentTab();
		}
	}

	private void closeCurrentTab() {
		// deleteTab(mCurrentView.getId());
		// don't delete the tab it will mess up the application stack
	}

	protected synchronized void newTab(boolean show, String url) {
		Log.i(Constants.TAG, "new Tab");
		mIsNewIntent = false;
		LightningView view = new LightningView(mActivity, url);
		mWebViewList.add(view);
		view.setId(mIdGenerator);
		mIdList.add(mIdGenerator);
		mIdGenerator++;
		if (show) {
			// TODO
			if (mCurrentView != null) {
				mBrowserFrame.removeView(mCurrentView.getWebView());
				mCurrentView.deactivateTab();
			}
			mCurrentView = view;
			mCurrentView.activateTab();
			mCurrentView.requestFocus();
			updateProgress(view.getProgress());
			updateUrl(view.getUrl());
		} else {
			view.deactivateTab();
		}
		animateTabAddition(view, show);
	}

	private synchronized void showTab(LightningView view) {
		if (view == null) {
			return;
		}
		if (mCurrentView != null) {
			mBrowserFrame.removeView(mCurrentView.getWebView());
			mCurrentView.deactivateTab();
		}
		mBrowserFrame.addView(view.getWebView());
		mCurrentView = view;
		mCurrentView.activateTab();
		mCurrentView.requestFocus();
		updateProgress(view.getProgress());
		updateUrl(view.getUrl());
		if (mFullScreen) {
			mUrlBar.bringToFront();
			mUrlBar.requestLayout();
			mUrlBar.invalidate();
		}
	}

	@Override
	public synchronized void showSelectedTab(int id) {
		mIsNewIntent = false;
		int index = mIdList.indexOf(id);
		if (index == -1) {
			return;
		}
		showTab(mWebViewList.get(index));
	}

	@Override
	public synchronized void deleteTab(int id) {
		if (viewIsAnimating) {
			return;
		}
		mTabLayout.clearDisappearingChildren();
		int position = mIdList.indexOf(id);
		if (position >= mWebViewList.size()) {
			return;
		}
		if (position == -1) {
			return;
		}
		int current = mIdList.indexOf(mCurrentView.getId());
		if (current == -1) {
			return;
		}
		LightningView reference = mWebViewList.get(position);
		if (reference == null) {
			return;
		}
		mTabScrollView.smoothScrollTo(mCurrentView.getTitleView().getLeft(), 0);
		final boolean isShown = reference.isShown();
		if (current > position) {
			if (reference.isShown()) {
				showTab(mWebViewList.get(position - 1));
			}

			animateTabRemoval(mWebViewList.get(position).getTitleView(),
					position, reference, isShown);
			reference.onDestroy();
			mWebViewList.remove(position);
			mIdList.remove(position);
		} else if (mWebViewList.size() > position + 1) {
			if (reference.isShown()) {
				showTab(mWebViewList.get(position + 1));
			}
			animateTabRemoval(mWebViewList.get(position).getTitleView(),
					position, reference, isShown);
			reference.onDestroy();
			mWebViewList.remove(position);
			mIdList.remove(position);
		} else if (mWebViewList.size() > 1) {
			if (reference.isShown()) {
				showTab(mWebViewList.get(position - 1));
			}
			animateTabRemoval(mWebViewList.get(position).getTitleView(),
					position, reference, isShown);
			reference.onDestroy();
			mWebViewList.remove(position);
			mIdList.remove(position);
		} else {
			if (mCurrentView.getUrl().startsWith(Constants.FILE)
					|| mCurrentView.getUrl().equals(mHomepage)) {
				closeActivity();
			} else {
				mWebViewList.remove(position);
				mIdList.remove(position);
				if (mPreferences.getBoolean(
						PreferenceConstants.CLEAR_CACHE_EXIT, false)
						&& mCurrentView != null) {
					mCurrentView.clearCache(true);
				}
				if (mPreferences.getBoolean(
						PreferenceConstants.CLEAR_HISTORY_EXIT, false)
						&& !isIncognito()) {
					clearHistory();
					Log.i(Constants.TAG, "History Cleared");

				}
				if (mPreferences.getBoolean(
						PreferenceConstants.CLEAR_COOKIES_EXIT, false)
						&& !isIncognito()) {
					clearCookies();
					Log.i(Constants.TAG, "Cookies Cleared");

				}
				if (reference != null) {
					reference.pauseTimers();
					reference.onDestroy();
				}
				mCurrentView = null;
				finish();
			}
		}

		Log.i(Constants.TAG, "Tab Deleted");
	}

	public boolean isIncognito() {
		return false;
	}

	private synchronized void animateTabRemoval(final TextView view,
			final int position, final LightningView reference,
			final boolean isShown) {
		mRemoveTab.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {

				mTabLayout.post(new Runnable() {
					public void run() {
						// it works without the runOnUiThread, but all UI
						// updates must
						// be done on the UI thread
						mActivity.runOnUiThread(new Runnable() {
							public void run() {
								mTabLayout.removeView(view);
								viewIsAnimating = false;
							}
						});
					}
				});
				if (mIsNewIntent && isShown) {
					mIsNewIntent = false;
					closeActivity();
				}

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationStart(Animation animation) {
				viewIsAnimating = true;
			}

		});
		new Runnable() {

			@Override
			public void run() {
				view.startAnimation(mRemoveTab);
			}

		}.run();
	}

	private synchronized void animateTabAddition(final LightningView view,
			final boolean show) {
		if (show) {
			view.setInvisible();
			mBrowserFrame.addView(view.getWebView());
		}
		if (mFullScreen) {
			mUrlBar.bringToFront();
			mUrlBar.requestLayout();
			mUrlBar.invalidate();
		}
		view.getTitleView().setVisibility(View.INVISIBLE);
		mTabLayout.addView(view.getTitleView());
		mAddTab.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation animation) {

				mTabScrollView.smoothScrollTo(mCurrentView.getTitleView()
						.getLeft(), 0);
				if (show) {
					view.setVisible();
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationStart(Animation animation) {
				view.getTitleView().setVisibility(View.VISIBLE);
			}

		});
		view.getTitleView().startAnimation(mAddTab);

	}

	public void addBookmark(Context context, String title, String url) {
		File book = new File(context.getFilesDir(), "bookmarks");
		File bookUrl = new File(context.getFilesDir(), "bookurl");
		HistoryItem bookmark = new HistoryItem(url, title);

		try {
			BufferedReader readUrlRead = new BufferedReader(new FileReader(
					bookUrl));
			String u;
			while ((u = readUrlRead.readLine()) != null) {
				if (u.contentEquals(url)) {
					readUrlRead.close();
					return;
				}
			}
			readUrlRead.close();

		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		} catch (NullPointerException ignored) {
		}
		try {
			BufferedWriter bookWriter = new BufferedWriter(new FileWriter(book,
					true));
			BufferedWriter urlWriter = new BufferedWriter(new FileWriter(
					bookUrl, true));
			bookWriter.write(title);
			urlWriter.write(url);
			bookWriter.newLine();
			urlWriter.newLine();
			bookWriter.close();
			urlWriter.close();
			mBookmarkList.add(bookmark);
		} catch (FileNotFoundException ignored) {
		} catch (IOException ignored) {
		} catch (NullPointerException ignored) {
		}
		mSearchAdapter.refreshBookmarks();
	}

	private void findInPage() {
		final AlertDialog.Builder finder = new AlertDialog.Builder(mActivity);
		finder.setTitle(getResources().getString(R.string.action_find));
		final EditText getHome = new EditText(this);
		getHome.setHint(getResources().getString(R.string.search_hint));
		finder.setView(getHome);
		finder.setPositiveButton(
				getResources().getString(R.string.search_hint),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = getHome.getText().toString();
						if (mCurrentView != null) {
							mCurrentView.find(text);
						}
					}
				});
		finder.show();
	}

	private void openHistory() {

		Thread history = new Thread(new Runnable() {

			@Override
			public void run() {
				String historyHtml = HistoryPageVariables.Heading;
				List<HistoryItem> historyList = getHistory();
				Iterator<HistoryItem> it = historyList.iterator();
				HistoryItem helper;
				while (it.hasNext()) {
					helper = it.next();
					historyHtml += HistoryPageVariables.Part1 + helper.getUrl()
							+ HistoryPageVariables.Part2 + helper.getTitle()
							+ HistoryPageVariables.Part3 + helper.getUrl()
							+ HistoryPageVariables.Part4;
				}

				historyHtml += HistoryPageVariables.End;
				File historyWebPage = new File(getFilesDir(), "history.html");
				try {
					FileWriter hWriter = new FileWriter(historyWebPage, false);
					hWriter.write(historyHtml);
					hWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				mCurrentView.loadUrl(Constants.FILE + historyWebPage);
				mSearch.setText("");
			}

		});
		history.run();
	}

	private List<HistoryItem> getHistory() {
		DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
		return databaseHandler.getLastHundredItems();
	}

	void searchTheWeb(String query) {
		if (query.equals("")) {
			return;
		}
		String SEARCH = mSearchText;
		query = query.trim();
		mCurrentView.stopLoading();

		if (query.startsWith("www.")) {
			query = Constants.HTTP + query;
		} else if (query.startsWith("ftp.")) {
			query = "ftp://" + query;
		}

		boolean containsPeriod = query.contains(".");
		boolean isIPAddress = (TextUtils.isDigitsOnly(query.replace(".", ""))
				&& (query.replace(".", "").length() >= 4) && query
				.contains("."));
		boolean aboutScheme = query.contains("about:");
		boolean validURL = (query.startsWith("ftp://")
				|| query.startsWith(Constants.HTTP)
				|| query.startsWith(Constants.FILE) || query
					.startsWith(Constants.HTTPS)) || isIPAddress;
		boolean isSearch = ((query.contains(" ") || !containsPeriod) && !aboutScheme);

		if (isIPAddress
				&& (!query.startsWith(Constants.HTTP) || !query
						.startsWith(Constants.HTTPS))) {
			query = Constants.HTTP + query;
		}

		if (isSearch) {
			try {
				query = URLEncoder.encode(query, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			mCurrentView.loadUrl(SEARCH + query);
		} else if (!validURL) {
			mCurrentView.loadUrl(Constants.HTTP + query);
		} else {
			mCurrentView.loadUrl(query);
		}
	}

	static class FullscreenHolder extends FrameLayout {

		public FullscreenHolder(Context ctx) {
			super(ctx);
			setBackgroundColor(ctx.getResources().getColor(
					android.R.color.black));
		}

		@Override
		public boolean onTouchEvent(MotionEvent evt) {
			return true;
		}

	}

	public void setFullscreen(boolean enabled) {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		if (enabled) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
			if (mCustomView != null) {
				mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			} else {
				mBrowserFrame
						.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			}
		}
		win.setAttributes(winParams);
	}

	public void setIsLoading() {
		if (!mSearch.hasFocus()) {
			mIcon = mDeleteIcon;
			mSearch.setCompoundDrawables(null, null, mDeleteIcon, null);
		}
	}

	public void setIsFinishedLoading() {
		if (!mSearch.hasFocus()) {
			mIcon = mRefreshIcon;
			mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
		}
	}

	private void refreshOrStop() {
		if (mCurrentView != null) {
			if (mCurrentView.getProgress() < 100) {
				mCurrentView.stopLoading();
			} else {
				mCurrentView.reload();
			}
		}
	}

	@Override
	public boolean isActionBarShown() {
		if (mUrlBar == null) {
			return false;
		} else {
			return mUrlBar.isShown();
		}
	}

	public class SortIgnoreCase implements Comparator<HistoryItem> {

		public int compare(HistoryItem o1, HistoryItem o2) {
			return o1.getTitle().toLowerCase(Locale.getDefault())
					.compareTo(o2.getTitle().toLowerCase(Locale.getDefault()));
		}

	}

}
