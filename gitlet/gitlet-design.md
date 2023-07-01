# Gitlet Design Document

**Name**: Divya Sivanandan

## Classes and Data Structures
###Commit
This class represents a commit node. It implements the java.io.Serializable class and java.util.Date.

**Fields**:
1. HashMap<String, String> _files: Maps filenames of all file(s) in the commit to unique sha1 values.
2. String _message: message included for a specific commit.
3. String _parent: The most recent commit before the current commit is the parent commit. This is the sha1 ID of the parent commit.
4. String _timestamp: The current time, date, etc or else the default timestamp for initial commit.
5. String sha1value: The sha1 hash value of the current commit.
6. DateTimeFormatter TIME_FORMAT: The formatter for _timestamp.
7. COMMIT_FOLDER: Reference to COMMITS folder in .gitlet.
8. String _initialID: the sha1 id of the initial commit.

###Command 
This class helps handle the course of action of the program depending on the type of input. 
Errors due to input type or number are also handled.

**Fields**:
1. File CWD: Refers to the current working directory.
2. File GITLET_FOLDER: The .gitlet folder where all data in the repository is tracked. 
3. File STAGING_AREA: A folder that contains with names of files staged which contains their blobs.
4. File BLOBS_FOLDER: A folder that contains snapshots of file contents. 
5. GitTree _tree: Current GitTree that represents the connection between all commits and branches. //might not need
6. HashMap<String, String> _staged: Maps filenames of added files to unique sha1 values which will snapshot file contents at the moment of addition. It is cleared after a commit.
7. ArrayList<String> _untracked: Contains the names of files in the repo that have not been added or have been removed.
8. ArrayList<String> _tracked: Contains the names of files in the repo that are tracked by the head commit. 
9. HashMap<String, String> _modified: Names of modified files mapped to their modification i.e. deleted, modified.
10. String _cmd: The command inputted. 
11. String[] _rest: Operands inputted.
12. String _master: The position of the master pointer in the GitTree. The string value is equivalent to the sha1 hash value of the commit it is pointing to.
13. Branch _activeBranch: the current branch. Commits made will be added to this branch.

###Branch 
This is to represent a series of commits.

**Fields**:
1. LinkedList<Commit> _commits: A linked list of commits that represents a single branch in the GitTree.
2. String _name: Name of the branch.
3. String _head: The sha1 of the most recent commit in the branch.
4. File BRANCH_FOLDER: Creates a "BRANCHES" directory in ".gitlet".

###GitTree (might be unnecessary)
Bring all the other classes together. Acts as the tree that connects separate branches. Makes necessary changes to the folders and file in .gitlet. 
All necessary folders and files will also be initialized and organized in this class.

**Fields**
1. ArrayList<String> _branches: Contains the list of branch names. A Branch is essentially a series of Commits.
2. String _latestCommit: to assign as parent to the following commit.
3. String _removed: List of files that need that have been removed and will be updated in the next commit.

## Algorithms
###Commit
1. Commit(): A constructor that creates a blank initial commit to use when a gitlet repository is set up.
2. Commit(String msg, HashMap<String, String> files, String[] parent): Constructs a commit based on input.
3. printCommit() : Prints a commit with format required by log command.
4. getMessage(): Returns the string msg that was inputted when the commit was constructed.
5. getTimestamp(): Returns the timestamp when a commit was created. 
6. getParent: Returns the sha1 hash value of the parent commit. 
7. getHash(): Returns the sha1 hash value of the commit.
8. getFiles(): Returns the files saved in the commit.
9. saveCommit(): Creates a file with name "sha1 Hash of commit", serializes the commit object and saves it to COMMITS folder in .gitlet.
10. fromFile(String sha1): Returns the commit object with specified sha1 ID.
11. getInitial(): returns the sha1 of the initial commit.

###Branch
1. Branch(): creates a branch with Commit() as it's starting node and name "master". A new file with name <branch-name> is created in the BRANCHES folder.
2. Branch(String name, Commit node, Branch active): Provide a new pointer at provided commit node named NAME. A new file with name of branch is created in the branches folder and the branch is saved in it.
3. getHead(): Returns the most recent commit made in the branch.
4. fromFile(String name): Returns the Branch named NAME saved in the BRANCHES folder in .gitlet.
5. saveBranch(): Searches for file with name <branch-name> in the BRANCHES folder. The file is updated to reflect changes on the branch.
6. add(Commit node): Add the commit to current branch.
7. remove(String name): Deletes a branch with name NAME from the branches folder in the Gitlet folder.
8. exists(String name): Returns true if a branch with NAME exists. 
9. getName(): Returns the name of the branch.
10. setHead(String ID): Sets the head pointer of the branch to commit with given ID.
11. containsCommit(String commID): Returns true if branch contains commit with commID.
12. getCommits() : Returns the commits saved in the branch.

###GitTree
1. GitTree(): Constructs a GitTree with only the master branch and initial commit. Serializes and saves tree to "tree" file in the gitlet folder.
2. addToBranch(String name, Commit node): Provide a new pointer at provided commit node. A new branch file is created in the BRANCHES folder if the branch doesn't already exist.
3. addBranch(String name, Commit node, Branch active): Add a new branch pointer to the GitTree.
4. addBranch(String name, String commID, Branch active): Add a new branch pointer to the GitTree.
5. setActive(String branchName): Set the active branch as branchName. Saves branch to file "active" in the gitlet folder.
6. fromFile(): Returns the deserialized GitTree from .gitlet.
7. saveTree(): Saves current version of tree to file in .gitlet.
8. branchExists(String name): Returns true if a branch with NAME exists.
9. removeBranch(String name): Deletes branch with name NAME.
10. findSplit(Branch curr, Branch given): Finds the latest common ancestor of two branches and returns the sha1 of the commit.
11. addRemoved(String filename): Adds filename to _removed arraylist.
12. clearRemoved(): clears _removed.
13. getBranches(): Returns all the branches in the tree as a list. 
14. getLatestCommit(): Returns the most recent commit's ID.
15. getMaster(): Returns the sha1 of the commit pointed by the master branch's head.
16. getRemoved(): Returns the list of files that have been removed and are yet to be committed.
17. setLatestCommit(String commID): sets the value of the latest commit to commID.

