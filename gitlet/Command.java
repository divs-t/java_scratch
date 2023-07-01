package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/** Command class execute commands passed in the main method. All the
 * logic for each command is included in static methods with relevant names.
 * All necessary folders and files will also be initialized and organized
 * in this class as needed. Makes necessary changes to the folders and files.
 * @author Divya Sivanandan */
public class Command implements Serializable {

    /** Current Working Directory. */
    static final File CWD = new File(".");

    /**The .gitlet FOLDER where all data in the repository is tracked.*/
    static final File GITLET_FOLDER = new File(".gitlet");

    /** A folder that acts as the staging area. */
    static final File STAGING_AREA = new File(".gitlet/STAGED");

    /** A folder that contains snapshots of file contents.*/
    static final File BLOBS_FOLDER = new File(".gitlet/BLOBS");


    /** Constructs a command and assigns values to instance variable depending
     * on whether or not a Gitlet version control system has been initialized.
     * Takes a String array INPUT from Main and does necessary error handling.*/
    public Command(String[] input) {
        if (input.length == 0) {
            Utils.message("Please enter a command");
            System.exit(0);
        }
        _cmd = input[0];
        _rest = Arrays.copyOfRange(input, 1, input.length);
        if (Main.initialized()) {
            _tree = GitTree.fromFile();
            File[] stagedFiles = STAGING_AREA.listFiles();
            for (File f : stagedFiles) {
                _staged.put(f.getName(), Utils.readContentsAsString(f));
            }
            setTracked();
            setUntracked();
            _activeBranch = getActiveBranch();
        } else {
            if (!_cmd.equals("init")) {
                Utils.message("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
        }
    }

    /** Creates a new Gitlet version-control system in the current directory.
     * The system automatically starts with the initial commit and a single
     * branch, master, that point to the initial commit. Since the initial
     * commit in all repositories will have the same content, all
     * repositories will share this commit by having the same UID and all
     * commits in all repositories will trace back to it. */
    public void init() {
        if (GITLET_FOLDER.exists()) {
            Utils.message("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_FOLDER.mkdir();
            Commit.COMMIT_FOLDER.mkdir();
            Branch.BRANCH_FOLDER.mkdir();
            BLOBS_FOLDER.mkdir();
            STAGING_AREA.mkdir();
            Utils.join(GITLET_FOLDER, "COMMITS");
            Utils.join(GITLET_FOLDER, "BRANCHES");
            Utils.join(GITLET_FOLDER, "STAGED");
            Utils.join(GITLET_FOLDER, "BLOBS");
            _tree = new GitTree();
            Utils.join(GITLET_FOLDER, "tracked");
            File activeBranch = Utils.join(GITLET_FOLDER, "active");
            Utils.writeContents(activeBranch, "master");
            _activeBranch = getActiveBranch();
        }
    }

    /** Adds a copy of the file with name FNAME as it is currently exists in
     *  the working directory to the staging area. Overwrites existing
     *  file in staging area if it already exists there. */
    public void add(String[] fName) throws IOException {
        for (String filename : fName) {
            File f = new File("./" + filename);
            if (!f.exists()) {
                Utils.message("File does not exist.");
                System.exit(0);
            } else {
                String contents = Utils.readContentsAsString(f);
                String fsha1 = Utils.sha1(contents);
                if (_staged.containsKey(filename)) {
                    _staged.replace(filename, fsha1);
                } else {
                    _staged.put(filename, fsha1);
                }
                if (_untracked.contains(filename)) {
                    _untracked.remove(filename);
                }
                if (_tree.getRemoved().contains(filename)) {
                    _tree.getRemoved().remove(filename);
                    _tree.saveTree();
                    _staged.remove(filename);
                    return;
                }
                String blobName = fsha1.substring(0, 5) + filename;
                File blobFile = new File(".gitlet/BLOBS/" + blobName);
                String hcID = getActiveBranch().getHead();
                Commit hc = Commit.fromFile(hcID);
                HashMap<String, String> files = hc.getFiles();
                if (blobFile.exists()) {
                    if (_tracked.contains(filename)) {
                        if (files.get(filename).equals(blobName)) {
                            _staged.remove(filename);
                            return;
                        }
                    }
                    File stagingFile = Utils.join(STAGING_AREA, filename);
                    Utils.writeContents(stagingFile, blobName);
                } else {
                    blobFile.createNewFile();
                    Utils.writeContents(blobFile, contents);
                    File stagingFile = Utils.join(STAGING_AREA, filename);
                    Utils.writeContents(stagingFile, blobName);
                }
            }
        }
    }

    /** Saves a snapshot of tracked files in the current commit and staging
     * area by creating a new Commit that has message MSG. By default, the
     * new commit will be a clone of it's parent commit's snapshot of file.
     * It will only update the contents of files it is tracking that have
     * been staged. A commit will save and start tracking files that were
     * staged for addition but weren't tracked by its parent. Files may
     * be untracked as a result of being staged for removal.
     */
    public void commit(String msg) {
        if (msg.equals("")) {
            Utils.message("Please enter a commit message.");
        } else {
            Branch active = getActiveBranch();
            String headCommit = active.getHead();
            String[] parent = new String[]{headCommit};
            File pInBranch = Utils.join(Commit.COMMIT_FOLDER, headCommit);
            Commit directParent = Utils.readObject(pInBranch, Commit.class);
            HashMap<String, String> parentFiles = directParent.getFiles();
            ArrayList<String> rm = _tree.getRemoved();
            if ((parentFiles != null && parentFiles.equals(_staged))
                    || (_staged.size() == 0
                    && _tree.getRemoved().size() == 0)) {
                Utils.message("No changes added to the commit.");
                System.exit(0);
            } else {
                HashMap<String, String> files = new HashMap<>();
                if (parentFiles != null) {
                    for (String filename : parentFiles.keySet()) {
                        files.put(filename, parentFiles.get(filename));
                        if (!rm.isEmpty() && rm.contains(filename)) {
                            files.remove(filename);
                        }
                    }
                }
                for (String filename : _staged.keySet()) {
                    files.put(filename, _staged.get(filename));
                }
                Commit newCommit = new Commit(msg, files, parent);
                _tree.addToBranch(active.getName(), newCommit);
                _tree.clearRemoved();
                for (File f : STAGING_AREA.listFiles()) {
                    f.delete();
                }
            }
        }
    }


    /** A commit when merge is called. Creates a new Commit object
     * that has messages MSG given and parents PARENTS. */
    public void mergeCommit(String msg, String[] parents) {
        Branch active = getActiveBranch();
        Commit hc = Commit.fromFile(active.getHead());
        HashMap<String, String> files = hc.getFiles();
        if (files == null) {
            files = new HashMap<>();
        }
        for (String filename: _staged.keySet()) {
            String blobName = _staged.get(filename);
            if (blobName.length() == Utils.UID_LENGTH) {
                blobName = _staged.get(filename).substring(0, 5) + filename;
            }
            files.put(filename, blobName);
        }
        for (String filename: _tree.getRemoved()) {
            files.remove(filename);
        }
        Commit newComm = new Commit(msg, files, parents);
        _tree.addToBranch(getActiveBranch().getName(), newComm);
        _tree.clearRemoved();
        for (File f: STAGING_AREA.listFiles()) {
            f.delete();
        }
    }


    /** Determine which checkout function to call based on input.*/
    public void whichCheckout() {
        if (_rest.length == 2 && _rest[0].equals("--")) {
            checkoutFile(_rest[1]);
        } else if (_rest.length == 3 && _rest[1].equals("--")) {
            checkoutCommit(_rest[0], _rest[2]);
        } else if (_rest.length == 1) {
            if (!_tree.getBranches().contains(_rest[0])) {
                Utils.message("No such branch exists.");
            } else if (getActiveBranch().getName().equals(_rest[0])) {
                Utils.message("No need to checkout the current branch.");
            } else if (!_untracked.isEmpty()) {
                Utils.message("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
            } else {
                checkoutBranch(_rest[0]);
            }
        } else {
            Utils.message("Incorrect operands.");
        }
    }

    /** Takes the version of the file with FILENAME as it exists in the head
     * commit of the active branch and puts it in the working directory,
     * overwriting the version of the file that's already there if
     * there is one. The new version of the file is not staged. */
    public void checkoutFile(String fileName) {
        String headCommitID = _activeBranch.getHead();
        checkoutCommit(headCommitID, fileName);
    }

    /** Takes all the files in the head commit of branch with BRANCHNAME and
     * puts them in the working directory, overwriting the versions of the files
     * already there if they exist. Set branch with branchName as the active
     * branch. Files tracked by current branch but not by branch with branchName
     * are deleted. The staging area is cleared, unless the checked-out branch
     * is the current branch. */
    public void checkoutBranch(String branchName) {
        Branch b = Branch.fromFile(branchName);
        Commit headCommit = Commit.fromFile(b.getHead());
        String hcID = headCommit.getHash();
        HashMap<String, String> files = headCommit.getFiles();
        _tracked.clear();
        List<String> filesINCWD = Utils.plainFilenamesIn(CWD);
        if (files == null) {
            for (String f: filesINCWD) {
                Utils.restrictedDelete(f);
            }
        } else {
            for (String filename: files.keySet()) {
                checkoutCommit(hcID, filename);
                _tracked.add(filename);
            }
            for (String f: filesINCWD) {
                if (!_tracked.contains(f)) {
                    Utils.restrictedDelete(f);
                }
            }
        }
        setActiveBranch(branchName);
        for (File f: STAGING_AREA.listFiles()) {
            f.delete();
        }
    }

    /** Takes the version of the file with FILENAME in the Commit with COMMITID,
     * and puts it in the working directory, in the working directory,
     * overwriting the version of the file that's already there if there is
     * one. The new version of the file is not staged. */
    public void checkoutCommit(String commitID, String fileName) {
        Commit commit = Commit.fromFile(commitID);
        HashMap<String, String> files = commit.getFiles();
        if (!files.containsKey(fileName)) {
            Utils.message("File does not exist in that commit.");
        } else {
            String fileSHA = files.get(fileName);
            String blobName = fileSHA.substring(0, 5) + fileName;
            File blob = Utils.join(BLOBS_FOLDER, blobName);
            String fileContents = Utils.readContentsAsString(blob);
            File inCWD = Utils.join(CWD, fileName);
            Utils.writeContents(inCWD, fileContents);
        }
    }

    /** Checks out all the files tracked by given Commit ID (maybe abbreviated).
     * Removes tracked files that are not present in given commit. Moves the
     * current branch's head to that commit. Staging area is cleared. */
    public void reset() {
        if (size() != 1) {
            Utils.message("Incorrect operands.");
        } else if (!_untracked.isEmpty()) {
            Utils.message("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
        } else {
            String commitID = _rest[0];
            Commit c = Commit.fromFile(commitID);
            HashMap<String, String> files = c.getFiles();
            for (String f: files.keySet()) {
                checkoutCommit(commitID, f);
            }
            getActiveBranch().setHead(commitID);
            _tree.setLatestCommit(commitID);
            setTracked();
            List<String> filesCWD = Utils.plainFilenamesIn(CWD);
            for (String fName: filesCWD) {
                if (_tracked == null || !_tracked.contains(fName)) {
                    File f = Utils.join(CWD, fName);
                    Utils.restrictedDelete(f);
                }
            }
            _tree.clearRemoved();
            List<String> stagedFiles = Utils.plainFilenamesIn(STAGING_AREA);
            for (String s: stagedFiles) {
                File f = Utils.join(STAGING_AREA, s);
                f.delete();
            }
        }
    }

    /** Displays what branches currently exist, and marks the current branch
     * with a *. Files that have been staged for addition or removal are
     * also displayed along with modified files that are not staged for
     * commit.*/
    public void status() {
        System.out.println("=== Branches ===");
        String activeName = getActiveBranch().getName();
        System.out.println("*" + activeName);
        for (String bName: _tree.getBranches()) {
            if (!bName.equals(activeName)) {
                System.out.println(bName);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String fName: _staged.keySet()) {
            System.out.println(fName);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String fName: _tree.getRemoved()) {
            System.out.println(fName);
        }
        System.out.println();
        setModified();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String fName: _modified.keySet()) {
            System.out.println(fName + " " + _modified.get(fName));
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String fName: _untracked) {
            System.out.println(fName);
        }
        System.out.println();
    }

    /** Starting at the current head commit, display information about each
     * commit backwards along the tree until the initial commit, following
     * the first parent commit links, ignoring any second parents found
     * in merge commits.*/
    public void log() {
        Branch b = getActiveBranch();
        String commitID = b.getHead();
        Commit curr = Commit.fromFile(commitID);
        String parentID = curr.getParent()[0];
        while (parentID != null) {
            curr.printCommit();
            if (curr.getParent() == null) {
                break;
            }
            parentID = curr.getParent()[0];
            if (parentID != null) {
                curr = Commit.fromFile(parentID);
            }
        }
    }

    /** Prints out information regarding all commits ever made in no
     * particular order. */
    public void globalLog() {
        if (size() > 0) {
            Utils.message("Incorrect operands.");
        } else {
            List<String> files = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
            for (String fName : files) {
                Commit curr = Commit.fromFile(fName);
                curr.printCommit();
            }
        }
    }

    /** Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits, it prints out the ids
     * on separate lines. The commit message is a single operand; to indicate
     * a multiword message, put the operand in quotation marks. */
    public void find() {
        List<String> files = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        int count = 0;
        String msg = _rest[0];
        for (String id : files) {
            Commit c = Commit.fromFile(id);
            if (msg.equals(c.getMessage())) {
                count++;
                System.out.println(id);
            }
        }
        if (count == 0) {
            Utils.message("Found no commit with that message.");
        }
    }

    /** Unstage the file with FILENAME if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for removal and
     * remove the file from the working directory if the user has not already
     * done so. File is not removed unless it is tracked. */
    public void rm(String filename) {
        Commit headCommit =  Commit.fromFile(getActiveBranch().getHead());
        HashMap<String, String> files = headCommit.getFiles();
        File f = new File("./" + filename);
        if (!f.exists() && !_tracked.contains(filename)) {
            Utils.message("File does not exist.");
        }
        boolean removed = false;
        if (_staged.containsKey(filename)) {
            _staged.remove(filename);
            File s = Utils.join(STAGING_AREA, filename);
            s.delete();
            removed = true;
        }
        if (files != null && files.containsKey(filename)) {
            _tree.addRemoved(filename);
            Utils.restrictedDelete(f);
            removed = true;
        }
        if (!removed) {
            Utils.message("No reason to remove the file.");
        }
    }

    /** Deletes the branch with the given name: only deletes the pointer,
     * not all commits associated with the branch. */
    public void rmBranch() {
        if (!_tree.branchExists(_rest[0])) {
            Utils.message("A branch with that name does not exist.");
        } else if (size() != 1) {
            Utils.message("Incorrect operands.");
        } else if (getActiveBranch().getName().equals(_rest[0])) {
            Utils.message("Cannot remove the current branch.");
        } else {
            _tree.removeBranch(_rest[0]);
        }
    }

    /** Creates a new branch with NAME, and points it at the current head node.
     * This command does not immediately switch to a newly created branch. */
    public void newBranch() {
        if (size() != 1) {
            Utils.message("Incorrect operands.");
        } else {
            _tree.addBranch(_rest[0], _tree.getLatestCommit(),
                    getActiveBranch());
        }
    }

    /** Merges the files from the given branch into the current branch.
     * Files that have been modified in the given branch since the split
     * point, but not in the current branch should be checkout out using
     * the head commit in the given branch and automatically staged.
     * If a file was removed from both the current and given branch,
     * but exists in CWD, it should be left untracked. Files only present
     * in the current branch should remain. Files only present in the
     * given branch are checked out and staged. Files absent in the given
     * branch are removed. Files absent in current branch remain absent.
     * Files modified in different ways are in conflict and handled
     * uniquely. Finally, automatically commit. */
    public void merge() throws IOException {
        String gb = _rest[0];
        if (!_tree.getBranches().contains(gb)) {
            Utils.message("A branch with that name does not exist.");
        } else {
            Branch cu = getActiveBranch();
            Branch gi = Branch.fromFile(gb);
            if (!_staged.isEmpty() || !_tree.getRemoved().isEmpty()) {
                Utils.message("You have uncommitted changes.");
            } else if (gb.equals(cu.getName())) {
                Utils.message("Cannot merge a branch with itself.");
            } else if (!_untracked.isEmpty()) {
                Utils.message("There is an untracked file in"
                        + " the way; delete it, or add and commit it first.");
            } else {
                String sID = _tree.findSplit(cu, gi), cID = cu.getHead();
                String gID = gi.getHead();
                Commit split = Commit.fromFile(sID);
                if (gID.equals(sID)) {
                    Utils.message("Given branch is an "
                            + "ancestor of the current branch.");
                } else if (cID.equals(sID)) {
                    checkoutBranch(gb);
                    Utils.message("Current branch fast-forwarded.");
                } else {
                    mergeHandling(cID, gID, sID, cu.getName(), gb);
                }
            }
        }
    }

    /** To handle a merge if none of the errors occur. Takes in the sha1 of the
     * active branch's head commit, CID, the given branch's head commit, GID,
     * and the split point, SID. Also takes in the name of the current active
     * branch, CU, and the given branch, GB. */
    public void mergeHandling(String cID, String gID, String sID, String cu,
                              String gb) throws IOException {
        Commit split = Commit.fromFile(sID);
        Commit currHC = Commit.fromFile(cID);
        Commit givenHC = Commit.fromFile(gID);
        HashMap<String, String> spf = split.getFiles();
        HashMap<String, String> cbf = currHC.getFiles();
        HashMap<String, String> gbf = givenHC.getFiles();
        if (spf != null) {
            for (String fName: spf.keySet()) {
                String cbfV = cbf.get(fName), spfV = spf.get(fName);
                String gbfV = gbf.get(fName);
                if (!(cbfV == null) && cbfV.equals(spfV)) {
                    if (!(gbfV == null) && !gbfV.equals(spfV)) {
                        checkoutCommit(gID, fName);
                        _staged.put(fName, gbfV);
                    } else if (gbfV == null) {
                        rm(fName);
                    }
                }
            }
        }
        for (String fName: gbf.keySet()) {
            String spfV = null;
            if (spf != null) {
                spfV = spf.get(fName);
            }
            String cbfV = cbf.get(fName), gbfV = gbf.get(fName);
            if (cbfV == null && spfV == null) {
                checkoutCommit(gID, fName);
                _staged.put(fName, gbfV);
            }
        }
        ArrayList<String> modDiff = getModified(spf, cbf, gbf);
        if (!modDiff.isEmpty()) {
            Utils.message("Encountered a merge conflict.");
            for (String f: modDiff) {
                String fileContent = "<<<<<<< HEAD\n";
                if (cbf.get(f) != null) {
                    File c = Utils.join(BLOBS_FOLDER, cbf.get(f));
                    fileContent += Utils.readContentsAsString(c);
                }
                fileContent += "=======\n";
                if (gbf.get(f) != null) {
                    File g = Utils.join(BLOBS_FOLDER, gbf.get(f));
                    fileContent += Utils.readContentsAsString(g);
                }
                fileContent += ">>>>>>>\n";
                File c = Utils.join(CWD, f);
                Utils.writeContents(c, fileContent);
                add(new String[]{f});
            }
        }
        String msg = "Merged " + gb + " into "
                + cu + ".";
        String[] parents = new String[] {cID, gID};
        mergeCommit(msg, parents);
    }

    /** Returns an arraylist of files that have been modified differently to
     * be used to handle merge conflicts by taking in the files at the
     * split point, SPF, the files at the current head commit, CBF, and
     * the files at the head commit of the given branch, GBF. */
    public ArrayList<String> getModified(HashMap<String, String> spf,
                                         HashMap<String, String> cbf,
                                         HashMap<String, String> gbf) {
        ArrayList<String> modified = new ArrayList<>();
        if (spf != null) {
            for (String fName: spf.keySet()) {
                if (!_staged.containsKey(fName)
                        && !_tree.getRemoved().contains(fName)) {
                    String cbfV = cbf.get(fName), spfV = spf.get(fName);
                    String gbfV = gbf.get(fName);
                    if (cbfV != null && gbfV != null) {
                        if (!spfV.equals(cbfV) && !spfV.equals(gbfV)
                                && !gbfV.equals(cbfV)) {
                            modified.add(fName);
                        }
                    } else if (cbfV == null) {
                        if (gbfV != null && !spfV.equals(gbfV)) {
                            modified.add(fName);
                        }
                    } else {
                        if (cbfV != null && !spfV.equals(cbfV)) {
                            modified.add(fName);
                        }
                    }
                }
            }
        } else {
            if (gbf != null && cbf != null) {
                for (String fName : gbf.keySet()) {
                    if (cbf.containsKey(fName)) {
                        if (!cbf.get(fName).equals(gbf.get(fName))) {
                            modified.add(fName);
                        }
                    }
                }
            }
        }
        return modified;
    }

    /** Returns the size of input, excluding the command. */
    public int size() {
        return _rest.length;
    }

    /** Returns the command inputted by user. */
    public String getCommand() {
        return _cmd;
    }

    /**Sets the active branch to branch with NAME. */
    public void setActiveBranch(String name) {
        _tree.setActive(name);
        _activeBranch = Branch.fromFile(name);
    }

    /** Returns the active Branch from file. */
    public Branch getActiveBranch() {
        File activeBranch = Utils.join(GITLET_FOLDER, "active");
        String name = Utils.readContentsAsString(activeBranch);
        return Branch.fromFile(name);
    }

    /** Updates _tracked based on the files tracked by the head commit. */
    public void setTracked() {
        if (!_tracked.isEmpty()) {
            _tracked.clear();
        }
        Branch active = getActiveBranch();
        Commit head = Commit.fromFile(active.getHead());
        HashMap<String, String> filesHC = head.getFiles();
        if (filesHC != null) {
            for (String fName: filesHC.keySet()) {
                _tracked.add(fName);
            }
        }
        if (_tree.getRemoved() != null) {
            ArrayList<String> removed = _tree.getRemoved();
            for (String fName: removed) {
                _tracked.remove(fName);
            }
        }
    }

    /** Compares list of files tracked and files in CWD. Adds files from
     * CWD that are not in tracked. */
    public void setUntracked() {
        File[] filesInCWD = CWD.listFiles();
        for (File f: filesInCWD) {
            if (f.isFile() && !_staged.containsKey(f.getName())
                    && !_tracked.contains(f.getName())) {
                _untracked.add(f.getName());
            }
        }
    }

    /** Checks if the files in the head commit and current working directory
     * match. Files with differences and their difference are added to
     * _modified. */
    public void setModified() {
        _modified.clear();
        Branch active = getActiveBranch();
        Commit head = Commit.fromFile(active.getHead());
        HashMap<String, String> files = head.getFiles();
        for (String f: _tracked) {
            File t = Utils.join(CWD, f);
            String id = Utils.sha1(Utils.readContentsAsString(t));
            String blobName = id.substring(0, 5) + f;
            if (files != null && !blobName.equals(files.get(f))
                    && !_staged.containsKey(f)) {
                _modified.put(f, "(modified)");
            } else if (_staged.containsKey(f)) {
                if (!_staged.get(f).equals(blobName)) {
                    _modified.put(f, "(modified)");
                } else if (!t.exists()) {
                    _modified.put(f, "(deleted)");
                }
            } else if (!t.exists() && !_tree.getRemoved().contains(f)) {
                _modified.put(f, "(deleted)");
            }
        }
    }

    /** Returns the inputs excluding the command. */
    public String[] getRest() {
        return _rest;
    }

    /** A GitTree represents the repository. This is the tree that the
     * all commits made are added to.*/
    private GitTree _tree;

    /** Maps filenames of added files to unique sha1 values which will
     * snapshot file contents at the moment of addition. _staging area
     * is cleared after a commit. */
    private HashMap<String, String> _staged = new HashMap<>();

    /**Contains the names of files in the repo that have not been added or
     * have been removed. Updated when a new file is created by adding to
     * _untracked and when a file is staged by removing from it. */
    private ArrayList<String> _untracked = new ArrayList<>();

    /**Contains the names of files in the repo that have not been added or
     * have been removed. Updated when a new file is added through the add
     * command and when a file is removed by removing from it. */
    private ArrayList<String> _tracked = new ArrayList<>();

    /**Names of modified files mapped to their modification
     * i.e. deleted, modified.*/
    private HashMap<String, String> _modified = new HashMap<>();

    /** Command inputted. */
    private String _cmd;

    /** Other arguments inputted excluding command. */
    private String[] _rest;

    /** The current branch where commits are added to. */
    private Branch _activeBranch;

    /**The position of the master pointer in the GitTree. The string
     * value is equivalent to the sha1 hash value of the commit
     * it is pointing to.*/
    private String _master;

}
