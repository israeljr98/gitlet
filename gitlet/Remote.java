package gitlet;

import java.io.Serializable;

public class Remote implements Serializable {

    public Remote(String name, String remoteDirectory) {
//        this.url = url;
        this.name = name;
        this.remoteDirectory = remoteDirectory;
    }

    public String getRemoteDirectory() {
        return this.remoteDirectory;
    }

    public String getName() {
        return this.name;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    private String url;

    private Branch branch;

    private String remoteDirectory;

    private String name;


}
