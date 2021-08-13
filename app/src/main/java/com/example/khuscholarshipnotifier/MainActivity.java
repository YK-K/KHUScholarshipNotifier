package com.example.khuscholarshipnotifier;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;  // 공지 목록을 띄워 줄 리사이클러 뷰
    private ArrayList<Item> itemList = new ArrayList(); // 공지 정보들을 보관할 Array List
    private long curTime;   // 시스템의 현재 시간을 보관할 변수(refresh 제한에 사용)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = (RecyclerView) findViewById(R.id.ItemList);
        getSupportActionBar().setTitle("장학 공지사항 목록");   // 앱 바 타이틀 변경


        //AsyncTask로 파싱 작업 진행
        new ItemParsing().execute();
    }

    private class ItemParsing extends AsyncTask<Void, Void, Void> {

        //진행바 표시
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //진행 다이얼로그 시작
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("공지를 가져오는 중입니다..");
            progressDialog.show();
        }

        private void executeNoticeListParsing(String urlString) {   //공지사항 목록을 파싱한다.
            try {
                Document doc = Jsoup.connect(urlString).get();
                Elements mElementDataSize = doc.select("tr"); //<tr> 태그 안에 있는 내용물만을 Elements 객체에 저장
                int mElementSize = mElementDataSize.size(); //<tr> 태그의 갯수를 알아냄

                for (Element elem : mElementDataSize) { // 찾아낸 각 <tr> 태그에 대해 정보를 추출
                    String parsed_title = elem.select("p").text();  //<p> 태그 안의 공지 제목을 가져옴
                    String parsed_link = elem.select("a").attr("href"); //<a href> 태그 안의 상세 공지 링크를 가져옴
                    String parsed_campus = elem.select("span[class=txtBox01 common]").text();    // 캠퍼스 정보를 가져옴
                    String parsed_date = elem.select("td[class=col04]").text(); // 공지 등록일을 가져옴
                    String parsed_type;
                    boolean parsed_hasFile = false;

                    if (elem.toString().contains("ico file")) { // 공지사항에 첨부파일이 있으면 parsed_hasFile을 true로 설정
                        parsed_hasFile = true;
                    }

                    if(parsed_title == "" || parsed_campus == "") {
                        continue;   // 추출한 정보가 빈칸(Title/Campus)이면 Item 객체를 추가히지 않음
                    }

                    if(parsed_title.contains("대외")) parsed_type = "대외"; // 장학금 종류(대외/교내/기타)를 공지 제목에서 판단
                    else if (parsed_title.contains("교내")) parsed_type = "교내";
                    else parsed_type = "기타";

                    itemList.add(new Item(parsed_title, parsed_link, parsed_campus, parsed_date, parsed_hasFile, parsed_type)); //ArrayList에 추출한 정보를 담은 Item 객체를 추가
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            executeNoticeListParsing("https://www.khu.ac.kr/kor/notice/list.do?category=SCHOLARSHIP&page=1"); //장학 공지사항 목록 1페이지 파싱
            executeNoticeListParsing("https://www.khu.ac.kr/kor/notice/list.do?category=SCHOLARSHIP&page=2"); //장학 공지사항 목록 2페이지 파싱
            executeNoticeListParsing("https://www.khu.ac.kr/kor/notice/list.do?category=SCHOLARSHIP&page=3"); //장학 공지사항 목록 3페이지 파싱
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //ArrayList를 인자로 해서 어댑터와 연결한다.
            RecyclerAdapter recyclerAdapter = new RecyclerAdapter(itemList);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(recyclerAdapter);

            //ProgressDialog를 지움
            progressDialog.dismiss();
            // 파싱이 끝난 시각을 기록
            curTime = System.currentTimeMillis();

            //SharedPreferences 데이터에서 마지막으로 확인한 공지의 링크를 가져와 비교
            SharedPreferences preferences = getSharedPreferences("sp", MODE_PRIVATE);  // 앱 내 데이터를 저장할 SharedPreferences API

            String LastNoticeLink = preferences.getString("LastNoticeLink", "No data");
            String firstParsedLink = itemList.get(0).getmLink();

            if ( firstParsedLink.compareTo(LastNoticeLink) != 0 ) { // SharedPreferences에 저장된 마지막으로 확인한 공지의 링크와 현재 첫 번째 공지 링크가 다를 경우
                //신규 공지사항 푸시 알람 전송
                sendNotification("신규 공지사항", "새로운 장학 공지사항이 있습니다.");
                // 마지막으로 확인한 공지 제목 갱신
                SharedPreferences.Editor editor = preferences.edit();    // SharedPreferences API Editor 객체
                editor.putString("LastNoticeLink", firstParsedLink);
                editor.apply();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 0 && resultCode == RESULT_OK ) {
            //성공적으로 작업을 완료하고 SubActivity에서 뒤로가기 버튼을 누른 경우 아무것도 하지 않음
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_noticelist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Context context = this;   // 현재 Context를 임시로 저장할 변수 생성(빌더를 다시 만들 때 씀)

        switch (item.getItemId()) {
            case R.id.refresh:  // 공지사항 목록을 다시 로드
                // 목록을 갱신한 지 10초가 되지 않았으면 새로고침하지 않음
                if( System.currentTimeMillis() - curTime < 10000 ) Toast.makeText(context, "잠시 후 다시 실행해 주세요.", Toast.LENGTH_SHORT).show();
                else {
                    itemList.clear();   // 아이템 리스트를 비움
                    new ItemParsing().execute(); //AsyncTask로 파싱 작업 다시 진행
                }
                return true;


            case R.id.filter: // 적용할 필터를 고를 수 있게 도와주는 AlertDialog 생성
                final String FilterItems[] = {"장학금 종류", "캠퍼스", "기간", "적용한 필터 해제"}; // 필터 목록 데이터
                final AlertDialog.Builder builder_Filter = new AlertDialog.Builder(this);
                final ArrayList<Item> itemList_filtered = new ArrayList(); // 필터링된 공지 정보들을 보관할 Array List

                builder_Filter.setTitle("필터 적용");
                builder_Filter.setIcon(R.drawable.ic_filter);
                builder_Filter.setItems(FilterItems, new DialogInterface.OnClickListener() {

                    AlertDialog.Builder builder2;   // 두번째 다이얼로그를 만들 빌더
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch(i) {
                            case 0: // 장학금 종류. 별도 다이얼로그를 생성하여 교내/대외/기타를 고르게 함.
                                final String FilterItems[] = {"교내", "대외", "기타"}; // 필터 목록 데이터
                                builder2 = new AlertDialog.Builder(context);
                                builder2.setTitle("교내/대외 필터 설정");
                                builder2.setIcon(R.drawable.ic_filter);
                                builder2.setSingleChoiceItems(FilterItems, -1, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        itemList_filtered.clear();
                                        for (Item item : itemList) {    // itemList에서 type이 설정한 필터와 같으면 itemList_filtered에 삽입
                                            if(item.getmType() == FilterItems[i]) itemList_filtered.add(item);
                                            else continue;
                                        }

                                        //필터링된 아이템 리스트로 리사이클러 뷰를 새로 구성
                                        RecyclerAdapter recyclerAdapter = new RecyclerAdapter(itemList_filtered);
                                        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                                        recyclerView.setLayoutManager(layoutManager);
                                        recyclerView.setAdapter(recyclerAdapter);
                                    }
                                });
                                builder2.setPositiveButton("필터 적용", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });
                                builder2.create().show();
                                break;
                            case 1: // 캠퍼스
                                final String FilterItems_campus[] = {"서울", "국제", "공통"}; // 필터 목록 데이터
                                builder2 = new AlertDialog.Builder(context);
                                builder2.setTitle("캠퍼스 필터 설정");
                                builder2.setIcon(R.drawable.ic_filter);
                                builder2.setSingleChoiceItems(FilterItems_campus, -1, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        itemList_filtered.clear();
                                        for (Item item : itemList) {    // itemList에서 type이 설정한 필터와 같으면 itemList_filtered에 삽입
                                            if(item.getmCampus().contains(FilterItems_campus[i])) itemList_filtered.add(item);
                                            else continue;
                                        }

                                        //필터링된 아이템 리스트로 리사이클러 뷰를 새로 구성
                                        RecyclerAdapter recyclerAdapter = new RecyclerAdapter(itemList_filtered);
                                        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                                        recyclerView.setLayoutManager(layoutManager);
                                        recyclerView.setAdapter(recyclerAdapter);
                                    }
                                });
                                builder2.setPositiveButton("필터 적용", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });
                                builder2.create().show();
                                break;
                            case 2: // 기간
                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                LayoutInflater inflater = getLayoutInflater();
                                View layout = inflater.inflate(R.layout.dialog_datepicker, null);
                                builder.setView(layout);
                                final DatePicker mDatepicker_startDate = (DatePicker)layout.findViewById(R.id.datepicker_startDate);
                                final DatePicker mDatepicker_endDate = (DatePicker)layout.findViewById(R.id.datepicker_endDate);
                                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String startdate;
                                        String enddate;  // 시작일과 종료일을 저장할 변수

                                        String year = Integer.toString(mDatepicker_startDate.getYear());
                                        String month = Integer.toString(mDatepicker_startDate.getMonth()+1);
                                        String date = Integer.toString(mDatepicker_startDate.getDayOfMonth());

                                        //month와 date가 10보다 작으면 앞에 "0'을 붙여 줌 ex - 07, 09
                                        if(Integer.parseInt(month) < 10) month = "0" + month;
                                        if(Integer.parseInt(date) < 10) date = "0" + date;

                                        startdate = year + "-" + month + "-" + date;

                                        year = Integer.toString(mDatepicker_endDate.getYear());
                                        month = Integer.toString(mDatepicker_endDate.getMonth()+1);
                                        date = Integer.toString(mDatepicker_endDate.getDayOfMonth());

                                        //month와 date가 10보다 작으면 앞에 "0'을 붙여 줌 ex - 07, 09
                                        if(Integer.parseInt(month) < 10) month = "0" + month;
                                        if(Integer.parseInt(date) < 10) date = "0" + date;

                                        enddate = year + "-" + month + "-" + date;

                                        // 생성한 문자열로 Date 객체 생성
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                        Date dStartDate = null;
                                        Date dEndDate = null;
                                        Date dItemDate = null;
                                        try {
                                           dStartDate = dateFormat.parse(startdate);
                                           dEndDate = dateFormat.parse(enddate);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }

                                        itemList_filtered.clear();
                                        for (Item item : itemList) {    // itemList에서 mDate가 설정한 필터에 해당하면 itemList_filtered에 삽입
                                            try {
                                                dItemDate = dateFormat.parse(item.getmDate());
                                                if (dStartDate.compareTo(dItemDate) <= 0 && dEndDate.compareTo(dItemDate) >= 0)
                                                    itemList_filtered.add(item);
                                                else continue;
                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        //필터링된 아이템 리스트로 리사이클러 뷰를 새로 구성
                                        RecyclerAdapter recyclerAdapter2 = new RecyclerAdapter(itemList_filtered);
                                        RecyclerView.LayoutManager layoutManager2 = new LinearLayoutManager(getApplicationContext());
                                        recyclerView.setLayoutManager(layoutManager2);
                                        recyclerView.setAdapter(recyclerAdapter2);
                                    }
                                });
                                builder.setNegativeButton(android.R.string.cancel, null);
                                builder.create().show();

                                break;
                            case 3: // 적용한 필터 해제
                                //필터가 적용되지 않은 원래 아이템 리스트로 리사이클러 뷰를 구성
                                RecyclerAdapter recyclerAdapter = new RecyclerAdapter(itemList);
                                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                                recyclerView.setLayoutManager(layoutManager);
                                recyclerView.setAdapter(recyclerAdapter);
                        }
                    }
                });
                builder_Filter.create().show();
                return true;


            case R.id.guide:    // 안내를 띄워줄 AlertDialog 생성
                final String items[] = {"장학금 안내", "애플리케이션 정보"}; // 안내 목록 데이터
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("어떤 안내를 제공해 드릴까요?");
                builder.setIcon(R.drawable.ic_question_mark);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch(i) {
                            case 0: // 경희대학교 장학팀 안내 페이지로 이동
                                Intent data;
                                data = new Intent();
                                data.setAction(Intent.ACTION_VIEW);
                                data.setData(Uri.parse("http://janghak.khu.ac.kr/01/01.php"));
                                startActivity(data);
                                break;
                            case 1: // 별도 다이얼로그를 생성하여 애플리케이션 정보를 표시
                                final AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                                builder2.setTitle("애플리케이션 정보");
                                builder2.setIcon(R.drawable.ic_question_mark);
                                builder2.setMessage("Code By 2016104111 김영교\n2020-2 모바일프로그래밍 기말 프로젝트");
                                builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });
                                builder2.create().show();
                                break;
                        }
                    }
                });
                builder.create().show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendNotification(String title, String text) {   // 전달받은 String들로 내용을 구성하여 Notification Alarm 전송
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "notification_ch_id");
        builder.setSmallIcon(R.drawable.ic_app);
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setAutoCancel(true);

        //OREO API 26 이상에서는 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            builder.setSmallIcon(R.drawable.ic_app);   //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남
            CharSequence channelName = "Notification channel";
            String description = "오레오 이상을 위한 것임";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel("notification_ch_id", channelName, importance);
            channel.setDescription(description);

            //notification channel을 시스템에 등록
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        } else builder.setSmallIcon(R.drawable.ic_app);
        //Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러

        assert notificationManager != null;

        notificationManager.notify(0, builder.build());
    }

}