package gitlet;

import java.util.ArrayList;
import java.io.File;
import java.io.Serializable;

/**
 * Acts as the tree that connects separate branches. Makes necessary changes
 * to the folders and file in .gitlet. GitTree only exists if a Gitlet
 * version-control system has been initialized in the current
 * working directory.
 * @author Divya Sivanandan */
public class GitTree implements Serializable {

    /**Initialized a GitTree which initial contains only the "initial commit"
     * and the "master" branch. */
    public GitTree() {
        Branch master = new Branch();
        _latestCommit = getMaster();
        _branches.add("master");
        this.saveTree();
    }

    /**Adds a new commit NODE to an existing branch with NAME. */
    public void addToBranch(String name, Commit node) {
        Branch b = Branch.fromFile(name);
        b.add(node);
        _latestCommit = node.getHash();
        this.saveTree();
    }

    /** Adds a branch (a new pointer) with name NAME at commit NODE.
     * * The ACTIVE branch is used to find the latest commit where the
    * new pointer will diverge from. */
    public void addBranch(String name, Commit node, Branch active) {
        if (this._branches.contains(name)) {
            Utils.message("A branch with that name already exist.");
            System.exit(0);
        } else {
            Branch newBranch = new Branch(name, node, active);
            _branches.add(name);
            this.saveTree();
        }
    }

    /** Adds a branch (a new pointer) with name NAME at commit with ID.
     * The ACTIVE branch is used to find the latest commit where the
     * new pointer will diverge from. */
    public void addBranch(String name, String id, Branch active) {
        addBranch(name, Commit.fromFile(id), active);
    }

    /**Sets the active branch to branch with BRANCHNAME where
     * commits will be added moving forward. */
    public void setActive(String branchName) {
        File activeBranch = Utils.join(Command.GITLET_FOLDER, "active");
        Utils.writeContents(activeBranch, branchName);
    }

    /** Serializes and saves GitTree object to a file.*/
    public void saveTree() {
        File tree = Utils.join(".gitlet", "tree");
        Utils.writeObject(tree, this);
    }

    /**Returns the GitTree object that represents the objects in the
     * Gitlet version-control system.*/
    public static GitTree fromFile() {
        File tree = new File(".gitlet/tree");
        if (!tree.exists()) {
            throw new GitletException("Tree not created.");
        }
        return Utils.readObject(tree, GitTree.class);
    }

    /** Checks if a branch with NAME already exists. Returns TRUE if
     * a branch with that name exists. */
    public boolean branchExists(String name) {
        if (_branches.contains(name)) {
            return true;
        } else {
            File b = new File(".giltet/BRANCHES/");
            return b.exists();
        }
    }

    /** Remove a branch with name NAME. */
    public void removeBranch(String name) {
        _branches.remove(name);
        Branch.remove(name);
        this.saveTree();
    }

    /** Finds the latest common ancestor of two branches, CURR and GIVEN,
     * to be used in merge and returns the sha1 of the commit. */
    public String findSplit(Branch curr, Branch given) {
        String currHC = curr.getHead(), givenHC = given.getHead();
        Commit currComm = Commit.fromFile(currHC);
        Commit givenComm = Commit.fromFile(givenHC);
        String split1 = currHC, split2 = givenHC;
        int d1 = 0, d2 = 0;
        if (!given.containsCommit(currHC)) {
            boolean found = false;
            while (!found) {
                String[] parents = currComm.getParent();
                d1++;
                if (given.containsCommit(parents[0])) {
                    split1 = parents[0];
                    found = true;
                }
                if (parents.length > 1) {
                    if (given.containsCommit(parents[1])) {
                        split1 = parents[1];
                        found = true;
                    }
                }
                currComm = Commit.fromFile(parents[0]);
            }
        }
        if (!curr.containsCommit(givenHC)) {
            boolean found = false;
            while (!found) {
                String[] parents = givenComm.getParent();
                if (curr.containsCommit(parents[0])) {
                    split2 = parents[0];
                    found = true;
                }
                if (parents.length > 1) {
                    if (curr.containsCommit(parents[1])) {
                        split2 = parents[1];
                        found = true;
                    }
                }
                givenComm = Commit.fromFile(parents[0]);
            }
        }
        Commit curB = Commit.fromFile(currHC);
        while (!split2.equals(currHC)) {
            d2++;
            currHC = curB.getParent()[0];
            curB = Commit.fromFile(currHC);
        }
        if (d2 < d1) {
            return split2;
        } else {
            return split1;
        }
    }

    /**Returns a list of the name of branches. */
    public ArrayList<String> getBranches() {
        return _branches;
    }

    /**Returns the most recent commit made. */
    public String getLatestCommit() {
        return _latestCommit;
    }

    /** Returns the sha1 of the commit that the master pointer
     * is pointing to. */
    public String getMaster() {
        Branch master = Branch.fromFile("master");
        return master.getHead();
    }

    /** Returns the list of files that have been removed. */
    public ArrayList<String> getRemoved() {
        return _removed;
    }

    /** Adds FILENAME to _removed. */
    public void addRemoved(String filename) {
        _removed.add(filename);
        this.saveTree();
    }

    /** Clear _removed. */
    public void clearRemoved() {
        _removed.clear();
        this.saveTree();
    }

    /** Takes in the commit id COMMITID and sets the value of the
     * latest commit.*/
    public void setLatestCommit(String commitID) {
        _latestCommit = commitID;
        this.saveTree();
    }

    /**Contains the list of branch names as contained in the BRANCHES folder.*/
    private ArrayList<String> _branches = new ArrayList<>();


    /** The most recent commit made. */
    private String _latestCommit;


    /** List of files that need to be removed in the next commit. */
    private ArrayList<String> _removed = new ArrayList<>();

}
