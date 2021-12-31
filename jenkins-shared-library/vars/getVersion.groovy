#!/usr/bin/env groovy
def call(version,password) {
    sh 'wget https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem'
    sh 'mongo --quiet --ssl nf-ami.cluster-cskaxjyiyvzs.us-east-1.docdb.amazonaws.com:27017 --sslCAFile rds-combined-ca-bundle.pem --username nfamibuilder --password ' +password+ ' --eval \'db.releases.find({"_id":{$regex:"\''+version+'\'"}}).toArray()\''
}