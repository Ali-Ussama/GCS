package map.util;

import android.location.Location;

import com.esri.android.map.MapView;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;

public class MapUtilites {


    public static void showFullExtent(MapView mMapView) {
        Point centerPt = new Point(1293655.4932430403, 2288926.3523502923);
        mMapView.zoomTo(centerPt, 8);
    }

    public static void zoomToPoint(MapView mMapView, Geometry mapPoint) {
        try {
            mapPoint = GeometryEngine.project(mapPoint, SpatialReference.create(4326), mMapView.getSpatialReference());
//            if (mapPoint instanceof Point) {
//                Point pointToZoom = (Point) mapPoint;
//                int factor = 10;
//                Envelope stExtent = new Envelope(pointToZoom.getX() - factor, pointToZoom.getY() - factor, pointToZoom.getX() + factor, pointToZoom.getY() + factor);
//                mMapView.setExtent(stExtent, 10, true);
//            } else

            if (mapPoint instanceof MultiPath) {
                mMapView.setExtent(mapPoint, 20, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Point getMapPoint(MapView mMapView, Location location) {
        return new Point(location.getLongitude(), location.getLatitude());
//		return (Point) GeometryEngine.project(wgspoint, SpatialReference.create(4326), mMapView.getSpatialReference());
    }


}
