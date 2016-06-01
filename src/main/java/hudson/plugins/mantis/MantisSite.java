package hudson.plugins.mantis;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.plugins.mantis.model.MantisCategory;
import hudson.plugins.mantis.model.MantisIssue;
import hudson.plugins.mantis.model.MantisNote;
import hudson.plugins.mantis.model.MantisProject;
import hudson.plugins.mantis.model.MantisProjectVersion;
import hudson.plugins.mantis.model.MantisViewState;
import hudson.plugins.mantis.soap.MantisSession;
import hudson.plugins.mantis.soap.MantisSessionFactory;
import hudson.plugins.mantis.soap.mantis120.IssueData;
import hudson.plugins.mantis.soap.mantis120.ObjectRef;
import hudson.util.Secret;
import java.io.PrintStream;
import java.math.BigInteger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Reperesents an external Mantis installation and configuration needed to access this
 * Mantis.
 *
 * @author Seiji Sogabe
 */
public final class MantisSite {

    /**
     * the root URL of Mantis installation.
     */
    private final URL url;

    /**
     * MantisVersion of Mantis.
     */
    private MantisVersion version = MantisVersion.V110;

    /**
     * user name for Mantis installation.
     */
    private final String userName;

    /**
     * password for Mantis installation.
     */
    @Deprecated
    private String password;

    /**
     * secret password for Mantis installation.
     */
    private Secret secretPassword;

    /**
     * user name for Basic Authentication.
     */
    private final String basicUserName;

    /**
     * password for Basic Authentication.
     */
    @Deprecated
    private String basicPassword;

    /**
     * secret password for Mantis installation.
     */
    private Secret secretBasicPassword;

    public static MantisSite get(final AbstractProject<?, ?> p) {
        final MantisProjectProperty mpp = p.getProperty(MantisProjectProperty.class);
        if (mpp != null) {
            final MantisSite site = mpp.getSite();
            if (site != null) {
                return site;
            }
        }

        final MantisSite[] sites = MantisProjectProperty.DESCRIPTOR.getSites();
        if (sites.length == 1) {
            return sites[0];
        }

        return null;
    }

    public URL getUrl() {
        return url;
    }

    public MantisVersion getVersion() {
        return version;
    }

    public String getUserName() {
        return userName;
    }

    @Deprecated
    public String getPassword() {
        return password;
    }

    public String getPlainPassword() {
        return Secret.toString(secretPassword);
    }

    public Secret getSecretPassword() {
        return secretPassword;
    }


    public String getName() {
        return url.toExternalForm();
    }

    public String getBasicUserName() {
        return basicUserName;
    }

    @Deprecated
    public String getBasicPassword() {
        return basicPassword;
    }
    
    public String getPlainBasicPassword() {
        return Secret.toString(secretBasicPassword);
    }

    public Secret getSecretBasicPassword() {
        return secretBasicPassword;
    }

    @DataBoundConstructor
    public MantisSite(final URL url, final String version, final String userName,
            final String password, final String basicUserName, final String basicPassword) {
        if (!url.toExternalForm().endsWith("/")) {
            try {
                this.url = new URL(url.toExternalForm() + '/');
            } catch (final MalformedURLException e) {
                throw new AssertionError(e);
            }
        } else {
            this.url = url;
        }
        this.version = MantisVersion.getVersionSafely(version, MantisVersion.V110);
        this.userName = Util.fixEmptyAndTrim(userName);
        this.secretPassword = Secret.fromString(Util.fixEmptyAndTrim(password));
        this.basicUserName = Util.fixEmptyAndTrim(basicUserName);
        this.secretBasicPassword = Secret.fromString(Util.fixEmptyAndTrim(basicPassword));
    }

    public String getIssueLink(int issueNo) {
        String u = getUrl().toExternalForm();
        return String.format("%sview.php?id=%d", u, issueNo);
    }  
    
    public boolean isConnect() {
        final String urlString = url.toExternalForm();
        try {
            final MantisSession session = createSession();
            List<MantisProject> projects = session.getProjects();
        } catch (final MantisHandlingException e) {
            LOGGER.log(Level.WARNING, Messages.MantisSite_FailedToConnectToMantis(urlString, e.getMessage()));
            return false;
        }

        LOGGER.log(Level.INFO, Messages.MantisSite_SucceedInConnectingToMantis(urlString));
        return true;
    }
    
    public MantisProjectVersion createProjectVersion(MantisProjectVersion version) throws MantisHandlingException {
        final MantisSession session = createSession();
        return session.addProjectVersion(version);
    }
    
    public MantisProjectVersion getLatestProjectVersion(MantisProjectVersion version) throws MantisHandlingException {
        if (version == null) {
            return null;
        }
        final MantisSession session = createSession();
        List<MantisProjectVersion> projectVersions = session.getProjectVersions(version.getProjectId());
        MantisProjectVersion uv = null;
        for (MantisProjectVersion projectVersion : projectVersions) {
            //LOGGER.log(Level.INFO, "projectVersion: " + projectVersion);
            if (!projectVersion.isReleased() 
                    && (uv == null || !uv.getDateOrder().before(projectVersion.getDateOrder()))) {
                //LOGGER.log(Level.INFO, "projectVersion found.");
                uv = projectVersion;
            }
        }
        return uv;
    }
    
