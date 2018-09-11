package inc.ahmedmourad.inventorial.model.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import inc.ahmedmourad.inventorial.model.database.InventorialContract.SuppliersEntry;
import inc.ahmedmourad.inventorial.model.database.InventorialContract.ProductsEntry;

public class InventorialProvider extends ContentProvider {

	private static final UriMatcher matcher = buildUriMatcher();
	private static InventorialDbHelper dbHelper;
	private SQLiteDatabase writableDatabase;

	private static final int SUPPLIERS = 100;
	private static final int PRODUCTS = 101;
	private static final int PRODUCT_SUPPLIER_PAIRS = 102;

	private static final int SINGLE_PRODUCT_SUPPLIER_PAIR = 200;

	private static final int PRODUCT_SUPPLIER_PAIRS_BY_SUPPLIER_ID = 300;

	private static final SQLiteQueryBuilder productsQueryBuilder = new SQLiteQueryBuilder();

	static {
		productsQueryBuilder.setTables(ProductsEntry.TABLE_NAME +
				" INNER JOIN " +
				SuppliersEntry.TABLE_NAME +
				" ON " +
				ProductsEntry.TABLE_NAME + "." + ProductsEntry.COLUMN_SUPPLIER_ID +
				" = " +
				SuppliersEntry.TABLE_NAME + "." + SuppliersEntry.COLUMN_ID
		);
	}

	private static UriMatcher buildUriMatcher() {

		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = InventorialContract.CONTENT_AUTHORITY;

		matcher.addURI(authority, InventorialContract.PATH_SUPPLIERS, SUPPLIERS);
		matcher.addURI(authority, InventorialContract.PATH_PRODUCTS, PRODUCTS);

		matcher.addURI(authority, InventorialContract.PATH_PRODUCTS + "/" + ProductsEntry.PATH_PAIRS, PRODUCT_SUPPLIER_PAIRS);

		matcher.addURI(authority, InventorialContract.PATH_PRODUCTS + "/" + ProductsEntry.PATH_PAIRS + "/#", SINGLE_PRODUCT_SUPPLIER_PAIR);

		matcher.addURI(authority, InventorialContract.PATH_PRODUCTS + "/" + ProductsEntry.PATH_PAIRS + "/" + ProductsEntry.COLUMN_SUPPLIER_ID + "/#", PRODUCT_SUPPLIER_PAIRS_BY_SUPPLIER_ID);

		return matcher;
	}

	@Override
	public boolean onCreate() {

		if (dbHelper == null && getContext() != null)
			dbHelper = InventorialDbHelper.getInstance(getContext());

		writableDatabase = dbHelper.getWritableDatabase();

		return true;
	}

