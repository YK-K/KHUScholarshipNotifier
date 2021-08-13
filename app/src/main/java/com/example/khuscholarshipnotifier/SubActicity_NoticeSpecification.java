package com.example.khuscholarshipnotifier;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class SubActicity_NoticeSpecification extends AppCompatActivity {

    private String url; // 세부 공지사항의 url 주소
    private String parsed_title = "NULL";
    private String parsed_campus = "NULL";
    private String parsed_date = "NULL";
    private String parsed_mainparagraph_raw_html = "NULL";
    private String parsed_download_link = "NULL";

    private TextView TextView_Title;
    private TextView TextView_Campus;
    private TextView TextView_Date;
    private TextView TextView_MainParagraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sub_acticity__notice_specification);

        Intent intent = getIntent();
        url = "https://www.khu.ac.kr/kor/notice/" + intent.getStringExtra("url"); //인텐트로 전달받은 url을 private 변수에 저장
        Log.d("passedURL", url);

        //각 뷰를 이어 줌
        TextView_Title = (TextView) findViewById(R.id.TextTitle_Specific);
        TextView_Campus = (TextView) findViewById(R.id.TextCampus_Specific);
        TextView_Date = (TextView) findViewById(R.id.TextDate_Specific);
        TextView_MainParagraph = (TextView) findViewById(R.id.TextMainParagraph);

        getSupportActionBar().setTitle("세부 공지사항");   // 앱 바 타이틀 변경

        //AsyncTask로 파싱 작업 시작
        new SubActicity_NoticeSpecification.ItemParsing().execute();

    }

    private class ItemParsing extends AsyncTask<Void, Void, Void> {

        //진행바 표시
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //진행 다이얼로그 시작
            progressDialog = new ProgressDialog(SubActicity_NoticeSpecification.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("공지를 가져오는 중입니다..");
            progressDialog.show();
        }

        private void executeNoticeParsing(String urlString) {   //공지 세부사항을 파싱한다.
            try {
                Document doc = Jsoup.connect(url).get();
                Elements mElementData = doc.select("p[class=txt06]"); //<p class="txt06"> 태그 안에 있는 내용물만을 Elements 객체에 저장
                parsed_title = mElementData.first().text();  // 그 중 첫번째가 공지 제목이니 그를 가져옴.
                mElementData = doc.select("span[class=txtBox01 common]"); //<span class="txtBox01 common"> 태그 안에 있는 내용물만을 Elements 객체에 저장
                parsed_campus = mElementData.first().text();    // 그 중 첫번째가 캠퍼스니 그를 가져옴.
                mElementData = doc.select("span[class=date rightBar]"); //<span class="date rightBar"> 태그 안에 있는 내용물만을 Elements 객체에 저장
                parsed_date = mElementData.first().text();    // 그 중 첫번째가 공지 날짜니 그를 가져옴.
                mElementData = doc.select("div[class=row contents clearfix]"); //<div class="row contents clearfix"> 태그 안에 있는 내용물만을 Elements 객체에 저장
                parsed_mainparagraph_raw_html = mElementData.first().toString(); // 본문 내용을 저장(html로 되어 있음)
                mElementData = doc.select("a[class=tit]");//첨부파일 다운로드 링크를 가져옴
                parsed_download_link = "https://www.khu.ac.kr" + mElementData.attr("href");

                Log.d("Parsed Items", parsed_title + " / " + parsed_campus + " / " + parsed_date + " / " + parsed_download_link);

            } catch (IOException e) {
                Intent data = new Intent();
                setResult(RESULT_CANCELED, data);
                finish();
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            executeNoticeParsing(url); // 인텐트로 전달받은 장학 공지 세부사항 페이지 파싱
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();

            Log.d("Parsed Items_oncreate()", parsed_title + " / " + parsed_campus + " / " + parsed_date);

            //파싱한 스트링들을 각 뷰에 표시
            TextView_Title.setText(parsed_title);
            TextView_Campus.setText(parsed_campus);
            TextView_Date.setText(parsed_date);
            TextView_MainParagraph.setText(Html.fromHtml(parsed_mainparagraph_raw_html));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_noticespec, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent data;

        switch (item.getItemId()) {
            case R.id.openInBrowser: // 브라우저로 해당 URL 실행
                data = new Intent();
                data.setAction(Intent.ACTION_VIEW);
                data.setData(Uri.parse(url));
                startActivity(data);
                return true;
            case R.id.download: // 본문 내 첨부파일을 다운로드
                if(!parsed_download_link.contains("download.do")) { // 링크 내 download.do 문자열이 없을 경우(첨부파일이 없을 경우)
                    Toast.makeText(this, "첨부 파일이 없습니다.", Toast.LENGTH_SHORT).show();
                    return true;
                }
                data = new Intent();
                data.setAction(Intent.ACTION_VIEW);
                data.setData(Uri.parse(parsed_download_link));
                startActivity(data);
                return true;
            case R.id.goback: // RESULT_OK를 보내고 finish
                data = new Intent();
                setResult(RESULT_OK, data);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}