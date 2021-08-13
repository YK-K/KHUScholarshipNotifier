package com.example.khuscholarshipnotifier;

import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.RecyclerViewHolder> {

    private ArrayList<Item> mItemList;

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        protected TextView campus;
        protected TextView title;
        protected TextView date;
        protected ImageView attatchment_icon;
        protected TextView type;
        protected TextView url;

        public RecyclerViewHolder(final View view) {
            super(view);
            this.campus = (TextView) view.findViewById(R.id.TextCampus);
            this.title = (TextView) view.findViewById(R.id.TextTitle);
            this.date = (TextView) view.findViewById(R.id.TextDate);
            this.attatchment_icon = (ImageView) view.findViewById(R.id.attatchment_file);
            this.type = (TextView) view.findViewById(R.id.TextType);
            this.url = (TextView) view.findViewById(R.id.TextNoticeUrl_Hidden);

            //클릭 이벤트에 대한 리스너 등록
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, SubActicity_NoticeSpecification.class);
                    intent.putExtra("url", url.getText());
                    context.startActivity(intent);
                }
            });
        }
    }


    public RecyclerAdapter(ArrayList<Item> list) {
        this.mItemList = list;
    }


    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.itemlist, viewGroup, false);
        RecyclerViewHolder viewHolder = new RecyclerViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder viewholder, int position) {

        viewholder.campus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        viewholder.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        viewholder.date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        viewholder.type.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        viewholder.url.setTextSize(TypedValue.COMPLEX_UNIT_SP, 0); //url은 보이지 않게 설정

        viewholder.campus.setGravity(Gravity.CENTER);
        viewholder.title.setGravity(Gravity.LEFT);
        viewholder.date.setGravity(Gravity.LEFT);
        viewholder.type.setGravity(Gravity.CENTER);

        viewholder.campus.setText(mItemList.get(position).getmCampus());
        viewholder.title.setText(mItemList.get(position).getmTitle());
        viewholder.date.setText(mItemList.get(position).getmDate());
        viewholder.type.setText(mItemList.get(position).getmType());
        viewholder.url.setText(mItemList.get(position).getmLink());

        if(mItemList.get(position).getHasFile() == false) { // 첨부파일이 없을 경우 첨부파일 아이콘을 숨김
            viewholder.attatchment_icon.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return (null != mItemList ? mItemList.size() : 0);
    }

}
