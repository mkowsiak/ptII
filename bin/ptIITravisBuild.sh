#!/bin/bash

# This script is called by $PTII/.travis.yml as part of the Travis-ci
# build at https://travis-ci.org/icyphy/ptII/

# This script depends on a number of environment variables being passed to it.

# The script invokes various builds depending on various environment variables.


# To test, set the environment variable:
#   PT_TRAVIS_P=true GITHUB_TOKEN=fixme sh -x $PTII/bin/ptIITravisBuild.sh
#   PT_TRAVIS_DOCS=true $PTII/bin/ptIITravisBuild.sh
#   PT_TRAVIS_GITHUB_ISSUE_JUNIT=true $PTII/bin/ptIITravisBuild.sh
#   PT_TRAVIS_PRIME_INSTALLER=true $PTII/bin/ptIITravisBuild.sh
#   PT_TRAVIS_TEST_CAPECODE_XML=true $PTII/bin/ptIITravisBuild.sh
#   PT_TRAVIS_TEST_INSTALLERS=true $PTII/bin/ptIITravisBuild.sh
#   PT_TRAVIS_TEST_REPORT_SHORT=true $PTII/bin/ptIITravisBuild.sh


# Because we are using if statements, we use a script, see
# https://groups.google.com/forum/#!topic/travis-ci/uaAP9zEdiCg
# http://steven.casagrande.io/articles/travis-ci-and-if-statements/

if [ ! -d $PTII/logs ]; then
    mkdir $PTII/logs
fi    

if [ ! -d $PTII/reports/junit ]; then
    mkdir -p $PTII/reports/junit
fi    

# If the output is more than 10k lines, then Travis fails, so we
# redirect voluminuous output into a log file.

# Number of lines to show from the log file.
lastLines=50

