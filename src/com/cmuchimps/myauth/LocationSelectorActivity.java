package com.cmuchimps.myauth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

public class LocationSelectorActivity extends MapActivity {
	private MapView mapView;
	private MapController mapController;
	private AutoCompleteTextView searchInput;
	private Button search;
	private Button submit;
	
	private LocationManager lm;
	private Geocoder gc;
	
	private GeoPoint point;
	private double selectedLat;
	private double selectedLong;
	private MyLocationOverlay me;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_selector);
		mapView = (MapView) findViewById(R.id.mapView);
		searchInput = (AutoCompleteTextView) findViewById(R.id.searchET);
		search = (Button) findViewById(R.id.searchButton);
		submit = (Button) findViewById(R.id.finSubButton);
		mapView.setBuiltInZoomControls(true);
		
		mapController = mapView.getController();
		mapController.setZoom(17);
		
		me = new MyLocationOverlay(this, mapView);
		mapView.getOverlays().add(me);
		
		gc = new Geocoder(getApplicationContext());
		
		lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		Location gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location wifi = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location currLoc;
		if (gps != null && wifi != null) {
			currLoc = (gps.getAccuracy() > wifi.getAccuracy() ? gps : wifi);
		} else {
			if (gps == null && wifi == null) {
				currLoc = new Location("CMU");
				currLoc.setLatitude(40.443746);
				currLoc.setLongitude(-79.941419);
			} else { //one of the two is null
				currLoc = (wifi == null ? gps : wifi);
			}
		}
		point = new GeoPoint((int) (currLoc.getLatitude() * 1E6),
						 (int) (currLoc.getLongitude() * 1E6));
		
		mapController.animateTo(point);
		
		PinOverlay mapOverlay = new PinOverlay();
		List<Overlay> overlays = mapView.getOverlays();
		overlays.clear();
		overlays.add(mapOverlay);
		
		// searchInput.addTextChangedListener(new SearchTextWatcher()); //UNCOMMENT for really slow autocomplete
	
		search.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String searchQuery = searchInput.getText().toString();
				if (!searchQuery.equalsIgnoreCase("")) {
					try {
						List<Address> allPossible = gc.getFromLocationName(searchQuery, 5);
						if (allPossible.size() > 0) {
							point = new GeoPoint(
									(int) (allPossible.get(0).getLatitude() * 1E6),
									(int) (allPossible.get(0).getLongitude() * 1E6));
							selectedLat = allPossible.get(0).getLatitude();
							selectedLong = allPossible.get(0).getLongitude();
							mapController.animateTo(point);
							
							mapView.invalidate();
							searchInput.setText("");
						} else {
							Toast.makeText(getApplicationContext(), 
									"We could not find that address. Please find the location on the map manually", 
									Toast.LENGTH_SHORT).show();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					Toast.makeText(getApplicationContext(), 
							"Please enter a search query...", 
							Toast.LENGTH_SHORT).show();
				}
			}
			
		});
		
		submit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//return to main activity if good
				Intent resultIntent = new Intent();
				resultIntent.putExtra("latitude", selectedLat);
				resultIntent.putExtra("longitude", selectedLong);
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
			}
		});
		
		mapView.invalidate();
	}
	
	
	@Override
    public void onBackPressed() {
		Intent resultIntent = new Intent();
		resultIntent.putExtra("latitude", selectedLat);
		resultIntent.putExtra("longitude", selectedLong);
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	class PinOverlay extends Overlay {
		private boolean isPinch = false;
		
		@Override
		public boolean draw(Canvas canvas, MapView mapView,
				boolean shadow, long when) {
			super.draw(canvas, mapView, shadow);
			
			Point screenPts = new Point();
			mapView.getProjection().toPixels(point, screenPts);
			
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pin);
			canvas.drawBitmap(bmp, screenPts.x, screenPts.y-20, null);
			
			return true;
		}
		
		@Override
		public boolean onTap(GeoPoint p, MapView mapView) {
			if (isPinch || p == null) {
				return false;
			} else {
				point = p;
				selectedLat = p.getLatitudeE6() / 1E6;
				selectedLong = p.getLongitudeE6() / 1E6;
				return true;
			}
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent e, MapView mapView)
		{
		    int fingers = e.getPointerCount();
		    if( e.getAction()==MotionEvent.ACTION_DOWN ){
		        isPinch=false;  // Touch DOWN, don't know if it's a pinch yet
		    }
		    if( e.getAction()==MotionEvent.ACTION_MOVE && fingers==2 ){
		        isPinch=true;   // Two fingers, def a pinch
		    }
		    return super.onTouchEvent(e,mapView);
		}
	}
	
	private class SearchTextWatcher implements TextWatcher {
		@Override
		public void afterTextChanged(Editable arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// TODO Auto-generated method stub
			// TODO Auto-generated method stub
			String searchQuery = searchInput.getText().toString();
			System.out.println("getting here?");
			try {
				List<Address> allPossible = gc.getFromLocationName(searchQuery, 20);
				if (allPossible.size() > 0) {
					ArrayList<String> addresses = new ArrayList<String>(allPossible.size());
					for (Address a : allPossible)
						if (a.getFeatureName() != null) addresses.add(a.getFeatureName());
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(
							getApplicationContext(), android.R.layout.simple_dropdown_item_1line,
							addresses.toArray(new String[addresses.size()]));
					searchInput.setAdapter(adapter);
					searchInput.showDropDown();
				} else {
					searchInput.dismissDropDown();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
