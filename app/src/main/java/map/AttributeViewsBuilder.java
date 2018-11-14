package map;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.esri.core.map.CodedValueDomain;
import com.esri.core.map.FeatureType;
import com.esri.core.map.Field;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.gcs.riyadh.R;

import util.DataCollectionApplication;

public class AttributeViewsBuilder {

    public final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss aa", Locale.ENGLISH);
    private Field[] fields = new Field[0];
    private final FeatureType[] types;
    private final Context context;
    private final LayoutInflater lInflater;
    private final int[] editableFieldIndexes;
    public HashMap<String, Object> attributes;
    private AttributeItem[] items;
    private ArrayAdapter<String> domainAdapter;

    private String featureValue;
    public static HashMap<String, String> featureCodeValues;
    public static HashMap<String, String> featureTypeCodeValues;
    public static String selectedCode;
    private String[] featureNames;
    private Spinner featureSpinner;


    public AttributeViewsBuilder(Context context, Field[] layerFields, FeatureType[] types, String typeIdFieldName) {

        this.context = context;
        this.lInflater = LayoutInflater.from(context);
        this.fields = layerFields;
        this.types = types;
        this.editableFieldIndexes = FeatureLayerUtils.createArrayOfFieldIndexes(this.fields);
        featureNames = new String[0];
        featureCodeValues = new HashMap<>();
        this.items = new AttributeItem[AttributeViewsBuilder.this.editableFieldIndexes.length];
    }

    public int getCount() {
        return this.editableFieldIndexes.length;
    }

    public Object getItem(int position) {
        int fieldIndex = this.editableFieldIndexes[position];
        AttributeItem row;

        if (items[position] == null) {
            row = new AttributeItem();
            row.setField(this.fields[fieldIndex]);
            Object value = this.attributes.get(fields[fieldIndex].getName());
            row.setValue(value);
            items[position] = row;
        } else {
            row = items[position];
        }
        return row;
    }

