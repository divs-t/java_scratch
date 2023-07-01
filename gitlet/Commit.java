package gitlet;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.time.ZonedDateTime;
import java.io.File;
import java.util.List;


/** A Commit object contains all the information that calling the
 * COMMIT command.
 * @author Divya Sivanandan */
public class Commit implements Serializable {

    /**Folder where all commits are saved.*/
    static final File COMMIT_FOLDER = new File(".gitlet/COMMITS");

    /** Automatic commit that starts the version-control system.
     * In other words, the root of the GitTree.
     */
    public Commit() {
        _message = "initial commit";
        _parent = null;
        _timestamp = "Wed Dec 31 16:00:00 1969 -0800";
        _files = null;
        byte[] serialized = Utils.serialize(this);
        _sha1value = Utils.sha1(serialized);
        _initialID = _sha1value;
        this.saveCommit();
    }

    /** Constructs a new Commit object with message MSG, that tracks files in
     * FILES which contains filenames mapped to blobNames, with parents PARENTS.
     * A commit will only have more than one parent if it is a merge commit.*/
    public Commit(String msg, HashMap<String, String> files, String[] parents) {
        _parent = parents;
        _message = msg;
        ZonedDateTime now = ZonedDateTime.now();
        _timestamp = TIME_FORMAT.format(now) + " -0800";
        _files = files;
        byte[] serialized = Utils.serialize(this);
        _sha1value = Utils.sha1(serialized);
        this.saveCommit();
    }

    /** Prints a commit as required for the log command. */
    public void printCommit() {
        System.out.println("===");
        System.out.format("commit %s%n", this.getHash());
        if (this.getParent() != null && this.getParent().length > 1) {
            String p1 = this.getParent()[0].substring(0, 7);
            String p2 = this.getParent()[1].substring(0, 7);
            System.out.format("Merge: %s %s%n", p1, p2);
        }
        System.out.format("Date: %s%n", this.getTimestamp());
        System.out.println(this.getMessage());
        System.out.println();
    }


    /** Returns the message that was inputted when the commit was made.*/
    public String getMessage() {
        return this._message;
    }

    /** Returns the timestamp of the commit when it was created. */
    public String getTimestamp() {
        return this._timestamp;
    }

    /** Returns the sha1 hash value of parent commit. */
    public String[] getParent() {
        return this._parent;
    }

    /** Returns the sha1 hash value of the commit. */
    public String getHash() {
        return this._sha1value;
    }

    /** Returns the files saved in this commit as a HashMap. */
    public HashMap<String, String> getFiles() {
        return this._files;
    }

    /**Creates a file with name "sha1 Hash of commit", serializes the
     *  commit object and saves it to COMMITS folder. */
    public void saveCommit() {
        File c = Utils.join(COMMIT_FOLDER, this.getHash());
        Utils.writeObject(c, this);
    }

    /** Takes in the unique SHA1 of a commit and returns the commit saved
     * in a file in the COMMIT_FOLDER with matching sha1. */
    public static Commit fromFile(String sha1) {
        int size = sha1.length();
        File c = new File(COMMIT_FOLDER, sha1);
        if (size < Utils.UID_LENGTH) {
            List<String> files = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
            for (String id : files) {
                String shortened = id.substring(0, size);
                if (shortened.equals(sha1)) {
                    c = new File(COMMIT_FOLDER, id);
                }
            }
        } else if (!c.exists()) {
            Utils.message("No commit with that id exists.");
            System.exit(0);
        }
        return Utils.readObject(c, Commit.class);
    }

    /** Returns the sha1 of the initial commit. */
    public static String getInitial() {
        return _initialID;
    }


    /** The files contained in the commit. File names are mapped to
     * unique sha1 values. */
    private HashMap<String, String> _files;

    /** Message included in the commit. */
    private String _message;

    /** Filename of the parent commit where the parent commit can be found. */
    private String[] _parent;

    /** Timestamp for the commit made. */
    private String _timestamp;

    /**The sha1 hash value. */
    private String _sha1value;

    /**Initial commit ID. */
    private static String _initialID;

    /**Representation for timestamp.*/
    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy");
}
