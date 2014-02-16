/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.SelectionBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import timber.log.Timber;

public class SeriesGuideProvider extends ContentProvider {

    public static final boolean LOGV = false;

    private static UriMatcher sUriMatcher;

    private static final int SHOWS = 100;

    private static final int SHOWS_ID = 101;

    private static final int SHOWS_FILTERED = 102;

    private static final int SHOWS_WITH_EPISODE = 103;


    private static final int EPISODES = 200;

    private static final int EPISODES_ID = 201;

    private static final int EPISODES_OFSHOW = 202;

    private static final int EPISODES_OFSEASON = 203;

    private static final int EPISODES_OFSEASON_WITHSHOW = 204;

    private static final int EPISODES_WITHSHOW = 205;

    private static final int EPISODES_ID_WITHSHOW = 206;


    private static final int SEASONS = 300;

    private static final int SEASONS_ID = 301;

    private static final int SEASONS_OFSHOW = 302;


    private static final int EPISODESEARCH = 400;

    private static final int EPISODESEARCH_ID = 401;


    private static final int LISTS = 500;

    private static final int LISTS_ID = 501;

    private static final int LISTS_WITH_LIST_ITEM_ID = 502;


    private static final int LIST_ITEMS = 600;

    private static final int LIST_ITEMS_ID = 601;

    private static final int LIST_ITEMS_WITH_DETAILS = 602;


    private static final int MOVIES = 700;

    private static final int MOVIES_ID = 701;

    private static final int SEARCH_SUGGEST = 800;

