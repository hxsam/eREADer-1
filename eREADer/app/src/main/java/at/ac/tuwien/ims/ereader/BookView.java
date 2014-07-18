package at.ac.tuwien.ims.ereader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

import at.ac.tuwien.ims.ereader.Entities.Book;
import at.ac.tuwien.ims.ereader.Entities.Chapter;
import at.ac.tuwien.ims.ereader.Entities.CurrentPosition;
import at.ac.tuwien.ims.ereader.Entities.Page;
import at.ac.tuwien.ims.ereader.Services.BookService;
import at.ac.tuwien.ims.ereader.Services.ReadingService;

/**
 * Created by Flo on 09.07.2014.
 */
public class BookView extends Activity {
    private ImageButton optButton;
    private ImageButton ffButton;
    private ImageButton fbButton;
    private ImageButton playButton;
    private ImageButton libbtn;
    private TextView content;
    private TextView title;
    private TextView chap_page;
    private ImageButton volumeButton;

    private Book book;
    private List<Chapter> chapters;
    private HashMap<Integer, List<Page>> pages;
    private int currentChapter;
    private int currentPage;
    private int currentSentence;
    private boolean playing=false;
    private boolean muted=false;

    private BookService bookService;
    private ReadingService readingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);
        if (getActionBar() != null)
            getActionBar().hide();

        bookService=new BookService(this);
        book=bookService.getBook(getIntent().getExtras().getInt("book_id"));
        int cha=getIntent().getExtras().getInt("chapter");
        chapters=bookService.getChaptersOfBook(book.getId());
        pages=bookService.getPagesOfChapters(chapters);

        optButton=(ImageButton)findViewById(R.id.optnbtn_book);
        optButton.setOnClickListener(btnListener);
        playButton=(ImageButton)findViewById(R.id.playbtn);
        playButton.setOnClickListener(btnListener);
        ffButton=(ImageButton)findViewById(R.id.ffbtn);
        ffButton.setOnClickListener(btnListener);
        fbButton=(ImageButton)findViewById(R.id.fbbtn);
        fbButton.setOnClickListener(btnListener);
        libbtn=(ImageButton)findViewById(R.id.libbtn);
        libbtn.setOnClickListener(btnListener);
        content=(TextView)findViewById(R.id.book_text);
        title=(TextView)findViewById(R.id.bktitletxt);
        chap_page=(TextView)findViewById(R.id.chap_page_txt);
        volumeButton =(ImageButton)findViewById(R.id.volume_btn);
        libbtn.setOnClickListener(btnListener);

        title.setText(book.getTitle());

        if (cha == -1) {
            CurrentPosition c=bookService.getCurrentPosition(book.getId());
            currentChapter = c.getCurrentChapter();
            currentPage = c.getCurrentPage();
            currentSentence = c.getCurrentSentence();
        } else {
            currentChapter=cha;
            currentPage=0;
            currentSentence=0;
        }
        readingService=new ReadingService(this, book, currentSentence);
        updateText();
    }

    @Override
    public void onDestroy() {
        readingService.close();
        super.onDestroy();
    }

    private View.OnClickListener btnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v==optButton) {
                Intent myIntent = new Intent(BookView.this, Settings.class);
                startActivity(myIntent);
            } else if (v==playButton) {
                if (playing) {
                    CurrentPosition c=new CurrentPosition(book.getId(), currentChapter, currentPage, readingService.getCurrentSentence());
                    bookService.updateCurrentPosition(c);
                    playing=false;
                    playButton.setImageDrawable(getResources().getDrawable(R.drawable.playbtn));
                    readingService.stopReadingCurrentText();
                } else {
                    playing=true;
                    playButton.setImageDrawable(getResources().getDrawable(R.drawable.pausebtn));
                    readingService.readPage();
                }
            } else if (v==ffButton) {
                if (currentChapter < chapters.size()-1) {
                    if (currentPage < pages.get(currentChapter).size()-1)
                        currentPage++;
                    else {
                        currentChapter++;
                        currentPage=0;
                    }
                    updateText();
                }
            } else if(v==fbButton) {
                if (currentChapter >= 0) {
                    if (currentPage > 0)
                        currentPage--;
                    else if (currentChapter > 0) {
                        currentChapter--;
                        currentPage=pages.get(currentChapter).size()-1;
                    }
                    updateText();
                }
            } else if (v==libbtn) {
                Intent myIntent = new Intent(BookView.this, MyLibrary.class);
                startActivity(myIntent);
            } else if (v== volumeButton) {
                //todo
                if (muted) {
                    muted=false;
                    volumeButton.setImageDrawable(getResources().getDrawable(R.drawable.notmuted));
                } else {
                    muted=true;
                    volumeButton.setImageDrawable(getResources().getDrawable(R.drawable.muted));
                }
            }
        }
    };

    private void updateText() {
        Page p=pages.get(currentChapter).get(currentPage);
        content.setText(p.getContent());
        readingService.updatePage(p.getContent(), currentSentence);
        chap_page.setText(chapters.get(currentChapter).getHeading() + ", " + getString(R.string.page) + " " + p.getPage_nr());

        if (currentChapter==0 && currentPage==0) {
            fbButton.setAlpha(0.2f);
            fbButton.setEnabled(false);
        }
        if (currentChapter==chapters.size()-1 && currentPage==pages.get(chapters.size()-1).size()-1) {
            ffButton.setAlpha(0.2f);
            ffButton.setEnabled(false);
        }
        if (currentChapter>0 || currentPage>0) {
            fbButton.setAlpha(1.f);
            fbButton.setEnabled(true);
        }
        if (currentChapter<chapters.size()-1 || currentPage<pages.get(currentChapter).size()-1) {
            ffButton.setAlpha(1.f);
            ffButton.setEnabled(true);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(BookView.this, message, Toast.LENGTH_SHORT).show();
    }
}
