package gitlet;

import java.util.HashMap;

public class MergeCommit extends Commit {
    public MergeCommit(String message, HashMap<String, Blob> files,
                       HashMap<String, Blob> pfiles, String parent, String secondParent) {
        super(message, files, pfiles, parent);
        _secondParent = secondParent;

    }

    public String getSecondParent() {
        return _secondParent;
    }

    /** Returns the contents of this commit as a String formatted
     * as a log entry. */
    public String toString() {
        return "commit " + getSHA() + "\n"
                + "Merge: " + getSHA().substring(0, 7) + " " + getSecondParent().substring(0, 7) + "\n"
                + "Date: " + getTimestamp() + "\n"
                + getMessage();
    }

    private String _secondParent;
}
