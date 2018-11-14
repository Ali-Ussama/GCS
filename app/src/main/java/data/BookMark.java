package data;

public class BookMark {
    String title;
    String json;
    int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public BookMark(String title, String json,int index) {
        this.title = title;
        this.json = json;
        this.index = index;
    }

    public BookMark() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }
}
