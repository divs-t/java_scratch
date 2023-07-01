package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Divya Sivanandan */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS is a String array that contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        Command curr = new Command(args);
        String cmd = curr.getCommand();
        if (cmd.equals("init")) {
            checkOperands(curr.getRest(), 0);
            curr.init();
        } else if (cmd.equals("add")) {
            hasOperands(curr.getRest(), 0);
            curr.add(curr.getRest());
        } else if (cmd.equals("commit")) {
            checkOperands(curr.getRest(), 1);
            curr.commit(curr.getRest()[0]);
        } else if (cmd.equals("checkout")) {
            curr.whichCheckout();
        } else if (cmd.equals("log")) {
            checkOperands(curr.getRest(), 0);
            curr.log();
        } else if (cmd.equals("rm")) {
            checkOperands(curr.getRest(), 1);
            curr.rm(curr.getRest()[0]);
        } else if (cmd.equals("global-log")) {
            curr.globalLog();
        } else if (cmd.equals("find")) {
            checkOperands(curr.getRest(), 1);
            curr.find();
        } else if (cmd.equals("branch")) {
            curr.newBranch();
        } else if (cmd.equals("rm-branch")) {
            curr.rmBranch();
        } else if (cmd.equals("reset")) {
            curr.reset();
        } else if (cmd.equals("status")) {
            curr.status();
        } else if (cmd.equals("merge")) {
            checkOperands(curr.getRest(), 1);
            curr.merge();
        } else {
            System.out.println("No command with that name exists.");
        }
        System.exit(0);
    }


    /** Checks whether a gitlet folder has been created in the
     * current working directory. Returns true if it exists. */
    public static boolean initialized() {
        return Command.GITLET_FOLDER.exists();
    }

    /** Returns error and exits if size of REST does not match SIZE. */
    public static void checkOperands(String[] rest, int size) {
        if (rest.length != size) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
    }

    /** Returns error and exits if the size of REST is equal to SIZE. */
    public static void hasOperands(String[] rest, int size) {
        if (rest.length == 0) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
    }





}
