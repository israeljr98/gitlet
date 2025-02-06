package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Israel Rodriguez
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        Gitlet g = new Gitlet(System.getProperty("user.dir"));
        String input1 = null; String input2 = null;
        String input3 = null; String input4 = null;
        try {
            input1 = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        if (args.length > 1) {
            input2 = args[1];
        }
        if (args.length > 2) {
            input3 = args[2];
        }
        if (args.length > 3) {
            input4 = args[3];
        }
        try {
            if (!g.commands().contains(input1)) {
                throw Utils.error("No command with that name exists.");
            } else if (input1.equals("init")) {
                g.init();
            } else if (input1.equals("add")) {
                g.add(input2);
            } else if (input1.equals("commit")) {
                g.commit(input2, null);
            } else if (input1.equals("rm")) {
                g.rm(input2);
            } else if (input1.equals("status")) {
                g.status();
            } else if (input1.equals("find")) {
                g.find(input2);
            } else if (input1.equals("log")) {
                g.log();
            } else if (input1.equals("global-log")) {
                g.globalLog();
            } else if (input1.equals("checkout")) {
                if (input4 != null) {
                    g.checkout(input2, input3, input4);
                } else if (input3 != null) {
                    g.checkout(input2, input3);
                } else {
                    g.checkoutBranch(input2);
                }
            } else if (input1.equals("branch")) {
                g.branch(input2);
            } else if (input1.equals("rm-branch")) {
                g.rmBranch(input2);
            } else if (input1.equals("reset")) {
                g.reset(input2);
            } else if (input1.equals("merge")) {
                g.merge(input2);
            } else if (input1.equals("add-remote")) {
                g.addRemote(input2, input3);
            } else if (input1.equals("rm-remote")) {
                g.removeRemote(input2);
            } else if (input1.equals("push")) {
                g.push(input2, input3);
            } else if (input1.equals("fetch")) {
                g.fetch(input2, input3);
            } else if (input1.equals("pull")) {
                g.pull(input2, input3);
            }
        } catch (GitletException ge) {
            System.out.println(ge.getMessage());
            System.exit(0);
        }
    }
}
