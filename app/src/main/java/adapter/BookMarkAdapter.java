package adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import data.BookMark;

import com.gcs.riyadh.R;

import util.DataCollectionApplication;


public class BookMarkAdapter extends BaseAdapter {

    ArrayList<BookMark> bookMarks;
    Context context;
    AlertDialog alertDialog;

    public BookMarkAdapter(Context context, ArrayList<BookMark> bookMarks, AlertDialog alertDialog) {
        this.context = context;
        this.bookMarks = bookMarks;
        this.alertDialog = alertDialog;
    }

    @Override
    public int getCount() {
        return bookMarks.size();
    }

    @Override
    public Object getItem(int position) {
        return bookMarks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_bookmark, null);
        TextView tvTitle = (TextView) v.findViewById(R.id.tvBookMarkTitle);
        ImageView icDelete = (ImageView) v.findViewById(R.id.icDelete);

        tvTitle.setText(bookMarks.get(position).getTitle());
        icDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataCollectionApplication.removeBookMark(bookMarks.get(position).getIndex());
                bookMarks.remove(position);
                notifyDataSetChanged();

                if (bookMarks.size() == 0 && alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }

            }
        });


        return v;
    }
}
