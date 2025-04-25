package gitlet;

import java.io.File;

import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.restrictedDelete;

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
    public Gitlet(String path) {
        _currDir = path;
        if (_currDir.contains(".gitlet")) {
            _currDir = path.replace(".gitlet", "");
        }
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
            _remotes = new HashMap<>();
            _headbranch = "";
            _headCommit = "";
            _gitletExists = true;
            _initHappened = false;
            _commands.addAll(Arrays.asList("init", "add", "commit",
                    "rm", "branch", "checkout", "merge", "status",
                    "log", "global-log", "reset", "rm-branch",
                    "find", "add-remote", "rm-remote", "push",
                    "fetch", "pull"));
        } else {
            _stage = Utils.readObject(Utils.join(_cwdPath, "stage"),
                    StagingArea.class);
            _commits = Utils.readObject(Utils.join(_cwdPath, "commits"),
                    HashMap.class);
            _branches = Utils.readObject(Utils.join(_cwdPath, "branches"),
                    HashMap.class);
            _remotes = Utils.readObject(Utils.join(_cwdPath, "remotes"),
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
        Utils.writeObject(Utils.join(_cwdPath, "remotes"), _remotes);
        Utils.writeObject(Utils.join(_cwdPath, "headCommit"), _headCommit);
        Utils.writeObject(Utils.join(_cwdPath, "gitletExists"), _gitletExists);
        Utils.writeObject(Utils.join(_cwdPath, "HEAD"), _hEAD);
        Utils.writeObject(Utils.join(_cwdPath, "initHappened"), _initHappened);
        Utils.writeObject(Utils.join(_cwdPath, "commands"), _commands);
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
        if (_stage.forRemoval().containsKey(filename)) {
            _stage.forRemoval().remove(filename);
        }
        if (_hEAD.getFiles().containsKey(filename)) {
            if (_hEAD.getFiles().get(filename).getSha()
                    .equals(newAdd.getSha())) {
                if (_stage.forAddition().containsKey(filename)) {
                    _stage.forAddition().remove(filename);
                }
                saveGitlet();
                return;
            }
        }
        _stage.forAddition().put(filename, newAdd);
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
        if (_stage.forAddition().containsKey(filename)) {
            _stage.forAddition().remove(filename);
        }
        else if (_hEAD.getFiles().containsKey(filename)) {
            Blob rem = _hEAD.getFiles().get(filename);
            _stage.forRemoval().put(filename, rem);
            String CWD = System.getProperty("user.dir");
            File f = Utils.join(CWD, filename);
            Utils.restrictedDelete(f);
        } else {
            throw Utils.error("No reason to remove the file.");
        }
        saveGitlet();
    }

    /**
     * Saves a snapshot of files, consisting of the files in the
     * staging area and certain files in the previous commit,
     * and commits them with message MSG.
     */
    public void commit(String msg, String secondParent) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (msg.isEmpty()) {
            throw Utils.error("Please enter a commit message.");
        }
        if (_stage.forAddition().isEmpty() && _stage.forRemoval().isEmpty()) {
            throw Utils.error("No changes added to the commit.");
        }
        Commit prev = _commits.get(_headCommit);
        HashMap<String, Blob> stagedAdd = new HashMap<>(_stage.forAddition());
        HashMap<String, Blob> pFiles = new HashMap<>(prev.getFiles());
        HashMap<String, Blob> updated = new HashMap<>();
        for (String rem : _stage.forRemoval().keySet()) {
            pFiles.remove(rem);
        }
        updated.putAll(pFiles);
        updated.putAll(stagedAdd);
        Commit c;
        if (secondParent == null) {
            c = new Commit(msg, updated, pFiles, _headCommit);
        } else {
            c = new MergeCommit(msg, updated, pFiles, _headCommit, secondParent);
        }

        _headCommit = c.getSHA();
        _commits.put(_headCommit, c);
        _branches.get(_headbranch).setID(_headCommit);
        _hEAD = c;
        _stage.clean();
        saveGitlet();
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

        File f = new File(_currDir);
        ArrayList<String> names = new ArrayList<String>(Arrays.asList(f.list()));
//        System.out.println("Names List: " + names);

        Set<String> allFiles = new TreeSet<>();
        allFiles.addAll(names);                     // working dir
        allFiles.addAll(_hEAD.getFiles().keySet()); // tracked files
        allFiles.addAll(_stage.forAddition().keySet());
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String filename : allFiles) {
            File file = Utils.join(f, filename);
            if (file.isDirectory()) {
                continue;
            }

            boolean exists = file.exists();
            byte[] fileContents = null;
            if (exists) {
                fileContents = Utils.readContents(file);
            }

            if (_hEAD.getFiles().containsKey(filename)
                    && !_stage.forAddition().containsKey(filename)
                    && exists
                    && !Arrays.equals(_hEAD.getFiles().get(filename).getContents(), fileContents)) {
                System.out.println(filename + " (modified)");

            } else if (_stage.forAddition().containsKey(filename)
                    && exists
                    && !Arrays.equals(_stage.forAddition().get(filename).getContents(), fileContents)) {
                System.out.println(filename + " (modified)");

            } else if (_stage.forAddition().containsKey(filename)
                    && !exists) {
                System.out.println(filename + " (deleted)");

            } else if (_hEAD.getFiles().containsKey(filename)
                    && !_stage.forRemoval().containsKey(filename)
                    && !exists) {
                System.out.println(filename + " (deleted)");
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");

        for (String filename : names) {
            if (Utils.join(f, filename).isDirectory()) {
                continue;
            }
            if ((!_stage.forAddition().containsKey(filename))
                && (!_hEAD.getFiles().containsKey(filename))
                && (!_stage.forRemoval().containsKey(filename))
                && (new File(filename).isFile())
                && (!(new File(filename).isDirectory()))) {
                System.out.println(filename);
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
     * commit (the front of the current branch) and puts it in
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
        checkoutFile(filename, desired);
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

        // Check for untracked files that would be overwritten
        for (String file : filesinDir) {
            boolean inCurrent = _hEAD.getFiles().containsKey(file);
            boolean inDesired = desiredC.getFiles().containsKey(file);

            if (!inCurrent && inDesired) {
                throw Utils.error("There is an untracked file in the way; delete it or add and commit it first.");
            }
        }

        // Delete files that exist in current commit but do not exist in the target commit
        for (String file : _hEAD.getFiles().keySet()) {
            if (!desiredC.getFiles().containsKey(file)) {
                restrictedDelete(file);
            }
        }

        // Write files from the new branch into the working directory
        for (String name : desiredC.getFiles().keySet()) {
            checkoutFile(name, desiredC);
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
        if (branchName == null) {
            throw Utils.error("No branch name provided.");
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
                restrictedDelete(file);
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
        if (!_branches.containsKey(branchName)) {
            throw Utils.error("A branch with that name does not exist.");
        }
        if (_headbranch.equals(branchName)) {
           throw Utils.error("Cannot merge a branch with itself.");
        }
        if (!_stage.isEmpty()) {
            throw Utils.error("You have uncommitted changes.");
        }


//        setAncestors(_headCommit);
//        printAncestors(_headCommit);

        String givenCommitID = _branches.get(branchName).getID();
        String splitPointId = findSplitPoint(_headCommit, givenCommitID);



        Commit currentBranchCommit = _commits.get(_headCommit);
        Commit givenBranchCommit = _commits.get(givenCommitID);
        Commit splitPointCommit = _commits.get(splitPointId);

        if (currentBranchCommit.getAncestors().contains(givenBranchCommit.getSHA())) {
            throw Utils.error("Given branch is an ancestor of the current branch.");
        }
        if (splitPointId.equals(_headCommit)) {
            checkoutBranch(branchName);
            throw Utils.error("Current branch fast-forwarded.");
        }

        HashMap<String, Blob> currentBranchFiles = currentBranchCommit.getFiles();
        HashMap<String, Blob> givenBranchFiles = givenBranchCommit.getFiles();
        HashMap<String, Blob> splitPointFiles = splitPointCommit.getFiles();

        List<String> filesinDir = Utils.plainFilenamesIn(_currDir);
        for (String file : filesinDir) {
            if (!_hEAD.getFiles().containsKey(file)
                    && givenBranchFiles.containsKey(file)) {
                if (!_stage.forAddition().containsKey(file)) {
                    throw Utils.error("There is an untracked file in the way;"
                            + " delete it or add and commit it first.");
                }
            }

        }

        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(splitPointFiles.keySet());
        allFiles.addAll(currentBranchFiles.keySet());
        allFiles.addAll(givenBranchFiles.keySet());



        for (String filename : allFiles) {
            boolean inSplitPoint = splitPointFiles.containsKey(filename);
            boolean inCurrentBranch = currentBranchFiles.containsKey(filename);
            boolean inGivenBranch = givenBranchFiles.containsKey(filename);

            if (inSplitPoint) {
                String splitPointFileContents = splitPointFiles.get(filename).getSha();

                // Case 1: File is in the current branch but deleted in the given branch
                if (!inGivenBranch && inCurrentBranch) {
                    if (splitPointFileContents.equals(currentBranchFiles.get(filename).getSha())) {
                        _stage.forRemoval().put(filename, currentBranchFiles.get(filename));
                        String CWD = System.getProperty("user.dir");
                        File f = Utils.join(CWD, filename);
                        Utils.restrictedDelete(f);
                    } else {
                        handleMergeConflict(
                                currentBranchFiles.get(filename),
                                givenBranchFiles.get(filename),
                                filename);
                    }
                }

                // Case 2: File was modified in both branches...
                if (inCurrentBranch && inGivenBranch) {
                    String currentBranchFileContents = currentBranchFiles.get(filename).getSha();
                    String givenBranchFileContents = givenBranchFiles.get(filename).getSha();
                    // ...identically → No action needed
                    if (splitPointFileContents.equals(currentBranchFileContents)
                            && splitPointFileContents.equals(givenBranchFileContents)) {
                        // No change needed: same modification in both branches
                    } else if (!splitPointFileContents.equals(givenBranchFileContents)) {
                        // Given branch modified the file differently → checkout and stage it
                        if (splitPointFileContents.equals(currentBranchFileContents)) {
                            checkoutFile(filename, givenBranchCommit);
                            _stage.forAddition().put(filename, givenBranchFiles.get(filename));
                        } else {
                            handleMergeConflict(
                                    currentBranchFiles.get(filename),
                                    givenBranchFiles.get(filename),
                                    filename);
                        }
                    }
                }
            } else {
                // Handle files not in the split point (new files).
                if (!inCurrentBranch && inGivenBranch) {
                    checkoutFile(filename, givenBranchCommit);
                    _stage.forAddition().put(filename, givenBranchFiles.get(filename));
                }
                if (inCurrentBranch && inGivenBranch) {
                    if (!currentBranchFiles.get(filename).getSha()
                            .equals(givenBranchFiles.get(filename).getSha())) {
                        handleMergeConflict(
                                currentBranchFiles.get(filename),
                                givenBranchFiles.get(filename),
                                filename);
                    }
                }
            }
        }
        commit("Merged " + branchName + " into " + _headbranch + ".", givenCommitID);
        if (_mergeConflictFound) {
            System.out.println("Encountered a merge conflict.");
            _mergeConflictFound = false;
        }
        saveGitlet();
    }

    private void checkoutFile(String filename, Commit givenBranchCommit) {
        Blob newVersion = givenBranchCommit.getFiles().get(filename);
        byte[] newContents = newVersion.getContents();
        File newFile = Utils.join(_currDir, filename);
        Utils.writeContents(newFile, newContents);
    }

    private void handleMergeConflict(Blob currBlob, Blob givenBlob, String filename) {
        String conflictedFile = createMergeConflictFile(currBlob, givenBlob);
        byte[] newContents = conflictedFile.getBytes();
        File newFile = Utils.join(_currDir, filename);
        Utils.writeContents(newFile, newContents);
        Blob conflictedFileBlob = new Blob(filename, _currDir);
        _stage.forAddition().put(filename, conflictedFileBlob);
        _mergeConflictFound = true;
    }

    public String createMergeConflictFile(Blob b1, Blob b2) {
        String currentBranchFileContents = b1.getBytes();
        String givenBranchFileContents = b2 == null ? "" : b2.getBytes();
        return "<<<<<<< HEAD\n"
        + currentBranchFileContents
        + "\n=======\n"
        + givenBranchFileContents
        + "\n>>>>>>>";
    }

    public Set<String> findCommitAncestors(String commitId, Map<String, Integer> distanceMap) {
        Set<String> ancestors = new HashSet<>();
        Stack<String> stack = new Stack<>();

        stack.push(commitId);
        distanceMap.put(commitId, 0);

        while (!stack.isEmpty()) {
            String currentId = stack.pop();
            if (currentId == null || ancestors.contains(currentId)) {
                continue;
            }
            ancestors.add(currentId);
            Commit commit = _commits.get(currentId);
            if (commit == null) {
                continue;
            }
            int currentDistance = distanceMap.get(currentId);
            // Handle first parent
            if (commit.parent() != null && !distanceMap.containsKey(commit.parent())) {
                distanceMap.put(commit.parent(), currentDistance + 1);
                stack.push(commit.parent());
            }
            // Handle second parent (merge commit case)
            if (commit.getSecondParent() != null && !distanceMap.containsKey(commit.getSecondParent())) {
                distanceMap.put(commit.getSecondParent(), currentDistance + 1);
                stack.push(commit.getSecondParent());
            }
        }
        _commits.get(commitId).setAncestors(ancestors);
        return ancestors;
    }

    private String findSplitPoint(String headCommitID, String givenCommitID) {
        HashMap<String, Integer> distanceMap = new HashMap<>();
        Set<String> headCommitAncestors = findCommitAncestors(headCommitID, distanceMap);
        String closestSplitPoint = null;
        int minDistance = Integer.MAX_VALUE;

        Stack<String> stack = new Stack<>();
        stack.push(givenCommitID);

        while (!stack.isEmpty()) {
            String currentId = stack.pop();
            Commit commit = _commits.get(currentId);

            if (commit == null) {
                continue;
            }

            // If the commit exists in head's ancestors, check the distance
            if (distanceMap.containsKey(currentId)) {
                int distance = distanceMap.get(currentId);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestSplitPoint = currentId;
                }
            }

            // Traverse the first parent
            if (commit.parent() != null) {
                stack.push(commit.parent());
            }

            // Traverse the second parent (merge commit case)
            if (commit.getSecondParent() != null) {
                stack.push(commit.getSecondParent());
            }
        }

        return closestSplitPoint;
    }

    public void printAncestors(String commitId) {
        Commit commit = _commits.get(commitId);
        System.out.println("These are the ancestors of commit " + commitId);
        System.out.println();
        for (String c : commit.getAncestors()) {
            System.out.println(c);
        }
    }

    /**
     *
     *
     * REMOTE COMMANDS
     *
     *
     */

    public void addRemote(String name, String remoteLocation) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (_remotes.containsKey(name)) {
            throw Utils.error("A remote with that name already exists.");
        }

//        String remoteDirectory = remoteLocation.replace('/', File.separatorChar);

        Remote newRemote = new Remote(name, remoteLocation);
        _remotes.put(name, newRemote);
        saveGitlet();
    }

    public void removeRemote(String name) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (!_remotes.containsKey(name)) {
            throw Utils.error("A remote with that name does not exist.");
        }
        _remotes.remove(name);
        saveGitlet();
    }

    public void push(String remote, String remoteBranchName) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (!_remotes.containsKey(remote)) {
            throw Utils.error("A remote with that name does not exist.");
        }

        Remote givenRemote = _remotes.get(remote);
        String remoteDir = givenRemote.getRemoteDirectory();
        if (!new File(remoteDir).exists()) {
            throw Utils.error("Remote directory not found.");
        }
        Gitlet remoteRepo = new Gitlet(remoteDir);
        HashMap<String, Branch> remoteBranches = remoteRepo._branches;
        if (!remoteBranches.containsKey(remoteBranchName)) {
            remoteBranches.put(remoteBranchName, new Branch(remoteBranchName, _headCommit));
        } else {
            Branch remoteBranch = remoteBranches.get(remoteBranchName);
            Map<String, Integer> localDistanceMap = new HashMap<>();
            Set<String> localBranchAncestors = findCommitAncestors(_headCommit, localDistanceMap);
            if (!localBranchAncestors.contains(remoteBranch.getID())) {
                throw Utils.error("Please pull down remote changes before pushing.");
            }
            Map<String, Integer> remoteDistanceMap = new HashMap<>();
            Set<String> remoteBranchAncestors = findCommitAncestors(remoteBranch.getID(), remoteDistanceMap);

            List<String> missingCommits = new ArrayList<>();
            for (String commitID : localBranchAncestors) {
                if (!remoteBranchAncestors.contains(commitID)) {
                    missingCommits.add(commitID);
                }
            }
            missingCommits.sort(Comparator.comparingInt(localDistanceMap::get));
            for (String commitID : missingCommits) {
                remoteRepo._commits.put(commitID, _commits.get(commitID));
            }
            remoteBranches.put(remoteBranchName, new Branch(remoteBranchName, _headCommit));
            remoteRepo.saveGitlet();
        }

//        String commonAncestor = findSplitPoint();

    }

    public void pull(String remote, String remoteBranch) {
        fetch(remote, remoteBranch);
        merge(remoteBranch);

    }

    public void fetch(String remote, String remoteBranchName) {
        if (!_initHappened) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
        if (!_remotes.containsKey(remote)) {
            throw Utils.error("A remote with that name does not exist.");
        }
        Remote givenRemote = _remotes.get(remote);
        String remoteDir = givenRemote.getRemoteDirectory();
        if (!new File(remoteDir).exists()) {
            throw Utils.error("Remote directory not found.");
        }
        Gitlet remoteRepo = new Gitlet(remoteDir);
        HashMap<String, Branch> remoteBranches = remoteRepo._branches;
        HashMap<String, Commit> remoteCommits = remoteRepo._commits;
        if (!remoteBranches.containsKey(remoteBranchName)) {
            throw Utils.error("That remote does not have that branch.");
        }
        HashMap<String, Integer> remoteDistanceMap = new HashMap<>();
        Set<String> remoteCommitAncestors = remoteRepo.findCommitAncestors(remoteRepo._headCommit, remoteDistanceMap);
        List<String> missingCommits = new ArrayList<>();
        for (String commitID : remoteCommitAncestors) {
            if (!_commits.containsKey(commitID)) {
                missingCommits.add(commitID);
            }
        }
        missingCommits.sort(Comparator.comparingInt(remoteDistanceMap::get));
        for (String commitID : missingCommits) {
            _commits.put(commitID, remoteCommits.get(commitID));
        }
        Branch fetchBranch = new Branch(remote + "/" + remoteBranchName, remoteRepo._headCommit);
        _branches.put(remote + "/" + remoteBranchName, fetchBranch);
        saveGitlet();
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
     * HashMap containing all commits made in this program.
     * It maps SHA-1 identifier to its corresponding commit.
     */
    private HashMap<String, Commit> _commits;

    /**
     * HashMap containing all branches made in this program. It maps
     * the name of a branch to its respective Branch object.
     */
    private HashMap<String, Branch> _branches;

    private HashMap<String, Remote> _remotes;

    /** Used to check if a Gitlet repository has already been initiated. */
    private boolean _gitletExists;

    /** Returns true if init has been called already. */
    private boolean _initHappened;

    private boolean _mergeConflictFound = false;

    /** Directory where this Gitlet repo is located. */
    private String _currDir;

    /** Holds all valid Gitlet commands. */
    private ArrayList<String> _commands = new ArrayList<>();

}
