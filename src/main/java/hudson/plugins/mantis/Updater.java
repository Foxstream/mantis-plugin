package hudson.plugins.mantis;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.mantis.changeset.ChangeSet;
import hudson.plugins.mantis.changeset.ChangeSetFactory;
import hudson.plugins.mantis.model.MantisIssue;
import hudson.plugins.mantis.model.MantisProjectVersion;
import hudson.scm.ChangeLogSet.Entry;
import java.io.IOException;

import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mantis update Logic.
 *
 * @author Seiji Sogabe
 */
final class Updater {

    private static final String CRLF = System.getProperty("line.separator");
    
    private final MantisIssueUpdater property;

    Updater(final MantisIssueUpdater property) {
        this.property = property;
    }

    boolean perform(final AbstractBuild<?, ?> build, final BuildListener listener) throws MantisHandlingException {

        final PrintStream logger = listener.getLogger();

        final MantisSite site = MantisSite.get(build.getProject());
        if (site == null) {
            Utility.log(logger, Messages.Updater_NoMantisSite());
            build.setResult(Result.FAILURE);
            return true;
        }

        final String rootUrl = Hudson.getInstance().getRootUrl();
        if (rootUrl == null) {
            Utility.log(logger, Messages.Updater_NoHudsonUrl());
            build.setResult(Result.FAILURE);
            return true;
        }
        
        //check build status before starting mantis operation
        final boolean update = !build.getResult().isWorseThan(Result.UNSTABLE);
        if (!update) {
            Utility.log(logger, Messages.Updater_KeepMantisIssueIdsForNextBuild());
            //build.addAction(new MantisCarryOverChangeSetAction(chnageSets));
            return true;
        }
        String projectVersion;
        String projectDescription;
        String issuesList;
        
        //Release operations on Mantis  (todo: member isKeepNotePrivate to rename to activateMantisOperations)
        if (this.property.isKeepNotePrivate())  
        {
            Utility.log(logger, Messages.tjd_monmsg("Performing mantis operations..." ));    

            final List<ChangeSet> chnageSets = new ArrayList<ChangeSet>(); 

            MantisProjectProperty mpp = MantisProjectProperty.get(build);
            int projectId = mpp.getProjectId();
                        
            if (build.getBuildVariables().get("Maintenance") != null)
                projectVersion = build.getBuildVariables().get("Majeure")+"."+build.getBuildVariables().get("Mineure")+"."+build.getBuildVariables().get("Maintenance");
            else
                projectVersion = build.getBuildVariables().get("Majeure")+"."+build.getBuildVariables().get("Mineure");
            
            projectDescription = build.getBuildVariables().get("Description");

            //Get issues for the selected project and the target version
            hudson.plugins.mantis.soap.mantis120.IssueHeaderData[] issuesHeaders = null;
            issuesHeaders = site.tjd_getTargetVersionIssues(projectId, projectVersion, logger);


            issuesList = "";  //will be used later for the changelog update
            //Browse all issues for this project and this version (as target version) to check that all are either validated either resolved
            for (hudson.plugins.mantis.soap.mantis120.IssueHeaderData header : issuesHeaders) {   
                if (header.getProject().intValue() == projectId)  //!!bug mantis api!! : filter on project id is not taken into account... so here it is
                {
                    if (header.getStatus().intValue() < 80)   
                    {
                        Utility.log(logger, Messages.tjd_monmsg("ERROR  The issue [" + header.getId().toString() + "] is neither resolved nor validated... Satus is lower than 80..." ));
                        build.setResult(Result.FAILURE);                
                    }
                    else
                    {
                        chnageSets.add(ChangeSetFactory.newInstance(header.getId().intValue()));            
                        issuesList =  issuesList + "," + header.getId().toString();  
                    }
                }
            }
            if (issuesList.length() > 1)
                issuesList = issuesList.substring(1);  //first comma removal



            //Release version on mantis
            BigInteger releasableVersion = site.checkProjectVersionReleasable(BigInteger.valueOf(projectId), projectVersion);
            if (releasableVersion != null)
            {
                MantisProjectVersion mpv = new MantisProjectVersion(BigInteger.valueOf(projectId), releasableVersion, 
                                            projectVersion, projectDescription + "\n" + Messages.MantisVersionRegister_VersionDescription(), true);
                site.updateProjectVersion2(mpv, logger);
            }
            else
            {
                Utility.log(logger, Messages.tjd_monmsg("ERROR  The version [" + projectVersion + "] is not releasable on Mantis..." ));
                build.setResult(Result.FAILURE);                
            }    

            //if some issues are neither resolved neither validated, or if the version is not releasable on mantis, we stop here
            if (build.getResult() == Result.FAILURE)
                return true;


            //Browse the issues list to update them, one by one (close+comment)
            for (final ChangeSet changeSet : chnageSets) {
                try {  
                    if (update) {
                        site.updateIssue(changeSet.getId(), projectVersion, property.isKeepNotePrivate(), 90, logger);
                        Utility.log(logger, Messages.Updater_Updating(changeSet.getId()));
                    }
                } catch (final MantisHandlingException e) {
                    Utility.log(logger, Messages.Updater_FailedToAddNote(changeSet, e.getMessage()));
                    LOGGER.log(Level.WARNING, Messages.Updater_FailedToAddNote_StarckTrace(changeSet), e);
                }
            } 
        }
        else
        {
            Utility.log(logger, Messages.tjd_monmsg("Mantis operations are skipped!!" ));
            return true;
        }
        
        //update changelog file
        if (this.property.isRecordChangelog())  
        {
            Utility.log(logger, Messages.tjd_monmsg("Updating changelog File..." ));        
            try {
                String log = "\r\n[Release] " + build.getProject().getName() + "-" + projectVersion + " \r\nFrom svn://foxserver/trunk/" + build.getProject().getName() + " rev " + build.getBuildVariables().get("Revision") + " \r\n";
                log = log + "Issue: " + issuesList;
                //Files.write(Paths.get(build.getParent().getBuildDir().toString() + "\\ChangeLog2.txt"), log.getBytes(), StandardOpenOption.APPEND);
                //Files.write(Paths.get(build.getParent().getWorkspace().toString() + "\\ChangeLog3.txt"), log.getBytes(), StandardOpenOption.APPEND);
                Files.write(Paths.get(build.getParent().getSomeWorkspace() + "\\ChangeLog.txt"), log.getBytes(), StandardOpenOption.APPEND);

            }catch (IOException e) {
                Utility.log(logger, Messages.tjd_monmsg(e.getMessage() ));
            }
        }
        else
        {
            Utility.log(logger, Messages.tjd_monmsg("Mantis operations are skipped!!" ));
            return true;
        }
        
        // build is not null, so mpp is not null
        //MantisProjectProperty mpp = MantisProjectProperty.get(build);
        //build.getActions().add(
        //        new MantisBuildAction(mpp.getRegexpPattern(), issues.toArray(new MantisIssue[0])));*/
        
        return true;
    }

