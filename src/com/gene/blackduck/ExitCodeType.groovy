pakcage com.gene.blackduck

// blackduck hub detect exitcode.java
public enum ExitCodeType implements Serialiable {
    UNEXPECTED(-1, "blackduck unexpected error."),
    SUCCESS(0, "the project successfully passed the blackDuck open-source governance gate!"),
    FAILURE_HUB_CONNECTIVITY(1, "unable to connect to the blackduck portal")
    FAILURE_TIMEOUT(2, "unable to scan the project with blackduck within the defined timeframe. scan was aborted because of timeout.")
    FAILURE_POLICY_VIOLATION(3, "the project Failed the blackduck open-source governance gate")
    FAILURE_PROXY_CONNECTIVITY(4, "failed connectiong to a proxy")
    FAILURE_BOM_TOOL(5, "the project's build too failed to run blackduck's bill-of-material discovery")
    FAILURE_SCAN(6, "blackduck scan failure")
    FAILURE_GENERAL_ERROR(99, "blackduck general error")
    FAILURE_UNKNOWN_ERROR(100, "blackduck unknonw error")

    public final int exitcode
    public final String desc

    public ExitCodeType(final int exitCode, final String desc) {
        this.exitCode = exitCode;
        this.desc = desc;

    }
    static public ExitCodeType lookup(int exitCode) {
        return (values.find{it.exitCode == exitCode})
    }
}