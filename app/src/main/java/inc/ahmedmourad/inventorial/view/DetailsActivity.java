package inc.ahmedmourad.inventorial.view;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.luseen.spacenavigation.SpaceItem;
import com.luseen.spacenavigation.SpaceNavigationView;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import inc.ahmedmourad.inventorial.R;
import inc.ahmedmourad.inventorial.defaults.DefaultSpaceOnClickListener;
import inc.ahmedmourad.inventorial.defaults.DefaultSpaceOnLongClickListener;
import inc.ahmedmourad.inventorial.model.database.InventorialDatabase;
import inc.ahmedmourad.inventorial.model.pojo.ProductSupplierPair;
import inc.ahmedmourad.inventorial.utils.BitmapUtils;
import inc.ahmedmourad.inventorial.utils.DialogUtils;
import inc.ahmedmourad.inventorial.utils.ErrorUtils;

public class DetailsActivity extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

	static final String KEY_PAIR = "d_pair";
	static final String KEY_PAIR_CAN_VIEW_SUPPLIER_PRODUCTS = "d_can_view_supplier_products";

	private static final int ID_LOADER = 2;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_toolbar)
	Toolbar toolbar;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_product_image)
	ImageView productImageView;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_product_price)
	TextView productPriceTextView;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_product_quantity)
	TextView productQuantityTextView;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_supplier_name)
	TextView supplierNameTextView;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_supplier_phone_number)
	TextView supplierPhoneNumberTextView;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_supplier_products_divider)
	View supplierProductsDivider;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_supplier_label)
	View supplierLabel;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_supplier_card)
	View supplierCard;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_supplier_products)
	Button supplierProductsButton;

	@SuppressWarnings("WeakerAccess")
	@BindView(R.id.details_space)
	SpaceNavigationView spaceView;

	private ProductSupplierPair pair;

	private Unbinder unbinder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_details);

		unbinder = ButterKnife.bind(this);

		setSupportActionBar(toolbar);

		if (getSupportActionBar() != null)
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final Intent intent = getIntent();

		if (intent != null) {

			if (intent.hasExtra(KEY_PAIR)) {
				pair = Parcels.unwrap(intent.getParcelableExtra(KEY_PAIR));
				setTitle(pair.getProduct().getName());
			} else {
				finish();
				ErrorUtils.general(this, null);
			}

			// Preventing inception
			if (intent.getBooleanExtra(KEY_PAIR_CAN_VIEW_SUPPLIER_PRODUCTS, true)) {
				supplierProductsButton.setVisibility(View.VISIBLE);
				supplierProductsDivider.setVisibility(View.VISIBLE);
			} else {
				supplierProductsButton.setVisibility(View.GONE);
				supplierProductsDivider.setVisibility(View.GONE);
			}
		}

		supplierProductsButton.setOnClickListener(this);

		initializeSpaceView();
		populateUi();

		getSupportLoaderManager().initLoader(ID_LOADER, null, this);
	}

	private void populateUi() {

		if (pair.getProduct().getQuantity() < 0)
			pair.getProduct().setQuantity(0);

		productPriceTextView.setText(getString(R.string.details_price, pair.getProduct().getPrice()));
		productImageView.setImageBitmap(BitmapUtils.fromByteArray(pair.getProduct().getImage()));

		displayQuantity();

		setSupplierVisible(pair.getSupplier().getName().trim().length() > 0 &&
				pair.getSupplier().getPhoneNumber().trim().length() > 0
		);

		supplierNameTextView.setText(pair.getSupplier().getName());
		supplierPhoneNumberTextView.setText(pair.getSupplier().getPhoneNumber());
	}

	private void setSupplierVisible(final boolean visible) {
		if (visible) {
			supplierLabel.setVisibility(View.VISIBLE);
			supplierCard.setVisibility(View.VISIBLE);
		} else {
			supplierLabel.setVisibility(View.GONE);
			supplierCard.setVisibility(View.GONE);
		}
	}

	private void displayQuantity() {
		productQuantityTextView.setText(getString(R.string.details_quantity, pair.getProduct().getQuantity()));
	}

	private void initializeSpaceView() {

		spaceView.setCentreButtonIcon(R.drawable.ic_edit);
		spaceView.setCentreButtonColor(ContextCompat.getColor(this, R.color.colorAccent));
		spaceView.setInActiveSpaceItemColor(Color.BLACK);
		spaceView.setActiveSpaceItemColor(Color.BLACK);
		spaceView.setSpaceBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
		spaceView.setSpaceItemTextSize(getResources().getDimensionPixelSize(R.dimen.space_text_size));

		spaceView.addSpaceItem(new SpaceItem(getString(R.string.increment), R.drawable.ic_up));
		spaceView.addSpaceItem(new SpaceItem(getString(R.string.decrement), R.drawable.ic_down));

		spaceView.setSpaceOnClickListener(new DefaultSpaceOnClickListener() {
			@Override
			public void onCentreButtonClick() {
				final Intent intent = new Intent(DetailsActivity.this, AddProductActivity.class);
				intent.putExtra(AddProductActivity.KEY_PAIR, Parcels.wrap(pair));
				startActivity(intent);
			}

			@Override
			public void onItemClick(int itemIndex, String itemName) {

				if (getString(R.string.increment).equals(itemName)) {

					increaseQuantity(1);

				} else if (getString(R.string.decrement).equals(itemName)) {

					if (pair.getProduct().getQuantity() < 1) {
						Toast.makeText(DetailsActivity.this, R.string.no_entities_left, Toast.LENGTH_LONG).show();
						return;
					}

					decreaseQuantity(1);
				}
			}
		});

		spaceView.setSpaceOnLongClickListener(new DefaultSpaceOnLongClickListener() {
			@Override
			public void onItemLongClick(int itemIndex, String itemName) {

				if (getString(R.string.increment).equals(itemName)) {

					DialogUtils.showNumberPickerDialog(DetailsActivity.this,
							getString(R.string.increase_quantity_by, pair.getProduct().getName()),
							R.string.increase,
							Integer.MAX_VALUE,
							DetailsActivity.this::increaseQuantity
					);

				} else if (getString(R.string.decrement).equals(itemName)) {

					if (pair.getProduct().getQuantity() < 1) {
						Toast.makeText(DetailsActivity.this, R.string.no_entities_left, Toast.LENGTH_LONG).show();
						return;
					}

					DialogUtils.showNumberPickerDialog(DetailsActivity.this,
							getString(R.string.decrease_quantity_by, pair.getProduct().getName()),
							R.string.decrease,
							(int) pair.getProduct().getQuantity(),
							DetailsActivity.this::decreaseQuantity
					);
				}
			}
		});
	}

	@SuppressWarnings("SameParameterValue")
	private void increaseQuantity(@IntRange(from = 1, to = Integer.MAX_VALUE) final int value) {
		pair.getProduct().setQuantity(pair.getProduct().getQuantity() + value);
		InventorialDatabase.getInstance().updateProduct(DetailsActivity.this,
				pair.getProduct().toContentValues(),
				pair.getProduct().getId()
		);
		displayQuantity();
	}

	@SuppressWarnings("SameParameterValue")
	private void decreaseQuantity(@IntRange(from = 1, to = Integer.MAX_VALUE) final int value) {

		pair.getProduct().setQuantity(pair.getProduct().getQuantity() - value);

		if (pair.getProduct().getQuantity() < 0)
			pair.getProduct().setQuantity(0);

		InventorialDatabase.getInstance().updateProduct(DetailsActivity.this,
				pair.getProduct().toContentValues(),
				pair.getProduct().getId()
		);

		displayQuantity();
	}

	private void deleteProduct() {
		DialogUtils.showDeleteProductConfirmationDialog(this, pair, (dialog, which) -> {
			InventorialDatabase.getInstance().deleteProduct(this, pair.getProduct().getId());
			finish();
			Toast.makeText(this, R.string.success, Toast.LENGTH_LONG).show();
		});
	}

	private void callNumber(@NonNull final String number) {
		final Intent intent = new Intent(Intent.ACTION_DIAL);
		intent.setData(Uri.parse("tel:" + number));
		if (intent.resolveActivity(getPackageManager()) != null)
			startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

			case R.id.details_action_delete:
				deleteProduct();
				return true;

			case R.id.details_action_order:
				callNumber(pair.getSupplier().getPhoneNumber());
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		unbinder.unbind();
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.details_supplier_products) {
			final Intent intent = new Intent(this, MainActivity.class);
			intent.putExtra(MainActivity.KEY_SUPPLIER_TO_DISPLAY, Parcels.wrap(pair.getSupplier()));
			startActivity(intent);
		}
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
		return InventorialDatabase.getInstance().getPair(this, pair.getProduct().getId());
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
		if (cursor.moveToFirst()) {
			pair = ProductSupplierPair.fromCursor(cursor);
			populateUi();
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {

	}
}
