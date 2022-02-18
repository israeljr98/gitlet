package gitlet;

import java.io.File;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.lang.model.util.ElementScanner6;

import java.util.Arrays;

/**
 * A Gitlet version-control system and all of its functions.
 * 
 * @author Israel Rodriguez
 */
public class Gitlet implements Serializable {

    /** For serializing purposes. */
    private static final long serialVersionUID = 0;

    /**
     * Starts a Gitlet program. If a repository has already
     * been initialized, it loads its last saved state.
     */
    public Gitlet() {
        _currDir = System.getProperty("user.dir");
        _cwd = Utils.join(_currDir, ".gitlet");
        _cwdPath = _cwd.getPath();
        try {
            _gitletExists = Utils.readObject(Utils.join(_cwdPath,
                    "gitletExists"), Boolean.class);
        } catch (IllegalArgumentException e) {
            _gitletExists = false;
        }
        if (!_gitletExists) {
            _stage = new StagingArea();
            _commits = new HashMap<>();
            _branches = new HashMap<>();
            _headbranch = "";
            _headCommit = "";
            _gitletExists = true;
            _initHappened = false;
            _commands.addAll(Arrays.asList("init", "add", "commit",
                    "rm", "branch", "checkout", "merge", "status",
                    "log", "global-log", "reset", "rm-branch",
                    "find"));
        } else {
            _stage = Utils.readObject(Utils.join(_cwdPath, "stage"),
                    StagingArea.class);
            _commits = Utils.readObject(Utils.join(_cwdPath, "commits"),
                    HashMap.class);
            _branches = Utils.readObject(Utils.join(_cwdPath, "branches"),
                    HashMap.class);
            _headbranch = Utils.readObject(Utils.join(_cwdPath, "headBranch"),
                    String.class);
            _headCommit = Utils.readObject(Utils.join(_cwdPath, "headCommit"),
                    String.class);
            _hEAD = Utils.readObject(Utils.join(_cwdPath, "HEAD"),
                    Commit.class);
            _commands = Utils.readObject(Utils.join(_cwdPath, "commands"),
                    ArrayList.class);
            _initHappened = Utils.readObject(Utils.join(_cwdPath,
                    "initHappened"), Boolean.class);
        }

    }

    /** Returns a list of all valid commands. */
    public ArrayList<String> commands() {
        return _commands;
    }

    /** Returns true if init has been called. */
    public boolean initHappened() {
        return _initHappened;
    }

    /** Writes onto disk the contents of this Gitlet repository. */
    public void saveGitlet() {
        Utils.writeObject(Utils.join(_cwdPath, "commits"), _commits);
        Utils.writeObject(Utils.join(_cwdPath, "stage"), _stage);
        Utils.writeObject(Utils.join(_cwdPath, "headBranch"), _headbranch);
        Utils.writeObject(Utils.join(_cwdPath, "branches"), _branches);
        Utils.writeObject(Utils.join(_cwdPath, "headCommit"), _headCommit);
        Utils.writeObject(Utils.join(_cwdPath, "gitletExists"), _gitletExists);
        Utils.writeObject(Utils.join(_cwdPath, "HEAD"), _hEAD);
        Utils.writeObject(Utils.join(_cwdPath, "initHappened"), _initHappened);
        Utils.writeObject(Utils.join(_cwdPath, "commands"), _commands);
    }

