#!/bin/bash
echo "Checking for JDK 1.8"
java -version 2>&1 | grep 1\.8\. || { echo "Wrong java version, exiting"; exit 2; }

branch=$(git symbolic-ref --short HEAD)

if [ $branch == "master" ]; then
    echo "Do not release from master branch"
    exit 1
fi

if [ $branch != "develop" ]; then
    echo "you should only run publish from develop branch? Continue? <enter> | CTRL-C"
    read
fi


mvn clean release:clean release:prepare
version=$(grep "project.rel.de.caluga\\\\\\:morphium" release.properties | cut -f2 -d=)
tag=$(grep "scm.tag" release.properties | cut -f2 -d=)

echo "Releasing $version - tagging as $tag"
mvn release:perform -Dgpg.passphrase='$GPG_PASSPHRASE'

git checkout master
git merge $tag
git push
git checkout develop

#mvn -DskipTests package
#mvn deploy:deploy-file -Dfile=target/morphium-$version.jar -Durl=http://repository.dev.genios.de/repository/genios-snapshot -DpomFile=pom.xml

echo curl -X POST -H 'Content-type: application/json' --data "{'text':'Deployed current $version to integration test server'}" $(<slackurl.inc)