    private String createUpdateText(final AbstractBuild<?, ?> build, final ChangeSet changeSet, final String rootUrl) {
        final String prjName = build.getProject().getName();
        final int prjNumber = build.getNumber();
        final String url = rootUrl + build.getUrl();

        final StringBuilder text = new StringBuilder();
        text.append(Messages.Updater_IssueIntegrated(prjName, prjNumber, url));
        text.append(CRLF).append(CRLF);
        
        if (property.isRecordChangelog()) {
            text.append(changeSet.createChangeLog());
        }
        return text.toString();
    }

    private List<ChangeSet> findChangeSets(final AbstractBuild<?, ?> build) {
        final List<ChangeSet> chnageSets = new ArrayList<ChangeSet>();

        final Run<?, ?> prev = build.getPreviousBuild();
        if (prev != null) {
            final MantisCarryOverChangeSetAction changeSetAction = prev.getAction(MantisCarryOverChangeSetAction.class);
            if (changeSetAction != null) {
                for (final ChangeSet changeSet : changeSetAction.getChangeSets()) {
                    chnageSets.add(changeSet);
                }
            }
        }

        chnageSets.addAll(findChangeSetsFromSCM(build));

        return chnageSets;
    }

    private List<ChangeSet> findChangeSetsFromSCM(final AbstractBuild<?, ?> build) {
        final List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
        
        MantisProjectProperty mpp = MantisProjectProperty.get(build);
        final Pattern pattern = mpp.getRegexpPattern();
        for (final Entry change : build.getChangeSet()) {
            final Matcher matcher = pattern.matcher(change.getMsg());
            while (matcher.find()) {
                int id;
                try {
                    id = Integer.parseInt(matcher.group(1));
                } catch (final NumberFormatException e) {
                    // if id is not number, skip
                    LOGGER.log(Level.WARNING, Messages.Updater_IllegalMantisId(matcher.group(1)));
                    continue;
                }
                changeSets.add(ChangeSetFactory.newInstance(id, build, change));
            }
            
            
        }
        
        
        
        return changeSets;
    }
    
    private static final Logger LOGGER = Logger.getLogger(Updater.class.getName());
}
