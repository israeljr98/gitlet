package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;


/** The staging area in a Gitlet repository.
 * @author Israel Rodriguez
 */
public class StagingArea implements Serializable {

    /** For serializing purposes. */
    private static final long serialVersionUID = 0;

    /** Creates a new Staging Area. */
    public StagingArea() {
        _stagedForAdd = new HashMap<>();
        _stagedForRem = new HashMap<>();
        _sortedAdd = new TreeMap<>();
        _sortedRem = new TreeMap<>();
    }


    /** Wipes the contents of the staging area. */
    public void clean() {
        _stagedForAdd.clear();
        _stagedForRem.clear();
        _sortedRem.clear();
        _sortedAdd.clear();
    }

    /** Returns the stage where files are staged for addition. */
    public HashMap<String, Blob> forAdd() {
        return _stagedForAdd;
    }

    /** Returns the stage where files are staged for removal. */
    public HashMap<String, Blob> forRem() {
        return _stagedForRem;
    }

    /** Returns the files staged for removal in lexicographic order. */
    public TreeMap<String, Blob> sortedRem() {
        _sortedRem.putAll(_stagedForRem);
        return _sortedRem;
    }

    /** Returns the files staged for addition in lexicographic order. */
    public TreeMap<String, Blob> sortedAdd() {
        _sortedAdd.putAll(_stagedForAdd);
        return _sortedAdd;
    }

    /** Returns True only if there are no changes staged.*/
    public boolean isEmpty() {
        return _stagedForAdd.isEmpty() && _stagedForRem.isEmpty();
    }

    /** HashMap that maps a filenames with their contents.
     * These files are staged for addition. */
    private HashMap<String, Blob> _stagedForAdd;

    /** HashMap that maps a filenames with their contents.
     * These files are staged for removal. */
    private HashMap<String, Blob> _stagedForRem;

    /** Used to lexicographically sort the files staged for
     * addition. */
    private TreeMap<String, Blob> _sortedAdd;

    /** Used to lexicographically sort the files staged for
     * removal. */
    private TreeMap<String, Blob> _sortedRem;
}
