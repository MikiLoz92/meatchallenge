package com.mikiloz.meat.utils;
// Created by Miguel Vera Belmonte on 04/02/2017.

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;

public class ScrollableMapView extends MapView {

    public ScrollableMapView(Context context) {
        super(context);
    }
    public ScrollableMapView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }
    public ScrollableMapView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }
    public ScrollableMapView(Context context, GoogleMapOptions googleMapOptions) {
        super(context, googleMapOptions);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Disallow ScrollView to intercept touch events.
                this.getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_UP:
                // Allow ScrollView to intercept touch events.
                this.getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }

        super.dispatchTouchEvent(ev);
        return true;
    }
}
