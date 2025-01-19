package gitlet;

import java.io.Serializable;

/** A branch pointer in Gitlet.
 * @author Israel Rodriguez
 */
public class Branch implements Serializable {

    /** For serializing purposes. */
    private static final long serialVersionUID = 0;

    /** Instantiates a new branch named NAME and the SHA-1
     * ID of the head commit it points to. */
    public Branch(String name, String id) {
        _name = name;
        _id = id;
    }

    /** Returns the name of the branch. */
    public String getName() {
        return _name;
    }

    /** Returns the SHA-1 of the commit this branch points to. */
    public String getID() {
        return _id;
    }

    /** Changes the commit this branch points to by setting
     * its id to NEWID. */
    public void setID(String newID) {
        _id = newID;
    }

    /** Name of branch. */
    private String _name;

    /** SHA-1 ID of the commit this branch points to. */
    private String _id;

}