###Command
1. Command(String[] input): Constructs a command and assigns values to instance variable depending on whether or not a Gitlet version control system has been initialized. Takes a String array INPUT from Main and does necessary error handling.
2. init(): initializes a .gitlet repository in current directory. Folders - BRANCHES, COMMITS, and BLOBS - would be created in .gitlet. If .gitlet folder already exist in the current directory, an error message is printed. All other commands can only be accepted if init() has been called in the directory.
3. add(String[] fName): Adds a copy of the file as it is currently exists in the working directory to the staging area. Overwrites existing file in staging area if it already exists there.
4. commit(String msg): Creates a new Commit with message, and files by calling the Commit(String msg, HashMap<String, String> files, GitTree _tree) constructor. Clears _staged. The HEAD pointer is moved to reflect the most recent commit. Checks _removed if any files from the previous commit should be removed. Update the head file of current branch.
5. mergeCommit(String msg, String[] parents): Creates a new commit for merges.
6. whichCheckout(): Determines which checkout function to call based on the input.
7. checkoutFile(String filename): Takes the version of the file with FILENAME as it exists in the head commit of the active branch and puts it in the working directory, overwriting the version of the file that's already there if there is one. The new version of the file is not staged.
8. checkoutBranch(String branchName): Takes all the files in the head commit of branch with branchName and puts them in the working directory, overwriting the versions of the files already there if they exist. Set branch with branchName as the active branch. Files tracked by current branch but not by branch with branchName are deleted. The staging area is cleared, unless the checked-out branch is the current branch.
9. checkoutCommit(String commitID, String filename): Takes the version of the file with FILENAME as it exists in the given commitID, and puts it in the working directory, in the working directory, overwriting the version of the file that's already there if there is one. The new version of the file is not staged.
10. rm(String filename): If file in _staged, remove from _staged. If file is tracked in current commit, adds file to _removed. Otherwise, do nothing.
11. log(): starting at the HEAD pointer in _tree, print out information about each commit backwards.
12. globalLog(): displays information regarding all commits ever made by recursively reading files in the COMMIT folder.
13. find(String msg): Prints out the sha1 IDs of all commits in _tree with the given msg by searching the COMMITs folder.
14. status(): Prints out branches that exists, files in _staged, _removed, _modified, and _untracked.
15. newBranch(): creates a new Branch with NAME that points at the current HEAD. A file with <branch-name> is created and saved in the BRANCHES folder.
16. rmBranch(): deletes branch with NAME in the BRANCHES folder. To changes made to the COMMIT folder.
17. reset(String commitID): checkout all files tracked by the commitID given. Removes tracked files that are not present in the commit.
18. merge(): Merges the files from the given branch into the current branch. Files that have been modified in the given branch since the split point, but not in the current branch should be checkout out using the head commit in the given branch and automatically staged. If a file was removed from both the current and given branch, but exists in CWD, it should be left untracked. Files only present in the current branch should remain. Files only present in the given branch are checked out and staged. Files absent in the given branch are removed. Files absent in current branch remain absent. Files modified in different ways are in conflict and handled uniquely. Finally, automatically commit.
19. getActiveBranch(): gets the _activeBranch from the active file in gitlet folder.
20. setMaster(): set the _master instance variable to the lastest commit in the master branch.
21. setActiveBranch(String name): Sets the active branch to branch with name NAME.
22. getModified() : Returns an arraylist of files that have been modified differently to be used to handle merge conflicts.
23. size() : Returns the number of operands inputed. 
24. getCommand(): Returns the command inputted by the user. 
25. setTracked(): Updates _tracked based on the files tracked by the head commit.
26. setUntracked(): Compares list of files tracked and files in CWD. Adds files from CWD that are not in tracked.
27. setModified(): Checks if the files in the head commit and current working directory match. Files with differences and their difference are added to _modified.
28. getRest(): Returns the inputs excluding the command.

## Persistence

####.gitlet FOLDER
* created along with all the neccessary in it when the "init" command is passed in.
* Folders in .gitlet: BRANCHES, HEAD, COMMITS, BLOBS
* Files contained: active (contains the name of the active branch), tree (contains the serialized GitTree object that represent the mapping that exists between each commit and different branches), tracked (contains the serialized arraylist with names of files currently tracked)

###BRANCHES FOLDER
* Branches are folders in the .gitlet directory named <branch name>_<first 4 sha1 digits>
* Branches contain files representing commits. 
* The active branch is represented by a file named aCTIVE_bRANCH in the .gitlet directory. 

###HEAD FOLDER
* The HEAD is used to store all the HEAD pointers of branches. //might not need if have Branch class
* Files are saved as <branch-name_HEAD>. 
* Updates are made each time a commit is made to add to a branch.

###BLOBS FOLDER
* The BLOBS folder contains files named <sha1 of _FILE_ commited> that contains the contents of _FILE_.

###COMMITS FOLDER
* The COMMITS folder contains all the commits ever made in the directory where .gitlet was initialized.
* The commit files will have name <sha1 value> and will contain the serialized commit object.
* The commit files will have the names of the files tracked and the sha1 values of the file contents that can be looked up in the blobs folder if the contents of the files are required.
