package gitlet;

public class Remote {

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



    private String url;

    private Branch branch;

    private String remoteDirectory;

    private String name;


}
