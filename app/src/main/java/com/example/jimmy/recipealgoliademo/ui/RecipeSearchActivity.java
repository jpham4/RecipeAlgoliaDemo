package com.example.jimmy.recipealgoliademo.ui;

import android.app.SearchManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.example.jimmy.recipealgoliademo.R;
import com.example.jimmy.recipealgoliademo.io.SearchResultsJsonParser;
import com.example.jimmy.recipealgoliademo.model.HighlightedResult;
import com.example.jimmy.recipealgoliademo.model.Recipe;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import org.json.JSONObject;

import java.util.Collection;
import java.util.List;

public class RecipeSearchActivity extends AppCompatActivity implements AbsListView.OnScrollListener, SearchView.OnQueryTextListener {

    private Client client;
    private Index index;
    private Query query;
    private SearchResultsJsonParser resultsParser = new SearchResultsJsonParser();
    private int lastSearchedSeqNo;
    private int lastDisplayedSeqNo;
    private int lastRequestedPage;
    private int lastDisplayedPage;
    private boolean endReached;

    // UI:
    private SearchView searchView;
    private ListView recipeListView;
    private RecipeAdapter recipeListAdapter;
    private ImageLoader imageLoader;
    private DisplayImageOptions displayImageOptions;
    private HighlightRenderer highlightRenderer;

    // Constants

    private static final int HITS_PER_PAGE = 20;

    /** Number of items before the end of the list past which we start loading more content. */
    private static final int LOAD_MORE_THRESHOLD = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_search);

        //Bind UI components.
        recipeListView = findViewById(R.id.listview_recipes);
        recipeListView.setAdapter(recipeListAdapter = new RecipeAdapter(this, R.layout.cell_recipe));
        recipeListView.setOnScrollListener(this);

        //Init Algolia.
        client = new Client("EY517IJ95R","566f46715310e80d5d7fa36a0dc54ea9");
        index = client.getIndex("recipes");

        //Pre-build query.
        query = new Query();
        query.setAttributesToRetrieve("name", "url", "image", "cookTime");
        query.setAttributesToHighlight("name");
        query.setHitsPerPage(HITS_PER_PAGE);

        // Configure Universal Image Loader.
        displayImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisk(true)
                .resetViewBeforeLoading(true)
                .displayer(new FadeInBitmapDisplayer(300))
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                imageLoader = ImageLoader.getInstance();
                ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Builder(RecipeSearchActivity.this)
                        .memoryCacheSize(2 * 1024 * 1024)
                        .memoryCacheSizePercentage(13) // default
                        .build();
                imageLoader.init(configuration);
            }
        }).start();

        highlightRenderer = new HighlightRenderer(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_search, menu);

        // Configure search view.
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    // Actions

    private void search() {
        final int currentSearchSeqNo = ++lastSearchedSeqNo;
        query.setQuery(searchView.getQuery().toString());
        lastRequestedPage = 0;
        lastDisplayedPage = -1;
        endReached = false;
        index.searchAsync(query, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (content != null && error == null) {
                    // NOTE: Check that the received results are newer that the last displayed results.
                    //
                    // Rationale: Although TCP imposes a server to send responses in the same order as
                    // requests, nothing prevents the system from opening multiple connections to the
                    // same server, nor the Algolia client to transparently switch to another server
                    // between two requests. Therefore the order of responses is not guaranteed.
                    if (currentSearchSeqNo <= lastDisplayedSeqNo) {
                        return;
                    }

                    List<HighlightedResult<Recipe>> results = resultsParser.parseResults(content);
                    if (results.isEmpty()) {
                        endReached = true;
                    } else {
                        recipeListAdapter.clear();
                        recipeListAdapter.addAll(results);
                        recipeListAdapter.notifyDataSetChanged();
                        lastDisplayedSeqNo = currentSearchSeqNo;
                        lastDisplayedPage = 0;
                    }

                    // Scroll the list back to the top.
                    recipeListView.smoothScrollToPosition(0);
                }
            }
        });
    }

    private void loadMore() {
        Query loadMoreQuery = new Query(query);
        loadMoreQuery.setPage(++lastRequestedPage);
        final int currentSearchSeqNo = lastSearchedSeqNo;
        index.searchAsync(loadMoreQuery, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                if (content != null && error == null) {
                    // Ignore results if they are for an older query.
                    if (lastDisplayedSeqNo != currentSearchSeqNo) {
                        return;
                    }

                    List<HighlightedResult<Recipe>> results = resultsParser.parseResults(content);
                    if (results.isEmpty()) {
                        endReached = true;
                    } else {
                        recipeListAdapter.addAll(results);
                        recipeListAdapter.notifyDataSetChanged();
                        lastDisplayedPage = lastRequestedPage;
                    }
                }
            }
        });
    }

    private class RecipeAdapter extends ArrayAdapter<HighlightedResult<Recipe>> {
        public RecipeAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup cell = (ViewGroup) convertView;
            if (cell == null) {
                cell = (ViewGroup) getLayoutInflater().inflate(R.layout.cell_recipe, null);
            }

            ImageView posterImageView = (ImageView) cell.findViewById(R.id.imageview_poster);
            TextView nameTextView = (TextView) cell.findViewById(R.id.textview_name);
            TextView cookTimeTextView = (TextView) cell.findViewById(R.id.textview_cookTime);

            HighlightedResult<Recipe> result = recipeListAdapter.getItem(position);

            imageLoader.displayImage(result.getResult().getImage(), posterImageView, displayImageOptions);
            nameTextView.setText(highlightRenderer.renderHighlights(result.getHighlight("name").getHighlightedValue()));
            cookTimeTextView.setText(String.format("%d", result.getResult().getCookTime()));

            return cell;
        }

        @Override
        public void addAll(Collection<? extends HighlightedResult<Recipe>> items) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                super.addAll(items);
            } else {
                for (HighlightedResult<Recipe> item : items) {
                    add(item);
                }
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        search();
        return true;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount == 0 || endReached) {
            return;
        }

        // Ignore if a new page has already been requested.
        if (lastRequestedPage > lastDisplayedPage) {
            return;
        }

        // Load more if we are sufficiently close to the end of the list.
        int firstInvisibleItem = firstVisibleItem + visibleItemCount;
        if (firstInvisibleItem + LOAD_MORE_THRESHOLD >= totalItemCount) {
            loadMore();
        }
    }
}