    public View getView(int position, boolean isGCS) {

        View container = null;

        AttributeItem item = (AttributeItem) getItem(position);

        if (item.getField().getName().equals(ColumnNames.A_FEATURETYPE)) {
            container = lInflater.inflate(R.layout.item_spinner, null);
            String typeStringValue = null;
            if (item.getValue() != null && !item.getValue().toString().equals("0")) {
                typeStringValue = item.getValue().toString();
            }
            Spinner spinner = createSpinnerViewFromArray(container, item.getField(), typeStringValue, isGCS);
            item.setView(spinner);

        } else if (item.getField().getName().equals(ColumnNames.A_FEATURE)) {
            container = lInflater.inflate(R.layout.item_spinner, null);
            if (item.getValue() != null) {
                featureValue = item.getValue().toString();
                Spinner spinner = createDomainSpinnerView(container, item.getField(), isGCS);
                item.setView(spinner);
            } else {
                Spinner spinner = createDomainSpinnerView(container, item.getField(), isGCS);
                item.setView(spinner);
            }
        } else if (item.getField().getName().equals(ColumnNames.A_PROVINCE) || item.getField().getName().equals(ColumnNames.A_DATASOURCE) || item.getField().getName().equals(ColumnNames.A_NAMESTATUS)) {
            container = lInflater.inflate(R.layout.item_spinner, null);
            if (item.getValue() != null) {
                String value = item.getValue().toString();
                Spinner spinner = createDomainSpinner(container, item.getField(), value, isGCS);
                item.setView(spinner);
            } else {
                Spinner spinner = createDomainSpinner(container, item.getField(), null, isGCS);
                item.setView(spinner);
            }
        } else if (item.getField().getName().equals(ColumnNames.ADMIN_NOTES)) {
            container = lInflater.inflate(R.layout.item_admin_notes, null);
            if (item.getValue() != null && !item.getValue().toString().isEmpty()) {
                ((TextView) container.findViewById(R.id.tvAdminNotes)).setText(item.getValue().toString());
                item.setView(container);
            } else {
                container.setVisibility(View.GONE);
                item.setView(container);
            }
        } else if (FeatureLayerUtils.FieldType.determineFieldType(item.getField()) == FeatureLayerUtils.FieldType.DATE) {
            container = lInflater.inflate(R.layout.item_date, null);
            long date = 0;
            if (item.getValue() != null) {
                date = Long.parseLong(item.getValue().toString());
            }
            Button dateButton = createDateButtonFromLongValue(container, item.getField(), date, isGCS);
            item.setView(dateButton);
            container.setVisibility(View.GONE);
        } else {
            View valueView = null;
            Log.i("getView", "A - " + item.getField().getAlias());

            if (FeatureLayerUtils.FieldType.determineFieldType(item.getField()) == FeatureLayerUtils.FieldType.STRING) {
                container = lInflater.inflate(R.layout.item_text, null);
                valueView = createAttributeRow(FeatureLayerUtils.FieldType.STRING, container, item.getField(), item.getValue(), isGCS);

            } else if (FeatureLayerUtils.FieldType.determineFieldType(item.getField()) == FeatureLayerUtils.FieldType.NUMBER) {

                container = lInflater.inflate(R.layout.item_text, null);
                if (item.getField().getName().equals(ColumnNames.SURVEYOR_ID) && !isGCS) {
                    valueView = createAttributeRow(FeatureLayerUtils.FieldType.NUMBER, container, item.getField(), DataCollectionApplication.getSurveyorId(), false);
                    container.setVisibility(View.GONE);
                } else if (item.getField().getName().equals(ColumnNames.SOURCE_ID) && !isGCS) {
                    valueView = createAttributeRow(FeatureLayerUtils.FieldType.NUMBER, container, item.getField(), DataCollectionApplication.getSurveyorId(), false);
                    container.setVisibility(View.GONE);
                } else if (item.getField().getName().equals(ColumnNames.SURVEYOR_ID) && isGCS) {
                    valueView = createAttributeRow(FeatureLayerUtils.FieldType.NUMBER, container, item.getField(), item.getValue(), false);
                    container.setVisibility(View.GONE);
                } else if (item.getField().getName().equals(ColumnNames.CHECK_SURVEYOR)) {
                    valueView = createAttributeRow(FeatureLayerUtils.FieldType.NUMBER, container, item.getField(), item.getValue(), false);
                    container.setVisibility(View.GONE);
                    /**-------------------------Ali Ussama Update----------------------------------*/
                } else if (item.getField().getName().equals(ColumnNames.F)) {
                    valueView = createAttributeRow(FeatureLayerUtils.FieldType.NUMBER, container, item.getField(), item.getValue(), false);
                    container.setVisibility(View.GONE);
                } else if (item.getField().getName().equals(ColumnNames.GN_ID)) {
                    valueView = createAttributeRow(FeatureLayerUtils.FieldType.NUMBER, container, item.getField(), item.getValue(), false);
                    container.setVisibility(View.GONE);
                    /**-------------------------Ali Ussama Update----------------------------------*/
                } else {
                    Log.i("getView", "B - " + item.getField().getAlias());
                    valueView = createAttributeRow(FeatureLayerUtils.FieldType.NUMBER, container, item.getField(), item.getValue(), isGCS);
                }
            } else if (FeatureLayerUtils.FieldType.determineFieldType(item.getField()) == FeatureLayerUtils.FieldType.DECIMAL) {
                Log.i("getView", "C - " + item.getField().getAlias());

                container = lInflater.inflate(R.layout.item_text, null);
                valueView = createAttributeRow(FeatureLayerUtils.FieldType.DECIMAL, container, item.getField(), item.getValue(), isGCS);

            }

            item.setView(valueView);

        }


        if (container != null) {
            if (ColumnNames.E_NAMESTATUS.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.A_NAMESTATUS.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.ELEVATION.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.MAP_50K.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.MAP_100K.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.E_PROVINCE.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.E_GOVERNORATE.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.E_CENTER.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.E_DATASOURCE.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.E_DISTRICT.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.E_FEATURETYPE.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            } else if (ColumnNames.E_FEATURE.equals(item.getField().getName())) {
                container.setVisibility(View.GONE);
            }
        }


        return container;

    }

    public void setFeatureSet(HashMap<String, Object> attributes) {
        this.attributes = attributes;
    }

