package com.ludocode.imgursampleapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImgurAdapter extends RecyclerView.Adapter<ImgurAdapter.ViewHolder> {

    private static final String TAG = "ImgurAdapter";

    private static final float minAspectRatio = 0.7f;

    // This is the Imgur ClientID for "com.ludocode.imgursampleapp".
    // If you fork this project, you need to change the client ID!
    private static final String clientID = "409360a6eb3ec01";

    // We store whether an error has occurred in the current
    // browse category. If so, we prevent further page requests
    // until the user chooses a new category. We also track
    // whether an image error happened to avoid sending the toast
    // repeatedly.
    private boolean mHasError = false;
    private boolean mHasImageError = false;

    private ImgurActivity mActivity;
    private String mGalleryUrl;
    private List<Item> mItems = new ArrayList<Item>();
    private Set<String> mIDs = new HashSet<String>();
    private StringDownloadTask mPageDownload;
    private int mPageCount;

    public ImgurAdapter(ImgurActivity activity) {
        mActivity = activity;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final CardView mCardView;
        private final ThumbnailImageView mImageView;

        private BitmapDownloadTask mBitmapDownload;
        private String mLink;

        private class BitmapDownloadTask extends AsyncTask<String, Void, Bitmap> {
            @Override
            protected Bitmap doInBackground(String... urls) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(urls[0]).openConnection();

                    // Setting the Client-ID on image requests seems to cause some images to
                    // 404, so for now, we only enable authorization on requests to the API.
                    //connection.setRequestProperty("Authorization", "Client-ID " + clientID);

                    return BitmapFactory.decodeStream(connection.getInputStream());
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                    // We return null here so onPostExecute() will call the
                    // error handler on the UI thread.
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    ViewHolder.this.onBitmapDownloaded(bitmap);
                } else {
                    ImgurAdapter.this.onImageErrorOccurred();
                }
            }
        }

        public ViewHolder(View view) {
            super(view);

            mCardView = (CardView)view.findViewById(R.id.card_view);
            mImageView = (ThumbnailImageView)view.findViewById(R.id.card_image);

            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(mLink)));
                }
            });
        }

        private void reset() {
            if (mBitmapDownload != null) {
                mBitmapDownload.cancel(false);
                mBitmapDownload = null;
            }
            mImageView.setImageDrawable(null);
            mImageView.setAspectRatio(1.0f);
        }

        public void bind(Item item) {
            reset();

            mImageView.setAspectRatio(item.getAspectRatio());
            mLink = item.getLink();

            mBitmapDownload = new BitmapDownloadTask();
            mBitmapDownload.execute(item.getThumbnailURL());
        }

        public void onBitmapDownloaded(Bitmap bitmap) {
            mImageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Item item = mItems.get(position);
        holder.bind(item);

        // If we're binding an image for the last downloaded page, we
        // tell the adapter to download another page. This ensures that
        // we always have a full additional page of data so we can
        // continuously scroll.
        if (item.getPageNumber() == mPageCount - 1 && mPageDownload == null)
            fetchNextPage();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // TODO remove most logd
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        return new ViewHolder(inflater.inflate(R.layout.image_card, parent, false));
    }

    private void onErrorOccurred() {
        if (mHasError)
            return;
        mHasError = true;
        Toast.makeText(mActivity.getApplicationContext(), mActivity.getString(R.string.imgur_error),
                Toast.LENGTH_SHORT).show();
    }

    private void onImageErrorOccurred() {
        if (mHasImageError)
            return;
        mHasImageError = true;
        Toast.makeText(mActivity.getApplicationContext(), mActivity.getString(R.string.imgur_image_error),
                Toast.LENGTH_SHORT).show();
    }

    private class StringDownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(urls[0]).openConnection();
                connection.setRequestProperty("Authorization", "Client-ID " + clientID);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer builder = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isCancelled())
                        return null;
                    builder.append(line).append("\n");
                }
                return builder.toString();
            } catch (IOException e) {
                Log.d(TAG, e.toString());
                // We return null here so onPostExecute() will call the
                // error handler on the UI thread.
            }
            return null;
        }

        @Override
        protected void onPostExecute(String json) {
            if (json != null)
                ImgurAdapter.this.onReceivedPage(json);
            else
                ImgurAdapter.this.onErrorOccurred();
        }
    }

    private void reset() {
        mHasError = false;
        mHasImageError = false;
        mPageCount = 0;
        if (mPageDownload != null) {
            mPageDownload.cancel(false);
            mPageDownload = null;
        }
        int currentCount = mItems.size();
        mItems.clear();
        mIDs.clear();

        // We call notifyDataSetChanged() instead of notifyItemRangeRemoved()
        // because we need to scroll to the top immediately (without animation),
        // so we can't allow the current views to fade out. See
        // ImgurActivity.onDataReset() for more info.
        notifyDataSetChanged();
        mActivity.onDataReset();
    }

    public void refresh() {
        reset();
        fetchNextPage();
    }

    public void loadGallery(String url) {
        reset();
        mGalleryUrl = url;
        fetchNextPage();
    }

    public void fetchNextPage() {
        assert(mPageDownload == null);

        // We ignore additional page downloads if we're in an error state
        if (mHasError)
            return;

        mPageDownload = new StringDownloadTask();
        mPageDownload.execute(mGalleryUrl + mPageCount + ".json");
    }

    private class Item {
        private final int mPageNumber;
        private final String mID;
        private final String mThumbnail;
        private final float mAspectRatio;

        public Item(int pageNumber, JSONObject json) throws JSONException {
            mPageNumber = pageNumber;
            mID = json.getString("id");
            // TODO: currently always using medium; check DPI?
            mThumbnail = "http://i.imgur.com/" + json.getString("id") + "m.jpg";

            mAspectRatio = Math.max(minAspectRatio, (float)json.getInt("width") / (float)json.getInt("height"));
        }

        public String getLink() {
            return "http://imgur.com/" + mID;
        }

        public String getThumbnailURL() {
            return mThumbnail;
        }

        public float getAspectRatio() {
            return mAspectRatio;
        }

        public int getPageNumber() {
            return mPageNumber;
        }
    }

    public void onReceivedPage(String json) {
        mPageDownload = null;

        List<Item> additionalItems = new ArrayList<Item>();
        List<String> additionalIDs = new ArrayList<String>();

        try {
            JSONObject obj = new JSONObject(json);
            JSONArray data = obj.getJSONArray("data");

            for (int i = 0; i < data.length(); ++i) {
                JSONObject item = data.getJSONObject(i);

                // For now we ignore albums and animated images (gif/gifv/webm).
                // We only show png and jpeg images.
                String type = item.optString("type");
                if (!"image/jpeg".equals(type) && !"image/png".equals(type))
                    continue;

                // We also skip any images that already exist. This is in case
                // the gallery changes during pagination, and later pages contain
                // ids we've already seen in earlier pages.
                String id = item.getString("id");
                if (mIDs.contains(id))
                    continue;

                // The item constructor parses the rest of the item info.
                // It will throw if the JSON is malformed.
                additionalItems.add(new Item(mPageCount, item));
                additionalIDs.add(id);
            }

        } catch (JSONException e) {
            Log.d(TAG, e.toString());
            onErrorOccurred();
            return;
        }

        Log.d(TAG, "Successfully downloaded page " + mPageCount + ", " +
                additionalItems.size() + " additional items");

        ++mPageCount;
        int currentSize = mItems.size();
        mItems.addAll(additionalItems);
        mIDs.addAll(additionalIDs);
        notifyItemRangeInserted(currentSize, additionalItems.size());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}
