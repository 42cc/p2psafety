package ua.p2psafety.setemails;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ua.p2psafety.R;
import ua.p2psafety.data.EmailsDatasourse;

/**
 * @author Taras Melon
 * @since 2014-01-09
 */
public class EmailsAdapter extends BaseAdapter {
    private EmailsDatasourse datasourse;
    private Typeface font;
    private List<String> items;
    private Context context;

    public EmailsAdapter(Context context) {
        this.context = context;
        this.datasourse = new EmailsDatasourse(context);
        this.items = datasourse.getAllEmails();
        this.font = Typeface.createFromAsset(context.getAssets(), "fonts/RobotoCondensed-Light.ttf");
    }

    public void addEmail(String email) {
        datasourse.addEmail(email);
        items.add(email);
        notifyDataSetChanged();

    }

    public void removeEmail(String email) {
        datasourse.removeEmail(email);
        items.remove(email);
        notifyDataSetChanged();

    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = newView(parent);
        } else {
            v = convertView;
        }
        bindView(v, position);
        return v;
    }

    private View newView(ViewGroup parent) {
        View v;
        LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.phone_list_item, parent, false);
        Log.w("VideoListAdapter", "added new View");
        return v;
    }

    private void bindView(View view, final int position) {
        final TextView txt_phone = (TextView) view.findViewById(R.id.txt_phone);

        txt_phone.setText(items.get(position));
        txt_phone.setTypeface(font);

        View ibtn_del = view.findViewById(R.id.ibtn_del);
        ibtn_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeEmail(txt_phone.getText().toString());
            }
        });

        View arrowUp = view.findViewById(R.id.arrowUpBtn);
        arrowUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position > 0) {
                    String curItem = items.get(position);
                    String prevItem = items.get(position-1);
                    items.set(position, prevItem);
                    items.set(position-1, curItem);
                    saveSortedData();
                    notifyDataSetChanged();
                }
            }
        });

        View arrowDown = view.findViewById(R.id.arrowDownBtn);
        arrowDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position < items.size()-1) {
                    String curItem = items.get(position);
                    String nextItem = items.get(position+1);
                    items.set(position, nextItem);
                    items.set(position+1, curItem);
                    saveSortedData();
                    notifyDataSetChanged();
                }
            }
        });
    }

    private void saveSortedData() {
        for (String item: items)
            datasourse.removeEmail(item);
        for (String item: items)
            datasourse.addEmail(item);
    }
}
