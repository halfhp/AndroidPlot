/*
 * Copyright 2015 AndroidPlot.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.androidplot.xy;

import android.content.res.*;
import android.graphics.*;
import android.view.*;

import com.androidplot.*;
import com.androidplot.Region;
import com.androidplot.test.*;
import com.androidplot.ui.*;

import org.junit.*;
import org.mockito.*;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class PanZoomTest extends AndroidplotTest {

    @Mock
    LayoutManager layoutManager;

    @Mock
    XYPlot xyPlot;

    @Mock
    TypedArray typedArray;

    @Mock
    SeriesRegistry seriesRegistry;

    RectRegion bounds = new RectRegion(0, 100, 0, 100);

    @Before
    public void setUp() throws Exception {
        when(xyPlot.getSeriesRegistry()).thenReturn(seriesRegistry);
        when(xyPlot.getBounds()).thenReturn(bounds);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testOnTouch_notifiesOnTouchListener() throws Exception {
        PanZoom panZoom = new PanZoom(xyPlot, PanZoom.Pan.BOTH, PanZoom.Zoom.SCALE);

        View.OnTouchListener listener = mock(View.OnTouchListener.class);
        panZoom.setDelegate(listener);

        MotionEvent motionEvent = mock(MotionEvent.class);
        panZoom.onTouch(xyPlot, motionEvent);

        verify(listener, times(1)).onTouch(xyPlot, motionEvent);
    }

    @Test
    public void testOnTouch_oneFingerMovePansButDoesNotZoom() throws Exception {
        PanZoom panZoom = spy(new PanZoom(xyPlot, PanZoom.Pan.BOTH, PanZoom.Zoom.SCALE));

        View.OnTouchListener listener = mock(View.OnTouchListener.class);
        panZoom.setDelegate(listener);

        MotionEvent moveEvent = mock(MotionEvent.class);

        doNothing().when(panZoom).calculatePan(
                any(PointF.class), any(Region.class), anyBoolean());

        when(moveEvent.getAction())
                .thenReturn(MotionEvent.ACTION_DOWN)
                .thenReturn(MotionEvent.ACTION_MOVE)
                .thenReturn(MotionEvent.ACTION_UP);

        panZoom.onTouch(xyPlot, moveEvent); // fires ACTION_DOWN
        panZoom.onTouch(xyPlot, moveEvent); // fires ACTION_MOVE
        panZoom.onTouch(xyPlot, moveEvent); // fires ACTION_UP

        verify(panZoom).pan(moveEvent);
        verify(panZoom, never()).zoom(moveEvent);
        verify(panZoom).reset();
    }

    @Test
    public void testOnTouch_twoFingersZoom() throws Exception {
        PanZoom panZoom = spy(new PanZoom(xyPlot, PanZoom.Pan.BOTH, PanZoom.Zoom.SCALE));

        View.OnTouchListener listener = mock(View.OnTouchListener.class);
        panZoom.setDelegate(listener);

        MotionEvent moveEvent = mock(MotionEvent.class);

        doNothing().when(panZoom).calculatePan(
                any(PointF.class), any(Region.class), anyBoolean());

        when(moveEvent.getAction())
                .thenReturn(MotionEvent.ACTION_DOWN)
                .thenReturn(MotionEvent.ACTION_POINTER_DOWN)
                .thenReturn(MotionEvent.ACTION_MOVE)
                .thenReturn(MotionEvent.ACTION_UP);

        final float pinchDistance = PanZoom.MIN_DIST_2_FING + 1;

        doReturn(new RectF(0, 0, pinchDistance, pinchDistance))
                .when(panZoom).fingerDistance(any(MotionEvent.class));

        panZoom.onTouch(xyPlot, moveEvent); // ACTION_DOWN
        panZoom.onTouch(xyPlot, moveEvent); // ACTION_POINTER_DOWN
        panZoom.onTouch(xyPlot, moveEvent); // ACTION_MOVE
        panZoom.onTouch(xyPlot, moveEvent); // fires ACTION_UP

        verify(panZoom).zoom(moveEvent);
        verify(panZoom).reset();
    }

    @Test
    public void testFingerDistance() {
        PanZoom panZoom = spy(new PanZoom(xyPlot, PanZoom.Pan.BOTH, PanZoom.Zoom.SCALE));

        RectF distance = panZoom.fingerDistance(0, 0, 10, 10);
        assertEquals(0f, distance.left);
        assertEquals(0f, distance.top);
        assertEquals(10f, distance.right);
        assertEquals(10f, distance.bottom);

        // no matter what order the coords are supplied, make sure the same rect is calculated:
        distance = panZoom.fingerDistance(10, 10, 0, 0);
        assertEquals(0f, distance.left);
        assertEquals(0f, distance.top);
        assertEquals(10f, distance.right);
        assertEquals(10f, distance.bottom);
    }
}