    /**
     * Saves a snapshot of files, consisting of the files in the
     * staging area and certain files in the previous commit,
     * and commits them with message MSG.
     */
    public void commit(String msg) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (msg.isEmpty()) {
            throw Utils.error("Please enter a commit message.");
        }
        if (_stage.forAdd().isEmpty() && _stage.forRem().isEmpty()) {
            throw Utils.error("No changes added to the commit.");
        }
        Commit prev = _commits.get(_headCommit);
        HashMap<String, Blob> stagedAdd = new HashMap<>(_stage.forAdd());
        HashMap<String, Blob> pFiles = new HashMap<>(prev.getFiles());
        HashMap<String, Blob> updated = new HashMap<>();
        for (String rem : _stage.forRem().keySet()) {
            pFiles.remove(rem);
        }
        updated.putAll(pFiles);
        updated.putAll(stagedAdd);
        Commit c = new Commit(msg, updated, pFiles, _headCommit);
        _headCommit = c.getSHA();
        _commits.put(_headCommit, c);
        _branches.get(_headbranch).setID(_headCommit);
        _hEAD = c;
        _stage.clean();
        saveGitlet();
    }

    /** Initializes a Gitlet version-control system. */
    public void init() {
        if (_cwd.exists()) {
            throw Utils.error("A gitlet version-control system "
                    + "already exists in the current directory");
        }
        _cwd.mkdirs();
        Commit initial = new Commit();
        String initialID = initial.getSHA();
        _commits.put(initialID, initial);
        _hEAD = initial;
        _headCommit = initialID;
        Branch master = new Branch("master", _headCommit);
        _headbranch = "master";
        _branches.put("master", master);
        _initHappened = true;
        saveGitlet();
    }

    /** Stages file with name FILENAME for addition. */
    public void add(String filename) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        Blob newAdd = new Blob(filename, _currDir);
        if (!newAdd.getLocation().exists()) {
            throw Utils.error("File does not exist.");
        }
        if (_stage.forRem().containsKey(filename)) {
            _stage.forRem().remove(filename);
        }
        if (_hEAD.getFiles().containsKey(filename)) {
            if (_hEAD.getFiles().get(filename).getSha()
                    .equals(newAdd.getSha())) {
                if (_stage.forAdd().containsKey(filename)) {
                    _stage.forAdd().remove(filename);
                }
                saveGitlet();
                return;
            }
        }
        _stage.forAdd().put(filename, newAdd);
        saveGitlet();
    }

    /**
     * If a file FILENAME is staged for addition, unstage it.
     * If the head commit is tracking the file, remove it
     * from the working directory and stage it for removal.
     */
    public void rm(String filename) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        boolean changesMade = false;
        Blob rem = null;
        if (!_stage.forAdd().containsKey(filename)
                && !_hEAD.getFiles().containsKey(filename)) {
            throw Utils.error("No reason to remove the file.");
        }
        if (_stage.forAdd().containsKey(filename)) {
            _stage.forAdd().remove(filename);
            changesMade = true;
        } else if (_hEAD.getFiles().containsKey(filename)) {
            rem = _hEAD.getFiles().get(filename);
            _stage.forRem().put(filename, rem);
            Utils.restrictedDelete(filename);
            changesMade = true;
        }
        if (changesMade) {
            saveGitlet();
        }
    }

    /**
     * Starting at the current HEAD commit,
     * it displays a log of all commits and some of their metadata.
     */
    public void log() {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        Commit currHEAD = _hEAD;
        String parent = currHEAD.parent();
        while (parent != null) {
            System.out.println("===");
            Commit temp = _commits.get(currHEAD.getSHA());
            System.out.println(temp.toString() + "\n");
            currHEAD = _commits.get(parent);
            if (currHEAD != null) {
                parent = currHEAD.parent();
            } else {
                parent = null;
            }
        }
    }

    /** Displays a log of all commits ever made in this repository. */
    public void globalLog() {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        for (Commit c : _commits.values()) {
            System.out.println("===");
            System.out.println(c.toString() + "\n");
        }

    }

    /**
     * Prints the SHA-1 identifiers of the commits whose
     * log message is the same as MESSAGE.
     */
    public void find(String message) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        boolean found = false;
        for (Commit c : _commits.values()) {
            if (c.getMessage().equals(message)) {
                System.out.println(c.getSHA());
                found = true;
            }
        }
        if (!found) {
            throw Utils.error("Found no commit with that message.");
        }
    }

    /**
     * Displays in lexicographic order the files that have
     * been staged for either addition or removal, as well as
     * all the branches that currently exist in this Gitlet.
     */
    public void status() {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        System.out.println("=== Branches ===");
        TreeMap<String, Branch> sorted = new TreeMap<>(_branches);
        for (String branch : sorted.keySet()) {
            if (branch.equals(_headbranch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String add : _stage.sortedAdd().keySet()) {
            System.out.println(add);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String rem : _stage.sortedRem().keySet()) {
            System.out.println(rem);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        File f = new File(_currDir);
        ArrayList<String> names = new ArrayList<String>(Arrays.asList(f.list()));
        for (String file : names) {
            if (_stage.sortedAdd().keySet().contains(file) ||
                    _stage.sortedRem().containsKey(file) ||
                    inCommit(file)) {
                continue;
            } else {
                System.out.println(file);
            }
        }
        System.out.println();
    }

    public boolean inCommit(String file) {
        for (Commit commit : _commits.values()) {
            if (commit.getFiles().containsKey(file)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Takes the version of the file FILENAME as it exists in the head
     * commit, the front of the current branch, and puts it in
     * the working directory, overwriting the version of the file
     * that's already there if there is one.
     * The new version of the file is not staged.
     * OP should be the operand "--".
     */
    public void checkout(String op, String filename) {
        checkout(_headCommit, op, filename);
    }

    /**
     * Takes the version of the file FILENAME as it exists in the
     * commit with the given COMMITID, and puts it in the working directory,
     * overwriting the version of the file that's already there if
     * there is one. The new version of the file is not staged.
     * OP should be the operand "--".
     */
    public void checkout(String commitID, String op,
            String filename) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (!op.equals("--")) {
            throw Utils.error("Incorrect operands.");
        }
        Commit desired = null;
        for (String id : _commits.keySet()) {
            if (id.startsWith(commitID)) {
                desired = _commits.get(id);
                break;
            }
        }
        if (desired == null) {
            throw Utils.error("No commit with that id exists");
        }
        if (!desired.getFiles().containsKey(filename)) {
            throw Utils.error("File does not exist in that commit");
        }
        Blob newVersion = desired.getFiles().get(filename);
        byte[] newContents = newVersion.getContents();
        File newFile = Utils.join(_currDir, filename);
        Utils.writeContents(newFile, newContents);
        saveGitlet();
    }

    /**
     * Takes all the files in the head commit pointed to by BRANCH
     * and puts them in the working directory, overwriting any existing
     * version.
     */
    public void checkoutBranch(String branch) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (!_branches.containsKey(branch)) {
            throw Utils.error("No such branch exists.");
        }
        if (branch.equals(_headbranch)) {
            throw Utils.error("No need to checkout the current branch.");
        }
        Branch desiredB = _branches.get(branch);
        Commit desiredC = _commits.get(desiredB.getID());
        List<String> filesinDir = Utils.plainFilenamesIn(_currDir);
        for (String file : filesinDir) {
            if (!_hEAD.getFiles().containsKey(file)
                    && desiredC.getFiles().containsKey(file)) {
                throw Utils.error("There is an untracked file in the way;"
                        + " delete it or add and commit it first.");
            }
            if (_hEAD.getFiles().containsKey(file)
                    && !desiredC.getFiles().containsKey(file)) {
                Utils.restrictedDelete(file);
            }
        }
        for (String name : desiredC.getFiles().keySet()) {
            Blob bVersion = desiredC.getFiles().get(name);
            byte[] bContents = bVersion.getContents();
            File newFile = Utils.join(_currDir, name);
            Utils.writeContents(newFile, bContents);
        }
        _headbranch = branch;
        _headCommit = desiredC.getSHA();
        _hEAD = desiredC;
        _stage.clean();
        saveGitlet();
    }

    /**
     * Creates a new branch with BRANCHNAME and points it
     * to the current head commit.
     */
    public void branch(String branchName) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (_branches.containsKey(branchName)) {
            throw Utils.error("A branch with that name already exists.");
        }
        Branch newBranch = new Branch(branchName, _headCommit);
        _branches.put(branchName, newBranch);
        saveGitlet();
    }

    /** Deletes the branch with name BRANCHNAME. */
    public void rmBranch(String branchName) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (!_branches.containsKey(branchName)) {
            throw Utils.error("A branch with that name does not exist.");
        }
        if (branchName.equals(_headbranch)) {
            throw Utils.error("Cannot remove the current branch.");
        }
        _branches.remove(branchName);
        saveGitlet();
    }

    /**
     * Checks out all the files tracked by the commit
     * whose identfier is COMMITID.
     */
    public void reset(String commitID) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        Commit reset = null;
        for (String id : _commits.keySet()) {
            if (id.startsWith(commitID)) {
                reset = _commits.get(id);
                break;
            }
        }
        if (reset == null) {
            throw Utils.error("No commit with that id exists");
        }
        List<String> filesinDir = Utils.plainFilenamesIn(_currDir);
        for (String file : filesinDir) {
            if (!_hEAD.getFiles().containsKey(file)
                    && reset.getFiles().containsKey(file)) {
                throw Utils.error("There is an untracked file in the way;"
                        + " delete it or add and commit it first.");
            }
            if (_hEAD.getFiles().containsKey(file)
                    && !reset.getFiles().containsKey(file)) {
                Utils.restrictedDelete(file);
            }
        }
        for (String name : reset.getFiles().keySet()) {
            Blob rVersion = reset.getFiles().get(name);
            byte[] rContents = rVersion.getContents();
            File newFile = Utils.join(_currDir, name);
            Utils.writeContents(newFile, rContents);
        }
        _hEAD = reset;
        _branches.get(_headbranch).setID(reset.getSHA());
        _stage.clean();
        saveGitlet();
    }

    /** Merges files from the BRANCHNAME into the current branch. */
    public void merge(String branchName) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
    }

    /**
     * The staging area in this Gitlet repository.
     * Contains both a stage for addition and a stage for removal.
     */
    private StagingArea _stage;

    /** Name of current branch. */
    private String _headbranch;

    /** SHA-1 identifier of the HEAD commit. */
    private String _headCommit;

    /** The HEAD pointer. */
    private Commit _hEAD;

    /**
     * File object representing the directory in which this
     * Gitlet repository is operating.
     */
    private File _cwd;

    /** Directory pathway of this Gitlet repository. */
    private String _cwdPath;

    /**
     * HashMap containing all commits made in this program. It maps
     * a SHA-1 identifier to its associated commit.
     */
    private HashMap<String, Commit> _commits;

    /**
     * HashMap containing all branches made in this program. It maps
     * the name of a branch to its respective Branch object.
     */
    private HashMap<String, Branch> _branches;

    /** Used to check if a Gitlet repository has already been initiated. */
    private boolean _gitletExists;

    /** Returns true if init has been called already. */
    private boolean _initHappened;

    /** The home directory of this computer. */
    private String _currDir;

    /** Holds all valid Gitlet commands. */
    private ArrayList<String> _commands = new ArrayList<>();

}
