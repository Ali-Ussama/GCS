package map.util;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.gcs.riyadh.R;

import java.util.ArrayList;

import data.Index;

/**
 * Created by eslamelhoseiny on 10/4/17.
 */

public class DialogSearchIndex extends Dialog {

    private ListView list;
    private EditText filterText = null;
    ArrayAdapter<Index> adapter = null;

    public DialogSearchIndex(Context context, ArrayList<Index> indices, AdapterView.OnItemClickListener onItemClickListener) {
        super(context);
        setContentView(R.layout.dialog_search_index);
        this.setTitle("Select Index");
        filterText = (EditText) findViewById(R.id.et_search_index);
        filterText.addTextChangedListener(filterTextWatcher);
        list = (ListView) findViewById(R.id.lv_index);
        adapter = new ArrayAdapter<>(context, R.layout.item_index, R.id.tv_index_label, indices);
        list.setAdapter(adapter);
        list.setOnItemClickListener(onItemClickListener);
    }
    private TextWatcher filterTextWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            adapter.getFilter().filter(s);
        }
    };
    @Override
    public void onStop(){
        filterText.removeTextChangedListener(filterTextWatcher);
    }}