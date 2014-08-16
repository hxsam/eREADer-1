package at.ac.tuwien.ims.ereader.Services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.NotificationCompat;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.github.johnpersano.supertoasts.SuperActivityToast;
import com.github.johnpersano.supertoasts.SuperToast;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import at.ac.tuwien.ims.ereader.BookViewerActivity;
import at.ac.tuwien.ims.ereader.Entities.Book;
import at.ac.tuwien.ims.ereader.Entities.Chapter;
import at.ac.tuwien.ims.ereader.Entities.CurrentPosition;
import at.ac.tuwien.ims.ereader.R;

/**
 * Created by Flo on 17.07.2014.
 */
public class ReadingService extends Service {
    private TextToSpeech ttsService;
    public static final String BROADCAST_ACTION = "at.ac.tuwien.ims.ereader.Services";
    public static final String ACTION_PLAY="at.ac.tuwien.ims.ereader.Services.ACTION_PLAY";
    public static final String ACTION_PAUSE="at.ac.tuwien.ims.ereader.Services.ACTION_PAUSE";

    private BookService bookService;

    private Book book;
    private Locale lang;
    private List<Chapter> chapters;
    private int currentChapter;
    private ArrayList<String> sentences;
    private int currentSentence;

    private Boolean muted=false;
    private Boolean playing=false;
    private Boolean reading=false;

    public ReadingService() {
        super();
    }

