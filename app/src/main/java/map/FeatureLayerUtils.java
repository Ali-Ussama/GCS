package map;

import com.esri.core.map.FeatureType;
import com.esri.core.map.Field;
import com.esri.core.map.Graphic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FeatureLayerUtils {

    private static boolean isFieldValidForEditing(Field field) {

        int fieldType = field.getFieldType();

        return field.isEditable() && fieldType != Field.esriFieldTypeOID
                && fieldType != Field.esriFieldTypeGeometry
                && fieldType != Field.esriFieldTypeBlob
                && fieldType != Field.esriFieldTypeRaster
                && fieldType != Field.esriFieldTypeGUID
                && fieldType != Field.esriFieldTypeXML;
    }

    public static boolean setAttribute(Map<String, Object> attrs, Graphic oldGraphic, Field field, String value) {

        boolean hasValueChanged = false;
        String oldValueForCheck;
        if (oldGraphic.getAttributeValue(field.getName()) == null || oldGraphic.getAttributeValue(field.getName()).equals("null") || oldGraphic.getAttributeValue(field.getName()).equals("")) {
            oldValueForCheck = "";
        } else {
            oldValueForCheck = String.valueOf(oldGraphic.getAttributeValue(field.getName())) ;
        }

        if (!value.equals(oldValueForCheck)) {

            if (FieldType.determineFieldType(field) == FieldType.STRING) {
                attrs.put(field.getName(), value);
                hasValueChanged = true;
            } else if (FieldType.determineFieldType(field) == FieldType.NUMBER) {
                if (value.equals("")) {
                    attrs.put(field.getName(), null);
                    hasValueChanged = true;
                } else {
                    int intValue = Integer.parseInt(value);
                    Object oldValue = oldGraphic.getAttributeValue(field.getName());
                    if (oldValue == null) {
                        attrs.put(field.getName(), intValue);
                        hasValueChanged = true;
                    } else {
                        if (intValue != Integer.parseInt(oldValue.toString())) {
                            attrs.put(field.getName(), intValue);
                            hasValueChanged = true;
                        }
                    }
                }
            } else if (FieldType.determineFieldType(field) == FieldType.DECIMAL) {
                if (value.equals("")) {
                    attrs.put(field.getName(), null);
                    hasValueChanged = true;
                } else {
                    double dValue = Double.parseDouble(value);
                    Object oldValue = oldGraphic.getAttributeValue(field.getName());
                    if (oldValue == null) {
                        attrs.put(field.getName(), dValue);
                        hasValueChanged = true;
                    } else {
                        if (dValue != Double.parseDouble(oldGraphic.getAttributeValue(field.getName()).toString())) {
                            attrs.put(field.getName(), dValue);
                            hasValueChanged = true;
                        }
                    }
                }
            }
        }
        return hasValueChanged;
    }

    public static String returnTypeIdFromTypeName(FeatureType[] types, String name) {

        for (FeatureType type : types) {
            if (type.getName().equals(name)) {
                return type.getId();
            }
        }
        return null;
    }

    public static int[] createArrayOfFieldIndexes(Field[] fields) {

        ArrayList<Integer> list = new ArrayList<>();
        int fieldCount = 0;

        for (int i = 0; i < fields.length; i++) {

            if (isFieldValidForEditing(fields[i])) {
                list.add(i);
                fieldCount++;
            }
        }

        int[] editableFieldIndexes = new int[fieldCount];

        for (int x = 0; x < list.size(); x++) {
            editableFieldIndexes[x] = list.get(x);
        }
        return editableFieldIndexes;
    }

    public static String[] createTypeNameArray(FeatureType[] types) {
        String[] typeNames = new String[types.length + 1];
        int i = 1;
        typeNames[0] = "أختار";
        for (FeatureType type : types) {

            typeNames[i] = type.getName();
            i++;
        }
        return typeNames;
    }

    public static HashMap<String, FeatureType> createTypeMapByValue(FeatureType[] types) {

        HashMap<String, FeatureType> typeMap = new HashMap<>();

        for (FeatureType type : types) {
            typeMap.put(type.getId(), type);
        }
        return typeMap;
    }

    public enum FieldType {
        NUMBER, STRING, DECIMAL, DATE;

        public static FieldType determineFieldType(Field field) {

            if (field.getFieldType() == Field.esriFieldTypeString) {
                return FieldType.STRING;
            } else if (field.getFieldType() == Field.esriFieldTypeSmallInteger || field.getFieldType() == Field.esriFieldTypeInteger) {
                return FieldType.NUMBER;
            } else if (field.getFieldType() == Field.esriFieldTypeSingle || field.getFieldType() == Field.esriFieldTypeDouble) {
                return FieldType.DECIMAL;
            } else if (field.getFieldType() == Field.esriFieldTypeDate) {
                return FieldType.DATE;
            }
            return null;
        }
    }
}
