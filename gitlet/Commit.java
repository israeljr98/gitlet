package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** A snapshot of the contents of files in a particular
 * directory.
 * @author Israel Rodriguez
 */
public class Commit implements Serializable {

    /** For serializing purposes. */
    private static final long serialVersionUID = 0;

    /** Creates a new commit with log MESSAGE that keeps
     * track of a snapshot of directory. This snapshot
     * consists of PFILES, that is the files tracked by this
     * commit's PARENT, and the newly added FILES in the
     * staging area. */
    public Commit(String message, HashMap<String, Blob> files,
                  HashMap<String, Blob> pfiles, String parent) {
        _message = message;
        _files = files;
        _parentFiles = pfiles;
        _parent = parent;
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf =
                new SimpleDateFormat("EEE MMM d kk:mm:ss YYYY ZZZ");
        _timestamp = sdf.format(c.getTime());
        _ancestors = new HashSet<>();
        _secondParent = null;
        setSHA();
    }

    /** Creates the initial commit. */
    public Commit() {
        _message = "initial commit";
        _timestamp = "Wed Dec 31 16:00:00 1969 -0800";
        _parent = "";
        _files = new HashMap<>();
        _parentFiles = new HashMap<>();
        _ancestors = new HashSet<>();
        _secondParent = null;
        setSHA();
    }

    /** Creates the SHA-1 identifier for this commit. It
     * incorporates the contents of its blob references alongside
     * other metadata. */
    public void setSHA() {
        StringBuilder blobBytes = new StringBuilder();
        for (Blob b : _files.values()) {
            blobBytes.append(b.getBytes());
        }
        _sha = Utils.sha1(_message + "commit" + _timestamp
                + _parent + blobBytes.toString());
    }

    /** Returns SHA-1 identifier. */
    public String getSHA() {
        return _sha;
    }

    public void setAncestors(Set<String> s) {
        _ancestors = s;
    }

    public Set<String> getAncestors() {
        return _ancestors;
    }

    /** Returns the blob references of this commit. */
    public HashMap<String, Blob> getFiles() {
        return _files;
    }

    /** Returns the blob references of this commit's parent. */
    public HashMap<String, Blob> getParentFiles() {
        return _parentFiles;
    }

    /** Returns this commit's log message. */
    public String getMessage() {
        return _message;
    }

    /** Returns this commit's time of creation. */
    public String getTimestamp() {return _timestamp;}

    /** Returns the SHA-1 identifier of this commit's parent. */
    public String parent() {
        return _parent;
    }

    public String getSecondParent() {
        return _secondParent;
    }

    public void setSecondParent(String secondParentId) {
        _secondParent = secondParentId;
    }


    /** Returns the contents of this commit as a String formatted
     * as a log entry. */
    public String toString() {
        return "commit " + _sha + "\n"
                + "Date: " + _timestamp + "\n" + _message;
    }

    /** Log message of this commit. */
    private String _message;

    /** Mapping between the files in this commit and their contents. */
    private HashMap<String, Blob> _files = new HashMap<>();

    /** Mapping between the files in this commit's parent
     * and their contents. */
    private HashMap<String, Blob> _parentFiles = new HashMap<>();

    /** This commit's ancestors. */
    private Set<String> _ancestors;

    /** Denotes the time at which this commit was made. */
    private String _timestamp;

    /** SHA-1 identifier for this commit's parent. */
    private String _parent;

    /** SHA-1 ID of this commit's merge parent*/
    private String _secondParent;

    /** SHA-1 identifier for this commit. */
    private String _sha;

}
