package data;

import com.esri.core.geometry.Geometry;

/**
 * Created by eslamelhoseiny on 10/4/17.
 */

public class Index {

    private Geometry geometry;
    private String sheetNumber;

    public Index(Geometry geometry, String sheetNumber) {
        this.geometry = geometry;
        this.sheetNumber = sheetNumber;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public String getSheetNumber() {
        return sheetNumber;
    }

    public void setSheetNumber(String sheetNumber) {
        this.sheetNumber = sheetNumber;
    }

    @Override
    public String toString() {
        return sheetNumber == null ? "Not Define" : sheetNumber;
    }
}