    public void startReading() {
        if (ttsService != null) {
            playing = true;
            setMuted(false);
            updateTTS();
            updateNotificationBar();
            broadcastUpdate();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (playing) {
                        synchronized (ttsService) {
                            while (reading);
                            if(!playing)
                                return;

                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID");
                            map.put(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "true");
                            ttsService.speak(sentences.get(currentSentence), TextToSpeech.QUEUE_FLUSH, map);

                            reading=true;
                        }
                    }
                }

            }).start();
        }
    }

    public void stopReading() { //todo if pause/play clicked too many times it skips...
        if (ttsService != null) {
            if (ttsService.isSpeaking())
                ttsService.stop();
            playing=false;
            broadcastUpdate();
        }
        updateNotificationBar();
    }

    private void broadcastUpdate() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("update", true);
        sendBroadcast(intent);
    }

    public void next() {
        if (currentChapter < chapters.size()-1) {
            stopReading();
            currentChapter++;
            currentSentence=0;
            updateSentences();
            broadcastUpdate();
        }
    }

    public void last() {
        if (currentChapter > 0) {
            stopReading();
            currentChapter--;
            currentSentence=0;
            updateSentences();
            broadcastUpdate();
        }
    }

    public String getCurrentSentence() {
        return sentences.get(currentSentence);
    }

    public void setCurrentSentence(int currentSentence) {
        this.currentSentence=currentSentence;
    }

    public String getCurrChapterHeading() {
        return chapters.get(currentChapter).getHeading();
    }

    public String getCurrentBookTitle() {
        return book.getTitle();
    }

    public int getCurrentChapter() {
        return currentChapter;
    }

    public int getNumberOfChaptersInCurrentBook() {
        return chapters.size();
    }

    public int[] getIndicesOfCurrentSentence() {
        int x[]=new int[2];

        if(sentences.size()==1) {
            x[0]=0;
            x[1]=sentences.get(0).length();
            return x;
        } else {
            int i=0;
            for (int j = 0; j < sentences.size(); j++) {
                if (j == currentSentence)
                    break;
                i += sentences.get(j).length();
            }
            x[0] = i;
            x[1] = i + getCurrentSentence().length();
            return x;
        }
    }

    public boolean getPlaying() {
        return playing;
    }

    public void setMuted(boolean muted) {
        this.muted=muted;
        AudioManager aManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        aManager.setStreamMute(AudioManager.STREAM_MUSIC, muted);
    }

    public boolean getMuted() {
        return muted;
    }

    public String getCurrentContent() {
        return chapters.get(currentChapter).getContent();
    }

    public void updateBook(Book b) {
        this.book=b;
        chapters=bookService.getChaptersOfBook(book.getId());

        CurrentPosition c=bookService.getCurrentPosition(book.getId());
        currentChapter=c.getCurrentChapter();
        currentSentence=c.getCurrentSentence();

        switch (b.getLanguage()) {
            case DE:
                lang=new Locale("de", "DE");
                break;
            case ES:
                lang=new Locale("es", "ES");
                break;
            default:
                lang=new Locale("en", "US");
        }
        updateSentences();
        updateTTS();
        broadcastUpdate();
    }

    private void updateTTS() {
        if (ttsService.isLanguageAvailable(lang)==TextToSpeech.LANG_COUNTRY_AVAILABLE)
            ttsService.setLanguage(lang);
    }

    private void updateSentences() {
        String content=getCurrentContent();
        sentences=new ArrayList<String>();
        BreakIterator it=null;
        if (lang!=null)
            it = BreakIterator.getSentenceInstance(lang);
        else
            it = BreakIterator.getSentenceInstance(Locale.US);
        it.setText(content);

        int lastIndex = it.first();
        while (lastIndex != BreakIterator.DONE) {
            int firstIndex = lastIndex;
            lastIndex = it.next();

            if (lastIndex != BreakIterator.DONE) {
                sentences.add(content.substring(firstIndex, lastIndex));
            }
        }
    }

    public int[] getIndicesOfClickedSentence(Layout layout, int x, int y) {
        int[] f=new int[2];
        f[0]=0;
        f[1]=0;

        String content=getCurrentContent();
        int line = layout.getLineForVertical(y);
        int clickedChar = layout.getOffsetForHorizontal(line, x);

        BreakIterator it=null;
        if (lang!=null)
            it = BreakIterator.getSentenceInstance(lang);
        else
            it = BreakIterator.getSentenceInstance(Locale.US);
        it.setText(content);

        int lastIndex = it.first();
        while (lastIndex != BreakIterator.DONE) {
            int firstIndex = lastIndex;
            lastIndex = it.next();

            if (lastIndex != BreakIterator.DONE && clickedChar >= firstIndex && clickedChar <= lastIndex) {
                f[0]=firstIndex;
                f[1]=lastIndex;
                break;
            }
        }
        return f;
    }

    public int getSentenceNumberByClick(Layout layout, int x, int y) {
        String content=getCurrentContent();
        int line = layout.getLineForVertical(y);
        int clickedChar = layout.getOffsetForHorizontal(line, x);

        BreakIterator it=null;
        if (lang!=null)
            it = BreakIterator.getSentenceInstance(lang);
        else
            it = BreakIterator.getSentenceInstance(Locale.US);
        it.setText(content);

        int i=0;
        int lastIndex = it.first();
        while (lastIndex != BreakIterator.DONE) {
            int firstIndex = lastIndex;
            lastIndex = it.next();

            if (lastIndex != BreakIterator.DONE && clickedChar >= firstIndex && clickedChar <= lastIndex)
                break;
            i++;
        }
        return i;
    }

    //todo doesnt always work in background?
    private void updateNotificationBar() {
        RemoteViews notificationView = new RemoteViews(getPackageName(), R.layout.navigation_bar);

        PendingIntent pendingIntentAction=null;
        if(playing) {
            Intent intentAction = new Intent(ACTION_PAUSE);
            pendingIntentAction = PendingIntent.getService(getApplicationContext(),
                    0, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationView.setImageViewResource(R.id.bar_btnPlay, R.drawable.pausebtn_bar);
        } else {
            Intent intentAction = new Intent(ACTION_PLAY);
            pendingIntentAction = PendingIntent.getService(getApplicationContext(),
                    0, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationView.setImageViewResource(R.id.bar_btnPlay, R.drawable.playbtn_bar);
        }

        notificationView.setOnClickPendingIntent(R.id.bar_btnPlay, pendingIntentAction);
        notificationView.setTextViewText(R.id.bar_title_book, getCurrentBookTitle());
        notificationView.setTextViewText(R.id.bar_chapter_page, getCurrChapterHeading());

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.logo_small_bar)
                        .setTicker(getString(R.string.notification_bar_welcome)+ ": "+getCurrentBookTitle())
                        .setContent(notificationView);

        Intent resultIntent = new Intent(this, BookViewerActivity.class);
        Bundle b = new Bundle();
        b.putInt("book_id", (int) book.getId());
        b.putInt("chapter", currentChapter);
        resultIntent.putExtras(b);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(BookViewerActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());
    }

    //--------------------------------------------------------------------------
    private final IBinder binder = new ReadingServiceBinder();

    public class ReadingServiceBinder extends Binder {
        public ReadingService getService() {
            return ReadingService.this;
        }
    }


    @Override
    public void onCreate (){
        super.onCreate();
        bookService=new BookService(this);

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("ttsStart", true);
        sendBroadcast(intent);

        ttsService=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    ttsService.setLanguage(Locale.US);
                    Log.d(ReadingService.class.getName(), "TTS initialized.");
                    Intent intent = new Intent(BROADCAST_ACTION);
                    intent.putExtra("ttsDone", true);
                    sendBroadcast(intent);
                }
            }
        });

        ttsService.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onDone(String utteranceId) {
                Log.d(ReadingService.class.getName(), "Stopped reading: " + sentences.get(currentSentence));
                if (currentSentence < sentences.size()-1) {
                    if(playing)
                        currentSentence++;
                    bookService.updateCurrentPosition(new CurrentPosition(book.getId(), currentChapter, currentSentence));
                    reading = false;
                } else if (currentSentence==sentences.size()-1
                        && currentChapter==chapters.size()-1) {
                    Log.d(ReadingService.class.getName(), "Reached end of book.");
                    stopReading();
                    bookService.updateCurrentPosition(new CurrentPosition(book.getId(), currentChapter, currentSentence));
                    reading = false;
                } else {
                    Log.d(ReadingService.class.getName(), "Reached end of chapter, skipping to the next.");
                    next();
                    bookService.updateCurrentPosition(new CurrentPosition(book.getId(), currentChapter, currentSentence));
                    reading = false;
                    startReading();
                }
                broadcastUpdate();
            }

            @Override
            public void onError(String utteranceId) {}

            @Override
            public void onStart(String utteranceId) {
                Log.d(ReadingService.class.getName(), "Starting to read: " + sentences.get(currentSentence));
                broadcastUpdate();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action)) {
                if (action.equals(ACTION_PLAY)) {
                    Log.d(ReadingService.class.getName(), "Pressed play from Notificationbar");
                    startReading();
                } else if(action.equals(ACTION_PAUSE)) {
                    Log.d(ReadingService.class.getName(), "Pressed pause from Notificationbar");
                    stopReading();
                }
            }
            broadcastUpdate();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        if (ttsService != null) {
            stopReading();
            ttsService.shutdown();
        }
    }
}