package at.ac.tuwien.ims.ereader.Entities;

/**
 * Created by Flo on 16.07.2014.
 */
public class CurrentPosition {
    private long book_id;
    private int currentContent;
    private int currentSentence;

    public CurrentPosition(long book_id, int currentContent, int currentSentence) {
        this.book_id = book_id;
        this.currentContent = currentContent;
        this.currentSentence = currentSentence;
    }

    public long getBook_id() {
        return book_id;
    }

    public void setBook_id(long book_id) {
        this.book_id = book_id;
    }

    public int getCurrentContent() {
        return currentContent;
    }

    public void setCurrentContent(int currentChapterInBook) {
        this.currentContent = currentChapterInBook;
    }

    public int getCurrentSentence() {
        return currentSentence;
    }

    public void setCurrentSentence(int currentSentence) {
        this.currentSentence = currentSentence;
    }

    @Override
    public String toString() {
        return "CurrentPosition{" +
                "book_id=" + book_id +
                ", currentContent=" + currentContent +
                ", currentSentence=" + currentSentence +
                '}';
    }
}
