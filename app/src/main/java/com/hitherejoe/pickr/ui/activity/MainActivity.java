package com.hitherejoe.pickr.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.hitherejoe.pickr.AndroidBoilerplateApplication;
import com.hitherejoe.pickr.R;
import com.hitherejoe.pickr.data.DataManager;
import com.hitherejoe.pickr.data.model.Location;
import com.hitherejoe.pickr.ui.adapter.LocationHolder;
import com.hitherejoe.pickr.util.DialogFactory;
import com.hitherejoe.pickr.util.SnackbarFactory;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;
import uk.co.ribot.easyadapter.EasyRecyclerAdapter;

public class MainActivity extends BaseActivity {

    @Bind(R.id.layout_main)
    CoordinatorLayout mLayoutRoot;

    @Bind(R.id.recycler_characters)
    RecyclerView mCharactersRecycler;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.progress_indicator)
    ProgressBar mProgressBar;

    int PLACE_PICKER_REQUEST = 1020;

    private DataManager mDataManager;
    private CompositeSubscription mSubscriptions;
    private EasyRecyclerAdapter<Location> mEasyRecycleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applicationComponent().inject(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mSubscriptions = new CompositeSubscription();
        mDataManager = AndroidBoilerplateApplication.get(this).getComponent().dataManager();
        setupToolbar();
        setupRecyclerView();
        loadLocations();

        startActivity(new Intent(this, SearchActivity.class));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                savePlace(place);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubscriptions.unsubscribe();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_github:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.fab_add_place)
    public void onAddPlaceCLick() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        List<String> ids = new ArrayList<>();
        ids.add(String.valueOf(Place.TYPE_BAR));
        ids.add(String.valueOf(Place.TYPE_FOOD));
        ids.add(String.valueOf(Place.TYPE_CAFE));
        ids.add(String.valueOf(Place.TYPE_BAKERY));

        PlaceFilter filter = new PlaceFilter(false, ids);
        Context context = getApplicationContext();
        try {
            startActivityForResult(builder.build(context), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
    }

    private void setupRecyclerView() {
        mCharactersRecycler.setLayoutManager(new LinearLayoutManager(this));
        mEasyRecycleAdapter = new EasyRecyclerAdapter<>(this, LocationHolder.class, mLocationListener);
        mCharactersRecycler.setAdapter(mEasyRecycleAdapter);
    }

    private void savePlace(Place place) {
        Location location = Location.fromPlace(place);
        mSubscriptions.add(mDataManager.saveLocation(location)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(mDataManager.getScheduler())
                .subscribe(new Subscriber<Location>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Location location) {

                    }
                }));
    }

    private void loadLocations() {
        mSubscriptions.add(mDataManager.getLocations()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(mDataManager.getScheduler())
                .subscribe(new Subscriber<Location>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable error) {
                        Timber.e("There was an error retrieving the characters " + error);
                        mProgressBar.setVisibility(View.GONE);
                        DialogFactory.createSimpleErrorDialog(MainActivity.this).show();
                    }

                    @Override
                    public void onNext(Location location) {
                        mProgressBar.setVisibility(View.GONE);
                        mEasyRecycleAdapter.addItem(location);
                    }
                }));
    }

    private void showDeleteDialog(final Location location) {
        DialogFactory.createSimpleYesNoErrorDialog(MainActivity.this, "Delete location", "Delete location?",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteLocation(location);
                    }
                }).show();
    }

    private void deleteLocation(Location location) {
        mSubscriptions.add(mDataManager.deleteLocation(location)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(mDataManager.getScheduler())
                .subscribe(new Subscriber<Location>() {
                    @Override
                    public void onCompleted() {
                        mEasyRecycleAdapter.notifyDataSetChanged();
                        SnackbarFactory.createSnackbar(
                                MainActivity.this, mLayoutRoot, "Location deleted").show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e("There was an error deleting the location " + e);
                    }

                    @Override
                    public void onNext(Location location) {

                    }
                }));
    }

    private LocationHolder.LocationListener mLocationListener = new LocationHolder.LocationListener() {
        @Override
        public void onLocationLongPress(Location location) {
            showDeleteDialog(location);
        }
    };

}
