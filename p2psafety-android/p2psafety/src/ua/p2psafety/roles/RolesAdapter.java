package ua.p2psafety.roles;


import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import ua.p2psafety.R;

public class RolesAdapter extends ArrayAdapter<Role> {
    Context mContext;

    private static class ViewHolder {
        TextView roleName;
        CheckBox roleChecked;
    }

    public RolesAdapter(Context context) {
        super(context, 0);
        mContext = context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Role role = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.role_list_item, null);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.roleName = (TextView) convertView.findViewById(R.id.role_name_text);
            viewHolder.roleChecked = (CheckBox) convertView.findViewById(R.id.role_check_box);

            Typeface font = Typeface.createFromAsset(mContext.getAssets(), "fonts/RobotoCondensed-Bold.ttf");
            viewHolder.roleName.setTypeface(font);

            convertView.setTag(viewHolder);
        }

        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.roleName.setText(String.valueOf(role.name));
        viewHolder.roleChecked.setChecked(Boolean.valueOf(role.checked));
        viewHolder.roleChecked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getItem(position).checked = isChecked;
            }
        });
        return convertView;
    }
}