	@Override
	public String getType(@NonNull final Uri uri) {

		final int match = matcher.match(uri);

		switch (match) {

			case SUPPLIERS:
				return SuppliersEntry.CONTENT_TYPE;

			case PRODUCTS:
				return SuppliersEntry.CONTENT_TYPE;

			case PRODUCT_SUPPLIER_PAIRS:
				return ProductsEntry.CONTENT_TYPE;

			case PRODUCT_SUPPLIER_PAIRS_BY_SUPPLIER_ID:
				return ProductsEntry.CONTENT_TYPE;

			case SINGLE_PRODUCT_SUPPLIER_PAIR:
				return ProductsEntry.CONTENT_ITEM_TYPE;

			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	@NonNull
	private Cursor getAllSuppliers(@Nullable final String sortOrder) {

		final String[] projection = {SuppliersEntry.COLUMN_ID,
				SuppliersEntry.COLUMN_NAME,
				SuppliersEntry.COLUMN_PHONE_NUMBER
		};

		return dbHelper.getReadableDatabase().query(SuppliersEntry.TABLE_NAME,
				projection,
				null,
				null,
				null,
				null,
				sortOrder
		);
	}

	@NonNull
	private Cursor getAllPairs() {

		final String[] projection = {ProductsEntry.COLUMN_ID,
				ProductsEntry.COLUMN_NAME,
				ProductsEntry.COLUMN_PRICE,
				ProductsEntry.COLUMN_QUANTITY,
				ProductsEntry.COLUMN_IMAGE,
				SuppliersEntry.COLUMN_ID,
				SuppliersEntry.COLUMN_NAME,
				SuppliersEntry.COLUMN_PHONE_NUMBER
		};

		return productsQueryBuilder.query(dbHelper.getReadableDatabase(),
				projection,
				null,
				null,
				null,
				null,
				ProductsEntry.TABLE_NAME + "." + ProductsEntry.COLUMN_ID + " DESC"
		);
	}

	@NonNull
	private Cursor getAllPairs(final long supplierId) {

		final String[] projection = {ProductsEntry.COLUMN_ID,
				ProductsEntry.COLUMN_NAME,
				ProductsEntry.COLUMN_PRICE,
				ProductsEntry.COLUMN_QUANTITY,
				ProductsEntry.COLUMN_IMAGE,
				SuppliersEntry.COLUMN_ID,
				SuppliersEntry.COLUMN_NAME,
				SuppliersEntry.COLUMN_PHONE_NUMBER
		};

		return productsQueryBuilder.query(dbHelper.getReadableDatabase(),
				projection,
				ProductsEntry.TABLE_NAME + "." + ProductsEntry.COLUMN_SUPPLIER_ID + " = ?",
				new String[]{Long.toString(supplierId)},
				null,
				null,
				ProductsEntry.TABLE_NAME + "." + ProductsEntry.COLUMN_ID + " DESC"
		);
	}

	@NonNull
	private Cursor getPair(final long productId) {

		final String[] projection = {ProductsEntry.COLUMN_NAME,
				ProductsEntry.COLUMN_PRICE,
				ProductsEntry.COLUMN_QUANTITY,
				ProductsEntry.COLUMN_IMAGE,
				SuppliersEntry.COLUMN_ID,
				SuppliersEntry.COLUMN_NAME,
				SuppliersEntry.COLUMN_PHONE_NUMBER
		};

		return productsQueryBuilder.query(dbHelper.getReadableDatabase(),
				projection,
				ProductsEntry.TABLE_NAME + "." + ProductsEntry.COLUMN_ID + " = ?",
				new String[]{Long.toString(productId)},
				null,
				null,
				null
		);
	}

	@Override
	public Cursor query(@NonNull final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {

		final Cursor cursor;

		switch (matcher.match(uri)) {

			case SUPPLIERS: {
				cursor = getAllSuppliers(sortOrder);
				break;
			}

			case PRODUCT_SUPPLIER_PAIRS: {
				cursor = getAllPairs();
				break;
			}

			case SINGLE_PRODUCT_SUPPLIER_PAIR: {
				cursor = getPair(ProductsEntry.getProductIdFromPairUri(uri));
				break;
			}

			case PRODUCT_SUPPLIER_PAIRS_BY_SUPPLIER_ID: {
				cursor = getAllPairs(ProductsEntry.getSupplierIdFromPairUri(uri));
				break;
			}

			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		if (getContext() != null)
			cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	private int getSupplierId(@NonNull final String name) {

		final Cursor cursor = writableDatabase.query(SuppliersEntry.TABLE_NAME,
				new String[]{SuppliersEntry.COLUMN_ID},
				SuppliersEntry.COLUMN_NAME + " = ?",
				new String[]{name},
				null,
				null,
				null
		);

		cursor.moveToFirst();

		final int id = cursor.getInt(0);

		cursor.close();

		return id;
	}

	@Override
	public Uri insert(@NonNull final Uri uri, final ContentValues values) {

		final Uri returnUri;

		switch (matcher.match(uri)) {

			case SUPPLIERS: {

				// Since we have foreign keys associated with our supplier ids, we don't want our record
				// replaced with a new one every time we reinsert the supplier, so since this version of SQLite
				// doesn't support ON CONFLICT UPDATE, we take this approach instead.

				final String name = values.getAsString(SuppliersEntry.COLUMN_NAME);

				final int rowsUpdated = update(uri,
						values,
						SuppliersEntry.COLUMN_NAME + " = ?",
						new String[]{name}
				);

				final long id;

				if (rowsUpdated > 0)
					id = getSupplierId(name);
				else
					id = writableDatabase.insertWithOnConflict(SuppliersEntry.TABLE_NAME,
							null,
							values,
							SQLiteDatabase.CONFLICT_ABORT
					);

				if (id > 0)
					returnUri = SuppliersEntry.buildSupplierUriWithId(id);
				else
					throw new SQLException("Failed to insert row into " + uri);

				break;
			}

			case PRODUCTS: {

				final long id = writableDatabase.insertWithOnConflict(ProductsEntry.TABLE_NAME,
						null,
						values,
						SQLiteDatabase.CONFLICT_REPLACE
				);

				if (id > 0)
					returnUri = ProductsEntry.buildProductUriWithId(id);
				else
					throw new SQLException("Failed to insert row into " + uri);

				break;
			}

			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		if (getContext() != null)
			getContext().getContentResolver().notifyChange(uri, null);

		return returnUri;
	}

	@Override
	public int update(@NonNull final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {

		final int rowsUpdated;

		switch (matcher.match(uri)) {

			case SUPPLIERS: {
				rowsUpdated = writableDatabase.update(SuppliersEntry.TABLE_NAME, values, selection, selectionArgs);
				break;
			}

			case PRODUCTS: {
				rowsUpdated = writableDatabase.update(ProductsEntry.TABLE_NAME, values, selection, selectionArgs);
				break;
			}

			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		if (rowsUpdated != 0 && getContext() != null)
			getContext().getContentResolver().notifyChange(uri, null);

		return rowsUpdated;
	}

	@Override
	public int delete(@NonNull final Uri uri, String selection, final String[] selectionArgs) {

		final int rowsDeleted;

		if (selection == null)
			selection = "1";

		switch (matcher.match(uri)) {

			case SUPPLIERS: {
				rowsDeleted = writableDatabase.delete(SuppliersEntry.TABLE_NAME, selection, selectionArgs);
				break;
			}

			case PRODUCTS: {
				rowsDeleted = writableDatabase.delete(ProductsEntry.TABLE_NAME, selection, selectionArgs);
				break;
			}

			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}

		if (rowsDeleted != 0 && getContext() != null)
			getContext().getContentResolver().notifyChange(uri, null);

		return rowsDeleted;
	}

	@Override
	public int bulkInsert(@NonNull final Uri uri, @NonNull final ContentValues[] values) {

		int count = 0;

		switch (matcher.match(uri)) {

			case SUPPLIERS: {

				writableDatabase.beginTransaction();

				for (final ContentValues value : values) {

					final long id = SuppliersEntry.getSupplierIdFromUri(insert(uri, value));

					if (id != -1)
						count++;
				}

				break;
			}

			case PRODUCTS: {

				writableDatabase.beginTransaction();

				for (final ContentValues value : values) {

					final long id = ProductsEntry.getProductIdFromUri(insert(uri, value));

					if (id != -1)
						count++;
				}

				break;
			}

			default:
				return super.bulkInsert(uri, values);
		}

		writableDatabase.setTransactionSuccessful();
		writableDatabase.endTransaction();

		if (getContext() != null && count != 0)
			getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}
}