    public BigInteger checkProjectVersionReleasable(java.math.BigInteger projectId, String version) throws MantisHandlingException {
        if (version == null) {
            throw new MantisHandlingException("version should not be null.");
        }
        final MantisSession session = createSession();
        List<MantisProjectVersion> projectVersions = session.getProjectUnreleasedVersions(projectId);
        //MantisProjectVersion uv = null;
        for (MantisProjectVersion projectVersion : projectVersions) {
            //LOGGER.log(Level.INFO, "projectVersion: " + projectVersion);
            if (projectVersion.getVersion().equals(version) && !projectVersion.isReleased()) {
                return projectVersion.getId();
            }
        }
        return null;
    }
    
    public MantisProjectVersion getLatestNotObsoleteProjectVersion(MantisProjectVersion version) throws MantisHandlingException {
        if (version == null) {
            return null;
        }
        final MantisSession session = createSession();
        List<MantisProjectVersion> projectVersions = session.getProjectVersions(version.getProjectId());
        MantisProjectVersion uv = null;
        for (MantisProjectVersion projectVersion : projectVersions) {
            //LOGGER.log(Level.INFO, "projectVersion: " + projectVersion);
            if (projectVersion.isReleased() && !projectVersion.isObsolete() && !version.getId().equals(projectVersion.getId()) 
                    && (uv == null || !uv.getDateOrder().before(projectVersion.getDateOrder()))) {
                //LOGGER.log(Level.INFO, "projectVersion found: " +projectVersion);
                uv = projectVersion;
            }
        }
        return uv;
    }
    
    public boolean updateProjectVersion2(MantisProjectVersion version, PrintStream logger) throws MantisHandlingException {
        final MantisSession session = createSession();
        return session.updateProjectVersion(version);
    }
    public boolean updateProjectVersion(MantisProjectVersion version) throws MantisHandlingException {
        final MantisSession session = createSession();
        return session.updateProjectVersion(version);
    }

    public MantisIssue getIssue(final int id) throws MantisHandlingException {
        final MantisSession session = createSession();
        return session.getIssue(id);
    }
    
    public hudson.plugins.mantis.soap.mantis120.IssueHeaderData[] /*java.math.BigInteger[]*/ tjd_getTargetVersionIssues(int project , String targetVersion, PrintStream logger) throws MantisHandlingException {
        final MantisSession session = createSession();
        //Utility.log(logger, Messages.tjd_monmsg("xxxxxxx"));
        return session.tjd_getTargetVersionIssues(project, targetVersion, logger);
    }
    
    public void updateIssue(final int id, final String projectVersion, final boolean keepNotePrivate, final int status, PrintStream logger)
            throws MantisHandlingException {

        MantisViewState viewState;
        if (keepNotePrivate) {
            viewState = MantisViewState.PRIVATE;
        } else {
            viewState = MantisViewState.PUBLIC;
        }
        MantisNote note = new MantisNote("Released version " + projectVersion, viewState);

        MantisSession session = createSession();
        //tjd ajouter ici les diverses actions sur tickets
        IssueData issue = session.getIssueData(id);
        //target_version
        issue.setFixed_in_version(projectVersion);        
        //status closed + comment
        issue.setStatus(new ObjectRef(java.math.BigInteger.valueOf(status), null));     
        session.addNote(id, note);  //on ajoute la note liée à la nouvelle release (et cloture du ticket)
        //ticket update
        session.updateIssue(id, issue, logger);  //on clot le ticket      
        
        //
    }

    public List<MantisProject> getProjects() throws MantisHandlingException {
        final MantisSession session = createSession();
        return session.getProjects();
    }
    
    public List<MantisCategory> getCategories(int projectId) throws MantisHandlingException {
        final MantisSession session = createSession();
        return session.getCategories(projectId);
    }

    public int addIssue(MantisIssue issue) throws MantisHandlingException {
        final MantisSession session = createSession();
        return session.addIssue(issue);
    }
    
    private MantisSession createSession() throws MantisHandlingException {
        return MantisSessionFactory.getSession(this);
    }


    public enum MantisVersion {
        /**
         * 1.1.X.
         */
        V110(Messages.MantisSite_MantisVersion_V110()),
        /**
         * 1.2.0a4 and later.
         */
        V120(Messages.MantisSite_MantisVersion_V120());

        private final String displayName;

        private MantisVersion(final String displayName) {
            this.displayName = displayName;
        }

        public static MantisVersion getVersionSafely(final String version, final MantisVersion def) {
            MantisVersion ret = def;
            for (final MantisVersion v : MantisVersion.values()) {
                if (v.name().equalsIgnoreCase(version)) {
                    ret = v;
                    break;
                }
            }
            return ret;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    protected Object readResolve() {
        if (password != null) {
            secretPassword = Secret.fromString(password);
            password = null;
        }
        if (basicPassword != null) {
            secretBasicPassword = Secret.fromString(basicPassword);
            basicPassword = null;
        }
        return this;
    }

    private static final Logger LOGGER = Logger.getLogger(MantisSite.class.getName());
}
