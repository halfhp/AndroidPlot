/*
 * Copyright 2016 AndroidPlot.com
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

import android.graphics.*;
import com.androidplot.exception.PlotRenderException;
import com.androidplot.ui.RenderStack;
import com.androidplot.ui.SeriesRenderer;
import com.androidplot.util.ValPixConverter;

import java.util.*;

/**
 * A faster implementation of of {@link LineAndPointRenderer}.  For performance reasons, has these constraints:
 * - Interpolation is not supported
 * - Does not draw fill
 * @since 1.2.0
 */
public class FastLineAndPointRenderer extends XYSeriesRenderer<XYSeries, FastLineAndPointRenderer.Formatter> {

    private float[] points;
    List<Integer> segmentOffsets = new ArrayList<>();
    List<Integer> segmentLengths = new ArrayList<>();
    public FastLineAndPointRenderer(XYPlot plot) {
        super(plot);
    }

    @Override
    protected void onRender(Canvas canvas, RectF plotArea, XYSeries series, Formatter formatter, RenderStack stack) throws PlotRenderException {

        segmentOffsets.clear();
        segmentLengths.clear();

        final int numPoints = series.size() * 2;
        if(points == null || points.length != numPoints) {
            // only allocate when necessary:
            points = new  float[series.size()*2];
        }

        final Number minX = getPlot().getCalculatedMinX();
        final Number maxX = getPlot().getCalculatedMaxX();
        final Number minY = getPlot().getCalculatedMinY();
        final Number maxY = getPlot().getCalculatedMaxY();

        int segmentLen = 0;
        boolean isLastPointNull = true;
        for (int i = 0, j = 0;  i < series.size(); i++, j+=2) {
            Number y = series.getY(i);
            Number x = series.getX(i);

            PointF thisPoint;
            if (y != null && x != null) {
                if(isLastPointNull) {
                    segmentOffsets.add(j);
                    segmentLen = 0;
                    isLastPointNull = false;
                }

                thisPoint = ValPixConverter.valToPix(
                        x, y,
                        plotArea,
                        minX, maxX,
                        minY, maxY);
                points[j] = thisPoint.x;
                points[j+1] = thisPoint.y;
                segmentLen+=2;

                // if this is the last point, account for it in segment lengths:
                if(i == series.size()-1) {
                    segmentLengths.add(segmentLen);
                }
            } else if(!isLastPointNull) {
                segmentLengths.add(segmentLen);
                isLastPointNull = true;
            }
        }

        // draw segments
        if(formatter.linePaint != null || formatter.vertexPaint != null) {
            for (int i = 0; i < segmentOffsets.size(); i++) {
                final int len = segmentLengths.get(i);
                final int offset = segmentOffsets.get(i);
                drawSegment(canvas, points, offset, len, formatter);

//                if(formatter.vertexPaint != null) {
//                    // draw vertices:
//                    canvas.drawPoints(points, offset, len, formatter.vertexPaint);
//                }
            }
        }
    }

    protected void drawSegment(Canvas canvas, float[] points, int offset, int len, Formatter formatter) {
        if(formatter.linePaint != null) {
            // draw lines:
            if (len >= 4) {
                // optimization to avoid using 2x storage space to represent the full path:
                if ((len & 2) != 0) {
                    canvas.drawLines(points, offset, len - 2, formatter.linePaint);
                    canvas.drawLines(points, offset + 2, len - 2, formatter.linePaint);
                } else {
                    canvas.drawLines(points, offset, len, formatter.linePaint);
                    canvas.drawLines(points, offset + 2, len - 4, formatter.linePaint);
                }
            }
        }

        if(formatter.vertexPaint != null) {
            // draw vertices:
            canvas.drawPoints(points, offset, len, formatter.vertexPaint);
        }
    }

    @Override
    protected void doDrawLegendIcon(Canvas canvas, RectF rect, Formatter formatter) {
        if(formatter.getLinePaint() != null) {
            canvas.drawLine(rect.left, rect.bottom, rect.right, rect.top, formatter.getLinePaint());
        }
    }

    /**
     * Formatter designed to work in tandem with {@link AdvancedLineAndPointRenderer}.
     * @since 0.9.9
     */
    public static class Formatter extends LineAndPointFormatter {

        public Formatter(Integer lineColor, Integer vertexColor, Integer fillColor, PointLabelFormatter plf) {
            super(lineColor, vertexColor, fillColor, plf);
        }

        public Formatter(Integer lineColor, Integer vertexColor,
                Integer fillColor, PointLabelFormatter plf, FillDirection fillDir) {
            super(lineColor, vertexColor, fillColor, plf, fillDir);
        }

        @Override
        protected void initLinePaint(Integer lineColor) {
            super.initLinePaint(lineColor);

            // disable anti-aliasing by default:
            getLinePaint().setAntiAlias(false);
        }

        @Override
        public Class<? extends SeriesRenderer> getRendererClass() {
            return FastLineAndPointRenderer.class;
        }

        @Override
        public SeriesRenderer getRendererInstance(XYPlot plot) {
            return new FastLineAndPointRenderer(plot);
        }
    }
}