    private Spinner createSpinnerViewFromArray(View container, Field field, Object value, boolean isGCS) {

        TextView fieldAlias = (TextView) container.findViewById(R.id.field_alias_txt);
        final Spinner spinner = (Spinner) container.findViewById(R.id.field_value_spinner);
        fieldAlias.setHint("نوع المعلم");
        spinner.setPrompt(field.getAlias());


        CodedValueDomain cvd = (CodedValueDomain) field.getDomain();
        featureTypeCodeValues = (HashMap<String, String>) cvd.getCodedValues();
        Set<String> keys = featureTypeCodeValues.keySet();
        String[] featureTypesNames = new String[keys.size() + 1];
        final String[] featureTypesCode = new String[keys.size() + 1];
        int i = 1;
        featureTypesNames[0] = "أختار";
        for (String code : keys) {
            featureTypesNames[i] = featureTypeCodeValues.get(code);
            featureTypesCode[i] = code;
            i++;
        }


        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this.context, android.R.layout.simple_spinner_item, featureTypesNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        if (value != null)
            spinner.setSelection(spinnerAdapter.getPosition(featureTypeCodeValues.get(value.toString())));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                featureCodeValues.clear();
                position--;

                CodedValueDomain domains = null;

                if (position == -1) {
                    featureNames = new String[1];
                    featureNames[0] = "أختار";
                } else {

                    FeatureType selectedType = null;
                    selectedCode = featureTypesCode[position + 1];
                    for (FeatureType type : types) {
                        if (type.getId().equals(selectedCode)) {
                            selectedType = type;
                            break;
                        }
                    }

                    if (selectedType != null && selectedType.getDomains().get(ColumnNames.A_FEATURE) instanceof CodedValueDomain) {
                        domains = (CodedValueDomain) selectedType.getDomains().get(ColumnNames.A_FEATURE);
                        Set<String> keys = domains.getCodedValues().keySet();
                        featureNames = new String[keys.size() + 1];
                        int i = 1;
                        featureNames[0] = "أختار";
                        for (String value : keys) {
                            featureNames[i++] = domains.getCodedValues().get(value);
                            featureCodeValues.put(domains.getCodedValues().get(value), value);
                        }
                    } else {
                        featureNames = new String[1];
                        featureNames[0] = "أختار";
                    }
                }
                domainAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, featureNames);
                domainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                if (featureSpinner != null)
                    featureSpinner.setAdapter(domainAdapter);

