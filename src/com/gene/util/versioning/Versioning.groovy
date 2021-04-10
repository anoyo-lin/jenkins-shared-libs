package com.gene.util.versioning

import com.cloudbees.groovy.cps.NonCPS

class Versioning {
    @NonCPS
    public static String getReleaseVersion(String versionString) {
        // If it's a snapshot version, we will only keep what comes before the "-"
        String[] versionBits = versionString.tokenize('-')
        return versionBits[0]
    }
    @NonCPS
    public static String getNextMinorVersion(String versionString) {
        boolean isSnapShotVersion = versionString.contains('-')
        String[] versionBits = versionString.tokenize('-')

        String snapshotVersionName
        if(isSnapShotVersion) {
            snapshotVersionName = '-' + versionBits[2].tokenize('-')[1]
        }

        int minor = Integer.parseInt(versionBits[1])
        minor++
        versionBits[1] = minor 
        versionBits[2] = 0

        // Build new version string
        for(int i=0; i < versionBits.size(); i++) {
            if (i>0) {
                newVersion += '.'
            }
            newVersion += versionBits[i]
        }

        if (isSnapShotVersion) {
            // if snapshot version, then versionBits[2] = <patch version>-SNAPSHOT
            String[] patchVersionBits = versionBits[2].tokenize('-')
            patch = Integer.parseInt(patchVersionBits[0])
            snapshotVersionName = '-' + patchVersionBits[1]
        } else {
            patch = Integer.parseInt(versionBits[2])
        }
        patch++
        versionBits[2]=patch

        // Build new Version string
        String newVersion = ''
        for(int i = 0; i < versionBits.size(); i++) {
            if(i>0) {
                newVersion += '.'
            }
            newVersion += versionBits[i]
        }
        if (isSnapShotVersion) {
            newVersion += snapshotVersionName
        }
        return newVersion
    }
}