package gitlet;

import java.io.IOException;
import java.util.LinkedList;
import java.io.File;
import java.io.Serializable;

/**Represents a series of commits.
 * @author Divya Sivanandan */
public class Branch implements Serializable {

    /** Folder that contains all the serializes branches that exist
     * in the GitTree. */
    static final File BRANCH_FOLDER = Utils.join(".gitlet", "BRANCHES");

    /**Constructs the initial master branch when a Gitlet version-control
     * system is initialized.*/
    public Branch() {
        _commits = new LinkedList<>();
        _commits.add(new Commit().getHash());
        _name = "master";
        _head = _commits.get(0);
        this.saveBranch();
    }

    /**Creates new BRANCH with name NAME and head pointer set at NODE. Copies
     * the existing commits from the ACTIVE branch onto itself. */
    public Branch(String name, Commit node, Branch active) {
        _commits = new LinkedList<>();
        LinkedList<String> activeCommits = active.getCommits();
        for (String id: activeCommits) {
            _commits.add(id);
        }
        _head = node.getHash();
        _name = name;
        this.saveBranch();
    }

    /**Reads in, deserializes and returns a BRANCH from a file with name NAME in
     * the BRANCH_FOLDER. If a branch with name passed in doesn't exist,
     * a GitException error is thrown. */
    public static Branch fromFile(String name) {
        File b = new File(".gitlet/BRANCHES/" + name);
        if (!b.exists()) {
            Utils.message("No such branch exists.");
            System.exit(0);
        }
        return Utils.readObject(b, Branch.class);
    }

    /**Saves a branch to a file in the BRANCH_FOLDER for future use.*/
    public void saveBranch() {
        try {
            File b = new File(".gitlet/BRANCHES/" + this._name);
            if (!b.exists()) {
                b.createNewFile();
            }
            Utils.writeObject(b, this);
        } catch (IOException exp) {
            throw new GitletException();
        }
    }

    /** Adds a commit NODE to branch. */
    public void add(Commit node) {
        this._commits.add(node.getHash());
        _head = node.getHash();
        this.saveBranch();
    }


    /** Deletes a branch with name NAME from the BRANCHES folder
     * in the Gitlet folder. */
    public static void remove(String name) {
        File b = Utils.join(BRANCH_FOLDER, name);
        b.delete();
    }

    /** Returns the NAME of the current branch. */
    public String getName() {
        return this._name;
    }

    /**Returns the sha1 hash of most recent commit in the branch.
     * This is the HEAD commit. */
    public String getHead() {
        return _head;
    }

    /** Sets the head pointer to commit with ID given. */
    public void setHead(String id) {
        _head = id;
        this.saveBranch();
    }

    /** Takes in a COMMID and checks if branch contains the commit with COMMID.
     * Returns true if the branch contains the commit. */
    public boolean containsCommit(String commID) {
        return this._commits.contains(commID);
    }

    /** Returns all the commits saved in this branch. */
    public LinkedList<String> getCommits() {
        return this._commits;
    }


    /** Returns the sha1 id of the head commit which is the most
     * recent commit in the branch. */
    private String _head;

    /**A linked-list of the sha1 hash values of commits that represents
     * a single branch in the GitTree.*/
    private LinkedList<String> _commits;

    /**Branch name.*/
    private String _name;

}
