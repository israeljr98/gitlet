package gitlet;

import java.io.Serializable;

public class Remote implements Serializable {

    public Remote(String name, String remoteDirectory) {
//        this.url = url;
        this._name = name;
        this._remoteDirectory = remoteDirectory;
    }

    public String getRemoteDirectory() {
        return this._remoteDirectory;
    }

    public String getName() {
        return this._name;
    }

    public Branch getBranch() {
        return _branch;
    }

    public void setBranch(Branch branch) {
        this._branch = branch;
    }

    /**  */
    public boolean isOnlineRemote() {
        return _remoteDirectory.startsWith("http://") || _remoteDirectory.startsWith("https://");
    }

    private String _url;

    private Branch _branch;

    private String _remoteDirectory;

    private String _name;


}
