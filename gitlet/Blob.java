package gitlet;

import java.io.File;
import java.io.Serializable;

/** A Gitlet blob, or a representation of the contents
 * of a file.
 * @author Israel Rodriguez
 */
public class Blob implements Serializable {

    /** For serializing purposes. */
    private static final long serialVersionUID = 0;

    /** Constructs a Blob object consisting of the
     * contents from file FILENAME in DIRECTORY. */
    Blob(String filename, String directory) {
        _location = Utils.join(directory, filename);
        _fileName = filename;
        if (!_location.exists()) {
            _contents = null;
        } else {
            _contents = Utils.readContents(_location);
            _bytes = new String(Utils.readContents(_location));
        }
        _sha = Utils.sha1(_bytes + _fileName + "blob");
    }

    /** Returns the contents of this blob as a byte array. */
    public byte[] getContents() {
        return _contents;
    }

    /** Returns the SHA-1 ID of this blob. */
    public String getSha() {
        return _sha;
    }

    /** Returns the contents of this blob represented as
     * a String. */
    public String getBytes() {
        return _bytes;
    }

    /** Returns the File where this blob is located. */
    public File getLocation() {
        return _location;
    }

    /** Returns the name of this blob. */
    public String filename() {
        return _fileName;
    }

    /** The contents of this blob represented as a string. */
    private String _bytes;

    /** The contents of this blob as a byte array. */
    private byte[] _contents;

    /** The SHA-1 ID of this blob. */
    private String _sha;

    /** The location of this blob (the directory where it is stored). */
    private File _location;

    /** Name of this blob. */
    private String _fileName;


}