    private static final int RENEW_FTSTABLE = 900;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri} variations supported by
     * this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher(Context context) {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = context.getPackageName() + ".provider";

        // Shows
        matcher.addURI(authority, SeriesGuideContract.PATH_SHOWS, SHOWS);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_SHOWS + "/" + SeriesGuideContract.PATH_FILTER
                        + "/*", SHOWS_FILTERED);
        matcher.addURI(authority, SeriesGuideContract.PATH_SHOWS + "/"
                + SeriesGuideContract.PATH_WITH_EPISODE, SHOWS_WITH_EPISODE);
        matcher.addURI(authority, SeriesGuideContract.PATH_SHOWS + "/*", SHOWS_ID);

        // Episodes
        matcher.addURI(authority, SeriesGuideContract.PATH_EPISODES, EPISODES);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_OFSEASON
                        + "/" + SeriesGuideContract.PATH_WITHSHOW + "/*",
                EPISODES_OFSEASON_WITHSHOW);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_OFSEASON
                        + "/*", EPISODES_OFSEASON);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_OFSHOW
                        + "/*", EPISODES_OFSHOW);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_WITHSHOW,
                EPISODES_WITHSHOW);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_EPISODES + "/" + SeriesGuideContract.PATH_WITHSHOW
                        + "/*", EPISODES_ID_WITHSHOW);
        matcher.addURI(authority, SeriesGuideContract.PATH_EPISODES + "/*", EPISODES_ID);

        // Seasons
        matcher.addURI(authority, SeriesGuideContract.PATH_SEASONS, SEASONS);
        matcher.addURI(authority,
                SeriesGuideContract.PATH_SEASONS + "/" + SeriesGuideContract.PATH_OFSHOW
                        + "/*", SEASONS_OFSHOW);
        matcher.addURI(authority, SeriesGuideContract.PATH_SEASONS + "/*", SEASONS_ID);

        // Lists
        matcher.addURI(authority, SeriesGuideContract.PATH_LISTS, LISTS);
        matcher.addURI(authority, SeriesGuideContract.PATH_LISTS + "/"
                + SeriesGuideContract.PATH_WITH_LIST_ITEM_ID + "/*", LISTS_WITH_LIST_ITEM_ID);
        matcher.addURI(authority, SeriesGuideContract.PATH_LISTS + "/*", LISTS_ID);

        // List items
        matcher.addURI(authority, SeriesGuideContract.PATH_LIST_ITEMS, LIST_ITEMS);
        matcher.addURI(authority, SeriesGuideContract.PATH_LIST_ITEMS + "/"
                + SeriesGuideContract.PATH_WITH_DETAILS, LIST_ITEMS_WITH_DETAILS);
        matcher.addURI(authority, SeriesGuideContract.PATH_LIST_ITEMS + "/*", LIST_ITEMS_ID);

        // Movies
        matcher.addURI(authority, SeriesGuideContract.PATH_MOVIES, MOVIES);
        matcher.addURI(authority, SeriesGuideContract.PATH_MOVIES + "/*", MOVIES_ID);

        // Search
        matcher.addURI(authority, SeriesGuideContract.PATH_EPISODESEARCH + "/"
                + SeriesGuideContract.PATH_SEARCH, EPISODESEARCH);
        matcher.addURI(authority, SeriesGuideContract.PATH_EPISODESEARCH + "/*", EPISODESEARCH_ID);

        // Suggestions
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

        // Ops
        matcher.addURI(authority, SeriesGuideContract.PATH_RENEWFTSTABLE, RENEW_FTSTABLE);

        return matcher;
    }

    private SeriesGuideDatabase mOpenHelper;

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        sUriMatcher = buildUriMatcher(context);
        mOpenHelper = new SeriesGuideDatabase(context);
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(mImportListener);
        return true;
    }

    final OnSharedPreferenceChangeListener mImportListener
            = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equalsIgnoreCase(SeriesGuidePreferences.KEY_DATABASEIMPORTED)) {
                if (sharedPreferences
                        .getBoolean(SeriesGuidePreferences.KEY_DATABASEIMPORTED, false)) {
                    mOpenHelper.close();
                    sharedPreferences.edit()
                            .putBoolean(SeriesGuidePreferences.KEY_DATABASEIMPORTED, false)
                            .commit();
                }
            }
        }
    };

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SHOWS:
            case SHOWS_FILTERED:
            case SHOWS_WITH_EPISODE:
                return Shows.CONTENT_TYPE;
            case SHOWS_ID:
                return Shows.CONTENT_ITEM_TYPE;
            case EPISODES:
            case EPISODES_OFSHOW:
            case EPISODES_OFSEASON:
            case EPISODES_OFSEASON_WITHSHOW:
            case EPISODES_WITHSHOW:
                return Episodes.CONTENT_TYPE;
            case EPISODES_ID:
            case EPISODES_ID_WITHSHOW:
                return Episodes.CONTENT_ITEM_TYPE;
            case SEASONS:
            case SEASONS_OFSHOW:
                return Seasons.CONTENT_TYPE;
            case SEASONS_ID:
                return Seasons.CONTENT_ITEM_TYPE;
            case LISTS:
            case LISTS_WITH_LIST_ITEM_ID:
                return Lists.CONTENT_TYPE;
            case LISTS_ID:
                return Lists.CONTENT_ITEM_TYPE;
            case LIST_ITEMS:
            case LIST_ITEMS_WITH_DETAILS:
                return ListItems.CONTENT_TYPE;
            case LIST_ITEMS_ID:
                return ListItems.CONTENT_ITEM_TYPE;
            case MOVIES:
                return Movies.CONTENT_TYPE;
            case MOVIES_ID:
                return Movies.CONTENT_ITEM_TYPE;
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case RENEW_FTSTABLE:
                return Episodes.CONTENT_TYPE; // however there is nothing returned
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (LOGV) {
            Timber.v("insert(uri=" + uri + ", values=" + values.toString() + ")");
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SHOWS: {
                insertAndNotify(db, uri, Tables.SHOWS, values);
                return Shows.buildShowUri(values.getAsString(Shows._ID));
            }
            case SEASONS: {
                insertAndNotify(db, uri, Tables.SEASONS, values);
                return Seasons.buildSeasonUri(values.getAsString(Seasons._ID));
            }
            case EPISODES: {
                long id = insertAndNotify(db, uri, Tables.EPISODES, values);
                return Lists.buildListUri(String.valueOf(id));
            }
            case LISTS: {
                insertAndNotify(db, uri, Tables.LISTS, values);
                return Lists.buildListUri(values.getAsString(Lists.LIST_ID));
            }
            case LIST_ITEMS: {
                insertAndNotify(db, uri, Tables.LIST_ITEMS, values);
                return ListItems.buildListItemUri(values.getAsString(ListItems.LIST_ITEM_ID));
            }
            case MOVIES: {
                insertAndNotify(db, uri, Tables.MOVIES, values);
                return Movies.buildMovieUri(values.getAsInteger(Movies.TMDB_ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private long insertAndNotify(SQLiteDatabase db, Uri uri, String table, ContentValues values) {
        long rowId = db.insertOrThrow(table, null, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return rowId;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (LOGV) {
            Timber.v("query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        }
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case RENEW_FTSTABLE: {
                SeriesGuideDatabase.onRenewFTSTable(db);
                return null;
            }
            case EPISODESEARCH: {
                if (selectionArgs == null) {
                    throw new IllegalArgumentException(
                            "selectionArgs must be provided for the Uri: " + uri);
                }
                return SeriesGuideDatabase.search(selection, selectionArgs, db);
            }
            case SEARCH_SUGGEST: {
                if (selectionArgs == null) {
                    throw new IllegalArgumentException(
                            "selectionArgs must be provided for the Uri: " + uri);
                }
                return SeriesGuideDatabase.getSuggestions(selectionArgs[0], db);
            }
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildSelection(uri, match);
                Cursor query = builder.where(selection, selectionArgs).query(db, projection,
                        sortOrder);
                query.setNotificationUri(getContext().getContentResolver(), uri);
                return query;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (LOGV) {
            Timber.v("update(uri=" + uri + ", values=" + values.toString() + ")");
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSelection(uri, sUriMatcher.match(uri));
        int retVal = builder.where(selection, selectionArgs).update(db, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (LOGV) {
            Timber.v("delete(uri=" + uri + ")");
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSelection(uri, sUriMatcher.match(uri));
        int retVal = builder.where(selection, selectionArgs).delete(db);
        getContext().getContentResolver().notifyChange(uri, null);
        return retVal;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside a {@link
     * SQLiteDatabase} transaction. All changes will be rolled back if any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
                db.yieldIfContendedSafely();
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        /*
         * A more efficient version of bulkInsert which matches the URI only
         * once.
         */
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SHOWS: {
                return bulkInsertHelper(uri, Tables.SHOWS, values);
            }
            case SEASONS: {
                return bulkInsertHelper(uri, Tables.SEASONS, values);
            }
            case EPISODES: {
                return bulkInsertHelper(uri, Tables.EPISODES, values);
            }
            case LISTS: {
                return bulkInsertHelper(uri, Tables.LISTS, values);
            }
            case LIST_ITEMS: {
                return bulkInsertHelper(uri, Tables.LIST_ITEMS, values);
            }
            case MOVIES: {
                return bulkInsertHelper(uri, Tables.MOVIES, values);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private int bulkInsertHelper(Uri uri, String table, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;

            for (int i = 0; i < numValues; i++) {
                db.insertOrThrow(table, null, values[i]);
                db.yieldIfContendedSafely();
            }
            db.setTransactionSuccessful();

            getContext().getContentResolver().notifyChange(uri, null);

            return numValues;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Builds selection using a {@link SelectionBuilder} to match the requested {@link Uri}.
     */
    private static SelectionBuilder buildSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch (match) {
            case SHOWS: {
                return builder.table(Tables.SHOWS);
            }
            case SHOWS_ID: {
                final String showId = Shows.getShowId(uri);
                return builder.table(Tables.SHOWS).where(Shows._ID + "=?", showId);
            }
            case SHOWS_FILTERED: {
                final String filter = uri.getLastPathSegment();
                return builder.table(Tables.SHOWS).where(Shows.TITLE + " LIKE ?",
                        "%" + filter + "%");
            }
            case SHOWS_WITH_EPISODE: {
                return builder.table(Tables.SHOWS_JOIN_EPISODES)
                        .mapToTable(Shows._ID, Tables.SHOWS)
                        .mapToTable(Shows.RATING, Tables.SHOWS);
            }
            case EPISODES: {
                return builder.table(Tables.EPISODES);
            }
            case EPISODES_ID: {
                final String episodeId = Episodes.getEpisodeId(uri);
                return builder.table(Tables.EPISODES).where(Episodes._ID + "=?", episodeId);
            }
            case EPISODES_OFSHOW: {
                final String showId = uri.getPathSegments().get(2);
                return builder.table(Tables.EPISODES).where(Shows.REF_SHOW_ID + "=?", showId);
            }
            case EPISODES_OFSEASON: {
                final String seasonId = uri.getPathSegments().get(2);
                return builder.table(Tables.EPISODES).where(Seasons.REF_SEASON_ID + "=?", seasonId);
            }
            case EPISODES_OFSEASON_WITHSHOW: {
                final String seasonId = uri.getPathSegments().get(3);
                return builder.table(Tables.EPISODES_JOIN_SHOWS)
                        .mapToTable(Episodes._ID, Tables.EPISODES)
                        .mapToTable(Episodes.RATING, Tables.EPISODES)
                        .where(Seasons.REF_SEASON_ID + "=?", seasonId);
            }
            case EPISODES_WITHSHOW: {
                return builder.table(Tables.EPISODES_JOIN_SHOWS)
                        .mapToTable(Episodes._ID, Tables.EPISODES)
                        .mapToTable(Episodes.RATING, Tables.EPISODES);
            }
            case EPISODES_ID_WITHSHOW: {
                final String episodeId = Episodes.getEpisodeId(uri);
                return builder.table(Tables.EPISODES_JOIN_SHOWS)
                        .mapToTable(Episodes._ID, Tables.EPISODES)
                        .mapToTable(Episodes.RATING, Tables.EPISODES)
                        .where(Qualified.EPISODES_EPISODE_ID + "=?", episodeId);
            }
            case SEASONS: {
                return builder.table(Tables.SEASONS);
            }
            case SEASONS_ID: {
                final String seasonId = Seasons.getSeasonId(uri);
                return builder.table(Tables.SEASONS).where(Seasons._ID + "=?", seasonId);
            }
            case SEASONS_OFSHOW: {
                final String showId = uri.getPathSegments().get(2);
                return builder.table(Tables.SEASONS).where(Shows.REF_SHOW_ID + "=?", showId);
            }
            case EPISODESEARCH_ID: {
                final String rowid = EpisodeSearch.getDocId(uri);
                return builder.table(Tables.EPISODES_SEARCH).where(EpisodeSearch._DOCID + "=?",
                        rowid);
            }
            case LISTS: {
                return builder.table(Tables.LISTS);
            }
            case LISTS_ID: {
                final String listId = Lists.getId(uri);
                return builder.table(Tables.LISTS).where(Lists.LIST_ID + "=?", listId);
            }
            case LISTS_WITH_LIST_ITEM_ID: {
                final String itemId = uri.getPathSegments().get(2);
                return builder
                        .table(Tables.LISTS
                                + " LEFT OUTER JOIN (" + SubQuery.LISTS_LIST_ITEM_ID
                                + "'" + itemId
                                + "%') AS " + Tables.LIST_ITEMS + " ON "
                                + Qualified.LISTS_LIST_ID + "=" + Qualified.LIST_ITEMS_LIST_ID)
                        .mapToTable(Lists._ID, Tables.LISTS);
            }
            case LIST_ITEMS: {
                return builder.table(Tables.LIST_ITEMS);
            }
            case LIST_ITEMS_ID: {
                final String list_item_id = ListItems.getId(uri);
                return builder.table(Tables.LIST_ITEMS).where(ListItems.LIST_ITEM_ID + "=?",
                        list_item_id);
            }
            case LIST_ITEMS_WITH_DETAILS: {
                return builder.table(Tables.LIST_ITEMS_WITH_DETAILS);
            }
            case MOVIES: {
                return builder.table(Tables.MOVIES);
            }
            case MOVIES_ID: {
                final String movieId = Movies.getId(uri);
                return builder.table(Tables.MOVIES).where(Movies.TMDB_ID + "=?", movieId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    private interface SubQuery {

        String LISTS_LIST_ITEM_ID = "SELECT * FROM "
                + Tables.LIST_ITEMS + " WHERE "
                + ListItems.LIST_ITEM_ID + " LIKE ";
    }

    /**
     * {@link SeriesGuideContract} fields that are fully qualified with a specific parent {@link
     * Tables}. Used when needed to work around SQL ambiguity.
     */
    private interface Qualified {

        String EPISODES_EPISODE_ID = Tables.EPISODES + "." + Episodes._ID;

        String LISTS_LIST_ID = Tables.LISTS + "." + Lists.LIST_ID;

        String LIST_ITEMS_LIST_ID = Tables.LIST_ITEMS + "." + Lists.LIST_ID;
    }
}