                if (featureValue != null && domains != null && !featureValue.equals("0")) {
                    featureSpinner.setSelection(domainAdapter.getPosition(domains.getCodedValues().get(featureValue)));
                    featureValue = null;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });

        if (isGCS) {
            spinner.setEnabled(false);
        }
        return spinner;
    }

    private Spinner createDomainSpinnerView(View container, Field field, boolean isGCS) {

        TextView fieldAlias = (TextView) container.findViewById(R.id.field_alias_txt);
        featureSpinner = (Spinner) container.findViewById(R.id.field_value_spinner);
        fieldAlias.setHint("المعلم");
        featureSpinner.setPrompt(field.getAlias());

        domainAdapter = new ArrayAdapter<>(this.context, android.R.layout.simple_spinner_item, featureNames);
        domainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        featureSpinner.setAdapter(domainAdapter);

        if (featureValue != null)
            featureSpinner.setSelection(domainAdapter.getPosition(featureValue));


        if (isGCS) {
            featureSpinner.setEnabled(false);
        }

        return featureSpinner;
    }

    private Spinner createDomainSpinner(View container, Field field, Object value, boolean isGCS) {

        TextView fieldAlias = (TextView) container.findViewById(R.id.field_alias_txt);
        Spinner domainSpinner = (Spinner) container.findViewById(R.id.field_value_spinner);
        fieldAlias.setHint(field.getAlias());
        domainSpinner.setPrompt(field.getAlias());


        if (field.getDomain() instanceof CodedValueDomain) {
            CodedValueDomain cvd = (CodedValueDomain) field.getDomain();
            Map<String, String> typeDomain = cvd.getCodedValues();
            Set<String> keys = typeDomain.keySet();
            String[] domainNames = new String[keys.size() + 1];
            int i = 1;
            domainNames[0] = "أختار";
            for (String s : keys) {
                domainNames[i++] = typeDomain.get(s);
            }

            ArrayAdapter<String> domainAdapter = new ArrayAdapter<>(this.context, android.R.layout.simple_spinner_item, domainNames);
            domainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            domainSpinner.setAdapter(domainAdapter);
            if (value != null)
                domainSpinner.setSelection(domainAdapter.getPosition(value.toString()));
            if (isGCS) {
                domainSpinner.setEnabled(false);
            }
        }
        return domainSpinner;
    }

    private Button createDateButtonFromLongValue(View container, Field field, long date, boolean isGCS) {

        TextView fieldAlias = (TextView) container.findViewById(R.id.field_alias_txt);
        Button dateButton = (Button) container.findViewById(R.id.field_date_btn);
        fieldAlias.setText(field.getAlias());
        if (date == 0) {
            dateButton.setText(DATE_FORMAT.format(new Date()));
        } else {
            dateButton.setText(DATE_FORMAT.format(date));
        }

        if (isGCS) {
            dateButton.setEnabled(false);
        }

        return dateButton;
    }

    private View createAttributeRow(FeatureLayerUtils.FieldType type, View container, Field field, Object value, boolean isGCS) {

        EditText fieldValue = (EditText) container.findViewById(R.id.field_value_txt);
        TextInputLayout fieldLayout = (TextInputLayout) container.findViewById(R.id.field_layout);

        if (field.getAlias().equals("ArabicName")) {
            fieldLayout.setHint("Arabic Name");
        } else if (field.getAlias().equals("EnglishName")) {
            fieldLayout.setHint("English Name");
        } else if (field.getAlias().equals("ArabicN")) {
            fieldLayout.setHint("Arabic Name");
        } else if (field.getAlias().equals("PhoneNumber")) {
            fieldLayout.setHint("Phone Number");
        } else if (field.getAlias().equals("OpeningHours")) {
            fieldLayout.setHint("Opening Hours");
        } else if (field.getAlias().equals("NameStatus")) {
            fieldLayout.setHint("Name Status");
        } else if (field.getAlias().equals("RomanN")) {
            fieldLayout.setHint("Roman Name");
        } else if (field.getAlias().equals("RomanName")) {
            fieldLayout.setHint("Roman Name");
        } else if (field.getAlias().equals("NameOfGuidance")) {
            fieldLayout.setHint("Name Of Guidance");
        } else if (field.getAlias().equals("DataSource")) {
            fieldLayout.setHint("Data Source");
        } else if (field.getName().equals("Notes")) {
            isGCS = false;
            fieldLayout.setHint(field.getAlias());
        } else if (field.getName().equals("AName")) {
            fieldLayout.setHint(field.getAlias());
            isGCS = false;
        } else if (field.getName().equals("EName")) {
            fieldLayout.setHint(field.getAlias());
            isGCS = false;
        } else {
            String title = field.getAlias();
            String hint = title.substring(0, 1).toUpperCase() + title.substring(1).toLowerCase();
            fieldLayout.setHint(hint);
        }
        if (field.getLength() > 0) {
            InputFilter.LengthFilter filter = new InputFilter.LengthFilter(field.getLength());
            fieldValue.setFilters(new InputFilter[]{filter});
        }

        if (value != null) {
            fieldValue.setText(value.toString(), TextView.BufferType.EDITABLE);
        } else {
            fieldValue.setText("", TextView.BufferType.EDITABLE);
        }
        /**----------------------------------------Ali Ussama Update----------------------------------*/
        if (isGCS) {
            if (field.getName().matches("COMMENTS"))
                fieldValue.setEnabled(true);
            else
                fieldValue.setEnabled(false);
        }
        /**----------------------------------------Ali Ussama Update----------------------------------*/

        if (type == FeatureLayerUtils.FieldType.STRING) {
            if (field.getAlias().equals("PhoneNumber")
                    || field.getAlias().equals("Phone1") || field.getAlias().equals("Phone2")
                    || field.getAlias().equals("Fax")) {
                fieldValue.setInputType(InputType.TYPE_CLASS_PHONE);
            } else if (field.getAlias().equals("Notes")) {
                fieldValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            } else
                fieldValue.setInputType(InputType.TYPE_CLASS_TEXT);
        } else if (type == FeatureLayerUtils.FieldType.NUMBER)
            fieldValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        else
            fieldValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);


        return fieldValue;
    }
}
