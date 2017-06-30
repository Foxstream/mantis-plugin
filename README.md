# mantis-plugin
Jenkins mantis plugin

This plugin is to be used to release versions.
The versions in mantis has to be as following : x.x or x.x.x
The jenkins job has to have the following parameters : majeure, mineure, and maintenance if 3 digits are used
These parameters define the version (x.x.x) as majeure.mineure.maintenance

All matching issues (project and version are the only criterias) in mantis must be resolved or validated (statuses 80 or 85), and the
version for the selected project has to be releasable (or an error occurs and the job is stopped)

In order to trigger the mantis operations, add the after build action "Release version on Mantis" and check the first boxe. If you want to update the changelog file with mantis tickets ID and version released, check the second boxe.
