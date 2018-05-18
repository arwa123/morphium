package de.caluga.morphium.cache;

/**
 * Created with IntelliJ IDEA.
 * User: stephan
 * Date: 15.04.14
 * Time: 10:06
 * To change this template use File | Settings | File Templates.
 */
public class CacheObject<T> {
    private Object result;
    private String key;
    private Class<? extends T> type;
    private long created;
    private long lru;


    public CacheObject(T result) {
        this.result = result;
        created = System.currentTimeMillis();
    }

    @SuppressWarnings("unused")
    public Class<? extends T> getType() {
        return type;
    }

    public void setType(Class<? extends T> type) {
        this.type = type;
    }

    @SuppressWarnings("unused")
    public Object getResult() {
        lru = System.currentTimeMillis();
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @SuppressWarnings("unused")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getCreated() {
        return created;
    }

    public long getLru() {
        return lru;
    }

    public void setLru(long lru) {
        this.lru = lru;
    }
}