# Copy the file or directory named by
# source-file-or-directory to directory-in-gh-pages.  For example
#   updateGhPages logs/installers.txt logs
# will copy logs/installers.txt to logs in the gh-pages and push it.
# If the last argument ends in a /, then a directory by that name is created.
# The reason we need this is because the Travis deploy to gh-pages seems
# to overwrite everything in the repo.
# Usage:
#   updateGhPages [-junitreport] fileOrDirectory1 [fileOrDirectory2 ...] destinationDirectory
#   -junitreport is optional, and if present, then "ant junitreport" is run.
#
updateGhPages () {
    if [ $1 = "-junitreport" ]; then
        length=$(($#-2))
        sources=${@:2:$length}
        destination=${@: -1}
    else
        length=$(($#-1))
        sources=${@:1:$length}
        destination=${@: -1}
    fi

    echo "updateGhPages() start: length: $length, sources: $sources, destination: $destination `date`";

    if [ -z "$GITHUB_TOKEN" ]; then
        echo "$0: GITHUB_TOKEN was not set, so $sources will not be copied to $destination in the gh-pages repo."
        return 
    fi

    df -k .
    TMP=/tmp/ptIITravisBuild_gh_pages.$$
    if [ ! -d $TMP ]; then
        mkdir $TMP
    fi
    lastwd=`pwd`
    cd $TMP

    # Get to the Travis build directory, configure git and clone the repo
    git config --global user.email "travis@travis-ci.org"
    git config --global user.name "travis-ci"

    # Don't echo GITHUB_TOKEN
    set +x
    git clone --depth=1 --single-branch --branch=gh-pages https://${GITHUB_TOKEN}@github.com/icyphy/ptII gh-pages
    set -x

    df -k .
    # Commit and Push the Changes
    cd gh-pages
    echo "$destination" | grep '.*/$'
    status=$?
    if [ $status -eq 0 ]; then
        if [ ! -d $destination ]; then
            mkdir -p $destination
            echo "$0: Created $destination in [pwd]."
        fi
    fi        


    # Delete report/junit/**/*.xml files that have a timestamp field older than one day.
    #
    # Using find on the file mod time does not work here.  It could be
    # Git not preserving the mod time, or it could be us removing the
    # GITHUB_TOKEN.  So, we look for the timestamp field.
    set -x
    filesToBeDeleted=""
    files=`find reports/junit -name "*.xml"`
    for file in $files
    do
        grep timestamp $file >& /dev/null
        status=$?
        today=`date "+%Y-%m-%d"`
        if [ $status -eq 0 ]; then
            # echo $file
            # Get the timestamp, convert it roughly to a day number, compare against today's day number
            # and return 1 if the timestamp is more than one day old.  Leap years are ignored.
            timestamp=`head -2 $file | grep timestamp | awk -F \" '{print $(NF-1)}' | awk -F T '{print $1}'`
            older=`echo $timestamp |
                        awk -v today=$today ' 
                        BEGIN {
                            mo["01"]=0; mo["02"]=31; mo["03"]=59; mo["04"]=90 
                            mo["05"]=120; mo["06"]=151; mo["07"]=181; mo["08"]=212
                            mo["Sep"]=243; mo["Oct"]=273; mo["Nov"]=304; mo["Dec"]=334
                        } 
                        {  
                            split(today, todayYMD, "-")
                            todayDoy = todayYMD[1] * 365 + mo[todayYMD[2]] + todayYMD[3]
                            split($1, doyYMD, "-") 
                            doy = doyYMD[1] *365 + mo[doyYMD[2]] + doyYMD[3]
                            olderThanOneDay = (todayDoy - doy) > 1
                            print olderThanOneDay
                        }' -`
             if [ $older -eq 1 ]; then
                 echo "$file: $timestamp is older than one day"
                 filesToBeDeleted="$filesToBeDeleted $file"
             fi
        fi
    done
    set +x

    if [ ! -z "$filesToBeDeleted" ]; then
        git rm -f $filesToBeDeleted
        git commit -m "Removed any .xml files in reports/junit that have a timestamp older than one day." $filesToBeDeleted
    fi


    cp -Rf $sources $destination

    if [ $1 = "-junitreport" ]; then
        ant junitreport
    fi

    # JUnit xml output will include the values of the environment,
    # which can include GITHUB_TOKEN, which is supposed to be secret.
    # So, we remove any lines checked in to gh-pages that mentions
    # GITHUB_TOKEN.
    echo "Remove any instances of GITHUB_TOKEN: "
    date
    # Don't echo GITHUB_TOKEN
    set +x
    files=`find . -type f`
    for file in $files
    do
        egrep -e  "GITHUB_TOKEN" $file > /dev/null
	retval=$?
	if [ $retval != 1 ]; then
            echo -n "$file "
            egrep -v "GITHUB_TOKEN" $file > $file.tmp
            mv $file.tmp $file
        fi
    done        
    echo "Done."
    set -x

    git add -f .
    date
    git status
    # git pull
    date
    git commit -m "Lastest successful travis build $TRAVIS_BUILD_NUMBER auto-pushed $1 to $2 in gh-pages."
    git pull
    git push origin gh-pages
    git push -f origin gh-pages

    cd $lastwd
    rm -rf $TMP
}

# Timeout a process.
function timeout() { perl -e 'alarm shift; exec @ARGV' "$@"; }

# Below here, the if statements should be in alphabetical order
# according to variable name.

# This build produces less than 10K lines, so we don't save the
# output to a log file.
if [ ! -z "$PT_TRAVIS_BUILD_ALL" ]; then
    ant build-all;
fi

if [ ! -z "$PT_TRAVIS_DOCS" ]; then \
    LOG=$PTII/logs/docs.txt
    # Create the Javadoc jar files for use by the installer and deploy
    # them to Github pages.

    # Note that there is a chance that the installer will use javadoc
    # jar files that are slightly out of date.

    ant javadoc jsdoc 2>&1 | grep -v GITHUB_TOKEN > $LOG 
    (cd doc; make install) 2>&1 | grep -v GITHUB_TOKEN >> $LOG 

    # No need to check in the log each time because this target is
    # easy to re-run.
    # updateGhPages $PTII/doc/codeDoc $PTII/doc/*.jar doc/
fi

# This build produces less than 10K lines, so we don't save the
# output to a log file.
if [ ! -z "$PT_TRAVIS_GITHUB_ISSUE_JUNIT" ]; then
    mkdir node_modules
    npm install @icyphy/github-issue-junit
    export JUNIT_LABEL=junit-results
    export JUNIT_RESULTS_NOT_DRY_RUN=false
    export GITHUB_ISSUE_JUNIT=https://api.github.com/repos/icyphy/ptII
    (cd node_modules/@icyphy/github-issue-junit/scripts; node junit-results.js) 
fi

# Use this for testing, it quickly runs "ant -p" and then updated the gh-pages repo.
if [ ! -z "$PT_TRAVIS_P" ]; then
    LOG=$PTII/logs/ant_p.txt
    echo "$0: Output will appear in $LOG"
    ant -p 2>&1 | grep -v GITHUB_TOKEN > $LOG 

    echo "$0: Start of last $lastLines lines of $LOG"
    tail -$lastLines $LOG
    updateGhPages $LOG logs/
fi

# Prime the cache in $PTII/vendors/installer so that the installers
# target is faster.
#
# This target is not regularly run, but remains if we have issues
# getting the build working with an empty cache
if [ ! -z "$PT_TRAVIS_PRIME_INSTALLER" ]; then
    make -C $PTII/adm/gen-11.0 USER=travis PTIIHOME=$PTII COMPRESS=gzip prime_installer
    ls $PTII/vendors/installer
fi

# Run the CapeCode tests.
if [ ! -z "$PT_TRAVIS_TEST_CAPECODE_XML" ]; then
    # Keep the log file in reports/junit so that we only need to
    # invoke updateGhPages once per target.
    LOG=$PTII/reports/junit/test.capecode.xml.txt
    echo "$0: Output will appear in $LOG"
    
    ifconfig -a
    
    timeout 2400 ant build test.capecode.xml 2>&1 | grep -v GITHUB_TOKEN > $LOG 

    echo "$0: Start of last $lastLines lines of $LOG"
    tail -$lastLines $LOG
    updateGhPages -junitreport $PTII/reports/junit reports/

    # test.capecode.xml runs the longest, so at the end, update the issue with the results.
    mkdir node_modules
    npm install @icyphy/github-issue-junit
    export JUNIT_LABEL=junit-results
    export JUNIT_RESULTS_NOT_DRY_RUN=false
    export GITHUB_ISSUE_JUNIT=https://api.github.com/repos/icyphy/ptII
    (cd node_modules/@icyphy/github-issue-junit/scripts; node junit-results.js) 
fi

# Build the installers.
if [ ! -z "$PT_TRAVIS_TEST_INSTALLERS" ]; then
    LOG=$PTII/logs/installers.txt
    echo "$0: Output will appear in $LOG"
    
    # Copy any jar files that may have previously been created so that
    # the build is faster.
    jars="codeDoc.jar codeDocBcvtb.jar codeDocCapeCode.jar codeDocHyVisual.jar codeDocViptos.jar codeDocVisualSense.jar"
    for jar in $jars
    do
        echo "Downloading $jar: `date`"
        wget --quiet -O $PTII/doc/$jar https://github.com/icyphy/ptII/releases/download/nightly/$jar
        ls -l $PTII/doc/$jar
        (cd $PTII; jar -xf $PTII/doc/$jar)
    done

    # Number of seconds to run the subprocess.  Can't be more than 50
    # minutes or 3000 seconds.  45 minutes is cutting it a bit close,
    # so we go with a maximum of 35 minutes or 2100 seconds.  We can't
    # use Travis' timeout feature because we want to copy the output
    # to gh-pages. The timeouts should vary so as to avoid git
    # conflicts.

    # If the cache of OpenCV is failing to load, then comment out the
    # build here and the deploy section in .travis.yml so that this
    # target can run to completion.
    timeout 2100 ant test.installers 2>&1 | grep -v GITHUB_TOKEN > $LOG
 
    # Free up space for clone of gh-pages
    df -k .
    rm -rf $PTII/adm/dists
    ant clean

    echo "$0: Start of last $lastLines lines of $LOG"
    tail -$lastLines $LOG
    cp $LOG $PTII/reports/junit
    updateGhPages -junitreport $PTII/reports/junit reports/

    ls -l $PTII/adm/gen-11.0

    # We use Travis-ci deploy to upload the release because GitHub has
    # a 100Mb limit unless we use Git LFS.
fi


# Run the short tests.
if [ ! -z "$PT_TRAVIS_TEST_REPORT_SHORT" ]; then
    # Keep the log file in reports/junit so that we only need to
    # invoke updateGhPages once per target.
    LOG=$PTII/reports/junit/test.report.short.txt
    echo "$0: Output will appear in $LOG"

    # Copy just codeDoc.jar from the release.
    # doc/test/docManager.tcl needs this.
    jar=codeDoc.jar
    if [ ! -f $PTII/doc/codeDoc.jar ]; then
        echo "Downloading $jar: `date`"
        wget --quiet -O $PTII/doc/$jar https://github.com/icyphy/ptII/releases/download/nightly/$jar
        ls -l $PTII/doc/$jar
    fi
    echo "Unjarring $PTII/doc/$jar: `date`"
    (cd $PTII; jar -xf $PTII/doc/$jar)

    echo "Invoking ant: `date`"
    # The timeouts should vary so as to avoid git conflicts.
    # Use build-all so that we build in lbnl.
    timeout 1800 ant build-all test.report.short 2>&1 | grep -v GITHUB_TOKEN > $LOG 

    echo "$0: Start of last $lastLines lines of $LOG"
    tail -$lastLines $LOG
    updateGhPages -junitreport $PTII/reports/junit reports/
fi
