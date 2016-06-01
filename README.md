# mantis-plugin
Jenkins mantis plugin

This plugin is to be used to release versions.
The versions in mantis has to be as following : x.x.x
The jenkins job has to have the following parameters : majeure, mineure,
maintenance
These parameters define the version (x.x.x) as majeur.mineur.maintenance

All matching issues in mantis must be resolved or validated, and the
version for the selected project has to be releasable (or an error
occurs and the job is stoped)

In order to trigger the mantis operations, add the after build action
"Release version on Mantis" and check the 2 boxes.
