package hudson.plugins.mantis.soap;

import hudson.plugins.mantis.MantisHandlingException;
import hudson.plugins.mantis.model.MantisCategory;
import hudson.plugins.mantis.model.MantisIssue;
import hudson.plugins.mantis.model.MantisNote;
import hudson.plugins.mantis.model.MantisProject;
import hudson.plugins.mantis.model.MantisProjectVersion;
import hudson.plugins.mantis.soap.mantis120.IssueData;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.List;

/**
 *
 * @author sogabe
 */
public interface MantisSession {

    void addNote(final int id, final MantisNote note) throws MantisHandlingException;
    
    public void updateIssue(final int id, final IssueData data, PrintStream logger) throws MantisHandlingException;

    String getVersion() throws MantisHandlingException;

    MantisIssue getIssue(final int id) throws MantisHandlingException;
    
    public IssueData getIssueData(final int id) throws MantisHandlingException;

    List<MantisProject> getProjects() throws MantisHandlingException;
    
    List<MantisCategory> getCategories(int projectId) throws MantisHandlingException;
    
    int addIssue(MantisIssue issue) throws MantisHandlingException;
    
    /*java.math.BigInteger[]*/ hudson.plugins.mantis.soap.mantis120.IssueHeaderData[] tjd_getTargetVersionIssues(int projectID , String targetVersion, PrintStream logger) throws MantisHandlingException;
    
    MantisProjectVersion addProjectVersion(MantisProjectVersion version) throws MantisHandlingException;
    
    //BigInteger updateProjectVersion2(MantisProjectVersion version, PrintStream logger) throws MantisHandlingException;
    boolean updateProjectVersion(MantisProjectVersion version) throws MantisHandlingException;
    
    List<MantisProjectVersion> getProjectVersions(BigInteger projectId) throws MantisHandlingException;

    List<MantisProjectVersion> getProjectUnreleasedVersions(BigInteger projectId) throws MantisHandlingException;
}
