package pseudoace;

public interface AceSessionHandler {
    public String transact(java.io.Closeable session, String req);
}
