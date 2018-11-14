package util;

/**
 * Created by Administrator on 3/2/2016.
 */

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class CustomAutoComplete extends AutoCompleteTextView {

    public CustomAutoComplete(Context context) {
        super(context);
    }

    public CustomAutoComplete(Context arg0, AttributeSet arg1) {
        super(arg0, arg1);
    }

    public CustomAutoComplete(Context arg0, AttributeSet arg1, int arg2) {
        super(arg0, arg1, arg2);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

//    @Override
//    protected void onFocusChanged(boolean focused, int direction,
//                                  Rect previouslyFocusedRect) {
//        super.onFocusChanged(focused, direction, previouslyFocusedRect);
//        if (focused) {
//            performFiltering(getText(), 0);
//        }
//    }

}