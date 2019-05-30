package com.clover.remote.clover.loyalty;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.clover.remote.order.DisplayLineItem;
import com.clover.remote.order.DisplayOrder;

public class DisplayOrderAdapter extends BaseAdapter {
  DisplayOrder displayOrder;
  Context context;

  DisplayOrderAdapter(Context context, DisplayOrder displayOrder) {
    this.displayOrder = displayOrder;
    this.context = context;
  }

  @Override
  public int getCount() {
    return displayOrder == null ? 0 : displayOrder.getLineItems().size();
  }

  @Override
  public Object getItem(int position) {
    return displayOrder.getLineItems().get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    // Get the data item for this position
    DisplayLineItem displayLineItem = (DisplayLineItem) getItem(position);
    // Check if an existing view is being reused, otherwise inflate the view
    View view = convertView;


    if (view == null) {
      LayoutInflater inflater = LayoutInflater.from(context);
      view = inflater.inflate(R.layout.row_display_order_line_item, parent, false);
    }

    TextView quantityTV = view.findViewById(R.id.quantity);
    TextView nameTV = view.findViewById(R.id.name);
    TextView amountTV = view.findViewById(R.id.amount);

    quantityTV.setText(displayLineItem.getQuantity());
    nameTV.setText(displayLineItem.getName());
    amountTV.setText(displayLineItem.getPrice());

    // Return the completed view to render on screen
    return view;
  }
}
