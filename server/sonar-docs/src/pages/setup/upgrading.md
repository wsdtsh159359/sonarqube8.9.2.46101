---
title: Upgrade the Server
url: /setup/upgrading/
---

## SonarQube Version Number Format
Before upgrading, it helps to understand how SonarQube version numbers work. Version numbers have up to three digits with each digit representing part of the release cycle:

![SonarQube version number format](/images/version.png)

**Major version number**  
The major version number represents a series of releases with high-level objectives for the release cycle. It's incremented with the release following an LTS version (for example, the release following 7.9 LTS was 8.0).

**Minor version number**  
The minor version number corresponds to incremental functional changes within a major release cycle. At the time of an LTS release, the release cycle is closed and the minor version number is frozen.

**Patch release number**  
Only on LTS versions, the patch release number represents patches to an LTS that fixed blocker or critical problems. The patch release number isn't considered in your upgrade migration path, and your migration path is the same no matter which patch number you are on.

## Migration Path
Upgrading across multiple non-LTS versions is handled automatically. However, if there's one or multiple LTS versions in your migration path, you must first migrate to each intermediate LTS and then to your target version, as shown in **Example 3** below.

[[info]]
|If you're migrating from an earlier patch version of an LTS, you can upgrade directly to the next LTS. You don't need to install any intermediate patch versions.

**Migration Path Examples**:

**Example 1** – From 7.1 > 8.1, the migration path is 7.1 > 7.9.6 LTS > 8.1  
**Example 2** – From 8.2 > 8.9 LTS, the migration path is 8.2 > the latest 8.9 LTS patch.  
**Example 3** – From 6.7.7 LTS > 8.9 LTS, the migration path is 6.7.7 LTS > 7.9.6 LTS > the latest 8.9 LTS patch.

## Upgrade Guide

This is a generic guide for upgrading across versions of SonarQube. Carefully read the [Release Upgrade Notes](/setup/upgrade-notes/) of your target version and of any intermediate version(s).

[[warning]]
| Before you start, back up your SonarQube Database. Upgrade problems are rare, but you'll want the backup if anything does happen.

### Database disk usage recommendations
During your upgrade, tables may be duplicated to speed up the migration process, and this could cause your database disk usage to double. Because of this, we recommend that your database disk usage is below 50% before starting a migration.

### Upgrading from the ZIP file

1. Download and unzip the SonarQube distribution of your edition in a fresh directory, let's say `$NEW_SONAR_HOME`
2. Manually install additional plugins that are compatible with your version of SonarQube. Use the [Compatibility Matrix](https://docs.sonarqube.org/display/PLUG/Plugin+Version+Matrix) to ensure that the versions you install are compatible with your server version. Simply copying plugins from the old server to the new is not recommended; incompatible or duplicate plugins could cause startup errors. Analysis of all languages provided by your edition is available by default without plugins.
3. Update the contents of `sonar.properties` and `wrapper.conf` files (in `$NEW_SONAR_HOME/conf`) with the settings of the related files in the `$OLD_SONAR_HOME/conf` directory (web server URL, database, ldap settings, etc.). Do not copy-paste the old files.
If you are using the Oracle DB, copy its JDBC driver into `$NEW_SONAR_HOME/extensions/jdbc-driver/oracle`
4. Stop your old SonarQube Server
5. Start your new SonarQube Server
6. Browse to `http://yourSonarQubeServerURL/setup` and follow the setup instructions
7. Reanalyze your projects to get fresh data

### Upgrading from the Docker image

[[info]]
| If you're upgrading with an Oracle database or you're using additional plugins, you can reuse your extensions volume from the previous version to avoid moving plugins or drivers. Use the [Compatibility Matrix](https://docs.sonarqube.org/display/PLUG/Plugin+Version+Matrix) to ensure that your plugins are compatible with your version. Analysis of all languages provided by your edition is available by default without plugins.

To upgrade SonarQube using the Docker image:

1. Stop and remove the existing SonarQube container (a restart from the UI is not enough as the environment variables are only evaluated during the first run, not during a restart):
    
	```console
	$ docker stop <container_id>
    $ docker rm <container_id>
	```

2. Run docker:

	```bash
	$> docker run -d --name sonarqube \
		-p 9000:9000 \
		-e SONAR_JDBC_URL=... \
		-e SONAR_JDBC_USERNAME=... \
		-e SONAR_JDBC_PASSWORD=... \
		-v sonarqube_data:/opt/sonarqube/data \
		-v sonarqube_extensions:/opt/sonarqube/extensions \
		-v sonarqube_logs:/opt/sonarqube/logs \
		<image_name>
	```

3. Go to `http://yourSonarQubeServerURL/setup` and follow the setup instructions.

4. Reanalyze your projects to get fresh data.

#### **From 8.9.x LTS to another 8.9.x LTS**

No specific Docker operations are needed, just use the new tag.

## Edition Upgrade
If you're moving to a different edition within the same version (upgrade or downgrade) the steps are exactly the same as above, without the need to browse to `http://yourSonarQubeServerURL/setup` or reanalyze your projects.

## Additional Information

### Oracle Clean-up

Starting with version 6.6, there's an additional step you may want to perform if you're using Oracle. On Oracle, the database columns to be dropped are now marked as UNUSED and are not physically dropped anymore. To reclaim disk space, Oracle administrators must drop these unused columns manually. The SQL request is `ALTER TABLE foo DROP UNUSED COLUMNS`. The relevant tables are listed in the system table `all_unused_col_tabs`.

### Additional Database Maintenance

We recommend refreshing your database's statistics and rebuilding your database's indices once you've finished the technical upgrade, but before you reanalyze your projects.

For PostgreSQL, that means executing `VACUUM FULL`. According to the PostgreSQL documentation:

```
In normal PostgreSQL operation, tuples that are deleted or obsoleted by an update are not physically removed from their table; they remain present until a VACUUM is done.
```

### Scanner Update

When upgrading SonarQube, you should also make sure you’re using the latest versions of the SonarQube scanners to take advantage of features and fixes on the scanner side. Please check the documentation pages of the Scanners you use for the most recent version compatible with SonarQube and your build tools.

### SonarQube as a Linux or Windows Service

If you use external configuration, such as a script or Windows Service to control your server, you'll need to update it to point to `$NEW_SONAR_HOME`.
- For Linux it depends how you implemented the service
- For Windows you can update your service by running:
```
sc config SonarQube binPath= "\"$NEW_SONAR_HOME\bin\windows-x86-64\wrapper.exe\" -s \"$NEW_SONAR_HOME\conf\wrapper.conf\""
```

### Rebuilding Indexes
If your upgrade requires the rebuild of Elasticsearch indexes, your projects and Applications will become available as they are reindexed. Portfolios won't be available until all projects are indexed.

## Release Upgrade Notes

Usually SonarQube releases come with some specific recommendations for upgrading from the previous version. You should read the upgrade notes for each version between your current version and the target version.
