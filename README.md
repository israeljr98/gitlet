# Gitlet

## What is Gitlet?

Gitlet is a tiny, simpler version of the Git version control system, written entirely in Java. It is capable of replicating a majority of Git's essential local functions, including initializing a repository, creating commits, instantiating multiple branches, and checking out previous versions of files.

## Gitlet Commands:

- ### init:
  - **Description**: Initializes a gitlet repository in the current directory. The files inside of the .gitlet directory store gitlet objects as byte streams with the help of Java I/O operations. This allows for the persistence of commit trees after a Gitlet command is run. 
  - **Usage**: `java gitlet.Main init` ![](gitlet/gitlet_gifs/gitlet_init.gif)
- ### status:
  - **Description**: Prints a description of the current state of the staging area. This description shows the name of all existing branches, the files that have been staged for addition, the files are staged for removal, and all the untracked files present in the current directory.
  - **Usage**: `java gitlet.Main status` ![](gitlet/gitlet_gifs/gitlet_status.gif)
- ### add:
  - **Description**: Stages a file with filename for addition.
  - **Usage**: `java gitlet.Main add <filename>` ![](gitlet/gitlet_gifs/gitlet_add.gif)
- ### rm:
  - **Description**: Unstages the file if it is currently staged for addition. If the file is tracked in the current commit, it stages it for removal and removes the file from the working directory if the user has not already done so.
  - **Usage**: `java gitlet.Main rm <filename>` ![](gitlet/gitlet_gifs/gitlet_rm_stage.gif)
- ### commit:
  - **Description**: Saves a snapshot of the files in the staging area so they can be restored at a later time if needed. Additioanlly, the command invoking records metadata pertaining to the commit, including a descriptive message, a timestamp, and a generated SHA-1 ID.
  - **Usage**: `java gitlet.Main commit <message>` ![](gitlet/gitlet_gifs/gitlet_commit.gif)
- ### log:
  - **Description**: Displays information about each commit along with its metadata starting from the current head commit all the way to the initial commit.
  - **Usage**: `java gitlet.Main log` <br> ![](gitlet/gitlet_gifs/gitlet_log.gif)
- ### branch:
  - **Description**: Creates a branch with the given branch name and points it to the current head node. In this instance, a branch is simply a name of a reference to a commit node.
  - **Usage**: `java gitlet.Main branch <branch name>` ![](gitlet/gitlet_gifs/gitlet_branch.gif)
- ### checkout:
  - **Description**: There are 3 possible use cases for this command.
    1. Takes the version of file as it exists in the head commit, and places it in the current working directory, overwriting the version that is there already if present. It does not stage the new version. <br> **Usage 1**: <br> `java gitlet.Main checkout -- <file name>` ![](gitlet/gitlet_gifs/gitlet_checkout_dash.gif)
    2. Takes the version of file as it exists in the commit with the given ID, and places it in the current working directory, overwriting the version that is there already if present. It does not stage the new version. <br> **Usage 2**: <br> `java gitlet.Main checkout <commit ID> -- <file name>` ![](gitlet/gitlet_gifs/gitlet_checkout_commitid.gif)
    3. Takes all files in the commit at the head of the given branch, and places them in the current working directory, overwriting the versions of the files that are already there if they exist. Also, at the end of this command, the given branch will now be considered the current branch (HEAD), and the staging area is cleared. <br> **Usage 3**: <br> `java gitlet.Main checkout <branch name>` ![](gitlet/gitlet_gifs/gitlet_checkout_branch.gif)
- ### find:
  - **Description**: Prints out the log entries of all the commits that share the given commit message.
  - **Usage**: `java gitlet.Main find <commit message>` ![](gitlet/gitlet_gifs/gitlet_find.gif)
- ### rm-branch:
  - **Description**: Deletes the branch with the given name. Specifically, it only deletes the reference to the branch. Any commits made on the branch persist.
  - **Usage**: `java gitlet.Main rm-branch <branch name>` ![](gitlet/gitlet_gifs/gitlet_rmbranch.gif)
